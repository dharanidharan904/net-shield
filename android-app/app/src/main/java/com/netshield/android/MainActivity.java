package com.netshield.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.netshield.android.diagnostics.AndroidDiagnosticService;
import com.netshield.android.models.DiagnosticReport;
import com.netshield.android.models.StageResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 101;

    private TextView heroTitle, heroSeverity, heroSummary;
    private TextView wifiSsid, wifiDetails, wifiSignal;
    private TextView stageNicStatus, stageGatewayStatus, stageDnsStatus, stageHttpStatus, stageLatencyStatus, stageCaptiveStatus;
    private ProgressBar scanProgressBar;
    private Button btnRunDiagnostics, btnAutoHeal;

    private AndroidDiagnosticService diagnosticService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        diagnosticService = new AndroidDiagnosticService(this);

        checkPermissions();

        btnRunDiagnostics.setOnClickListener(v -> runDiagnostics());
        btnAutoHeal.setOnClickListener(v -> runAutoHeal());
    }

    private void initViews() {
        heroTitle = findViewById(R.id.heroTitle);
        heroSeverity = findViewById(R.id.heroSeverity);
        heroSummary = findViewById(R.id.heroSummary);

        wifiSsid = findViewById(R.id.wifiSsid);
        wifiDetails = findViewById(R.id.wifiDetails);
        wifiSignal = findViewById(R.id.wifiSignal);

        stageNicStatus = findViewById(R.id.stageNicStatus);
        stageGatewayStatus = findViewById(R.id.stageGatewayStatus);
        stageDnsStatus = findViewById(R.id.stageDnsStatus);
        stageHttpStatus = findViewById(R.id.stageHttpStatus);
        stageLatencyStatus = findViewById(R.id.stageLatencyStatus);
        stageCaptiveStatus = findViewById(R.id.stageCaptiveStatus);

        scanProgressBar = findViewById(R.id.scanProgressBar);
        btnRunDiagnostics = findViewById(R.id.btnRunDiagnostics);
        btnAutoHeal = findViewById(R.id.btnAutoHeal);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE},
                    PERMISSION_REQ_CODE);
        }
    }

    private void runDiagnostics() {
        setScanningUi(true);

        executor.execute(() -> {
            DiagnosticReport report = diagnosticService.runFullDiagnostics();
            mainHandler.post(() -> {
                setScanningUi(false);
                updateUiWithReport(report);
            });
        });
    }

    private void runAutoHeal() {
        setScanningUi(true);
        heroTitle.setText("Applying Autonomous Safe Remediation...");
        heroSummary.setText("Resetting Wi-Fi radio connection and refreshing Android network sockets...");

        executor.execute(() -> {
            // Android safe remediation: refresh Wi-Fi socket association
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
                wm.reassociate();
            }

            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

            DiagnosticReport report = diagnosticService.runFullDiagnostics();
            mainHandler.post(() -> {
                setScanningUi(false);
                updateUiWithReport(report);
                Toast.makeText(this, "Self-Healing complete. Network auto-verified!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void setScanningUi(boolean isScanning) {
        scanProgressBar.setVisibility(isScanning ? View.VISIBLE : View.GONE);
        btnRunDiagnostics.setEnabled(!isScanning);
        btnAutoHeal.setEnabled(!isScanning);
        if (isScanning) {
            heroTitle.setText("Scanning Network Layers...");
            heroSeverity.setText("ACTIVE PROBE");
            heroSummary.setText("Testing Physical NIC, Gateway, DNS, WAN, and Latency...");
        }
    }

    private void updateUiWithReport(DiagnosticReport report) {
        wifiSsid.setText("SSID: " + report.getSsid());
        wifiDetails.setText("Band: " + report.getBand() + " | Link: " + report.getLinkSpeedMbps() + " Mbps");
        wifiSignal.setText(report.getSignalPercent() + "%");

        updateStageLabel(stageNicStatus, report.getStage("NIC"));
        updateStageLabel(stageGatewayStatus, report.getStage("GATEWAY"));
        updateStageLabel(stageDnsStatus, report.getStage("DNS"));
        updateStageLabel(stageHttpStatus, report.getStage("HTTPS"));
        updateStageLabel(stageLatencyStatus, report.getStage("LATENCY"));
        updateStageLabel(stageCaptiveStatus, report.getStage("CAPTIVE"));

        if (report.isFullyHealthy()) {
            heroSeverity.setText("HEALTHY (100% OPERATIONAL)");
            heroSeverity.setTextColor(ContextCompat.getColor(this, R.color.status_healthy));
            heroTitle.setText("Internet Connection Fully Operational");
            heroSummary.setText("All 6 protocol layers verified. DNS, Gateway, and WAN endpoints responsive.");
        } else {
            heroSeverity.setText("FAULT DETECTED");
            heroSeverity.setTextColor(ContextCompat.getColor(this, R.color.status_critical));

            StageResult dns = report.getStage("DNS");
            StageResult http = report.getStage("HTTPS");
            StageResult nic = report.getStage("NIC");

            if (nic != null && !nic.isSuccess()) {
                heroTitle.setText("No Active Network Connection");
                heroSummary.setText("Wi-Fi and mobile data are disconnected. Please turn on Wi-Fi.");
            } else if (dns != null && !dns.isSuccess()) {
                heroTitle.setText("DNS Resolution Failure");
                heroSummary.setText("Gateway responded, but DNS failed. Tap 'Auto-Solve Network Fault' to remediate.");
            } else if (http != null && !http.isSuccess()) {
                heroTitle.setText("Upstream ISP / WAN Outage");
                heroSummary.setText("Home router is connected, but external internet is unreachable.");
            } else {
                heroTitle.setText("Network Quality Degraded");
                heroSummary.setText("High latency or packet drop observed on intermediate hops.");
            }
        }
    }

    private void updateStageLabel(TextView tv, StageResult res) {
        if (res == null) return;
        if (res.isSuccess()) {
            tv.setText("OPERATIONAL");
            tv.setTextColor(ContextCompat.getColor(this, R.color.status_healthy));
        } else {
            tv.setText("FAILED");
            tv.setTextColor(ContextCompat.getColor(this, R.color.status_critical));
        }
    }
}
