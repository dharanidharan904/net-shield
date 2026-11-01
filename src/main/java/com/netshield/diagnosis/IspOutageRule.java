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
 * Diagnosis Rule: Detects upstream Internet Service Provider (ISP) WAN outage.
 */
public class IspOutageRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult gateway = report.getGatewayResult();
        StageResult http = report.getHttpResult();
        StageResult latency = report.getLatencyResult();
        StageResult dns = report.getDnsResult();

        if (gateway == null || http == null) return false;

        boolean gatewayOk = gateway.getStatus() == StageStatus.SUCCESS;
        boolean httpFailed = http.getStatus() == StageStatus.FAILED;
        boolean latencyFailed = latency != null && latency.getStatus() == StageStatus.FAILED;
        boolean dnsFailed = dns != null && dns.getStatus() == StageStatus.FAILED;

        // Gateway works, but all external stages (HTTP, Latency, DNS) fail completely
        return gatewayOk && httpFailed && (latencyFailed || dnsFailed);
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult gateway = report.getGatewayResult();
        String gatewayIp = gateway != null ? gateway.getDetail("gatewayIp", "Gateway") : "Gateway";

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_GATEWAY,
                "Local Gateway (" + gatewayIp + ") responded normally; internal LAN communication is intact.",
                Severity.HEALTHY,
                "Router LAN interface operational"
        ));
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_HTTP,
                "All external HTTPS web endpoints and public IPs timed out.",
                Severity.CRITICAL,
                "WAN uplink non-responsive"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Inspect Modem WAN / Optical / Cable LED Status",
                "Look at the physical LEDs on your modem or fiber ONT. Look for 'Internet', 'WAN', 'PON', or 'Optical'. A red, amber, or blinking light indicates an ISP fiber/cable line fault.",
                null, null, false,
                "WAN optical or coaxial signals are managed by your ISP."));

        actions.add(new RecoveryAction(2,
                "Power Cycle Both Modem and Router",
                "Unplug power from both modem and router for 60 seconds. Plug the modem in first, wait 2 minutes for sync, then plug in the router.",
                null, null, false,
                "Forces the ISP DSLAM/CMTS/OLT to re-establish the Point-to-Point Protocol (PPPoE / IPoE) connection."));

        actions.add(new RecoveryAction(3,
                "Contact Your Internet Service Provider (ISP)",
                "Check your ISP's mobile app or support line for regional neighborhood fiber cuts, maintenance windows, or billing holds.",
                null, null, false,
                "Local LAN is fully verified; failure is occurring on the upstream carrier backbone."));

        return new DiagnosisOutcome(
                DiagnosisCategory.ISP_OUTAGE,
                "Upstream ISP Outage Detected",
                "Your computer and local router are working properly, but your router has lost its connection to your Internet Service Provider.",
                ConfidenceLevel.HIGH,
                Severity.CRITICAL,
                evidence,
                actions
        );
    }
}
