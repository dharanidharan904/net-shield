package com.netshield.android.models;

public class StageResult {
    private final String stageName;
    private final boolean success;
    private final long durationMs;
    private final String message;

    public StageResult(String stageName, boolean success, long durationMs, String message) {
        this.stageName = stageName;
        this.success = success;
        this.durationMs = durationMs;
        this.message = message;
    }

    public String getStageName() { return stageName; }
    public boolean isSuccess() { return success; }
    public long getDurationMs() { return durationMs; }
    public String getMessage() { return message; }
}
