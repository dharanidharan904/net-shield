package com.netshield.diagnosis;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.RecoveryAction;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnosis Rule: Fallback evaluator for ambiguous or unclassified network symptoms.
 */
public class InconclusiveRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 0; // Evaluated last
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        return true; // Always matches if no higher-priority rule claimed the report
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        List<DiagnosticEvidence> evidence = new ArrayList<>();

        for (StageResult stage : report.getStageResults().values()) {
            evidence.add(new DiagnosticEvidence(
                    stage.getStageName(),
                    stage.getMessage(),
                    stage.getStatus().getColorHex().equals("#10b981") ? Severity.HEALTHY : Severity.INFO,
                    "Status: " + stage.getStatus().getLabel()
            ));
        }

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Run a Repeat Diagnostic Scan",
                "Transient network fluctuations or momentary Wi-Fi handoffs can cause temporary test timeouts. Click 'Run Diagnostics' to re-test.",
                null, null, false,
                "Repeat measurements filter out sporadic packet loss."));

        actions.add(new RecoveryAction(2,
                "Inspect Windows Event Viewer for Network Stack Warnings",
                "Look for recent DHCP-Client, Netwtw (Intel Wi-Fi), or DNS Client Events in Windows System Logs.",
                "eventvwr.msc", null, false,
                "Windows logs hardware disconnects and driver reset events directly."));

        return new DiagnosisOutcome(
                DiagnosisCategory.INCONCLUSIVE,
                "Inconclusive Diagnostic Findings",
                "Diagnostic checks yielded mixed or ambiguous results. Some stages succeeded while others returned intermittent responses.",
                ConfidenceLevel.LOW,
                Severity.INFO,
                evidence,
                actions
        );
    }
}
