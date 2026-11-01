package com.netshield.diagnostics;

import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Concrete diagnostic that probes standard captive portal endpoints (e.g. Google generate_204)
 * to detect Wi-Fi authentication / hotel login walls.
 */
public class RealCaptivePortalDiagnostic implements CaptivePortalDiagnostic {

    @Override
    public StageResult checkCaptivePortal(String probeUrl, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        String target = (probeUrl != null && !probeUrl.isBlank())
                ? probeUrl
                : "http://connectivitycheck.gstatic.com/generate_204";

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("probeUrl", target);

        HttpURLConnection conn = null;
        try {
            URL url = URI.create(target).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(false); // Do NOT follow redirects
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NetShield/1.0");

            int responseCode = conn.getResponseCode();
            long duration = System.currentTimeMillis() - startTime;
            details.put("responseCode", responseCode);

            if (responseCode == 204) {
                details.put("isCaptivePortal", false);
                return StageResult.success("Captive Portal Detection", duration,
                        "Direct internet access verified: HTTP 204 No Content received (no captive portal)",
                        details);
            }

            // HTTP 301/302/307/308 indicates redirection to a login portal
            if (responseCode == 301 || responseCode == 302 || responseCode == 307 || responseCode == 308) {
                String redirectLocation = conn.getHeaderField("Location");
                details.put("isCaptivePortal", true);
                details.put("portalUrl", redirectLocation != null ? redirectLocation : "Unknown URL");

                return new StageResult("Captive Portal Detection", StageStatus.FAILED, duration,
                        "Captive Portal Detected! Network traffic redirected to login page: " + redirectLocation,
                        details, "HTTP " + responseCode + " Redirect to Portal");
            }

            // If response is HTTP 200 with HTML, check if it's an intercepted captive page
            if (responseCode == 200) {
                StringBuilder bodySnippet = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null && count < 20) {
                        bodySnippet.append(line).append(" ");
                        count++;
                    }
                }
                String bodyStr = bodySnippet.toString().toLowerCase();
                boolean looksLikePortal = bodyStr.contains("<html") || bodyStr.contains("login") ||
                        bodyStr.contains("splash") || bodyStr.contains("guest");

                details.put("isCaptivePortal", looksLikePortal);
                details.put("bodySnippet", bodySnippet.toString().trim());

                if (looksLikePortal) {
                    return new StageResult("Captive Portal Detection", StageStatus.FAILED, duration,
                            "Captive Portal Interception: Probe returned HTML splash page instead of HTTP 204",
                            details, "HTTP 200 HTML Interception");
                }
            }

            // Other codes (e.g. 500, 403)
            return StageResult.inconclusive("Captive Portal Detection", duration,
                    "Inconclusive response from probe server: HTTP " + responseCode,
                    details);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            details.put("error", e.getMessage());
            return StageResult.inconclusive("Captive Portal Detection", duration,
                    "Could not complete captive portal check: " + e.getMessage(),
                    details);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
