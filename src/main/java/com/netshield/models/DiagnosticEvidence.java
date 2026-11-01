package com.netshield.models;

/**
 * Concrete factual observation discovered by diagnostics that supports a diagnosis rule.
 */
public class DiagnosticEvidence {
    private final String stageSource;
    private final String observation;
    private final Severity severity;
    private final String technicalDetail;

    public DiagnosticEvidence(String stageSource, String observation, Severity severity, String technicalDetail) {
        this.stageSource = stageSource;
        this.observation = observation;
        this.severity = severity;
        this.technicalDetail = technicalDetail;
    }

    public String getStageSource() {
        return stageSource;
    }

    public String getObservation() {
        return observation;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTechnicalDetail() {
        return technicalDetail;
    }

    @Override
    public String toString() {
        return "[" + stageSource + "] " + observation + " (" + technicalDetail + ")";
    }
}
