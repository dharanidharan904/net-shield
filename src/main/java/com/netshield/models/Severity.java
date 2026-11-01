package com.netshield.models;

/**
 * Diagnostic impact severity.
 */
public enum Severity {
    HEALTHY("Healthy", "#10b981"),
    INFO("Notice", "#3b82f6"),
    WARNING("Degraded", "#f59e0b"),
    CRITICAL("Outage", "#ef4444");

    private final String label;
    private final String colorHex;

    Severity(String label, String colorHex) {
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
