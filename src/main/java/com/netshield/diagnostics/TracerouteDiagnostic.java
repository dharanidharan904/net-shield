package com.netshield.diagnostics;

import com.netshield.models.TracerouteReport;

/**
 * Diagnostic interface for performing hop-by-hop route path analysis and bottleneck detection.
 */
public interface TracerouteDiagnostic {
    TracerouteReport traceRoute(String targetHost, int maxHops, int timeoutMs);
}
