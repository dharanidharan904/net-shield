package com.netshield.diagnostics;

import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import com.netshield.util.PlatformUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Concrete diagnostic for discovering and verifying default gateway reachability.
 */
public class RealGatewayDiagnostic implements GatewayDiagnostic {

    @Override
    public StageResult checkGateway(String customGatewayIp, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        String gatewayIp = (customGatewayIp != null && !customGatewayIp.isBlank())
                ? customGatewayIp.trim()
                : PlatformUtils.detectDefaultGatewayWindows();

        Map<String, Object> details = new LinkedHashMap<>();

        if (gatewayIp == null || gatewayIp.isBlank()) {
            long duration = System.currentTimeMillis() - startTime;
            details.put("gatewayIp", "Unknown / Not Detected");
            return StageResult.failed("Default Gateway", duration,
                    "No default gateway address found in system routing table",
                    details, "Missing default gateway configuration");
        }

        details.put("gatewayIp", gatewayIp);

        try {
            InetAddress addr = InetAddress.getByName(gatewayIp);
            long pingStart = System.currentTimeMillis();
            boolean reachable = addr.isReachable(timeoutMs);
            long rtt = System.currentTimeMillis() - pingStart;

            // Many routers drop ICMP echo packets for security. If ICMP fails, probe TCP port 80/443/53.
            if (!reachable) {
                long socketPingStart = System.currentTimeMillis();
                reachable = probeGatewaySocket(gatewayIp, timeoutMs);
                if (reachable) {
                    rtt = System.currentTimeMillis() - socketPingStart;
                    details.put("probeMethod", "TCP Socket Handshake (Router Web/DNS Port)");
                } else {
                    details.put("probeMethod", "ICMP Echo & TCP Probes");
                }
            } else {
                details.put("probeMethod", "ICMP Echo Ping");
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            details.put("latencyMs", reachable ? rtt : -1);

            if (reachable) {
                return StageResult.success("Default Gateway", totalDuration,
                        "Default Gateway (" + gatewayIp + ") is reachable in " + rtt + " ms",
                        details);
            } else {
                return StageResult.timedOut("Default Gateway", totalDuration,
                        "Default Gateway (" + gatewayIp + ") did not respond within " + timeoutMs + " ms",
                        details);
            }

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            details.put("error", e.getMessage());
            return StageResult.failed("Default Gateway", totalDuration,
                    "Error reaching gateway " + gatewayIp + ": " + e.getMessage(),
                    details, e.getMessage());
        }
    }

    private boolean probeGatewaySocket(String ip, int timeoutMs) {
        int[] ports = {80, 443, 53, 8080};
        for (int port : ports) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), Math.min(timeoutMs, 1000));
                return true;
            } catch (Exception ignored) {
                // Try next port
            }
        }
        return false;
    }
}
