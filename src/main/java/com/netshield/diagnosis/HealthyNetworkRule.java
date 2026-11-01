package com.netshield.diagnosis;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.RecoveryAction;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnosis Rule: Confirms normal, healthy internet connectivity across all layers.
 */
public class HealthyNetworkRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult iface = report.getInterfaceResult();
        StageResult gateway = report.getGatewayResult();
        StageResult dns = report.getDnsResult();
        StageResult http = report.getHttpResult();

        if (iface == null || gateway == null || dns == null || http == null) {
            return false;
        }

        return iface.getStatus() == StageStatus.SUCCESS &&
                gateway.getStatus() == StageStatus.SUCCESS &&
                dns.getStatus() == StageStatus.SUCCESS &&
                http.getStatus() == StageStatus.SUCCESS;
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        List<DiagnosticEvidence> evidence = new ArrayList<>();

        StageResult iface = report.getInterfaceResult();
        if (iface != null) {
            evidence.add(new DiagnosticEvidence(DiagnosticReport.STAGE_INTERFACE,
                    "Active adapter '" + iface.getDetail("primaryAdapterName", "LAN") + "' configured with IP " + iface.getDetail("primaryIpv4", ""),
                    Severity.HEALTHY, "Link operational"));
        }

        StageResult gateway = report.getGatewayResult();
        if (gateway != null) {
            evidence.add(new DiagnosticEvidence(DiagnosticReport.STAGE_GATEWAY,
                    "Gateway (" + gateway.getDetail("gatewayIp", "") + ") reachable in " + gateway.getDetail("latencyMs", 0L) + " ms",
                    Severity.HEALTHY, "ICMP / TCP socket response"));
        }

        StageResult dns = report.getDnsResult();
        if (dns != null) {
            evidence.add(new DiagnosticEvidence(DiagnosticReport.STAGE_DNS,
                    "System DNS successfully resolved test domains (avg " + String.format("%.1f", dns.getDetail("avgSystemLatencyMs", 0.0)) + " ms)",
                    Severity.HEALTHY, "DNS queries resolved"));
        }

        StageResult http = report.getHttpResult();
        if (http != null) {
            evidence.add(new DiagnosticEvidence(DiagnosticReport.STAGE_HTTP,
                    "HTTPS endpoints responded normally (avg " + String.format("%.1f", http.getDetail("avgHttpLatencyMs", 0.0)) + " ms)",
                    Severity.HEALTHY, "Transport & application layer verified"));
        }

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "No Action Required",
                "Your internet connection is operating normally. All diagnostic probes completed successfully.",
                null, null, false,
                "Every tested layer (Physical, Data Link, Network, Transport, and Application) is responding without anomalies."));

        return new DiagnosisOutcome(
                DiagnosisCategory.HEALTHY_ONLINE,
                "Internet Connection is Healthy and Operational",
                "All network diagnostic checks passed. Full external internet connectivity is confirmed.",
                ConfidenceLevel.CERTAIN,
                Severity.HEALTHY,
                evidence,
                actions
        );
    }
}
