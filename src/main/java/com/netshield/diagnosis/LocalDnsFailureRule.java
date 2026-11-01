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
 * Diagnosis Rule: Detects when local system/router DNS resolver fails while direct upstream DNS is alive.
 */
public class LocalDnsFailureRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 70;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult dns = report.getDnsResult();
        StageResult gateway = report.getGatewayResult();
        if (dns == null || gateway == null) return false;

        boolean gatewayOk = gateway.getStatus() == StageStatus.SUCCESS;
        boolean systemDnsFailed = !dns.getDetail("systemDnsSuccess", true);
        boolean directDnsOk = dns.getDetail("directDnsSuccess", false);

        return gatewayOk && systemDnsFailed && directDnsOk;
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult dns = report.getDnsResult();

        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_DNS,
                "System DNS resolver failed, but direct public DNS queries to 1.1.1.1 / 8.8.8.8 succeeded.",
                Severity.WARNING,
                dns != null ? dns.getMessage() : "Local resolver failure"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Flush Local Windows DNS Resolver Cache",
                "Clear corrupted DNS resolver entries using the safe 'ipconfig /flushdns' utility.",
                "ipconfig /flushdns", "FLUSH_DNS", false,
                "Removes stale or poisoned DNS lookup records cached in Windows RAM."));

        actions.add(new RecoveryAction(2,
                "Configure Static Public DNS (Cloudflare / Google)",
                "Open Windows Network Connections, select your adapter properties -> IPv4 -> Use DNS: 1.1.1.1 (Primary) and 8.8.8.8 (Secondary).",
                null, "OPEN_NETWORK_CONNECTIONS", false,
                "Bypasses buggy or unresponsive DNS caching daemons on consumer ISP routers."));

        actions.add(new RecoveryAction(3,
                "Test Upstream DNS Resolution via Nslookup",
                "Run 'nslookup google.com 1.1.1.1' in Command Prompt to verify external name servers directly.",
                "nslookup google.com 1.1.1.1", null, false,
                "Confirms whether port 53 UDP packets are successfully traversing your router to public DNS."));

        return new DiagnosisOutcome(
                DiagnosisCategory.LOCAL_DNS_FAILURE,
                "Local Router DNS Resolution Failure",
                "Your local router or ISP DNS forwarder is unresponsive, but public DNS servers (1.1.1.1) are operational.",
                ConfidenceLevel.HIGH,
                Severity.WARNING,
                evidence,
                actions
        );
    }
}
