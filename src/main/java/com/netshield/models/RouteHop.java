package com.netshield.models;

/**
 * Model representing a single intermediate router hop in a network route trace.
 */
public class RouteHop {
    private final int hopNumber;
    private final String ipAddress;
    private final double rtt1Ms;
    private final double rtt2Ms;
    private final double rtt3Ms;
    private final double avgRttMs;
    private final boolean timedOut;
    private final boolean isBottleneck;

    public RouteHop(int hopNumber, String ipAddress, double rtt1Ms, double rtt2Ms, double rtt3Ms,
                    boolean timedOut, boolean isBottleneck) {
        this.hopNumber = hopNumber;
        this.ipAddress = ipAddress != null ? ipAddress : "*";
        this.rtt1Ms = rtt1Ms;
        this.rtt2Ms = rtt2Ms;
        this.rtt3Ms = rtt3Ms;
        this.timedOut = timedOut;
        this.isBottleneck = isBottleneck;

        int count = 0;
        double sum = 0;
        if (rtt1Ms >= 0) { sum += rtt1Ms; count++; }
        if (rtt2Ms >= 0) { sum += rtt2Ms; count++; }
        if (rtt3Ms >= 0) { sum += rtt3Ms; count++; }
        this.avgRttMs = count > 0 ? (sum / count) : -1.0;
    }

    public int getHopNumber() {
        return hopNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public double getRtt1Ms() {
        return rtt1Ms;
    }

    public double getRtt2Ms() {
        return rtt2Ms;
    }

    public double getRtt3Ms() {
        return rtt3Ms;
    }

    public double getAvgRttMs() {
        return avgRttMs;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public boolean isBottleneck() {
        return isBottleneck;
    }

    public String getDisplayLatency() {
        if (timedOut || avgRttMs < 0) return "* (Timed Out)";
        return String.format("%.1f ms", avgRttMs);
    }
}
