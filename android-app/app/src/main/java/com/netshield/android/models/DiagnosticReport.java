package com.netshield.android.models;

import java.util.Map;

public class DiagnosticReport {
    private final Map<String, StageResult> stageResults;
    private final String ssid;
    private final int signalPercent;
    private final String band;
    private final int linkSpeedMbps;

    public DiagnosticReport(Map<String, StageResult> stageResults, String ssid, int signalPercent, String band, int linkSpeedMbps) {
        this.stageResults = stageResults;
        this.ssid = ssid;
        this.signalPercent = signalPercent;
        this.band = band;
        this.linkSpeedMbps = linkSpeedMbps;
    }

    public StageResult getStage(String name) {
        return stageResults.get(name);
    }

    public Map<String, StageResult> getStageResults() {
        return stageResults;
    }

    public String getSsid() { return ssid; }
    public int getSignalPercent() { return signalPercent; }
    public String getBand() { return band; }
    public int getLinkSpeedMbps() { return linkSpeedMbps; }

    public boolean isFullyHealthy() {
        for (StageResult s : stageResults.values()) {
            if (!s.isSuccess()) return false;
        }
        return true;
    }
}
