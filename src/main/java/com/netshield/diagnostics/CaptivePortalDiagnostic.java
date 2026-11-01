package com.netshield.diagnostics;

import com.netshield.models.StageResult;

/**
 * Diagnostic interface for detecting captive portal interception (HTTP 302/307 login redirects).
 */
public interface CaptivePortalDiagnostic {
    StageResult checkCaptivePortal(String probeUrl, int timeoutMs);
}
