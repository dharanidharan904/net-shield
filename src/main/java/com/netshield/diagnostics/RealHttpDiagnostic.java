package com.netshield.diagnostics;

import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/**
 * Concrete diagnostic that checks HTTPS reachability, TCP/TLS handshake, and HTTP response codes.
 */
public class RealHttpDiagnostic implements HttpDiagnostic {

    @Override
    public StageResult checkHttpReachability(List<String> targetUrls, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> details = new LinkedHashMap<>();

        List<String> urls = (targetUrls != null && !targetUrls.isEmpty())
                ? targetUrls
                : List.of("https://1.1.1.1", "https://www.google.com", "https://www.cloudflare.com");

        int successCount = 0;
        long totalLatency = 0;
        List<String> endpointResults = new ArrayList<>();
        boolean sslErrorDetected = false;

        for (String targetUrl : urls) {
            long reqStart = System.currentTimeMillis();
            HttpURLConnection conn = null;
            try {
                URL url = URI.create(targetUrl).toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(timeoutMs);
                conn.setReadTimeout(timeoutMs);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "NetShield-Diagnostic-Tool/1.0");

                int code = conn.getResponseCode();
                long latency = System.currentTimeMillis() - reqStart;

                // Any HTTP response (2xx, 3xx, 4xx) indicates reachability at the transport & application layers
                if (code > 0) {
                    successCount++;
                    totalLatency += latency;
                    endpointResults.add(targetUrl + " -> HTTP " + code + " (" + latency + "ms)");
                }
            } catch (javax.net.ssl.SSLException e) {
                sslErrorDetected = true;
                endpointResults.add(targetUrl + " -> SSL/TLS Handshake Error: " + e.getMessage());
            } catch (Exception e) {
                endpointResults.add(targetUrl + " -> Connection Failed: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        boolean allFailed = successCount == 0;
        double avgLatency = successCount > 0 ? (double) totalLatency / successCount : -1;

        details.put("totalEndpointsTested", urls.size());
        details.put("successfulEndpoints", successCount);
        details.put("avgHttpLatencyMs", avgLatency);
        details.put("sslErrorDetected", sslErrorDetected);
        details.put("endpointDetails", endpointResults);

        if (successCount == urls.size()) {
            return StageResult.success("HTTPS Reachability", duration,
                    "All " + successCount + " HTTPS endpoints reachable (avg " + String.format("%.1f", avgLatency) + " ms)",
                    details);
        } else if (successCount > 0) {
            return StageResult.inconclusive("HTTPS Reachability", duration,
                    "Partial HTTPS reachability: " + successCount + "/" + urls.size() + " endpoints responded (avg " + String.format("%.1f", avgLatency) + " ms)",
                    details);
        } else if (sslErrorDetected) {
            return StageResult.failed("HTTPS Reachability", duration,
                    "HTTPS failed due to SSL/TLS handshake errors (possible captive portal or clock skew)",
                    details, "SSL/TLS handshake failure");
        } else {
            return StageResult.failed("HTTPS Reachability", duration,
                    "Outage: None of the tested HTTPS endpoints responded within " + timeoutMs + " ms",
                    details, "HTTPS connection timed out or refused across all endpoints");
        }
    }
}
