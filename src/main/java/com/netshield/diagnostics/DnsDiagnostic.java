package com.netshield.diagnostics;

import com.netshield.models.StageResult;
import java.util.List;

/**
 * Diagnostic interface for testing system DNS resolver and direct recursive DNS servers.
 */
public interface DnsDiagnostic {
    StageResult checkDns(List<String> domains, List<String> directDnsServers, int timeoutMs);
}
