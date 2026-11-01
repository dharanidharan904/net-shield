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
 * Diagnosis Rule: Detects when local default gateway is completely unreachable.
 */
public class GatewayUnreachableRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult iface = report.getInterfaceResult();
        StageResult gateway = report.getGatewayResult();
        if (gateway == null) return false;

        boolean ifaceOk = iface != null && iface.getStatus() == StageStatus.SUCCESS;
        boolean gatewayFailed = gateway.getStatus() == StageStatus.FAILED || gateway.getStatus() == StageStatus.TIMED_OUT;

        return ifaceOk && gatewayFailed;
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult gateway = report.getGatewayResult();
        String gatewayIp = gateway != null ? gateway.getDetail("gatewayIp", "Default Gateway") : "Default Gateway";

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_GATEWAY,
                "Default Gateway (" + gatewayIp + ") failed to respond to diagnostic pings.",
                Severity.CRITICAL,
                gateway != null ? gateway.getMessage() : "Gateway timeout"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Reboot Your Local Router / Gateway",
                "Unplug the power adapter of your router/modem, wait 30 seconds, and plug it back in. Wait 2 minutes for it to boot.",
                null, null, false,
                "The gateway is your local hop to the wider internet; if it freezes, all external network communication fails."));

        actions.add(new RecoveryAction(2,
                "Check Physical LAN Port or Wi-Fi Connection",
                "If connected via Ethernet, ensure the cable is in a LAN port (numbered 1-4) on the router and the port LED is illuminated.",
                null, null, false,
                "Bad patch cables or damaged switch ports cause link intermittency between PC and router."));

        actions.add(new RecoveryAction(3,
                "Test Gateway IP via Command Prompt",
                "Manually ping your gateway IP to inspect ICMP response: ping " + gatewayIp,
                "ping " + gatewayIp, null, false,
                "Verifies whether the router kernel is handling network packets at the IP layer."));

        return new DiagnosisOutcome(
                DiagnosisCategory.GATEWAY_UNREACHABLE,
                "Default Gateway Unreachable",
                "Your computer is connected to the local network adapter, but the local router/gateway (" + gatewayIp + ") is not responding.",
                ConfidenceLevel.HIGH,
                Severity.CRITICAL,
                evidence,
                actions
        );
    }
}
