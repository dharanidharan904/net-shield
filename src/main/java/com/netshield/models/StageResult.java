package com.netshield.models;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result record from a single network diagnostic stage.
 */
public class StageResult {
    private final String stageName;
    private final StageStatus status;
    private final long durationMs;
    private final String message;
    private final Map<String, Object> details;
    private final Instant executedAt;
    private final String errorDetail;

    public StageResult(String stageName, StageStatus status, long durationMs,
                       String message, Map<String, Object> details, String errorDetail) {
        this.stageName = stageName;
        this.status = status;
        this.durationMs = durationMs;
        this.message = message != null ? message : "";
        this.details = details != null ? Collections.unmodifiableMap(new LinkedHashMap<>(details)) : Collections.emptyMap();
        this.executedAt = Instant.now();
        this.errorDetail = errorDetail;
    }

    public static StageResult success(String stageName, long durationMs, String message, Map<String, Object> details) {
        return new StageResult(stageName, StageStatus.SUCCESS, durationMs, message, details, null);
    }

    public static StageResult failed(String stageName, long durationMs, String message, Map<String, Object> details, String error) {
        return new StageResult(stageName, StageStatus.FAILED, durationMs, message, details, error);
    }

    public static StageResult timedOut(String stageName, long durationMs, String message, Map<String, Object> details) {
        return new StageResult(stageName, StageStatus.TIMED_OUT, durationMs, message, details, "Connection or lookup timed out");
    }

    public static StageResult inconclusive(String stageName, long durationMs, String message, Map<String, Object> details) {
        return new StageResult(stageName, StageStatus.INCONCLUSIVE, durationMs, message, details, null);
    }

    public static StageResult unsupported(String stageName, String reason) {
        return new StageResult(stageName, StageStatus.UNSUPPORTED, 0, reason, Collections.emptyMap(), reason);
    }

    public static StageResult running(String stageName) {
        return new StageResult(stageName, StageStatus.RUNNING, 0, "Diagnostic probe running...", Collections.emptyMap(), null);
    }

    public String getStageName() {
        return stageName;
    }

    public StageStatus getStatus() {
        return status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key, T defaultValue) {
        Object val = details.get(key);
        if (val == null) return defaultValue;
        try {
            return (T) val;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}
