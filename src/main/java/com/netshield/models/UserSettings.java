package com.netshield.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User-configurable settings for test endpoints, timeouts, and monitoring.
 */
public class UserSettings {
    private List<String> dnsTestDomains;
    private List<String> directDnsServers;
    private List<String> httpsTargetUrls;
    private String captiveCheckUrl;
    private int timeoutMs;
    private int pingCount;
    private int continuousIntervalSec;
    private boolean soundAlertsEnabled;
    private boolean autoHealingEnabled;

    public UserSettings() {
        // Defaults
        this.dnsTestDomains = new ArrayList<>(Arrays.asList("google.com", "cloudflare.com", "wikipedia.org"));
        this.directDnsServers = new ArrayList<>(Arrays.asList("1.1.1.1", "8.8.8.8", "9.9.9.9"));
        this.httpsTargetUrls = new ArrayList<>(Arrays.asList("https://1.1.1.1", "https://www.google.com", "https://www.cloudflare.com"));
        this.captiveCheckUrl = "http://connectivitycheck.gstatic.com/generate_204";
        this.timeoutMs = 3000;
        this.pingCount = 5;
        this.continuousIntervalSec = 30;
        this.soundAlertsEnabled = true;
        this.autoHealingEnabled = false;
    }

    public List<String> getDnsTestDomains() {
        return dnsTestDomains;
    }

    public void setDnsTestDomains(List<String> dnsTestDomains) {
        this.dnsTestDomains = dnsTestDomains;
    }

    public List<String> getDirectDnsServers() {
        return directDnsServers;
    }

    public void setDirectDnsServers(List<String> directDnsServers) {
        this.directDnsServers = directDnsServers;
    }

    public List<String> getHttpsTargetUrls() {
        return httpsTargetUrls;
    }

    public void setHttpsTargetUrls(List<String> httpsTargetUrls) {
        this.httpsTargetUrls = httpsTargetUrls;
    }

    public String getCaptiveCheckUrl() {
        return captiveCheckUrl;
    }

    public void setCaptiveCheckUrl(String captiveCheckUrl) {
        this.captiveCheckUrl = captiveCheckUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = Math.max(500, Math.min(15000, timeoutMs));
    }

    public int getPingCount() {
        return pingCount;
    }

    public void setPingCount(int pingCount) {
        this.pingCount = Math.max(1, Math.min(20, pingCount));
    }

    public int getContinuousIntervalSec() {
        return continuousIntervalSec;
    }

    public void setContinuousIntervalSec(int continuousIntervalSec) {
        this.continuousIntervalSec = Math.max(5, Math.min(300, continuousIntervalSec));
    }

    public boolean isSoundAlertsEnabled() {
        return soundAlertsEnabled;
    }

    public void setSoundAlertsEnabled(boolean soundAlertsEnabled) {
        this.soundAlertsEnabled = soundAlertsEnabled;
    }

    public boolean isAutoHealingEnabled() {
        return autoHealingEnabled;
    }

    public void setAutoHealingEnabled(boolean autoHealingEnabled) {
        this.autoHealingEnabled = autoHealingEnabled;
    }
}
