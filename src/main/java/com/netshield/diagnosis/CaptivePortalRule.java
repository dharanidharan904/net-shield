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
 * Diagnosis Rule: Detects captive portal / Wi-Fi splash screen interception.
 */
public class CaptivePortalRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 85;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult captive = report.getCaptivePortalResult();
        if (captive == null) return false;
        return captive.getDetail("isCaptivePortal", false);
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult captive = report.getCaptivePortalResult();
        String portalUrl = captive != null ? captive.getDetail("portalUrl", "http://connectivitycheck.gstatic.com/generate_204") : "http://connectivitycheck.gstatic.com/generate_204";

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_CAPTIVE,
                "HTTP probe was redirected to captive portal: " + portalUrl,
                Severity.WARNING,
                captive != null ? captive.getMessage() : "Captive portal detected"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Open Browser to Complete Login",
                "Open your web browser and accept terms or enter your credentials on the network login page.",
                null, "LAUNCH_BROWSER_LOGIN", false,
                "Public, hotel, and campus Wi-Fi networks block external internet traffic until authentication is completed."));

        actions.add(new RecoveryAction(2,
                "Disable Active VPN During Login",
                "If you are running a VPN client (e.g. WireGuard, OpenVPN, Cisco AnyConnect), temporarily disconnect it.",
                null, null, false,
                "VPN tunnels attempt to encrypt traffic before the captive portal allows packets through, creating a deadlock."));

        actions.add(new RecoveryAction(3,
                "Trigger Manual Login Portal Page",
                "If the login portal does not pop up automatically, navigate to http://neverssl.com or http://1.1.1.1 in your browser.",
                null, null, false,
                "Accessing an unencrypted HTTP URL forces the router's DNS/HTTP interceptor to display the splash screen."));

        return new DiagnosisOutcome(
                DiagnosisCategory.CAPTIVE_PORTAL_DETECTED,
                "Captive Portal / Wi-Fi Authentication Required",
                "Your Wi-Fi network requires web-based authentication or terms acceptance before allowing external internet access.",
                ConfidenceLevel.HIGH,
                Severity.WARNING,
                evidence,
                actions
        );
    }
}
