package com.netshield.diagnostics;

import com.netshield.models.StageResult;

/**
 * Diagnostic interface for inspecting physical and logical network adapters.
 */
public interface InterfaceDiagnostic {
    StageResult checkInterfaces(int timeoutMs);
}
