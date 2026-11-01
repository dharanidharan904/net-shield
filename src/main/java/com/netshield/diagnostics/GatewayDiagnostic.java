package com.netshield.diagnostics;

import com.netshield.models.StageResult;

/**
 * Diagnostic interface for discovering and verifying default gateway reachability.
 */
public interface GatewayDiagnostic {
    StageResult checkGateway(String customGatewayIp, int timeoutMs);
}
