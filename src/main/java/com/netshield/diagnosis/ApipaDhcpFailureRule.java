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
 * Diagnosis Rule: Detects DHCP lease failure resulting in self-assigned APIPA (169.254.x.x) address.
 */
public class ApipaDhcpFailureRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult iface = report.getInterfaceResult();
        if (iface == null) return false;
        boolean hasApipa = iface.getDetail("hasApipa", false);
        String ip = iface.getDetail("primaryIpv4", "");
        return hasApipa || (ip != null && ip.startsWith("169.254."));
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult iface = report.getInterfaceResult();
        String primaryIp = iface != null ? iface.getDetail("primaryIpv4", "169.254.x.x") : "169.254.x.x";

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_INTERFACE,
                "Adapter received an Automatic Private IP Addressing (APIPA) address: " + primaryIp,
                Severity.CRITICAL,
                "APIPA (RFC 3927) is invoked when the local DHCP server fails to assign a valid network lease."
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Restart Local Router / Gateway",
                "Power cycle your Wi-Fi router or residential gateway. Unplug it for 15 seconds and reconnect.",
                null, null, false,
                "Routers occasionally freeze their internal DHCP server daemon or exhaust their allocated IP pool."));

        actions.add(new RecoveryAction(2,
                "Request a New DHCP Lease",
                "Execute 'ipconfig /renew' in Command Prompt or Windows PowerShell to request a fresh DHCP lease.",
                "ipconfig /renew", null, false,
                "Forces the Windows DHCP client service to broadcast a new DHCPDISCOVER packet."));

        actions.add(new RecoveryAction(3,
                "Inspect Adapter IP Settings",
                "Open Windows Network Connections to verify your adapter is set to 'Obtain an IP address automatically'.",
                null, "OPEN_NETWORK_CONNECTIONS", false,
                "Statically configured invalid IP addresses can prevent normal DHCP negotiation."));

        return new DiagnosisOutcome(
                DiagnosisCategory.DHCP_APIPA_FAILURE,
                "DHCP Lease Failure (APIPA 169.254.x.x)",
                "Your computer connected to the local link but received no IP lease from your router's DHCP server.",
                ConfidenceLevel.CERTAIN,
                Severity.CRITICAL,
                evidence,
                actions
        );
    }
}
