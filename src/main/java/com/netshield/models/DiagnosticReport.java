package com.netshield.models;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregates all results from a diagnostic test run across every OSI/TCP-IP layer.
 */
public class DiagnosticReport {
    public static final String STAGE_INTERFACE = "Network Interfaces";
    public static final String STAGE_GATEWAY = "Default Gateway";
    public static final String STAGE_DNS = "DNS Resolution";
    public static final String STAGE_HTTP = "HTTPS Reachability";
    public static final String STAGE_LATENCY = "Latency & Packet Loss";
    public static final String STAGE_CAPTIVE = "Captive Portal Detection";

    private final String id;
    private final Instant timestamp;
    private final Map<String, StageResult> stageResults;

    public DiagnosticReport(Map<String, StageResult> stageResults) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.stageResults = stageResults != null ? Collections.unmodifiableMap(new LinkedHashMap<>(stageResults)) : Collections.emptyMap();
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, StageResult> getStageResults() {
        return stageResults;
    }

    public StageResult getStage(String name) {
        return stageResults.get(name);
    }

    public StageResult getInterfaceResult() {
        return stageResults.get(STAGE_INTERFACE);
    }

    public StageResult getGatewayResult() {
        return stageResults.get(STAGE_GATEWAY);
    }

    public StageResult getDnsResult() {
        return stageResults.get(STAGE_DNS);
    }

    public StageResult getHttpResult() {
        return stageResults.get(STAGE_HTTP);
    }

    public StageResult getLatencyResult() {
        return stageResults.get(STAGE_LATENCY);
    }

    public StageResult getCaptivePortalResult() {
        return stageResults.get(STAGE_CAPTIVE);
    }

    public boolean hasAnyFailure() {
        return stageResults.values().stream().anyMatch(s -> s.getStatus() == StageStatus.FAILED || s.getStatus() == StageStatus.TIMED_OUT);
    }

    public boolean allStagesSuccessful() {
        return !stageResults.isEmpty() && stageResults.values().stream().allMatch(s -> s.getStatus() == StageStatus.SUCCESS);
    }

    public double getAverageLatencyMs() {
        StageResult latency = getLatencyResult();
        if (latency != null && latency.getDetails().containsKey("avgLatencyMs")) {
            return (double) latency.getDetails().get("avgLatencyMs");
        }
        return -1.0;
    }

    public double getPacketLossPercentage() {
        StageResult latency = getLatencyResult();
        if (latency != null && latency.getDetails().containsKey("packetLossPercent")) {
            return (double) latency.getDetails().get("packetLossPercent");
        }
        return 0.0;
    }
}
