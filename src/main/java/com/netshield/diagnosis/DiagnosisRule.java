package com.netshield.diagnosis;

import com.netshield.models.DiagnosticReport;
import com.netshield.models.DiagnosisOutcome;

/**
 * Strategy interface for rule-based root cause identification.
 */
public interface DiagnosisRule {
    /**
     * Priority of the rule. Higher priority rules are evaluated first.
     */
    int getPriority();

    /**
     * Checks if this rule matches the diagnostic report findings.
     */
    boolean matches(DiagnosticReport report);

    /**
     * Synthesizes diagnosis outcome, concrete evidence, and safe recovery steps.
     */
    DiagnosisOutcome evaluate(DiagnosticReport report);
}
