package com.netshield.diagnostics;

import com.netshield.models.StageResult;

/**
 * Diagnostic interface for measuring RTT latency, jitter, and packet loss rate.
 */
public interface LatencyDiagnostic {
    StageResult measureLatencyAndLoss(String targetHost, int pingCount, int timeoutMs);
}
