package com.netshield.diagnostics;

import com.netshield.models.StageResult;
import java.util.List;

/**
 * Diagnostic interface for testing HTTPS reachability, TCP handshakes, and SSL/TLS handshakes.
 */
public interface HttpDiagnostic {
    StageResult checkHttpReachability(List<String> targetUrls, int timeoutMs);
}
