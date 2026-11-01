package com.netshield.models;

/**
 * Result record from a network bandwidth and speed test.
 */
public class SpeedTestResult {
    private final double downloadMbps;
    private final double uploadMbps;
    private final double pingLatencyMs;
    private final long totalBytesDownloaded;
    private final long totalBytesUploaded;
    private final long durationMs;
    private final boolean success;
    private final String error;

    public SpeedTestResult(double downloadMbps, double uploadMbps, double pingLatencyMs,
                           long totalBytesDownloaded, long totalBytesUploaded,
                           long durationMs, boolean success, String error) {
        this.downloadMbps = downloadMbps;
        this.uploadMbps = uploadMbps;
        this.pingLatencyMs = pingLatencyMs;
        this.totalBytesDownloaded = totalBytesDownloaded;
        this.totalBytesUploaded = totalBytesUploaded;
        this.durationMs = durationMs;
        this.success = success;
        this.error = error;
    }

    public static SpeedTestResult failed(String error) {
        return new SpeedTestResult(0, 0, -1, 0, 0, 0, false, error);
    }

    public double getDownloadMbps() {
        return downloadMbps;
    }

    public double getUploadMbps() {
        return uploadMbps;
    }

    public double getPingLatencyMs() {
        return pingLatencyMs;
    }

    public long getTotalBytesDownloaded() {
        return totalBytesDownloaded;
    }

    public long getTotalBytesUploaded() {
        return totalBytesUploaded;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
