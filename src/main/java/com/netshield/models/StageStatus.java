package com.netshield.models;

/**
 * Execution status for individual diagnostic check stages.
 * Fulfills requirement: distinguish failed, timed-out, unsupported, and inconclusive checks.
 */
public enum StageStatus {
    SUCCESS("Operational", "#10b981"),
    FAILED("Failed", "#ef4444"),
    TIMED_OUT("Timed Out", "#f59e0b"),
    INCONCLUSIVE("Inconclusive", "#8b5cf6"),
    UNSUPPORTED("Unsupported", "#64748b"),
    RUNNING("Testing...", "#06b6d4"),
    SKIPPED("Skipped", "#475569");

    private final String label;
    private final String colorHex;

    StageStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getColorHex() {
        return colorHex;
    }
}
