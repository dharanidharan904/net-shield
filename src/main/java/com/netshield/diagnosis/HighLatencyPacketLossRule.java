package com.netshield.diagnosis;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.RecoveryAction;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnosis Rule: Detects severe network quality degradation, bufferbloat, jitter, or packet loss.
 */
public class HighLatencyPacketLossRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult latency = report.getLatencyResult();
        if (latency == null) return false;

        double loss = latency.getDetail("packetLossPercent", 0.0);
        double avg = latency.getDetail("avgLatencyMs", 0.0);
        double jitter = latency.getDetail("jitterMs", 0.0);

        // Degraded if loss between 10% and 80%, or high latency / jitter
        return (loss >= 15.0 && loss < 100.0) || avg > 200.0 || jitter > 50.0;
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult latency = report.getLatencyResult();
        double loss = latency != null ? latency.getDetail("packetLossPercent", 0.0) : 0.0;
        double avg = latency != null ? latency.getDetail("avgLatencyMs", 0.0) : 0.0;
        double jitter = latency != null ? latency.getDetail("jitterMs", 0.0) : 0.0;

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_LATENCY,
                String.format("Packet Loss: %.0f%% | Average Latency: %.1f ms | Jitter: %.1f ms", loss, avg, jitter),
                Severity.WARNING,
                latency != null ? latency.getMessage() : "High latency and packet drops"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Switch from 2.4 GHz to 5 GHz Wi-Fi or Use Ethernet",
                "2.4 GHz Wi-Fi is prone to severe interference from microwaves, Bluetooth devices, and neighboring access points. Switch to a 5 GHz band or plug in an Ethernet cable.",
                null, null, false,
                "Significantly reduces radio interference and airtime congestion."));

        actions.add(new RecoveryAction(2,
                "Check for Background Bandwidth Consumption (Bufferbloat)",
                "Open Windows Task Manager (Ctrl+Shift+Esc) -> Performance -> Network to check if Steam, Windows Update, cloud backups (OneDrive, Google Drive), or torrents are saturating your upload bandwidth.",
                "taskmgr.exe", null, false,
                "Bufferbloat occurs when router buffers become bloated by maxed-out uploads, causing latency to spike by hundreds of milliseconds."));

        actions.add(new RecoveryAction(3,
                "Move Closer to Wi-Fi Access Point",
                "Weak Wi-Fi signal (below -70 dBm) causes physical packet retransmissions and intermittent packet drops.",
                null, null, false,
                "Improves Signal-to-Noise Ratio (SNR) for the wireless radio."));

        return new DiagnosisOutcome(
                DiagnosisCategory.HIGH_LATENCY_PACKET_LOSS,
                "Severe Network Degradation / Packet Loss",
                String.format("Internet is connected, but experiencing high packet loss (%.0f%%) and elevated latency (%.1f ms).", loss, avg),
                ConfidenceLevel.HIGH,
                Severity.WARNING,
                evidence,
                actions
        );
    }
}
