package com.netshield.diagnostics;

import com.netshield.models.RouteHop;
import com.netshield.models.TracerouteReport;
import com.netshield.util.SafeCommandRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete diagnostic for executing and analyzing Windows tracert route paths.
 */
public class RealTracerouteDiagnostic implements TracerouteDiagnostic {
    private static final SafeCommandRunner RUNNER = new SafeCommandRunner();
    private static final Pattern HOP_LINE_PATTERN = Pattern.compile("^\\s*([0-9]+)\\s+(?:(<?[0-9]+)\\s*ms|\\*)\\s+(?:(<?[0-9]+)\\s*ms|\\*)\\s+(?:(<?[0-9]+)\\s*ms|\\*)\\s+([0-9a-fA-F.:]+|Request timed out\\.?.*)", Pattern.CASE_INSENSITIVE);

    @Override
    public TracerouteReport traceRoute(String targetHost, int maxHops, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        String target = (targetHost != null && !targetHost.isBlank()) ? targetHost.trim() : "1.1.1.1";
        int hopsLimit = Math.max(1, Math.min(30, maxHops));
        int waitMs = Math.max(250, Math.min(2000, timeoutMs));

        try {
            SafeCommandRunner.CommandResult res = RUNNER.execute(
                    Arrays.asList("tracert", "-d", "-h", String.valueOf(hopsLimit), "-w", String.valueOf(waitMs), target),
                    35 // Maximum 35 seconds overall timeout
            );
            return parseTracertOutput(target, res.getRawOutput(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            return new TracerouteReport(target, List.of(), System.currentTimeMillis() - startTime,
                    false, -1, "Traceroute failed: " + e.getMessage());
        }
    }

    /**
     * Parses raw tracert output into structured RouteHop objects.
     * Package-private for offline unit testing.
     */
    public TracerouteReport parseTracertOutput(String targetHost, String rawOutput, long executionTimeMs) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return new TracerouteReport(targetHost, List.of(), executionTimeMs, false, -1, "No traceroute output received.");
        }

        List<RouteHop> hops = new ArrayList<>();
        String[] lines = rawOutput.split("\r?\n");
        boolean reached = false;
        double previousRtt = -1;
        int bottleneckHop = -1;
        double maxJump = 0;

        for (String line : lines) {
            Matcher m = HOP_LINE_PATTERN.matcher(line);
            if (m.find()) {
                int hopNum = Integer.parseInt(m.group(1));

                double r1 = parseRtt(m.group(2));
                double r2 = parseRtt(m.group(3));
                double r3 = parseRtt(m.group(4));

                String targetOrTimeout = m.group(5).trim();
                boolean timedOut = targetOrTimeout.toLowerCase().contains("timed out") && (r1 < 0 && r2 < 0 && r3 < 0);
                String ip = timedOut ? "*" : targetOrTimeout;

                // Check for destination reach
                if (!timedOut && (ip.equalsIgnoreCase(targetHost) || ip.contains(targetHost))) {
                    reached = true;
                }

                // Check for latency spike / bottleneck
                double currentAvg = averageValid(r1, r2, r3);
                boolean isBottleneck = false;
                if (currentAvg > 0 && previousRtt > 0) {
                    double delta = currentAvg - previousRtt;
                    if (delta > 40.0 && delta > maxJump) {
                        maxJump = delta;
                        bottleneckHop = hopNum;
                        isBottleneck = true;
                    }
                }
                if (currentAvg > 0) {
                    previousRtt = currentAvg;
                }

                hops.add(new RouteHop(hopNum, ip, r1, r2, r3, timedOut, isBottleneck));
            }
        }

        String summary;
        if (hops.isEmpty()) {
            summary = "Traceroute completed with 0 hops detected.";
        } else if (reached) {
            summary = String.format("Destination %s reached in %d hops (total trace time: %.1fs).",
                    targetHost, hops.size(), executionTimeMs / 1000.0);
        } else {
            summary = String.format("Route to %s stalled or timed out at hop %d.",
                    targetHost, hops.get(hops.size() - 1).getHopNumber());
        }

        return new TracerouteReport(targetHost, hops, executionTimeMs, reached, bottleneckHop, summary);
    }

    private double parseRtt(String str) {
        if (str == null || str.equals("*")) return -1.0;
        try {
            String clean = str.replace("<", "").trim();
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private double averageValid(double... vals) {
        double sum = 0;
        int count = 0;
        for (double v : vals) {
            if (v >= 0) {
                sum += v;
                count++;
            }
        }
        return count > 0 ? (sum / count) : -1.0;
    }
}
