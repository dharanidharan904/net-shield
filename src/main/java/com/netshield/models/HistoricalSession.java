package com.netshield.models;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Historical record of a diagnostic scan stored in local SQLite database.
 */
public class HistoricalSession {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final long id;
    private final String sessionUuid;
    private final Instant timestamp;
    private final DiagnosisCategory category;
    private final Severity severity;
    private final ConfidenceLevel confidence;
    private final String summary;
    private final double gatewayLatencyMs;
    private final double dnsLatencyMs;
    private final double httpsLatencyMs;
    private final double packetLossPercent;
    private final double jitterMs;

    public HistoricalSession(long id, String sessionUuid, Instant timestamp,
                             DiagnosisCategory category, Severity severity,
                             ConfidenceLevel confidence, String summary,
                             double gatewayLatencyMs, double dnsLatencyMs,
                             double httpsLatencyMs, double packetLossPercent, double jitterMs) {
        this.id = id;
        this.sessionUuid = sessionUuid;
        this.timestamp = timestamp;
        this.category = category;
        this.severity = severity;
        this.confidence = confidence;
        this.summary = summary;
        this.gatewayLatencyMs = gatewayLatencyMs;
        this.dnsLatencyMs = dnsLatencyMs;
        this.httpsLatencyMs = httpsLatencyMs;
        this.packetLossPercent = packetLossPercent;
        this.jitterMs = jitterMs;
    }

    public long getId() {
        return id;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return FORMATTER.format(timestamp);
    }

    public DiagnosisCategory getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public ConfidenceLevel getConfidence() {
        return confidence;
    }

    public String getSummary() {
        return summary;
    }

    public double getGatewayLatencyMs() {
        return gatewayLatencyMs;
    }

    public double getDnsLatencyMs() {
        return dnsLatencyMs;
    }

    public double getHttpsLatencyMs() {
        return httpsLatencyMs;
    }

    public double getPacketLossPercent() {
        return packetLossPercent;
    }

    public double getJitterMs() {
        return jitterMs;
    }
}
