package com.netshield.models;

/**
 * Diagnostic confidence rating indicating uncertainty level of root cause diagnosis.
 */
public enum ConfidenceLevel {
    CERTAIN("Certain (100%)", "#10b981", 1.0),
    HIGH("High Confidence (>85%)", "#3b82f6", 0.85),
    MEDIUM("Medium Confidence (60-85%)", "#f59e0b", 0.65),
    LOW("Low Confidence (<60%)", "#ef4444", 0.40);

    private final String displayLabel;
    private final String colorHex;
    private final double score;

    ConfidenceLevel(String displayLabel, String colorHex, double score) {
        this.displayLabel = displayLabel;
        this.colorHex = colorHex;
        this.score = score;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getColorHex() {
        return colorHex;
    }

    public double getScore() {
        return score;
    }
}
