package com.netshield.models;

/**
 * Encapsulates Wi-Fi radio parameters, link speeds, channel, and signal quality metrics.
 */
public class WifiNetworkInfo {
    private final boolean connected;
    private final String interfaceName;
    private final String ssid;
    private final String bssid;
    private final String radioType;
    private final String band;
    private final int channel;
    private final double receiveRateMbps;
    private final double transmitRateMbps;
    private final int signalPercent;
    private final int signalDbm;
    private final String qualityRating;
    private final boolean congested24Ghz;

    public WifiNetworkInfo(boolean connected, String interfaceName, String ssid, String bssid,
                           String radioType, String band, int channel,
                           double receiveRateMbps, double transmitRateMbps, int signalPercent) {
        this.connected = connected;
        this.interfaceName = interfaceName != null ? interfaceName : "N/A";
        this.ssid = ssid != null ? ssid : "Disconnected";
        this.bssid = bssid != null ? bssid : "N/A";
        this.radioType = radioType != null ? radioType : "N/A";
        this.band = band != null ? band : (channel > 14 ? "5 GHz" : "2.4 GHz");
        this.channel = channel;
        this.receiveRateMbps = receiveRateMbps;
        this.transmitRateMbps = transmitRateMbps;
        this.signalPercent = Math.max(0, Math.min(100, signalPercent));
        this.signalDbm = (this.signalPercent / 2) - 100;

        // Quality rating classification
        if (!connected) {
            this.qualityRating = "DISCONNECTED";
            this.congested24Ghz = false;
        } else if (this.signalPercent >= 80) {
            this.qualityRating = "EXCELLENT";
            this.congested24Ghz = "2.4 GHz".equals(this.band);
        } else if (this.signalPercent >= 60) {
            this.qualityRating = "GOOD";
            this.congested24Ghz = "2.4 GHz".equals(this.band);
        } else if (this.signalPercent >= 40) {
            this.qualityRating = "FAIR";
            this.congested24Ghz = true;
        } else {
            this.qualityRating = "POOR";
            this.congested24Ghz = true;
        }
    }

    public static WifiNetworkInfo disconnected() {
        return new WifiNetworkInfo(false, "Wi-Fi", "Disconnected", null, "N/A", "N/A", 0, 0, 0, 0);
    }

    public boolean isConnected() {
        return connected;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getSsid() {
        return ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public String getRadioType() {
        return radioType;
    }

    public String getBand() {
        return band;
    }

    public int getChannel() {
        return channel;
    }

    public double getReceiveRateMbps() {
        return receiveRateMbps;
    }

    public double getTransmitRateMbps() {
        return transmitRateMbps;
    }

    public int getSignalPercent() {
        return signalPercent;
    }

    public int getSignalDbm() {
        return signalDbm;
    }

    public String getQualityRating() {
        return qualityRating;
    }

    public boolean isCongested24Ghz() {
        return congested24Ghz;
    }

    public String getSignalBadgeColor() {
        return switch (qualityRating) {
            case "EXCELLENT" -> "#10b981";
            case "GOOD" -> "#3b82f6";
            case "FAIR" -> "#f59e0b";
            case "POOR", "DISCONNECTED" -> "#ef4444";
            default -> "#64748b";
        };
    }
}
