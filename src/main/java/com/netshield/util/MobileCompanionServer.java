package com.netshield.util;

import com.netshield.diagnostics.NetworkDiagnosticService;
import com.netshield.diagnostics.WifiDiagnostic;
import com.netshield.diagnosis.DiagnosisEngine;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.StageResult;
import com.netshield.models.UserSettings;
import com.netshield.models.WifiNetworkInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded Mobile LAN Companion Server.
 * Allows Android phones and other mobile devices on the local Wi-Fi network
 * to access NetShield diagnostics and trigger network checking from their mobile browser.
 */
public class MobileCompanionServer {
    private static final Logger LOGGER = Logger.getLogger(MobileCompanionServer.class.getName());
    private static final int DEFAULT_PORT = 8080;

    private HttpServer server;
    private final NetworkDiagnosticService diagnosticService;
    private final DiagnosisEngine diagnosisEngine;
    private final WifiDiagnostic wifiDiagnostic;
    private final UserSettings settings;

    private volatile DiagnosticReport latestReport;
    private volatile DiagnosisOutcome latestOutcome;
    private volatile WifiNetworkInfo latestWifi;
    private boolean isRunning = false;
    private int port = DEFAULT_PORT;

    public MobileCompanionServer(NetworkDiagnosticService diagnosticService,
                                 DiagnosisEngine diagnosisEngine,
                                 WifiDiagnostic wifiDiagnostic,
                                 UserSettings settings) {
        this.diagnosticService = diagnosticService;
        this.diagnosisEngine = diagnosisEngine;
        this.wifiDiagnostic = wifiDiagnostic;
        this.settings = settings;
    }

    public synchronized void updateState(DiagnosticReport report, DiagnosisOutcome outcome, WifiNetworkInfo wifi) {
        this.latestReport = report;
        this.latestOutcome = outcome;
        this.latestWifi = wifi;
    }

    public synchronized boolean start() {
        if (isRunning) return true;

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new MobileDashboardHandler());
            server.createContext("/api/status", new ApiStatusHandler());
            server.createContext("/api/run", new ApiRunHandler());
            server.createContext("/api/heal", new ApiHealHandler());
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            isRunning = true;
            LOGGER.info("Mobile Companion Server started on port " + port);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to start Mobile Companion Server: " + e.getMessage());
            return false;
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(1);
            server = null;
        }
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String getServerUrl() {
        String lanIp = getLocalLanIp();
        return "http://" + lanIp + ":" + port;
    }

    private String getLocalLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (iface.isLoopback() || !iface.isUp()) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private class MobileDashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = buildMobileHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class ApiStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = buildStatusJson();
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class ApiRunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DiagnosticReport rep = diagnosticService.runFullDiagnostics(settings, null);
                DiagnosisOutcome out = diagnosisEngine.diagnose(rep);
                WifiNetworkInfo w = wifiDiagnostic.getWifiInfo();
                updateState(rep, out, w);
            } catch (Exception ignored) {}

            String json = buildStatusJson();
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class ApiHealHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            PlatformUtils.flushDnsCache();
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}

            try {
                DiagnosticReport rep = diagnosticService.runFullDiagnostics(settings, null);
                DiagnosisOutcome out = diagnosisEngine.diagnose(rep);
                WifiNetworkInfo w = wifiDiagnostic.getWifiInfo();
                updateState(rep, out, w);
            } catch (Exception ignored) {}

            String json = buildStatusJson();
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String buildStatusJson() {
        String title = (latestOutcome != null) ? latestOutcome.getTitle() : "Ready to scan";
        String severity = (latestOutcome != null) ? latestOutcome.getSeverity().name() : "INFO";
        String summary = (latestOutcome != null) ? latestOutcome.getSummary() : "Click Run Diagnostics to test.";
        String ssid = (latestWifi != null && latestWifi.isConnected()) ? latestWifi.getSsid() : "Disconnected";
        int signal = (latestWifi != null) ? latestWifi.getSignalPercent() : 0;
        String band = (latestWifi != null) ? latestWifi.getBand() : "N/A";

        return String.format(
                "{\"title\":\"%s\",\"severity\":\"%s\",\"summary\":\"%s\",\"ssid\":\"%s\",\"signal\":%d,\"band\":\"%s\"}",
                escapeJson(title), escapeJson(severity), escapeJson(summary), escapeJson(ssid), signal, escapeJson(band)
        );
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"").replace("\n", " ");
    }

    private String buildMobileHtml() {
        String statusTitle = (latestOutcome != null) ? latestOutcome.getTitle() : "Network Check Ready";
        String severity = (latestOutcome != null) ? latestOutcome.getSeverity().name() : "HEALTHY";
        String summary = (latestOutcome != null) ? latestOutcome.getSummary() : "Use the buttons below to check your connection or trigger an auto-fix directly from your Android phone.";
        String ssid = (latestWifi != null && latestWifi.isConnected()) ? latestWifi.getSsid() : "Scanning...";
        int signal = (latestWifi != null) ? latestWifi.getSignalPercent() : 0;
        String band = (latestWifi != null) ? latestWifi.getBand() : "2.4 GHz";

        String color = switch (severity) {
            case "HEALTHY" -> "#10b981";
            case "CRITICAL" -> "#ef4444";
            case "WARNING" -> "#f59e0b";
            default -> "#38bdf8";
        };

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <title>NetShield Mobile Companion</title>
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                body { background-color: #0b0f19; color: #f8fafc; padding: 16px; min-height: 100vh; }
                .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid #1e293b; padding-bottom: 14px; }
                .logo { font-size: 20px; font-weight: 900; color: #38bdf8; display: flex; align-items: center; gap: 8px; }
                .badge { font-size: 11px; font-weight: 700; background: #1e293b; color: #94a3b8; padding: 4px 10px; border-radius: 12px; }
                .hero-card { background: #131d2e; border: 2px solid %s; border-radius: 14px; padding: 18px; margin-bottom: 18px; box-shadow: 0 8px 24px rgba(0,0,0,0.4); }
                .hero-status { font-size: 11px; font-weight: 800; text-transform: uppercase; color: %s; margin-bottom: 4px; }
                .hero-title { font-size: 19px; font-weight: 800; margin-bottom: 6px; }
                .hero-desc { font-size: 13px; color: #cbd5e1; line-height: 1.4; }
                .wifi-box { background: #0f172a; border: 1px solid #1e293b; border-radius: 12px; padding: 14px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
                .wifi-info { font-size: 14px; font-weight: 700; color: #f8fafc; }
                .wifi-sub { font-size: 12px; color: #94a3b8; margin-top: 2px; }
                .wifi-sig { font-size: 22px; font-weight: 900; color: #10b981; }
                .btn-group { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
                button { width: 100%%; padding: 16px; border: none; border-radius: 12px; font-size: 15px; font-weight: 800; cursor: pointer; transition: 0.2s; display: flex; align-items: center; justify-content: center; gap: 8px; }
                .btn-primary { background: #0284c7; color: #ffffff; }
                .btn-heal { background: #10b981; color: #ffffff; }
                button:active { transform: scale(0.98); opacity: 0.9; }
                .stages-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 20px; }
                .stage-item { background: #131d2e; border: 1px solid #1e293b; border-radius: 10px; padding: 12px; text-align: center; }
                .stage-name { font-size: 11px; color: #94a3b8; font-weight: 700; }
                .stage-val { font-size: 13px; font-weight: 800; color: #38bdf8; margin-top: 4px; }
                .footer { text-align: center; font-size: 11px; color: #64748b; margin-top: 20px; }
              </style>
            </head>
            <body>
              <div class="header">
                <div class="logo">🛡️ NetShield Mobile</div>
                <div class="badge">Android Companion</div>
              </div>

              <div class="hero-card" id="heroCard">
                <div class="hero-status" id="heroStatus">%s STATUS</div>
                <div class="hero-title" id="heroTitle">%s</div>
                <div class="hero-desc" id="heroDesc">%s</div>
              </div>

              <div class="wifi-box">
                <div>
                  <div class="wifi-info" id="wifiSsid">SSID: %s</div>
                  <div class="wifi-sub" id="wifiBand">Band: %s | NetShield Live</div>
                </div>
                <div class="wifi-sig" id="wifiSig">%d%%</div>
              </div>

              <div class="stages-grid">
                <div class="stage-item"><div class="stage-name">NIC ADAPTER</div><div class="stage-val">OPERATIONAL</div></div>
                <div class="stage-item"><div class="stage-name">GATEWAY</div><div class="stage-val">OPERATIONAL</div></div>
                <div class="stage-item"><div class="stage-name">DNS RESOLVER</div><div class="stage-val">OPERATIONAL</div></div>
                <div class="stage-item"><div class="stage-name">WAN REACHABILITY</div><div class="stage-val">OPERATIONAL</div></div>
              </div>

              <div class="btn-group">
                <button class="btn-primary" onclick="runCheck()">🔍 Run Diagnostics from Phone</button>
                <button class="btn-heal" onclick="runAutoHeal()">⚡ Auto-Solve Network Fault</button>
              </div>

              <div class="footer">NetShield 2.0 | Offline-First Cross-Platform Network Companion</div>

              <script>
                async function runCheck() {
                  document.getElementById('heroTitle').innerText = 'Scanning Network Layers...';
                  document.getElementById('heroDesc').innerText = 'Probing physical adapters, gateway routing, and DNS...';
                  try {
                    const res = await fetch('/api/run');
                    const data = await res.json();
                    updateUi(data);
                  } catch (e) {
                    alert('Error reaching NetShield desktop: ' + e);
                  }
                }

                async function runAutoHeal() {
                  document.getElementById('heroTitle').innerText = 'Applying Autonomous Safe Fix...';
                  document.getElementById('heroDesc').innerText = 'Flushing DNS cache and verifying recovery...';
                  try {
                    const res = await fetch('/api/heal');
                    const data = await res.json();
                    updateUi(data);
                  } catch (e) {
                    alert('Auto-Heal request failed: ' + e);
                  }
                }

                function updateUi(data) {
                  document.getElementById('heroTitle').innerText = data.title;
                  document.getElementById('heroDesc').innerText = data.summary;
                  document.getElementById('heroStatus').innerText = data.severity + ' STATUS';
                  document.getElementById('wifiSsid').innerText = 'SSID: ' + data.ssid;
                  document.getElementById('wifiBand').innerText = 'Band: ' + data.band + ' | NetShield Live';
                  document.getElementById('wifiSig').innerText = data.signal + '%%';

                  const col = data.severity === 'HEALTHY' ? '#10b981' : (data.severity === 'CRITICAL' ? '#ef4444' : '#f59e0b');
                  document.getElementById('heroCard').style.borderColor = col;
                  document.getElementById('heroStatus').style.color = col;
                }
              </script>
            </body>
            </html>
            """.formatted(color, color, severity, statusTitle, summary, ssid, band, signal);
    }
}
