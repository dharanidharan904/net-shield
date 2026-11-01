package com.netshield.ui;

import com.netshield.diagnostics.NetworkDiagnosticService;
import com.netshield.diagnosis.DiagnosisEngine;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.RecoveryAction;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import com.netshield.models.UserSettings;
import com.netshield.util.PlatformUtils;
import com.netshield.util.SafeCommandRunner;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Interactive One-Click Safe Recovery Wizard with before-and-after verification diff.
 */
public class RecoveryWizardDialog extends Stage {

    private final DiagnosticReport originalReport;
    private final DiagnosisOutcome originalOutcome;
    private final NetworkDiagnosticService diagnosticService;
    private final DiagnosisEngine diagnosisEngine;
    private final UserSettings settings;
    private final Runnable onResolvedCallback;

    private VBox stepContainer;
    private Button actionButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    private VBox diffBox;

    public RecoveryWizardDialog(Stage owner, DiagnosticReport report, DiagnosisOutcome outcome,
                                NetworkDiagnosticService service, DiagnosisEngine engine,
                                UserSettings settings, Runnable onResolvedCallback) {
        this.originalReport = report;
        this.originalOutcome = outcome;
        this.diagnosticService = service;
        this.diagnosisEngine = engine;
        this.settings = settings;
        this.onResolvedCallback = onResolvedCallback;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("NetShield — Guided Safe Recovery Wizard");
        setWidth(640);
        setHeight(560);
        setResizable(false);

        buildUi();
    }

    private void buildUi() {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0b0f19;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🧙‍♂️");
        icon.setStyle("-fx-font-size: 24px;");

        VBox titleBox = new VBox(2);
        Label title = new Label("One-Click Safe Recovery Wizard");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");
        Label sub = new Label("Non-destructive automated remediation with before/after verification");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, sub);

        header.getChildren().addAll(icon, titleBox);

        // Problem Card
        VBox problemCard = new VBox(6);
        problemCard.setStyle("-fx-background-color: #131d2e; -fx-padding: 12px; -fx-background-radius: 8px; -fx-border-color: #1e293b; -fx-border-radius: 8px;");

        Label probTitle = new Label("Current Detected Issue: " + originalOutcome.getTitle());
        probTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: #ef4444; -fx-font-size: 13px;");

        Label probDesc = new Label(originalOutcome.getSummary());
        probDesc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        probDesc.setWrapText(true);

        problemCard.getChildren().addAll(probTitle, probDesc);

        // Proposed Fix Step
        RecoveryAction primaryAction = originalOutcome.getRecoveryActions().isEmpty()
                ? RecoveryAction.manual(1, "Flush DNS and Reset Resolvers", "Flushes stale cache records", "Clean local resolver state")
                : originalOutcome.getRecoveryActions().get(0);

        VBox fixCard = new VBox(6);
        fixCard.setStyle("-fx-background-color: #111827; -fx-padding: 12px; -fx-background-radius: 8px; -fx-border-color: #0284c7; -fx-border-radius: 8px;");

        Label fixHeader = new Label("Automated Safe Step: " + primaryAction.getTitle());
        fixHeader.setStyle("-fx-font-weight: 700; -fx-text-fill: #38bdf8; -fx-font-size: 13px;");

        Label fixDesc = new Label(primaryAction.getDescription());
        fixDesc.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        fixDesc.setWrapText(true);

        fixCard.getChildren().addAll(fixHeader, fixDesc);

        // Progress & Status
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        statusLabel = new Label("Ready to apply safe remediation.");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        // Diff Container
        diffBox = new VBox(6);
        diffBox.setStyle("-fx-background-color: #0d1322; -fx-padding: 10px; -fx-background-radius: 6px;");
        diffBox.setVisible(false);

        // Footer Actions
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button("Close");
        cancelButton.getStyleClass().add("btn-secondary");
        cancelButton.setOnAction(e -> close());

        actionButton = new Button("Apply Safe Fix & Auto-Verify");
        actionButton.getStyleClass().add("btn-primary");
        actionButton.setOnAction(e -> executeSafeFixAndVerify(primaryAction));

        footer.getChildren().addAll(cancelButton, actionButton);

        root.getChildren().addAll(
                header,
                new Separator(),
                problemCard,
                fixCard,
                statusLabel,
                progressBar,
                diffBox,
                new Separator(),
                footer
        );

        Scene scene = new Scene(root);
        String css = getClass().getResource("/com/netshield/css/netshield.css") != null
                ? getClass().getResource("/com/netshield/css/netshield.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);
        setScene(scene);
    }

    private void executeSafeFixAndVerify(RecoveryAction action) {
        actionButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate
        statusLabel.setText("Executing safe diagnostic reset...");

        Task<DiagnosticReport> task = new Task<>() {
            @Override
            protected DiagnosticReport call() throws Exception {
                // 1. Execute safe remediation (e.g. flush DNS cache)
                PlatformUtils.flushDnsCache();
                Thread.sleep(800); // Allow OS resolver cache to settle

                // 2. Re-run complete diagnostics for before-and-after comparison
                return diagnosticService.runFullDiagnostics(settings, null);
            }
        };

        task.setOnSucceeded(e -> {
            DiagnosticReport newReport = task.getValue();
            DiagnosisOutcome newOutcome = diagnosisEngine.diagnose(newReport);
            renderDiff(newReport, newOutcome);

            progressBar.setProgress(1.0);
            actionButton.setDisable(false);
            actionButton.setText("Run Another Fix");

            if (onResolvedCallback != null) {
                onResolvedCallback.run();
            }
        });

        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            statusLabel.setText("Remediation verification failed: " + task.getException().getMessage());
            actionButton.setDisable(false);
        });

        Thread th = new Thread(task, "NetShield-Recovery-Wizard");
        th.setDaemon(true);
        th.start();
    }

    private void renderDiff(DiagnosticReport newReport, DiagnosisOutcome newOutcome) {
        diffBox.getChildren().clear();
        diffBox.setVisible(true);

        Label diffTitle = new Label("Before vs After Diagnostic Verification:");
        diffTitle.setStyle("-fx-font-weight: 800; -fx-text-fill: #f8fafc;");

        // Before State
        HBox beforeRow = new HBox(8);
        beforeRow.setAlignment(Pos.CENTER_LEFT);
        Label bLabel = new Label("BEFORE:");
        bLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #ef4444;");
        Label bVal = new Label(originalOutcome.getTitle() + " (" + originalOutcome.getSeverity() + ")");
        bVal.setStyle("-fx-text-fill: #cbd5e1;");
        beforeRow.getChildren().addAll(bLabel, bVal);

        // After State
        HBox afterRow = new HBox(8);
        afterRow.setAlignment(Pos.CENTER_LEFT);
        Label aLabel = new Label("AFTER :");
        aLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #10b981;");
        Label aVal = new Label(newOutcome.getTitle() + " (" + newOutcome.getSeverity() + ")");
        aVal.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: 700;");
        afterRow.getChildren().addAll(aLabel, aVal);

        // Verdict
        boolean improved = newOutcome.getSeverity().ordinal() < originalOutcome.getSeverity().ordinal();
        Label verdict = new Label(improved
                ? "🎉 Remediation Successful! Network connection improved."
                : "⚠️ State unchanged. Physical router reboot may be required.");
        verdict.setStyle("-fx-font-weight: 800; -fx-text-fill: " + (improved ? "#10b981" : "#f59e0b") + "; -fx-padding: 6 0 0 0;");

        diffBox.getChildren().addAll(diffTitle, beforeRow, afterRow, verdict);
        statusLabel.setText("Verification probe finished.");
    }
}
