package com.netshield.diagnostics;

import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import com.netshield.util.SafeCommandRunner;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete diagnostic that measures RTT latency samples, packet loss percentage, and jitter.
 */
public class RealLatencyDiagnostic implements LatencyDiagnostic {
    private static final SafeCommandRunner COMMAND_RUNNER = new SafeCommandRunner();
    private static final Pattern TIME_PATTERN = Pattern.compile("time[=<]([0-9]+)ms", Pattern.CASE_INSENSITIVE);

    @Override
    public StageResult measureLatencyAndLoss(String targetHost, int pingCount, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        String host = (targetHost != null && !targetHost.isBlank()) ? targetHost : "1.1.1.1";
        int samples = Math.max(1, Math.min(20, pingCount));
        int perPingTimeout = Math.min(timeoutMs, 2000);

        List<Double> rttSamples = new ArrayList<>();
        int lostPackets = 0;

        // Run N ping probes
        for (int i = 0; i < samples; i++) {
            Double rtt = pingWindows(host, perPingTimeout);
            if (rtt != null) {
                rttSamples.add(rtt);
            } else {
                // Try fallback socket probe if ICMP is blocked
                Double socketRtt = probeSocketLatency(host, perPingTimeout);
                if (socketRtt != null) {
                    rttSamples.add(socketRtt);
                } else {
                    lostPackets++;
                }
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("targetHost", host);
        details.put("totalSamples", samples);
        details.put("receivedSamples", rttSamples.size());
        details.put("lostSamples", lostPackets);

        double packetLossPercent = ((double) lostPackets / samples) * 100.0;
        details.put("packetLossPercent", packetLossPercent);

        if (rttSamples.isEmpty()) {
            details.put("avgLatencyMs", -1.0);
            details.put("minLatencyMs", -1.0);
            details.put("maxLatencyMs", -1.0);
            details.put("jitterMs", 0.0);

            return StageResult.failed("Latency & Packet Loss", totalDuration,
                    "100% Packet Loss: Target host " + host + " failed to respond to " + samples + " consecutive pings",
                    details, "100% packet loss");
        }

        double min = rttSamples.stream().min(Double::compare).orElse(0.0);
        double max = rttSamples.stream().max(Double::compare).orElse(0.0);
        double avg = rttSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Jitter calculation: average absolute difference between consecutive ping samples
        double totalDiff = 0.0;
        int diffCount = 0;
        for (int i = 1; i < rttSamples.size(); i++) {
            totalDiff += Math.abs(rttSamples.get(i) - rttSamples.get(i - 1));
            diffCount++;
        }
        double jitter = diffCount > 0 ? (totalDiff / diffCount) : 0.0;

        details.put("minLatencyMs", min);
        details.put("maxLatencyMs", max);
        details.put("avgLatencyMs", avg);
        details.put("jitterMs", jitter);
        details.put("rttHistory", rttSamples);

        if (packetLossPercent == 0.0 && avg < 80.0 && jitter < 30.0) {
            return StageResult.success("Latency & Packet Loss", totalDuration,
                    String.format("Pristine link: %.1f ms avg latency (min %.1f / max %.1f), 0%% loss, %.1f ms jitter",
                            avg, min, max, jitter),
                    details);
        } else if (packetLossPercent > 30.0 || avg > 250.0 || jitter > 80.0) {
            return StageResult.inconclusive("Latency & Packet Loss", totalDuration,
                    String.format("Degraded quality: %.1f ms avg latency, %.0f%% packet loss, %.1f ms jitter",
                            avg, packetLossPercent, jitter),
                    details);
        } else {
            return StageResult.success("Latency & Packet Loss", totalDuration,
                    String.format("Acceptable link: %.1f ms avg latency, %.0f%% loss, %.1f ms jitter",
                            avg, packetLossPercent, jitter),
                    details);
        }
    }

    private Double pingWindows(String host, int timeoutMs) {
        try {
            SafeCommandRunner.CommandResult res = COMMAND_RUNNER.execute(
                    Arrays.asList("ping", "-n", "1", "-w", String.valueOf(timeoutMs), host), 3
            );
            if (res.isSuccess()) {
                Matcher matcher = TIME_PATTERN.matcher(res.getRawOutput());
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Double probeSocketLatency(String host, int timeoutMs) {
        int[] ports = {53, 443, 80};
        for (int port : ports) {
            long start = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                return (double) (System.currentTimeMillis() - start);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
