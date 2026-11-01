package com.netshield.models;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete hop-by-hop route path, bottleneck location, and destination reachability.
 */
public class TracerouteReport {
    private final String targetHost;
    private final List<RouteHop> hops;
    private final long executionTimeMs;
    private final boolean destinationReached;
    private final int bottleneckHop;
    private final String summary;

    public TracerouteReport(String targetHost, List<RouteHop> hops, long executionTimeMs,
                            boolean destinationReached, int bottleneckHop, String summary) {
        this.targetHost = targetHost;
        this.hops = hops != null ? Collections.unmodifiableList(hops) : Collections.emptyList();
        this.executionTimeMs = executionTimeMs;
        this.destinationReached = destinationReached;
        this.bottleneckHop = bottleneckHop;
        this.summary = summary != null ? summary : "";
    }

    public String getTargetHost() {
        return targetHost;
    }

    public List<RouteHop> getHops() {
        return hops;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public boolean isDestinationReached() {
        return destinationReached;
    }

    public int getBottleneckHop() {
        return bottleneckHop;
    }

    public String getSummary() {
        return summary;
    }
}
