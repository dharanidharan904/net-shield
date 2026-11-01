package com.netshield.ui;

import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Reusable visual component representing an individual diagnostic stage card.
 */
public class StageCardComponent extends VBox {

    private final String stageName;
    private final Label stageTitleLabel;
    private final Label statusBadge;
    private final Label durationBadge;
    private final Label summaryLabel;
    private final VBox detailsBox;

    public StageCardComponent(String icon, String stageName, String description) {
        this.stageName = stageName;

        getStyleClass().add("stage-card");
        setSpacing(8);
        setPadding(new Insets(12));
        setPrefWidth(320);

        // Header: Icon + Title + Status Pill
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        stageTitleLabel = new Label(stageName);
        stageTitleLabel.getStyleClass().add("stage-title");
        HBox.setHgrow(stageTitleLabel, Priority.ALWAYS);

        statusBadge = new Label("Idle");
        statusBadge.getStyleClass().addAll("pill-badge", "badge-info");

        durationBadge = new Label("--");
        durationBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: 600;");

        header.getChildren().addAll(iconLabel, stageTitleLabel, durationBadge, statusBadge);

        // Summary message
        summaryLabel = new Label(description);
        summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        summaryLabel.setWrapText(true);

        // Details container
        detailsBox = new VBox(4);
        detailsBox.setStyle("-fx-padding: 4 0 0 0;");

        getChildren().addAll(header, summaryLabel, detailsBox);
    }

    public void setRunning() {
        statusBadge.setText("Testing...");
        statusBadge.getStyleClass().removeAll("badge-success", "badge-failed", "badge-warning", "badge-info");
        statusBadge.getStyleClass().add("badge-running");
        summaryLabel.setText("Executing diagnostic probes...");
        durationBadge.setText("...");
        detailsBox.getChildren().clear();
    }

    public void updateResult(StageResult result) {
        if (result == null) return;

        durationBadge.setText(result.getDurationMs() + " ms");
        summaryLabel.setText(result.getMessage());

        statusBadge.setText(result.getStatus().getLabel());
        statusBadge.getStyleClass().removeAll("badge-success", "badge-failed", "badge-warning", "badge-info", "badge-running");

        switch (result.getStatus()) {
            case SUCCESS -> statusBadge.getStyleClass().add("badge-success");
            case FAILED -> statusBadge.getStyleClass().add("badge-failed");
            case TIMED_OUT, INCONCLUSIVE -> statusBadge.getStyleClass().add("badge-warning");
            case RUNNING -> statusBadge.getStyleClass().add("badge-running");
            default -> statusBadge.getStyleClass().add("badge-info");
        }

        // Populate technical details
        detailsBox.getChildren().clear();
        int shown = 0;
        for (Map.Entry<String, Object> entry : result.getDetails().entrySet()) {
            if (shown >= 3) break; // Keep card clean and readable
            if (entry.getValue() != null && !(entry.getValue() instanceof java.util.Collection)) {
                Label detailLabel = new Label(formatKey(entry.getKey()) + ": " + entry.getValue());
                detailLabel.setStyle("-fx-font-size: 11px; -fx-font-family: monospace; -fx-text-fill: #60a5fa;");
                detailsBox.getChildren().add(detailLabel);
                shown++;
            }
        }
    }

    public void resetToIdle(String defaultDesc) {
        statusBadge.setText("Ready");
        statusBadge.getStyleClass().removeAll("badge-success", "badge-failed", "badge-warning", "badge-running");
        statusBadge.getStyleClass().add("badge-info");
        durationBadge.setText("--");
        summaryLabel.setText(defaultDesc);
        detailsBox.getChildren().clear();
    }

    private String formatKey(String key) {
        // e.g. "primaryIpv4" -> "Primary IPv4"
        return key.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace("Ms", " (ms)")
                .replace("Ipv4", "IPv4");
    }

    public String getStageName() {
        return stageName;
    }
}
