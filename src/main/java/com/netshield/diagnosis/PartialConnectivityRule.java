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
 * Diagnosis Rule: Detects partial internet connectivity (some endpoints work, others fail or timeout).
 */
public class PartialConnectivityRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult http = report.getHttpResult();
        if (http == null) return false;
        return http.getStatus() == StageStatus.INCONCLUSIVE ||
                (http.getDetail("successfulEndpoints", 0) > 0 &&
                        http.getDetail("successfulEndpoints", 0) < http.getDetail("totalEndpointsTested", 0));
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult http = report.getHttpResult();

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_HTTP,
                "Partial web connectivity: Some tested domains responded while others timed out or failed.",
                Severity.WARNING,
                http != null ? http.getMessage() : "Incomplete reachability"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Check Windows Proxy Settings",
                "Open Windows Settings -> Network & Internet -> Proxy. Ensure 'Automatically detect settings' is ON and 'Use a proxy server' is OFF unless required by your campus or workplace.",
                null, "OPEN_SETTINGS", false,
                "Misconfigured manual proxies cause specific hostnames to hang or fail while raw traffic succeeds."));

        actions.add(new RecoveryAction(2,
                "Inspect VPN and Tunneling Adapters",
                "If you recently disconnected from a VPN or university tunnel, restart your network adapter to reset routing table entries.",
                null, "OPEN_NETWORK_CONNECTIONS", false,
                "Lingering split-tunnel route entries can direct certain subnet traffic into dead interfaces."));

        actions.add(new RecoveryAction(3,
                "Verify System Clock & Date Synchronization",
                "Ensure your Windows date, time, and timezone are synchronized automatically. SSL/TLS handshakes fail if clock skew exceeds 5 minutes.",
                null, null, false,
                "Digital certificates are strictly validated against host system time."));

        return new DiagnosisOutcome(
                DiagnosisCategory.PARTIAL_CONNECTIVITY,
                "Partial Internet Connectivity Detected",
                "Your computer can reach some web destinations, but access to other services is blocked, filtered, or timing out.",
                ConfidenceLevel.MEDIUM,
                Severity.WARNING,
                evidence,
                actions
        );
    }
}
