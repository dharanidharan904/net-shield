package com.netshield.ui;

import com.netshield.diagnostics.NetworkDiagnosticService;
import com.netshield.diagnostics.RealTracerouteDiagnostic;
import com.netshield.diagnostics.RealWifiDiagnostic;
import com.netshield.diagnostics.SpeedTestService;
import com.netshield.diagnostics.TracerouteDiagnostic;
import com.netshield.diagnostics.WifiDiagnostic;
import com.netshield.diagnosis.DiagnosisEngine;
import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.HistoricalSession;
import com.netshield.models.RecoveryAction;
import com.netshield.models.RouteHop;
import com.netshield.models.SpeedTestResult;
import com.netshield.models.StageResult;
import com.netshield.models.TracerouteReport;
import com.netshield.models.TroubleshootingArticle;
import com.netshield.models.UserSettings;
import com.netshield.models.WifiNetworkInfo;
import com.netshield.persistence.DatabaseManager;
import com.netshield.persistence.HistoryDao;
import com.netshield.persistence.SettingsDao;
import com.netshield.util.HtmlReportGenerator;
import com.netshield.util.KnowledgeBaseRepository;
import com.netshield.util.MobileCompanionServer;
import com.netshield.util.NotificationManager;
import com.netshield.util.PlatformUtils;
import com.netshield.util.ReportExporter;
import com.netshield.util.SafeCommandRunner;
import com.netshield.util.SoundNotification;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main Controller that orchestrates the NetShield user interface, background diagnostics,
 * rule evaluation, visual traceroute, Wi-Fi analyzer, speed test, automated device notifications,
 * history charts, and settings management.
 */
public class MainDashboardController {

    private final Stage primaryStage;
    private final NetworkDiagnosticService diagnosticService;
    private final DiagnosisEngine diagnosisEngine;
    private final DatabaseManager dbManager;
    private final HistoryDao historyDao;
    private final SettingsDao settingsDao;
    private final KnowledgeBaseRepository knowledgeBase;

    // Enhanced Diagnostic Modules
    private final WifiDiagnostic wifiDiagnostic;
    private final TracerouteDiagnostic tracerouteDiagnostic;
    private final SpeedTestService speedTestService;
    private final NotificationManager notificationManager;

    private UserSettings currentSettings;
    private Task<DiagnosticReport> currentDiagnosticTask;
    private ScheduledExecutorService monitorScheduler;

    private DiagnosticReport lastReport;
    private DiagnosisOutcome lastOutcome;
    private WifiNetworkInfo lastWifiInfo;
    private MobileCompanionServer mobileServer;

    // UI Controls: Navbar
    private Label statusBadgeTop;
    private Button runDiagnosticsButton;
    private Button cancelDiagnosticsButton;
    private ToggleButton continuousMonitorToggle;
    private ToggleButton autoHealToggle;
    private Button wizardButtonTop;
    private boolean isAutoHealingInProgress = false;

    // UI Controls: Live Dashboard Hero
    private VBox heroBanner;
    private Label heroStatusLabel;
    private Label heroTitleLabel;
    private Label heroSummaryLabel;
    private Label heroConfidenceBadge;

    private ProgressBar progressBar;
    private Label progressLabel;

    // Stage Cards
    private StageCardComponent interfaceCard;
    private StageCardComponent gatewayCard;
    private StageCardComponent dnsCard;
    private StageCardComponent httpCard;
    private StageCardComponent latencyCard;
    private StageCardComponent captiveCard;

    // Evidence & Recovery Boxes
    private VBox evidenceContainer;
    private VBox recoveryContainer;
    private TextArea consoleLogArea;

    // History Tab Controls
    private LineChart<String, Number> latencyChart;
    private XYChart.Series<String, Number> gatewaySeries;
    private XYChart.Series<String, Number> dnsSeries;
    private XYChart.Series<String, Number> httpSeries;

    private Label statTotalScansLabel;
    private Label statUptimeLabel;
    private Label statAvgLatencyLabel;
    private Label statTotalOutagesLabel;
    private TableView<HistoricalSession> historyTableView;
    private ObservableList<HistoricalSession> historyDataList;

    // Status Bar
    private Label statusBarText;
    private Label lastScanTimestampLabel;

    public MainDashboardController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.dbManager = new DatabaseManager();
        this.historyDao = new HistoryDao(this.dbManager);
        this.settingsDao = new SettingsDao(this.dbManager);
        this.currentSettings = this.settingsDao.loadSettings();
        this.diagnosticService = new NetworkDiagnosticService();
        this.diagnosisEngine = new DiagnosisEngine();
        this.knowledgeBase = new KnowledgeBaseRepository();

        this.wifiDiagnostic = new RealWifiDiagnostic();
        this.tracerouteDiagnostic = new RealTracerouteDiagnostic();
        this.speedTestService = new SpeedTestService();

        // Initialize Notification Manager with tray integration
        this.notificationManager = new NotificationManager(
                () -> Platform.runLater(() -> {
                    if (primaryStage != null) {
                        primaryStage.show();
                        primaryStage.toFront();
                    }
                }),
                () -> Platform.runLater(this::startDiagnostics)
        );

        // Initialize Mobile Companion Server for Android & Phone Checking
        this.mobileServer = new MobileCompanionServer(this.diagnosticService, this.diagnosisEngine, this.wifiDiagnostic, this.currentSettings);
        this.mobileServer.start();
        if (primaryStage != null) {
            primaryStage.setOnCloseRequest(e -> {
                if (mobileServer != null) mobileServer.stop();
            });
        }
    }

    public BorderPane buildMainLayout() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-container");

        root.setTop(buildTopNavbar());

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Live Diagnostics", buildDashboardTab());
        Tab tracerouteTab = new Tab("Route & Hop Map", buildTracerouteTab());
        Tab wifiTab = new Tab("Wi-Fi & Radio", buildWifiTab());
        Tab speedTab = new Tab("Speed & Bandwidth", buildSpeedTestTab());
        Tab historyTab = new Tab("History & Analytics", buildHistoryTab());
        Tab knowledgeTab = new Tab("Offline Knowledge Base", buildKnowledgeBaseTab());
        Tab settingsTab = new Tab("Configuration & Endpoints", buildSettingsTab());

        tabPane.getTabs().addAll(
                dashboardTab, tracerouteTab, wifiTab, speedTab, historyTab, knowledgeTab, settingsTab
        );
        root.setCenter(tabPane);

        root.setBottom(buildStatusBar());

        // Refresh stats on start
        refreshHistoryStats();

        return root;
    }

    // =========================================================================
    // 1. TOP NAVBAR
    // =========================================================================
    private HBox buildTopNavbar() {
        HBox navbar = new HBox(12);
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.getStyleClass().add("top-navbar");

        VBox brandBox = new VBox(2);
        Label brandTitle = new Label("NETSHIELD 2.0");
        brandTitle.getStyleClass().add("brand-title");
        Label brandSub = new Label("Offline-First Internet Failure Detection & Recovery Tool");
        brandSub.getStyleClass().add("brand-subtitle");
        brandBox.getChildren().addAll(brandTitle, brandSub);

        HBox.setHgrow(brandBox, Priority.ALWAYS);

        statusBadgeTop = new Label("System Ready");
        statusBadgeTop.getStyleClass().addAll("pill-badge", "badge-info");

        runDiagnosticsButton = new Button("Run Diagnostics");
        runDiagnosticsButton.getStyleClass().add("btn-primary");
        runDiagnosticsButton.setOnAction(e -> startDiagnostics());

        cancelDiagnosticsButton = new Button("Cancel");
        cancelDiagnosticsButton.getStyleClass().add("btn-danger");
        cancelDiagnosticsButton.setDisable(true);
        cancelDiagnosticsButton.setOnAction(e -> cancelDiagnostics());

        wizardButtonTop = new Button("🧙‍♂️ Safe Fix Wizard");
        wizardButtonTop.getStyleClass().add("btn-secondary");
        wizardButtonTop.setOnAction(e -> openRecoveryWizard());

        continuousMonitorToggle = new ToggleButton("Continuous Monitor");
        continuousMonitorToggle.getStyleClass().add("btn-secondary");
        continuousMonitorToggle.setOnAction(e -> toggleContinuousMonitor());

        autoHealToggle = new ToggleButton(currentSettings.isAutoHealingEnabled() ? "⚡ Auto-Heal: ACTIVE" : "⚡ Auto-Heal: OFF");
        autoHealToggle.getStyleClass().add("btn-secondary");
        if (currentSettings.isAutoHealingEnabled()) {
            autoHealToggle.setSelected(true);
            autoHealToggle.setStyle("-fx-border-color: #38bdf8; -fx-text-fill: #38bdf8; -fx-font-weight: 700;");
        }
        autoHealToggle.setOnAction(e -> toggleAutoHeal());

        Button exportHtmlBtn = new Button("🖨️ PDF/HTML Report");
        exportHtmlBtn.getStyleClass().add("btn-secondary");
        exportHtmlBtn.setOnAction(e -> exportHtmlReport());

        Button mobileBtn = new Button("📱 Android Mobile");
        mobileBtn.getStyleClass().add("btn-secondary");
        mobileBtn.setOnAction(e -> showMobileCompanionDialog());

        navbar.getChildren().addAll(
                brandBox,
                statusBadgeTop,
                runDiagnosticsButton,
                cancelDiagnosticsButton,
                wizardButtonTop,
                continuousMonitorToggle,
                autoHealToggle,
                exportHtmlBtn,
                mobileBtn
        );

        return navbar;
    }

    // =========================================================================
    // 2. LIVE DASHBOARD TAB
    // =========================================================================
    private Node buildDashboardTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(20));

        // Hero Summary Banner
        heroBanner = new VBox(8);
        heroBanner.getStyleClass().addAll("hero-banner", "hero-info");

        HBox heroHeader = new HBox(12);
        heroHeader.setAlignment(Pos.CENTER_LEFT);

        heroStatusLabel = new Label("NETWORK MONITOR IDLE");
        heroStatusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #93c5fd;");

        heroConfidenceBadge = new Label("Confidence: Ready");
        heroConfidenceBadge.getStyleClass().addAll("pill-badge", "badge-info");

        heroHeader.getChildren().addAll(heroStatusLabel, heroConfidenceBadge);

        heroTitleLabel = new Label("Ready to Run Internet Connectivity Diagnostics");
        heroTitleLabel.getStyleClass().add("hero-title");

        heroSummaryLabel = new Label("Click 'Run Diagnostics' above to inspect physical adapters, gateway routing, DNS resolution, HTTPS reachability, latency, and captive portal interception.");
        heroSummaryLabel.getStyleClass().add("hero-summary");
        heroSummaryLabel.setWrapText(true);

        heroBanner.getChildren().addAll(heroHeader, heroTitleLabel, heroSummaryLabel);

        // Progress indicators
        VBox progressBox = new VBox(4);
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);

        progressLabel = new Label("Diagnostic Pipeline: Idle");
        progressLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        progressBox.getChildren().addAll(progressBar, progressLabel);

        // 6 Stage Cards (Responsive FlowPane)
        FlowPane stagesPane = new FlowPane(12, 12);
        stagesPane.setAlignment(Pos.CENTER);

        interfaceCard = new StageCardComponent("🔌", "Network Interfaces", "Inspects physical adapters, link status, and IPv4 addresses.");
        gatewayCard = new StageCardComponent("🌐", "Default Gateway", "Discovers router gateway and tests ICMP/socket reachability.");
        dnsCard = new StageCardComponent("🧭", "DNS Resolution", "Tests system resolver and direct RFC 1035 UDP queries to 1.1.1.1.");
        httpCard = new StageCardComponent("🔒", "HTTPS Reachability", "Validates TCP handshakes, TLS negotiation, and HTTP status codes.");
        latencyCard = new StageCardComponent("⏱️", "Latency & Packet Loss", "Computes RTT latency, jitter variance, and packet loss rate.");
        captiveCard = new StageCardComponent("🛡️", "Captive Portal Detection", "Probes HTTP 204 endpoints to detect Wi-Fi splash redirection.");

        stagesPane.getChildren().addAll(
                interfaceCard, gatewayCard, dnsCard, httpCard, latencyCard, captiveCard
        );

        // Responsive width binding
        for (Node child : stagesPane.getChildren()) {
            if (child instanceof StageCardComponent scc) {
                scc.prefWidthProperty().bind(stagesPane.widthProperty().divide(3).subtract(16));
            }
        }

        // Split Section: Left = Evidence & Root Cause, Right = Safe Step-by-Step Recovery
        HBox diagnosisSection = new HBox(16);
        diagnosisSection.setAlignment(Pos.TOP_LEFT);

        // Left: Diagnostic Evidence
        VBox evidenceCard = new VBox(10);
        evidenceCard.getStyleClass().add("content-card");
        HBox.setHgrow(evidenceCard, Priority.ALWAYS);

        Label evidenceHeader = new Label("Diagnostic Evidence & Findings");
        evidenceHeader.getStyleClass().add("card-title");
        Label evidenceSub = new Label("Concrete technical observations gathered across each tested layer");
        evidenceSub.getStyleClass().add("card-subtitle");

        evidenceContainer = new VBox(8);
        evidenceContainer.getChildren().add(new Label("No diagnostics executed yet. Click 'Run Diagnostics' to begin."));

        evidenceCard.getChildren().addAll(evidenceHeader, evidenceSub, new Separator(), evidenceContainer);

        // Right: Safe Step-by-Step Recovery
        VBox recoveryCard = new VBox(10);
        recoveryCard.getStyleClass().add("content-card");
        HBox.setHgrow(recoveryCard, Priority.ALWAYS);

        Label recoveryHeader = new Label("Safe Step-by-Step Recovery Guidance");
        recoveryHeader.getStyleClass().add("card-title");
        Label recoverySub = new Label("Actionable, non-destructive steps tailored to the detected root cause");
        recoverySub.getStyleClass().add("card-subtitle");

        recoveryContainer = new VBox(8);
        recoveryContainer.getChildren().add(new Label("Recovery advice will appear here based on scan findings."));

        recoveryCard.getChildren().addAll(recoveryHeader, recoverySub, new Separator(), recoveryContainer);

        diagnosisSection.getChildren().addAll(evidenceCard, recoveryCard);

        // Live Diagnostic Console Log
        VBox consoleCard = new VBox(8);
        consoleCard.getStyleClass().add("content-card");

        Label consoleHeader = new Label("Live Diagnostic Event Console");
        consoleHeader.getStyleClass().add("card-title");

        consoleLogArea = new TextArea();
        consoleLogArea.getStyleClass().add("console-area");
        consoleLogArea.setEditable(false);
        consoleLogArea.setPrefRowCount(7);
        appendConsoleLog("NetShield 2.0 initialized. Privacy-first local storage and real-time state change monitoring active.");

        consoleCard.getChildren().addAll(consoleHeader, consoleLogArea);

        container.getChildren().addAll(heroBanner, progressBox, stagesPane, diagnosisSection, consoleCard);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0b0f19; -fx-background-color: transparent;");
        return scrollPane;
    }

    // =========================================================================
    // 3. VISUAL ROUTE & HOP MAP TAB (TRACEROUTE)
    // =========================================================================
    private Node buildTracerouteTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(20));

        VBox headerCard = new VBox(12);
        headerCard.getStyleClass().add("content-card");

        Label title = new Label("Hop-by-Hop Visual Route Trace");
        title.getStyleClass().add("card-title");
        Label sub = new Label("Traces intermediate network routers to pinpoint exactly where latency spikes or packet drops occur.");
        sub.getStyleClass().add("card-subtitle");

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        TextField targetField = new TextField("1.1.1.1");
        targetField.setPromptText("Target Host (e.g. 1.1.1.1, google.com)");
        targetField.setPrefWidth(220);

        TextField hopsField = new TextField("15");
        hopsField.setPromptText("Max Hops");
        hopsField.setPrefWidth(80);

        Button startTraceBtn = new Button("Trace Network Route");
        startTraceBtn.getStyleClass().add("btn-primary");

        ProgressBar traceProgress = new ProgressBar(0);
        traceProgress.setPrefWidth(160);
        traceProgress.setVisible(false);

        Label traceStatus = new Label("Ready to trace.");
        traceStatus.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        controls.getChildren().addAll(new Label("Target:"), targetField, new Label("Hops:"), hopsField, startTraceBtn, traceProgress, traceStatus);
        headerCard.getChildren().addAll(title, sub, new Separator(), controls);

        // Visual Hops Canvas Container
        VBox hopsContainer = new VBox(12);
        hopsContainer.getStyleClass().add("content-card");
        VBox.setVgrow(hopsContainer, Priority.ALWAYS);

        Label hopsTitle = new Label("Route Topology & Latency Map:");
        hopsTitle.getStyleClass().add("card-title");

        FlowPane hopsFlow = new FlowPane(12, 12);
        hopsFlow.setAlignment(Pos.CENTER_LEFT);
        hopsFlow.setStyle("-fx-padding: 10px;");

        Label emptyLabel = new Label("Click 'Trace Network Route' to map intermediate hops between this device and the destination.");
        emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
        hopsFlow.getChildren().add(emptyLabel);

        hopsContainer.getChildren().addAll(hopsTitle, hopsFlow);

        startTraceBtn.setOnAction(e -> {
            String target = targetField.getText().trim();
            int maxHops = 15;
            try { maxHops = Integer.parseInt(hopsField.getText().trim()); } catch (Exception ignored) {}

            startTraceBtn.setDisable(true);
            traceProgress.setVisible(true);
            traceProgress.setProgress(-1);
            traceStatus.setText("Tracing route to " + target + " (hop by hop)...");
            appendConsoleLog("Started traceroute to target: " + target);

            int finalHops = maxHops;
            Task<TracerouteReport> traceTask = new Task<>() {
                @Override
                protected TracerouteReport call() {
                    return tracerouteDiagnostic.traceRoute(target, finalHops, 1000);
                }
            };

            traceTask.setOnSucceeded(ev -> {
                TracerouteReport report = traceTask.getValue();
                traceProgress.setVisible(false);
                startTraceBtn.setDisable(false);
                traceStatus.setText(report.getSummary());
                renderVisualHops(hopsFlow, report);
                appendConsoleLog("Traceroute finished: " + report.getSummary());
            });

            traceTask.setOnFailed(ev -> {
                traceProgress.setVisible(false);
                startTraceBtn.setDisable(false);
                traceStatus.setText("Traceroute error: " + traceTask.getException().getMessage());
            });

            Thread th = new Thread(traceTask, "NetShield-Traceroute-Worker");
            th.setDaemon(true);
            th.start();
        });

        container.getChildren().addAll(headerCard, hopsContainer);

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }

    private void renderVisualHops(FlowPane flow, TracerouteReport report) {
        flow.getChildren().clear();

        // 1. Host Node
        VBox hostNode = createHopNodeCard(0, "This Device", "Localhost", "0.0 ms", false, false);
        flow.getChildren().add(hostNode);

        for (RouteHop hop : report.getHops()) {
            Label arrow = new Label("──▶");
            arrow.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            flow.getChildren().add(arrow);

            boolean isDest = hop.getHopNumber() == report.getHops().size() && report.isDestinationReached();
            VBox hopNode = createHopNodeCard(
                    hop.getHopNumber(),
                    isDest ? "Destination Target" : ("Hop #" + hop.getHopNumber()),
                    hop.getIpAddress(),
                    hop.getDisplayLatency(),
                    hop.isTimedOut(),
                    hop.isBottleneck()
            );
            flow.getChildren().add(hopNode);
        }

        if (report.getBottleneckHop() > 0) {
            Label bottleneckAlert = new Label(String.format("⚠️ Bottleneck detected at Hop #%d: Latency experienced a significant jump.", report.getBottleneckHop()));
            bottleneckAlert.setStyle("-fx-background-color: rgba(245, 158, 11, 0.2); -fx-text-fill: #fbbf24; -fx-padding: 8 12 8 12; -fx-background-radius: 6px; -fx-font-weight: 700;");
            flow.getChildren().add(new Separator());
            flow.getChildren().add(bottleneckAlert);
        }
    }

    private VBox createHopNodeCard(int hopNum, String title, String ip, String latency, boolean timedOut, boolean bottleneck) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        card.setPrefWidth(140);

        String border = timedOut ? "#ef4444" : (bottleneck ? "#f59e0b" : "#3b82f6");
        String bg = timedOut ? "#2d1212" : (bottleneck ? "#2e1a06" : "#131d2e");
        card.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1.5px;", bg, border));

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        Label ipLbl = new Label(ip);
        ipLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #f8fafc; -fx-font-family: monospace;");

        Label latLbl = new Label(latency);
        latLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (timedOut ? "#ef4444" : (bottleneck ? "#f59e0b" : "#10b981")) + ";");

        card.getChildren().addAll(t, ipLbl, latLbl);
        return card;
    }

    // =========================================================================
    // 4. WI-FI & RADIO ANALYZER TAB
    // =========================================================================
    private Node buildWifiTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(20));

        HBox topBox = new HBox(12);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Wi-Fi Signal & Radio Telemetry");
        title.getStyleClass().add("card-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh Wi-Fi Stats");
        refreshBtn.getStyleClass().add("btn-primary");

        topBox.getChildren().addAll(title, refreshBtn);

        // Main Wi-Fi Panel
        VBox wifiCard = new VBox(16);
        wifiCard.getStyleClass().add("content-card");

        // Top Gauge Row
        HBox gaugeRow = new HBox(20);
        gaugeRow.setAlignment(Pos.CENTER_LEFT);

        VBox signalBox = new VBox(4);
        signalBox.setStyle("-fx-background-color: #172338; -fx-padding: 18px; -fx-background-radius: 10px; -fx-border-color: #334155; -fx-border-radius: 10px;");
        signalBox.setAlignment(Pos.CENTER);
        signalBox.setPrefWidth(220);

        Label sigPercentLabel = new Label("--%");
        sigPercentLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #10b981;");

        Label sigDbmLabel = new Label("Signal: -- dBm");
        sigDbmLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        Label qualityLabel = new Label("Status: Ready");
        qualityLabel.getStyleClass().addAll("pill-badge", "badge-info");

        signalBox.getChildren().addAll(sigPercentLabel, sigDbmLabel, qualityLabel);

        // Grid of Wi-Fi Details
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        HBox.setHgrow(grid, Priority.ALWAYS);

        Label ssidVal = new Label("Disconnected / Scanning...");
        Label bssidVal = new Label("N/A");
        Label radioVal = new Label("N/A");
        Label bandVal = new Label("N/A");
        Label chanVal = new Label("N/A");
        Label ratesVal = new Label("N/A");

        ssidVal.setStyle("-fx-font-weight: 800; -fx-text-fill: #f8fafc;");
        bssidVal.setStyle("-fx-font-family: monospace; -fx-text-fill: #60a5fa;");
        radioVal.setStyle("-fx-font-weight: 700; -fx-text-fill: #38bdf8;");
        bandVal.setStyle("-fx-font-weight: 700; -fx-text-fill: #a78bfa;");
        chanVal.setStyle("-fx-font-weight: 700; -fx-text-fill: #fbbf24;");
        ratesVal.setStyle("-fx-font-weight: 700; -fx-text-fill: #34d399;");

        grid.add(new Label("Network SSID:"), 0, 0);
        grid.add(ssidVal, 1, 0);

        grid.add(new Label("Access Point (BSSID):"), 0, 1);
        grid.add(bssidVal, 1, 1);

        grid.add(new Label("Radio Protocol:"), 0, 2);
        grid.add(radioVal, 1, 2);

        grid.add(new Label("Frequency Band:"), 2, 0);
        grid.add(bandVal, 3, 0);

        grid.add(new Label("Current Wi-Fi Channel:"), 2, 1);
        grid.add(chanVal, 3, 1);

        grid.add(new Label("Link Speed (RX/TX):"), 2, 2);
        grid.add(ratesVal, 3, 2);

        gaugeRow.getChildren().addAll(signalBox, grid);

        // Interference Advisory Banner
        VBox advisoryBox = new VBox(6);
        advisoryBox.setStyle("-fx-background-color: #131b2e; -fx-padding: 12px; -fx-background-radius: 8px;");
        Label advTitle = new Label("Wireless Environment Advisory:");
        advTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: #cbd5e1;");
        Label advDesc = new Label("Checking wireless interference factors...");
        advDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        advDesc.setWrapText(true);
        advisoryBox.getChildren().addAll(advTitle, advDesc);

        wifiCard.getChildren().addAll(gaugeRow, new Separator(), advisoryBox);

        // Refresh action
        Runnable updateWifi = () -> {
            WifiNetworkInfo info = wifiDiagnostic.getWifiInfo();
            lastWifiInfo = info;

            if (info.isConnected()) {
                sigPercentLabel.setText(info.getSignalPercent() + "%");
                sigPercentLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: " + info.getSignalBadgeColor() + ";");
                sigDbmLabel.setText(String.format("Signal: %d dBm", info.getSignalDbm()));
                qualityLabel.setText(info.getQualityRating());

                ssidVal.setText(info.getSsid());
                bssidVal.setText(info.getBssid());
                radioVal.setText(info.getRadioType());
                bandVal.setText(info.getBand());
                chanVal.setText("Channel " + info.getChannel());
                ratesVal.setText(String.format("RX: %.0f Mbps / TX: %.0f Mbps", info.getReceiveRateMbps(), info.getTransmitRateMbps()));

                if (info.isCongested24Ghz()) {
                    advDesc.setText("⚠️ Connected via 2.4 GHz band. This frequency suffers heavy interference from Bluetooth, microwaves, and neighboring apartments. Switch to a 5 GHz SSID if available.");
                    advisoryBox.setStyle("-fx-background-color: rgba(245, 158, 11, 0.2); -fx-padding: 12px; -fx-background-radius: 8px;");
                } else {
                    advDesc.setText("✅ Connected on 5 GHz band. Clean airtime with minimal wireless co-channel interference.");
                    advisoryBox.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-padding: 12px; -fx-background-radius: 8px;");
                }
            } else {
                sigPercentLabel.setText("0%");
                sigPercentLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #ef4444;");
                sigDbmLabel.setText("Signal: Disconnected");
                qualityLabel.setText("DISCONNECTED");
                ssidVal.setText("No Active Wi-Fi Connection");
                bssidVal.setText("N/A");
                radioVal.setText("N/A");
                bandVal.setText("N/A");
                chanVal.setText("N/A");
                ratesVal.setText("N/A");
                advDesc.setText("No active Wi-Fi connection detected. If you are using Ethernet, Wi-Fi radio metrics are not applicable.");
            }
        };

        refreshBtn.setOnAction(e -> updateWifi.run());
        updateWifi.run(); // Initial fetch

        container.getChildren().addAll(topBox, wifiCard);
        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }

    // =========================================================================
    // 5. SPEED & BANDWIDTH TEST TAB
    // =========================================================================
    private Node buildSpeedTestTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(20));

        VBox speedCard = new VBox(18);
        speedCard.getStyleClass().add("content-card");
        speedCard.setAlignment(Pos.CENTER);
        speedCard.setPadding(new Insets(30));

        Label title = new Label("Real-Time Bandwidth Speedometer");
        title.getStyleClass().add("card-title");
        Label sub = new Label("Measures real-time streaming download throughput, latency, and packet transfer speed.");
        sub.getStyleClass().add("card-subtitle");

        // Speedometer Digital Dial
        VBox dialBox = new VBox(6);
        dialBox.setAlignment(Pos.CENTER);
        dialBox.setStyle("-fx-background-color: #0d1322; -fx-padding: 30px; -fx-background-radius: 12px; -fx-border-color: #1e293b; -fx-border-radius: 12px;");

        Label speedNumber = new Label("0.0");
        speedNumber.setStyle("-fx-font-size: 64px; -fx-font-weight: 900; -fx-text-fill: #38bdf8;");

        Label speedUnit = new Label("Mbps (Download Throughput)");
        speedUnit.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        dialBox.getChildren().addAll(speedNumber, speedUnit);

        ProgressBar speedProgress = new ProgressBar(0);
        speedProgress.setPrefWidth(400);
        speedProgress.setPrefHeight(10);

        Label speedStatus = new Label("Speedometer Idle");
        speedStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        Button runSpeedBtn = new Button("Start Bandwidth Speed Test");
        runSpeedBtn.getStyleClass().add("btn-primary");
        runSpeedBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 24 10 24;");

        // Stat Grid Below Dial
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER);

        Label dlStat = new Label("-- Mbps");
        Label ulStat = new Label("-- Mbps");
        Label pingStat = new Label("-- ms");
        Label bytesStat = new Label("0.0 MB");

        statsRow.getChildren().addAll(
                createStatTile("PEAK DOWNLOAD", dlStat),
                createStatTile("ESTIMATED UPLOAD", ulStat),
                createStatTile("TCP LATENCY", pingStat),
                createStatTile("DATA TRANSFERRED", bytesStat)
        );

        runSpeedBtn.setOnAction(e -> {
            runSpeedBtn.setDisable(true);
            speedStatus.setText("Initiating socket speed test...");
            speedProgress.setProgress(0);
            appendConsoleLog("Starting live bandwidth speed test...");

            Task<SpeedTestResult> speedTask = new Task<>() {
                @Override
                protected SpeedTestResult call() {
                    return speedTestService.runSpeedTest((msg, prog) -> {
                        Platform.runLater(() -> {
                            speedStatus.setText(msg);
                            speedProgress.setProgress(prog);
                            // Extract numeric Mbps from message if available
                            if (msg.contains("Mbps")) {
                                try {
                                    String[] parts = msg.split(":");
                                    if (parts.length > 1) {
                                        String numStr = parts[1].replace("Mbps", "").replace("Download", "").trim();
                                        speedNumber.setText(numStr);
                                    }
                                } catch (Exception ignored) {}
                            }
                        });
                    });
                }
            };

            speedTask.setOnSucceeded(ev -> {
                SpeedTestResult res = speedTask.getValue();
                runSpeedBtn.setDisable(false);
                speedProgress.setProgress(1.0);

                if (res.isSuccess()) {
                    speedNumber.setText(String.format("%.1f", res.getDownloadMbps()));
                    dlStat.setText(String.format("%.1f Mbps", res.getDownloadMbps()));
                    ulStat.setText(String.format("%.1f Mbps", res.getUploadMbps()));
                    pingStat.setText(String.format("%.1f ms", res.getPingLatencyMs()));
                    bytesStat.setText(String.format("%.2f MB", res.getTotalBytesDownloaded() / (1024.0 * 1024.0)));
                    speedStatus.setText("Speed test completed successfully.");
                    appendConsoleLog(String.format("Speed test finished: %.1f Mbps Download, %.1f ms Latency", res.getDownloadMbps(), res.getPingLatencyMs()));
                } else {
                    speedStatus.setText("Error: " + res.getError());
                    appendConsoleLog("Speed test failed: " + res.getError());
                }
            });

            speedTask.setOnFailed(ev -> {
                runSpeedBtn.setDisable(false);
                speedStatus.setText("Speed test encountered an exception.");
            });

            Thread th = new Thread(speedTask, "NetShield-SpeedTest-Worker");
            th.setDaemon(true);
            th.start();
        });

        speedCard.getChildren().addAll(title, sub, dialBox, speedProgress, speedStatus, runSpeedBtn, new Separator(), statsRow);
        container.getChildren().add(speedCard);

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }

    // =========================================================================
    // 6. HISTORY & ANALYTICS TAB
    // =========================================================================
    private Node buildHistoryTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(20));

        // Top 4 Stat Tiles
        HBox statTiles = new HBox(16);
        statTiles.setAlignment(Pos.CENTER);

        statTotalScansLabel = new Label("0");
        statUptimeLabel = new Label("100%");
        statAvgLatencyLabel = new Label("0.0 ms");
        statTotalOutagesLabel = new Label("0");

        statTiles.getChildren().addAll(
                createStatTile("TOTAL SCANS", statTotalScansLabel),
                createStatTile("UPTIME PERCENTAGE", statUptimeLabel),
                createStatTile("AVERAGE WEB LATENCY", statAvgLatencyLabel),
                createStatTile("OUTAGES RECORDED", statTotalOutagesLabel)
        );

        // Historical Latency Line Chart
        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("content-card");

        Label chartHeader = new Label("Historical Latency Trend (ms)");
        chartHeader.getStyleClass().add("card-title");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Scan Time");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Latency (ms)");

        latencyChart = new LineChart<>(xAxis, yAxis);
        latencyChart.setTitle("Diagnostic Latency Over Time");
        latencyChart.setPrefHeight(260);
        latencyChart.setAnimated(false);

        gatewaySeries = new XYChart.Series<>();
        gatewaySeries.setName("Gateway (ms)");
        dnsSeries = new XYChart.Series<>();
        dnsSeries.setName("DNS Resolver (ms)");
        httpSeries = new XYChart.Series<>();
        httpSeries.setName("HTTPS Web (ms)");

        latencyChart.getData().addAll(gatewaySeries, dnsSeries, httpSeries);
        chartCard.getChildren().addAll(chartHeader, latencyChart);

        // History Table
        VBox tableCard = new VBox(10);
        tableCard.getStyleClass().add("content-card");

        HBox tableHeaderBox = new HBox(12);
        tableHeaderBox.setAlignment(Pos.CENTER_LEFT);

        Label tableTitle = new Label("Diagnostic Session History Log");
        tableTitle.getStyleClass().add("card-title");
        HBox.setHgrow(tableTitle, Priority.ALWAYS);

        Button exportCsvBtn = new Button("Export to CSV");
        exportCsvBtn.getStyleClass().add("btn-secondary");
        exportCsvBtn.setOnAction(e -> exportHistoryCsv());

        Button exportJsonBtn = new Button("Export to JSON");
        exportJsonBtn.getStyleClass().add("btn-secondary");
        exportJsonBtn.setOnAction(e -> exportHistoryJson());

        Button clearHistoryBtn = new Button("Clear History");
        clearHistoryBtn.getStyleClass().add("btn-danger");
        clearHistoryBtn.setOnAction(e -> clearHistoryData());

        tableHeaderBox.getChildren().addAll(tableTitle, exportCsvBtn, exportJsonBtn, clearHistoryBtn);

        historyTableView = new TableView<>();
        historyDataList = FXCollections.observableArrayList();
        historyTableView.setItems(historyDataList);
        historyTableView.setPrefHeight(300);

        TableColumn<HistoricalSession, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTimestamp()));
        timeCol.setPrefWidth(150);

        TableColumn<HistoricalSession, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory().getDefaultTitle()));
        categoryCol.setPrefWidth(240);

        TableColumn<HistoricalSession, String> sevCol = new TableColumn<>("Severity");
        sevCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeverity().getLabel()));
        sevCol.setPrefWidth(90);

        TableColumn<HistoricalSession, String> confCol = new TableColumn<>("Confidence");
        confCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getConfidence().getDisplayLabel()));
        confCol.setPrefWidth(150);

        TableColumn<HistoricalSession, String> gwCol = new TableColumn<>("Gateway (ms)");
        gwCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f", d.getValue().getGatewayLatencyMs())));
        gwCol.setPrefWidth(100);

        TableColumn<HistoricalSession, String> lossCol = new TableColumn<>("Loss %");
        lossCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f%%", d.getValue().getPacketLossPercent())));
        lossCol.setPrefWidth(80);

        TableColumn<HistoricalSession, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSummary()));
        summaryCol.setPrefWidth(300);

        historyTableView.getColumns().addAll(timeCol, categoryCol, sevCol, confCol, gwCol, lossCol, summaryCol);

        tableCard.getChildren().addAll(tableHeaderBox, historyTableView);

        container.getChildren().addAll(statTiles, chartCard, tableCard);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0b0f19; -fx-background-color: transparent;");
        return scrollPane;
    }

    private VBox createStatTile(String label, Label valueLabel) {
        VBox tile = new VBox(4);
        tile.getStyleClass().add("stat-tile");
        tile.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tile, Priority.ALWAYS);

        valueLabel.getStyleClass().add("stat-value");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");

        tile.getChildren().addAll(valueLabel, lbl);
        return tile;
    }

    // =========================================================================
    // 7. OFFLINE KNOWLEDGE BASE TAB
    // =========================================================================
    private Node buildKnowledgeBaseTab() {
        HBox split = new HBox(16);
        split.setPadding(new Insets(20));

        // Left: Search & Article List
        VBox leftPane = new VBox(10);
        leftPane.getStyleClass().add("content-card");
        leftPane.setPrefWidth(360);

        Label kbTitle = new Label("Troubleshooting Handbook");
        kbTitle.getStyleClass().add("card-title");
        Label kbSub = new Label("100% Offline-accessible network resolution guides");
        kbSub.getStyleClass().add("card-subtitle");

        TextField searchField = new TextField();
        searchField.setPromptText("Search troubleshooting articles...");

        VBox articleListBox = new VBox(8);
        ScrollPane listScroll = new ScrollPane(articleListBox);
        listScroll.setFitToWidth(true);
        listScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        leftPane.getChildren().addAll(kbTitle, kbSub, searchField, listScroll);

        // Right: Reader
        VBox rightPane = new VBox(14);
        rightPane.getStyleClass().add("content-card");
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        Label readerTitle = new Label("Select an Article");
        readerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");

        Label readerCategory = new Label("");
        readerCategory.getStyleClass().addAll("pill-badge", "badge-info");

        VBox contentBox = new VBox(12);

        ScrollPane readerScroll = new ScrollPane(contentBox);
        readerScroll.setFitToWidth(true);
        readerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(readerScroll, Priority.ALWAYS);

        rightPane.getChildren().addAll(readerTitle, readerCategory, new Separator(), readerScroll);

        // Populate article list
        Runnable updateList = () -> {
            articleListBox.getChildren().clear();
            List<TroubleshootingArticle> list = knowledgeBase.search(searchField.getText());
            for (TroubleshootingArticle a : list) {
                VBox item = new VBox(4);
                item.setStyle("-fx-background-color: #172338; -fx-background-radius: 6px; -fx-padding: 8px; -fx-cursor: hand;");
                Label t = new Label(a.getTitle());
                t.setStyle("-fx-font-weight: 700; -fx-text-fill: #e2e8f0;");
                Label c = new Label(a.getCategory());
                c.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                item.getChildren().addAll(t, c);

                item.setOnMouseClicked(e -> {
                    readerTitle.setText(a.getTitle());
                    readerCategory.setText(a.getCategory());
                    populateArticleDetails(contentBox, a);
                });
                articleListBox.getChildren().add(item);
            }
        };

        searchField.textProperty().addListener((obs, oldV, newV) -> updateList.run());
        updateList.run();

        // Select first article by default
        if (!knowledgeBase.getAllArticles().isEmpty()) {
            TroubleshootingArticle first = knowledgeBase.getAllArticles().get(0);
            readerTitle.setText(first.getTitle());
            readerCategory.setText(first.getCategory());
            populateArticleDetails(contentBox, first);
        }

        split.getChildren().addAll(leftPane, rightPane);
        return split;
    }

    private void populateArticleDetails(VBox container, TroubleshootingArticle a) {
        container.getChildren().clear();

        Label symH = new Label("Symptoms:");
        symH.setStyle("-fx-font-weight: 700; -fx-text-fill: #f59e0b;");
        Label sym = new Label(a.getSymptoms());
        sym.setWrapText(true);
        sym.setStyle("-fx-text-fill: #cbd5e1;");

        Label causeH = new Label("Technical Root Cause:");
        causeH.setStyle("-fx-font-weight: 700; -fx-text-fill: #ef4444;");
        Label cause = new Label(a.getTechnicalCause());
        cause.setWrapText(true);
        cause.setStyle("-fx-text-fill: #cbd5e1;");

        Label stepH = new Label("Safe Step-by-Step Recovery Steps:");
        stepH.setStyle("-fx-font-weight: 700; -fx-text-fill: #10b981;");

        VBox stepsBox = new VBox(6);
        int stepIdx = 1;
        for (String s : a.getRecoverySteps()) {
            Label sl = new Label(stepIdx + ". " + s);
            sl.setWrapText(true);
            sl.setStyle("-fx-text-fill: #e2e8f0;");
            stepsBox.getChildren().add(sl);
            stepIdx++;
        }

        container.getChildren().addAll(symH, sym, new Separator(), causeH, cause, new Separator(), stepH, stepsBox);

        if (!a.getSafeCommands().isEmpty()) {
            Label cmdH = new Label("Safe Diagnostic Commands:");
            cmdH.setStyle("-fx-font-weight: 700; -fx-text-fill: #38bdf8;");
            VBox cmdBox = new VBox(6);
            for (String cmd : a.getSafeCommands()) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                Label cl = new Label(cmd);
                cl.setStyle("-fx-font-family: monospace; -fx-background-color: #080c14; -fx-padding: 4 8 4 8; -fx-background-radius: 4px; -fx-text-fill: #38bdf8;");
                Button copyBtn = new Button("Copy");
                copyBtn.getStyleClass().add("btn-action-copy");
                copyBtn.setOnAction(e -> copyToClipboard(cmd));
                row.getChildren().addAll(cl, copyBtn);
                cmdBox.getChildren().add(row);
            }
            container.getChildren().addAll(new Separator(), cmdH, cmdBox);
        }
    }

    // =========================================================================
    // 8. SETTINGS TAB
    // =========================================================================
    private Node buildSettingsTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(20));

        VBox card = new VBox(16);
        card.getStyleClass().add("content-card");
        card.setMaxWidth(800);

        Label title = new Label("Diagnostic Pipeline Configuration");
        title.getStyleClass().add("card-title");
        Label sub = new Label("Customize test targets, socket timeouts, and probe parameters.");
        sub.getStyleClass().add("card-subtitle");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);

        TextField dnsDomainsField = new TextField(String.join(", ", currentSettings.getDnsTestDomains()));
        dnsDomainsField.setPrefWidth(400);

        TextField directDnsField = new TextField(String.join(", ", currentSettings.getDirectDnsServers()));
        TextField httpsUrlsField = new TextField(String.join(", ", currentSettings.getHttpsTargetUrls()));
        TextField captiveUrlField = new TextField(currentSettings.getCaptiveCheckUrl());
        TextField timeoutField = new TextField(String.valueOf(currentSettings.getTimeoutMs()));
        TextField pingCountField = new TextField(String.valueOf(currentSettings.getPingCount()));
        TextField intervalField = new TextField(String.valueOf(currentSettings.getContinuousIntervalSec()));
        CheckBox soundCheck = new CheckBox("Enable audible alerts on network status changes");
        soundCheck.setSelected(currentSettings.isSoundAlertsEnabled());
        CheckBox autoHealCheck = new CheckBox("⚡ Enable Automatic Self-Healing (Auto-remediate recoverable faults like DNS cache & ARP)");
        autoHealCheck.setSelected(currentSettings.isAutoHealingEnabled());

        int row = 0;
        form.add(new Label("DNS Test Hostnames (comma separated):"), 0, row);
        form.add(dnsDomainsField, 1, row++);

        form.add(new Label("Direct Public DNS Servers (IPs):"), 0, row);
        form.add(directDnsField, 1, row++);

        form.add(new Label("HTTPS Target Endpoints:"), 0, row);
        form.add(httpsUrlsField, 1, row++);

        form.add(new Label("Captive Portal Probe URL:"), 0, row);
        form.add(captiveUrlField, 1, row++);

        form.add(new Label("Socket Timeout (ms):"), 0, row);
        form.add(timeoutField, 1, row++);

        form.add(new Label("Ping Sample Count (probes):"), 0, row);
        form.add(pingCountField, 1, row++);

        form.add(new Label("Continuous Monitor Interval (sec):"), 0, row);
        form.add(intervalField, 1, row++);

        form.add(soundCheck, 1, row++);
        form.add(autoHealCheck, 1, row++);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = new Button("Save Settings");
        saveBtn.getStyleClass().add("btn-primary");

        Button resetBtn = new Button("Reset to Defaults");
        resetBtn.getStyleClass().add("btn-secondary");

        Label saveNotice = new Label("");
        saveNotice.setStyle("-fx-text-fill: #10b981; -fx-font-weight: 600;");

        saveBtn.setOnAction(e -> {
            try {
                currentSettings.setDnsTestDomains(parseList(dnsDomainsField.getText()));
                currentSettings.setDirectDnsServers(parseList(directDnsField.getText()));
                currentSettings.setHttpsTargetUrls(parseList(httpsUrlsField.getText()));
                currentSettings.setCaptiveCheckUrl(captiveUrlField.getText().trim());
                currentSettings.setTimeoutMs(Integer.parseInt(timeoutField.getText().trim()));
                currentSettings.setPingCount(Integer.parseInt(pingCountField.getText().trim()));
                currentSettings.setContinuousIntervalSec(Integer.parseInt(intervalField.getText().trim()));
                currentSettings.setSoundAlertsEnabled(soundCheck.isSelected());
                currentSettings.setAutoHealingEnabled(autoHealCheck.isSelected());

                if (autoHealToggle != null) {
                    autoHealToggle.setSelected(autoHealCheck.isSelected());
                    autoHealToggle.setText(autoHealCheck.isSelected() ? "⚡ Auto-Heal: ACTIVE" : "⚡ Auto-Heal: OFF");
                    autoHealToggle.setStyle(autoHealCheck.isSelected() ? "-fx-border-color: #38bdf8; -fx-text-fill: #38bdf8; -fx-font-weight: 700;" : "");
                }

                settingsDao.saveSettings(currentSettings);
                saveNotice.setText("Settings saved successfully.");
                appendConsoleLog("Settings updated and persisted to SQLite.");
            } catch (Exception ex) {
                saveNotice.setStyle("-fx-text-fill: #ef4444;");
                saveNotice.setText("Error: " + ex.getMessage());
            }
        });

        resetBtn.setOnAction(e -> {
            currentSettings = new UserSettings();
            dnsDomainsField.setText(String.join(", ", currentSettings.getDnsTestDomains()));
            directDnsField.setText(String.join(", ", currentSettings.getDirectDnsServers()));
            httpsUrlsField.setText(String.join(", ", currentSettings.getHttpsTargetUrls()));
            captiveUrlField.setText(currentSettings.getCaptiveCheckUrl());
            timeoutField.setText(String.valueOf(currentSettings.getTimeoutMs()));
            pingCountField.setText(String.valueOf(currentSettings.getPingCount()));
            intervalField.setText(String.valueOf(currentSettings.getContinuousIntervalSec()));
            soundCheck.setSelected(currentSettings.isSoundAlertsEnabled());
            autoHealCheck.setSelected(currentSettings.isAutoHealingEnabled());

            if (autoHealToggle != null) {
                autoHealToggle.setSelected(false);
                autoHealToggle.setText("⚡ Auto-Heal: OFF");
                autoHealToggle.setStyle("");
            }

            settingsDao.saveSettings(currentSettings);
            saveNotice.setText("Defaults restored.");
        });

        btnBox.getChildren().addAll(saveBtn, resetBtn, saveNotice);
        card.getChildren().addAll(title, sub, new Separator(), form, btnBox);

        container.getChildren().add(card);
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0b0f19; -fx-background-color: transparent;");
        return scrollPane;
    }

    private List<String> parseList(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        String[] parts = text.split(",");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (!p.trim().isEmpty()) {
                list.add(p.trim());
            }
        }
        return list;
    }

    // =========================================================================
    // 9. STATUS BAR
    // =========================================================================
    private HBox buildStatusBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");

        statusBarText = new Label("Engine: Idle | SQLite: Connected | Mode: Offline-First");
        statusBarText.getStyleClass().add("status-bar-text");
        HBox.setHgrow(statusBarText, Priority.ALWAYS);

        lastScanTimestampLabel = new Label("Last Scan: Never");
        lastScanTimestampLabel.getStyleClass().add("status-bar-text");

        bar.getChildren().addAll(statusBarText, lastScanTimestampLabel);
        return bar;
    }

    // =========================================================================
    // 10. DIAGNOSTICS EXECUTION & STATE-CHANGE NOTIFICATIONS
    // =========================================================================
    public void startDiagnostics() {
        if (currentDiagnosticTask != null && currentDiagnosticTask.isRunning()) {
            return;
        }

        runDiagnosticsButton.setDisable(true);
        cancelDiagnosticsButton.setDisable(false);
        statusBadgeTop.setText("Testing Network...");
        statusBadgeTop.getStyleClass().removeAll("badge-success", "badge-failed", "badge-warning", "badge-info");
        statusBadgeTop.getStyleClass().add("badge-running");

        // Reset stage cards
        interfaceCard.setRunning();
        gatewayCard.setRunning();
        dnsCard.setRunning();
        httpCard.setRunning();
        latencyCard.setRunning();
        captiveCard.setRunning();

        appendConsoleLog("Starting full multi-layer network diagnostic scan...");

        currentDiagnosticTask = new Task<>() {
            @Override
            protected DiagnosticReport call() throws Exception {
                return diagnosticService.runFullDiagnostics(currentSettings, (stage, progress) -> {
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress);
                        progressLabel.setText("Active Stage: " + stage + " (" + (int)(progress * 100) + "%)");
                    });
                });
            }
        };

        currentDiagnosticTask.setOnSucceeded(e -> {
            DiagnosticReport report = currentDiagnosticTask.getValue();
            handleDiagnosticSuccess(report);
        });

        currentDiagnosticTask.setOnCancelled(e -> {
            appendConsoleLog("Diagnostic scan cancelled by user.");
            resetRunButtons();
            statusBadgeTop.setText("Scan Cancelled");
            statusBadgeTop.getStyleClass().removeAll("badge-running");
            statusBadgeTop.getStyleClass().add("badge-warning");
            progressLabel.setText("Scan cancelled");
            progressBar.setProgress(0);
        });

        currentDiagnosticTask.setOnFailed(e -> {
            Throwable ex = currentDiagnosticTask.getException();
            appendConsoleLog("Diagnostic task error: " + (ex != null ? ex.getMessage() : "Unknown"));
            resetRunButtons();
            statusBadgeTop.setText("Scan Error");
            statusBadgeTop.getStyleClass().removeAll("badge-running");
            statusBadgeTop.getStyleClass().add("badge-failed");
        });

        Thread worker = new Thread(currentDiagnosticTask, "NetShield-Diagnostic-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void handleDiagnosticSuccess(DiagnosticReport report) {
        this.lastReport = report;
        this.lastOutcome = diagnosisEngine.diagnose(report);

        // Fetch Wi-Fi metadata safely
        try {
            this.lastWifiInfo = wifiDiagnostic.getWifiInfo();
        } catch (Exception ignored) {
            this.lastWifiInfo = WifiNetworkInfo.disconnected();
        }

        // Update stage cards with actual measured results
        interfaceCard.updateResult(report.getInterfaceResult());
        gatewayCard.updateResult(report.getGatewayResult());
        dnsCard.updateResult(report.getDnsResult());
        httpCard.updateResult(report.getHttpResult());
        latencyCard.updateResult(report.getLatencyResult());
        captiveCard.updateResult(report.getCaptivePortalResult());

        // Update Hero Banner
        updateHeroBanner(lastOutcome);

        // Update Evidence Chain & Recovery Action items
        populateEvidence(lastOutcome.getEvidenceList());
        populateRecoveryActions(lastOutcome.getRecoveryActions());

        // Check for state changes and automatically alert the host device!
        String changeEvent = notificationManager.checkAndNotifyStateChange(report, lastOutcome, lastWifiInfo);
        if (changeEvent != null) {
            appendConsoleLog("🔔 [STATE CHANGE NOTIFICATION] " + changeEvent);
        }

        // Persist to SQLite
        historyDao.saveSession(report, lastOutcome);

        // Refresh History tab data & line chart
        refreshHistoryStats();

        // Sync with Android Mobile Companion Server
        if (this.mobileServer != null) {
            this.mobileServer.updateState(report, lastOutcome, lastWifiInfo);
        }

        // Console Log summary
        appendConsoleLog("Scan complete -> Diagnosis: " + lastOutcome.getTitle() + " [" + lastOutcome.getConfidence().getDisplayLabel() + "]");

        // Update status bar
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        lastScanTimestampLabel.setText("Last Scan: " + nowStr);
        statusBarText.setText("Engine: Idle | Last Result: " + lastOutcome.getCategory().name());

        resetRunButtons();

        // Trigger autonomous self-healing if enabled and fault detected
        if (currentSettings.isAutoHealingEnabled() && lastOutcome.getSeverity() != com.netshield.models.Severity.HEALTHY) {
            if (!isAutoHealingInProgress) {
                triggerAutoRemediation(report, lastOutcome);
            }
        }
    }

    private void updateHeroBanner(DiagnosisOutcome outcome) {
        heroBanner.getStyleClass().removeAll("hero-healthy", "hero-critical", "hero-warning", "hero-info");

        switch (outcome.getSeverity()) {
            case HEALTHY -> heroBanner.getStyleClass().add("hero-healthy");
            case CRITICAL -> heroBanner.getStyleClass().add("hero-critical");
            case WARNING -> heroBanner.getStyleClass().add("hero-warning");
            case INFO -> heroBanner.getStyleClass().add("hero-info");
        }

        heroStatusLabel.setText(outcome.getSeverity().name() + " STATUS DETECTED");
        heroTitleLabel.setText(outcome.getTitle());
        heroSummaryLabel.setText(outcome.getSummary());

        heroConfidenceBadge.setText(outcome.getConfidence().getDisplayLabel());
        heroConfidenceBadge.setStyle("-fx-background-color: " + outcome.getConfidence().getColorHex() + "33; -fx-text-fill: #ffffff; -fx-border-color: " + outcome.getConfidence().getColorHex() + ";");

        statusBadgeTop.setText(outcome.getCategory().getDefaultTitle());
        statusBadgeTop.getStyleClass().removeAll("badge-success", "badge-failed", "badge-warning", "badge-info", "badge-running");
        switch (outcome.getSeverity()) {
            case HEALTHY -> statusBadgeTop.getStyleClass().add("badge-success");
            case CRITICAL -> statusBadgeTop.getStyleClass().add("badge-failed");
            case WARNING -> statusBadgeTop.getStyleClass().add("badge-warning");
            default -> statusBadgeTop.getStyleClass().add("badge-info");
        }
    }

    private void populateEvidence(List<DiagnosticEvidence> evidenceList) {
        evidenceContainer.getChildren().clear();
        if (evidenceList.isEmpty()) {
            evidenceContainer.getChildren().add(new Label("No specific diagnostic anomalies observed."));
            return;
        }

        for (DiagnosticEvidence ev : evidenceList) {
            HBox item = new HBox(8);
            item.setAlignment(Pos.TOP_LEFT);

            Label badge = new Label(ev.getStageSource());
            badge.setStyle("-fx-font-size: 10px; -fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-padding: 2 6 2 6; -fx-background-radius: 4px;");

            VBox textBox = new VBox(2);
            Label obs = new Label(ev.getObservation());
            obs.setStyle("-fx-font-weight: 600; -fx-text-fill: #f1f5f9;");
            obs.setWrapText(true);

            Label tech = new Label(ev.getTechnicalDetail());
            tech.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            tech.setWrapText(true);

            textBox.getChildren().addAll(obs, tech);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            item.getChildren().addAll(badge, textBox);
            evidenceContainer.getChildren().add(item);
        }
    }

    private void populateRecoveryActions(List<RecoveryAction> actions) {
        recoveryContainer.getChildren().clear();
        if (actions.isEmpty()) {
            recoveryContainer.getChildren().add(new Label("No recovery steps required."));
            return;
        }

        for (RecoveryAction act : actions) {
            VBox actionBox = new VBox(6);
            actionBox.setStyle("-fx-background-color: #131b2e; -fx-background-radius: 6px; -fx-padding: 10px; -fx-border-color: #1e293b; -fx-border-radius: 6px;");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Label num = new Label(String.valueOf(act.getStepNumber()));
            num.setStyle("-fx-background-color: #0284c7; -fx-text-fill: #ffffff; -fx-font-weight: 800; -fx-padding: 2 8 2 8; -fx-background-radius: 12px;");

            Label title = new Label(act.getTitle());
            title.setStyle("-fx-font-weight: 700; -fx-text-fill: #f8fafc;");
            HBox.setHgrow(title, Priority.ALWAYS);

            header.getChildren().addAll(num, title);

            Label desc = new Label(act.getDescription());
            desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
            desc.setWrapText(true);

            Label exp = new Label("Why: " + act.getExplanation());
            exp.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            exp.setWrapText(true);

            actionBox.getChildren().addAll(header, desc, exp);

            // Safe Command or Helper Action Button
            if (act.getSafeCommand() != null || act.getHelperActionId() != null) {
                HBox toolBar = new HBox(8);
                toolBar.setAlignment(Pos.CENTER_LEFT);

                if (act.getSafeCommand() != null) {
                    Label cmdLabel = new Label(act.getSafeCommand());
                    cmdLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-background-color: #080c14; -fx-text-fill: #38bdf8; -fx-padding: 3 6 3 6; -fx-background-radius: 4px;");

                    Button copyBtn = new Button("Copy Command");
                    copyBtn.getStyleClass().add("btn-action-copy");
                    copyBtn.setOnAction(e -> copyToClipboard(act.getSafeCommand()));

                    toolBar.getChildren().addAll(cmdLabel, copyBtn);
                }

                if (act.getHelperActionId() != null) {
                    Button actionBtn = createHelperActionButton(act.getHelperActionId());
                    if (actionBtn != null) {
                        toolBar.getChildren().add(actionBtn);
                    }
                }

                actionBox.getChildren().add(toolBar);
            }

            recoveryContainer.getChildren().add(actionBox);
        }
    }

    private Button createHelperActionButton(String actionId) {
        switch (actionId) {
            case "OPEN_NETWORK_CONNECTIONS" -> {
                Button b = new Button("Open Network Connections (ncpa.cpl)");
                b.getStyleClass().add("btn-secondary");
                b.setOnAction(e -> PlatformUtils.openWindowsNetworkConnections());
                return b;
            }
            case "OPEN_SETTINGS" -> {
                Button b = new Button("Open Windows Network Settings");
                b.getStyleClass().add("btn-secondary");
                b.setOnAction(e -> PlatformUtils.openWindowsNetworkSettings());
                return b;
            }
            case "FLUSH_DNS" -> {
                Button b = new Button("Flush DNS Cache Now");
                b.getStyleClass().add("btn-primary");
                b.setOnAction(e -> {
                    SafeCommandRunner.CommandResult res = PlatformUtils.flushDnsCache();
                    appendConsoleLog("Executed: ipconfig /flushdns -> Exit: " + res.getExitCode());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "DNS Cache flushed successfully.");
                    alert.show();
                });
                return b;
            }
            case "LAUNCH_BROWSER_LOGIN" -> {
                Button b = new Button("Open Login Portal in Browser");
                b.getStyleClass().add("btn-primary");
                b.setOnAction(e -> {
                    String url = "http://connectivitycheck.gstatic.com/generate_204";
                    if (lastReport != null && lastReport.getCaptivePortalResult() != null) {
                        String detected = lastReport.getCaptivePortalResult().getDetail("portalUrl", null);
                        if (detected != null) url = detected;
                    }
                    PlatformUtils.openWebBrowser(url);
                });
                return b;
            }
            default -> {
                return null;
            }
        }
    }

    public void cancelDiagnostics() {
        if (currentDiagnosticTask != null && currentDiagnosticTask.isRunning()) {
            currentDiagnosticTask.cancel(true);
        }
    }

    private void resetRunButtons() {
        runDiagnosticsButton.setDisable(false);
        cancelDiagnosticsButton.setDisable(true);
    }

    private void openRecoveryWizard() {
        if (lastReport == null || lastOutcome == null) {
            startDiagnostics();
        }
        new RecoveryWizardDialog(
                primaryStage,
                lastReport != null ? lastReport : new DiagnosticReport(Map.of()),
                lastOutcome != null ? lastOutcome : new DiagnosisOutcome(DiagnosisCategory.HEALTHY_ONLINE, "Connection Healthy", "All stages OK", ConfidenceLevel.CERTAIN, com.netshield.models.Severity.HEALTHY, List.of(), List.of()),
                diagnosticService, diagnosisEngine, currentSettings, this::startDiagnostics
        ).show();
    }

    // =========================================================================
    // 11. CONTINUOUS MONITORING
    // =========================================================================
    private void toggleContinuousMonitor() {
        if (continuousMonitorToggle.isSelected()) {
            continuousMonitorToggle.setText("Continuous Monitor: ACTIVE");
            continuousMonitorToggle.setStyle("-fx-border-color: #10b981; -fx-text-fill: #34d399;");
            int interval = currentSettings.getContinuousIntervalSec();
            appendConsoleLog("Continuous Monitor activated: repeating every " + interval + " seconds.");

            monitorScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NetShield-Continuous-Monitor");
                t.setDaemon(true);
                return t;
            });

            monitorScheduler.scheduleAtFixedRate(() -> {
                Platform.runLater(() -> {
                    if (currentDiagnosticTask == null || !currentDiagnosticTask.isRunning()) {
                        startDiagnostics();
                    }
                });
            }, 0, interval, TimeUnit.SECONDS);

        } else {
            continuousMonitorToggle.setText("Continuous Monitor");
            continuousMonitorToggle.setStyle("");
            appendConsoleLog("Continuous Monitor stopped.");
            if (monitorScheduler != null && !monitorScheduler.isShutdown()) {
                monitorScheduler.shutdownNow();
            }
        }
    }

    // =========================================================================
    // 11B. AUTONOMOUS SELF-HEALING ENGINE
    // =========================================================================
    private void toggleAutoHeal() {
        boolean selected = autoHealToggle.isSelected();
        currentSettings.setAutoHealingEnabled(selected);
        settingsDao.saveSettings(currentSettings);

        if (selected) {
            autoHealToggle.setText("⚡ Auto-Heal: ACTIVE");
            autoHealToggle.setStyle("-fx-border-color: #38bdf8; -fx-text-fill: #38bdf8; -fx-font-weight: 700;");
            appendConsoleLog("⚡ [AUTONOMOUS SELF-HEALING ACTIVATED] NetShield will now automatically detect and resolve network faults in real time.");
            // If current state is faulty, trigger immediate heal!
            if (lastOutcome != null && lastOutcome.getSeverity() != com.netshield.models.Severity.HEALTHY && lastReport != null) {
                triggerAutoRemediation(lastReport, lastOutcome);
            }
        } else {
            autoHealToggle.setText("⚡ Auto-Heal: OFF");
            autoHealToggle.setStyle("");
            appendConsoleLog("⚡ [AUTONOMOUS SELF-HEALING DEACTIVATED] Reverted to manual remediation mode.");
        }
    }

    private void triggerAutoRemediation(DiagnosticReport report, DiagnosisOutcome outcome) {
        if (isAutoHealingInProgress) {
            return;
        }
        isAutoHealingInProgress = true;

        appendConsoleLog("⚡ [SELF-HEALING ENGINE] Fault detected: " + outcome.getTitle() + " [" + outcome.getSeverity() + "]");
        appendConsoleLog("⚡ [SELF-HEALING ENGINE] Initiating autonomous safe remediation workflow...");
        statusBarText.setText("⚡ Self-Healing: Resolving " + outcome.getTitle() + "...");

        Task<DiagnosticReport> healTask = new Task<>() {
            @Override
            protected DiagnosticReport call() throws Exception {
                // Step 1: Execute targeted safe remediation based on category
                DiagnosisCategory category = outcome.getCategory();
                switch (category) {
                    case LOCAL_DNS_FAILURE, COMPLETE_DNS_FAILURE -> {
                        appendConsoleLog("⚡ [SELF-HEALING] Flushing corrupted Windows DNS resolver cache (ipconfig /flushdns)...");
                        PlatformUtils.flushDnsCache();
                    }
                    case CAPTIVE_PORTAL_DETECTED -> {
                        appendConsoleLog("⚡ [SELF-HEALING] Captive portal detected! Launching browser authentication page...");
                        PlatformUtils.openWebBrowser(currentSettings.getCaptiveCheckUrl());
                    }
                    case HIGH_LATENCY_PACKET_LOSS, PARTIAL_CONNECTIVITY -> {
                        appendConsoleLog("⚡ [SELF-HEALING] Flushing DNS cache and refreshing resolver sockets...");
                        PlatformUtils.flushDnsCache();
                    }
                    case NO_ACTIVE_ADAPTER, GATEWAY_UNREACHABLE, ISP_OUTAGE -> {
                        appendConsoleLog("⚠️ [SELF-HEALING] Physical/Hardware layer fault detected. Automated software remediation cannot repair physical disconnection.");
                    }
                    default -> {
                        appendConsoleLog("⚡ [SELF-HEALING] Applying universal safe resolver cache reset...");
                        PlatformUtils.flushDnsCache();
                    }
                }

                // Step 2: Settle time for network stack
                Thread.sleep(1200);

                // Step 3: Run immediate auto-verification probe
                appendConsoleLog("⚡ [SELF-HEALING] Running instant auto-verification probe...");
                return diagnosticService.runFullDiagnostics(currentSettings, null);
            }
        };

        healTask.setOnSucceeded(e -> {
            DiagnosticReport newReport = healTask.getValue();
            DiagnosisOutcome newOutcome = diagnosisEngine.diagnose(newReport);

            // Update state
            this.lastReport = newReport;
            this.lastOutcome = newOutcome;

            // Update UI components
            interfaceCard.updateResult(newReport.getInterfaceResult());
            gatewayCard.updateResult(newReport.getGatewayResult());
            dnsCard.updateResult(newReport.getDnsResult());
            httpCard.updateResult(newReport.getHttpResult());
            latencyCard.updateResult(newReport.getLatencyResult());
            captiveCard.updateResult(newReport.getCaptivePortalResult());

            updateHeroBanner(newOutcome);
            populateEvidence(newOutcome.getEvidenceList());
            populateRecoveryActions(newOutcome.getRecoveryActions());

            // Check if solved or improved
            boolean improved = newOutcome.getSeverity().ordinal() < outcome.getSeverity().ordinal()
                    || newOutcome.getSeverity() == com.netshield.models.Severity.HEALTHY;

            if (improved) {
                appendConsoleLog("🎉 [SELF-HEALING SUCCESS] Network fault automatically resolved! Connection is now: " + newOutcome.getTitle() + " (" + newOutcome.getSeverity() + ")");
                notificationManager.showToast("⚡ NetShield Self-Healing", "Network fault automatically resolved! (" + newOutcome.getTitle() + ")", java.awt.TrayIcon.MessageType.INFO);
                SoundNotification.playAlert();
            } else {
                appendConsoleLog("⚠️ [SELF-HEALING NOTICE] Automated safe action completed. Current state: " + newOutcome.getTitle());
                if (newOutcome.getSeverity() == com.netshield.models.Severity.CRITICAL) {
                    appendConsoleLog("⚠️ [SELF-HEALING NOTICE] Physical intervention required: Check router power or Wi-Fi switch.");
                }
            }

            // Persist verified session
            historyDao.saveSession(newReport, newOutcome);
            refreshHistoryStats();

            if (this.mobileServer != null) {
                this.mobileServer.updateState(newReport, newOutcome, lastWifiInfo);
            }

            statusBarText.setText("⚡ Self-Healing: Complete | State: " + newOutcome.getCategory().name());
            isAutoHealingInProgress = false;
        });

        healTask.setOnFailed(e -> {
            isAutoHealingInProgress = false;
            appendConsoleLog("⚡ [SELF-HEALING ERROR] Self-healing workflow failed: " + healTask.getException().getMessage());
            statusBarText.setText("Engine: Idle | Self-Healing Error");
        });

        Thread th = new Thread(healTask, "NetShield-AutoHealer");
        th.setDaemon(true);
        th.start();
    }

    // =========================================================================
    // 12. EXPORT & REPORT GENERATION
    // =========================================================================
    private void exportHtmlReport() {
        if (lastReport == null || lastOutcome == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please run a diagnostic scan first before generating the report.");
            alert.show();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save NetShield Executive Report (HTML / PDF Ready)");
        chooser.setInitialFileName("NetShield_Report_" + System.currentTimeMillis() + ".html");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Report (*.html)", "*.html"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                HtmlReportGenerator.generateReport(lastReport, lastOutcome, lastWifiInfo, file);
                appendConsoleLog("Generated executive report: " + file.getAbsolutePath());
                // Open in default browser so user can Print to PDF immediately
                PlatformUtils.openWebBrowser(file.toURI().toString());
            } catch (Exception e) {
                appendConsoleLog("Report generation error: " + e.getMessage());
            }
        }
    }

    private void showMobileCompanionDialog() {
        String url = (mobileServer != null) ? mobileServer.getServerUrl() : "http://localhost:8080";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("NetShield — Android & Mobile Checking");
        alert.setHeaderText("📱 Check Network from Your Android Phone");
        alert.setContentText(
                "Connect your Android phone to the same Wi-Fi network and open this URL in Chrome / Firefox:\n\n" +
                "👉  " + url + "\n\n" +
                "Features available directly on your Android phone:\n" +
                "• Run full network diagnostics remotely\n" +
                "• Check real-time Wi-Fi signal & channel\n" +
                "• One-tap '⚡ Auto-Solve Network Fault' remediation\n" +
                "• Live status card with color-coded severity\n\n" +
                "(The link has been automatically copied to your clipboard!)"
        );
        copyToClipboard(url);
        alert.show();
    }

    private void exportHistoryCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Diagnostic History to CSV");
        chooser.setInitialFileName("netshield_history.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                ReportExporter.exportHistoryToCsv(historyDao.getAllSessions(), file);
                appendConsoleLog("Exported history to CSV: " + file.getAbsolutePath());
            } catch (Exception e) {
                appendConsoleLog("CSV export error: " + e.getMessage());
            }
        }
    }

    private void exportHistoryJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Diagnostic History to JSON");
        chooser.setInitialFileName("netshield_history.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                ReportExporter.exportHistoryToJson(historyDao.getAllSessions(), file);
                appendConsoleLog("Exported history to JSON: " + file.getAbsolutePath());
            } catch (Exception e) {
                appendConsoleLog("JSON export error: " + e.getMessage());
            }
        }
    }

    private void refreshHistoryStats() {
        List<HistoricalSession> sessions = historyDao.getRecentSessions(50);
        historyDataList.setAll(sessions);

        statTotalScansLabel.setText(String.valueOf(sessions.size()));
        statUptimeLabel.setText(String.format("%.1f%%", historyDao.getUptimePercentage()));
        statAvgLatencyLabel.setText(String.format("%.1f ms", historyDao.getAverageLatency()));
        statTotalOutagesLabel.setText(String.valueOf(historyDao.getTotalOutagesCount()));

        // Update Line Chart (reverse so oldest is left, newest is right)
        gatewaySeries.getData().clear();
        dnsSeries.getData().clear();
        httpSeries.getData().clear();

        List<HistoricalSession> chartPoints = new ArrayList<>(sessions);
        java.util.Collections.reverse(chartPoints);

        // Keep last 15 points on chart
        if (chartPoints.size() > 15) {
            chartPoints = chartPoints.subList(chartPoints.size() - 15, chartPoints.size());
        }

        for (HistoricalSession s : chartPoints) {
            String timeLabel = s.getFormattedTimestamp().substring(11); // HH:mm:ss
            gatewaySeries.getData().add(new XYChart.Data<>(timeLabel, Math.max(0, s.getGatewayLatencyMs())));
            dnsSeries.getData().add(new XYChart.Data<>(timeLabel, Math.max(0, s.getDnsLatencyMs())));
            httpSeries.getData().add(new XYChart.Data<>(timeLabel, Math.max(0, s.getHttpsLatencyMs())));
        }
    }

    private void clearHistoryData() {
        historyDao.clearHistory();
        refreshHistoryStats();
        appendConsoleLog("Historical scan database cleared.");
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
        appendConsoleLog("Copied to clipboard: " + text);
    }

    private void appendConsoleLog(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = "[" + time + "] " + message + "\n";
        Platform.runLater(() -> {
            if (consoleLogArea != null) {
                consoleLogArea.appendText(entry);
            }
        });
    }

    public void shutdown() {
        if (monitorScheduler != null && !monitorScheduler.isShutdown()) {
            monitorScheduler.shutdownNow();
        }
        if (currentDiagnosticTask != null && currentDiagnosticTask.isRunning()) {
            currentDiagnosticTask.cancel(true);
        }
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
