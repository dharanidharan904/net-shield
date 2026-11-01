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
 * Diagnosis Rule: Detects complete DNS failure (both local and public DNS queries blocked).
 */
public class CompleteDnsFailureRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 65;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult dns = report.getDnsResult();
        StageResult gateway = report.getGatewayResult();
        if (dns == null || gateway == null) return false;

        boolean gatewayOk = gateway.getStatus() == StageStatus.SUCCESS;
        boolean systemDnsFailed = !dns.getDetail("systemDnsSuccess", true);
        boolean directDnsFailed = !dns.getDetail("directDnsSuccess", true);

        // If gateway is up but both system and direct DNS failed, but IP pings to 1.1.1.1 might still pass
        StageResult latency = report.getLatencyResult();
        boolean ipPingWorks = latency != null && latency.getStatus() == StageStatus.SUCCESS;

        return gatewayOk && systemDnsFailed && directDnsFailed && ipPingWorks;
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult dns = report.getDnsResult();

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_DNS,
                "Both system resolver and direct public DNS queries (1.1.1.1 / 8.8.8.8 port 53) failed.",
                Severity.CRITICAL,
                dns != null ? dns.getMessage() : "Total DNS outage"
        ));
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_LATENCY,
                "Direct IP communication is functional, indicating Layer 3 IP routing is alive.",
                Severity.INFO,
                "Direct IP pings succeeded while domain lookups timed out"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Inspect Third-Party Antivirus or Firewall",
                "Certain antivirus programs (e.g. Avast, McAfee, Norton) feature 'DNS Shields' that can block outbound UDP port 53 traffic when misconfigured.",
                null, null, false,
                "Third-party security suites often intercept port 53 sockets and drop packets if their background service stalls."));

        actions.add(new RecoveryAction(2,
                "Enable DNS over HTTPS (DoH) in Web Browser",
                "Open your browser settings (Chrome/Edge/Firefox), navigate to Privacy & Security -> Use Secure DNS -> Cloudflare (1.1.1.1).",
                null, null, false,
                "DoH encrypts DNS traffic inside standard HTTPS port 443, bypassing ISP or local port 53 UDP filters."));

        actions.add(new RecoveryAction(3,
                "Flush Local DNS Resolver Cache",
                "Execute 'ipconfig /flushdns' in Command Prompt to reset the Windows DNS Client resolver cache.",
                "ipconfig /flushdns", "FLUSH_DNS", false,
                "Cleans any corrupt entries from local host resolver memory."));

        return new DiagnosisOutcome(
                DiagnosisCategory.COMPLETE_DNS_FAILURE,
                "Total DNS Infrastructure Failure",
                "Your computer can reach the internet by raw IP address, but all domain name lookups (port 53) are blocked or failing.",
                ConfidenceLevel.HIGH,
                Severity.CRITICAL,
                evidence,
                actions
        );
    }
}
