package com.netshield.diagnosis;

import com.netshield.models.DiagnosticReport;
import com.netshield.models.DiagnosisOutcome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Rule Engine that processes diagnostic test reports against prioritized rules
 * to determine the root cause, confidence level, evidence chain, and recovery actions.
 */
public class DiagnosisEngine {

    private final List<DiagnosisRule> rules;

    public DiagnosisEngine() {
        this(defaultRules());
    }

    public DiagnosisEngine(List<DiagnosisRule> customRules) {
        List<DiagnosisRule> sorted = new ArrayList<>(customRules);
        sorted.sort(Comparator.comparingInt(DiagnosisRule::getPriority).reversed());
        this.rules = Collections.unmodifiableList(sorted);
    }

    public static List<DiagnosisRule> defaultRules() {
        List<DiagnosisRule> list = new ArrayList<>();
        list.add(new NoActiveAdapterRule());
        list.add(new ApipaDhcpFailureRule());
        list.add(new CaptivePortalRule());
        list.add(new GatewayUnreachableRule());
        list.add(new LocalDnsFailureRule());
        list.add(new CompleteDnsFailureRule());
        list.add(new IspOutageRule());
        list.add(new HighLatencyPacketLossRule());
        list.add(new PartialConnectivityRule());
        list.add(new HealthyNetworkRule());
        list.add(new InconclusiveRule());
        return list;
    }

    /**
     * Evaluates a report against the rule set and returns the diagnosis outcome.
     */
    public DiagnosisOutcome diagnose(DiagnosticReport report) {
        if (report == null) {
            throw new IllegalArgumentException("Diagnostic report cannot be null");
        }

        for (DiagnosisRule rule : rules) {
            if (rule.matches(report)) {
                return rule.evaluate(report);
            }
        }

        // Safety fallback if no rules matched
        return new InconclusiveRule().evaluate(report);
    }

    public List<DiagnosisRule> getRules() {
        return rules;
    }
}
