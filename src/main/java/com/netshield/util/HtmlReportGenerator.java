package com.netshield.util;

import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.RecoveryAction;
import com.netshield.models.StageResult;
import com.netshield.models.WifiNetworkInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates an executive, print-ready HTML/PDF report with modern styling.
 */
public class HtmlReportGenerator {

    public static File generateReport(DiagnosticReport report, DiagnosisOutcome outcome,
                                      WifiNetworkInfo wifiInfo, File destination) throws IOException {
        String html = buildHtml(report, outcome, wifiInfo);
        try (FileWriter writer = new FileWriter(destination, StandardCharsets.UTF_8)) {
            writer.write(html);
        }
        return destination;
    }

    private static String buildHtml(DiagnosticReport report, DiagnosisOutcome outcome, WifiNetworkInfo wifi) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String statusColor = outcome.getSeverity().getColorHex();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n<title>NetShield Diagnostic Report - ").append(report.getId()).append("</title>\n");
        sb.append("<style>\n");
        sb.append("  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 30px; line-height: 1.5; }\n");
        sb.append("  .container { max-width: 900px; margin: 0 auto; background: #1e293b; border-radius: 12px; padding: 35px; border: 1px solid #334155; box-shadow: 0 10px 25px rgba(0,0,0,0.4); }\n");
        sb.append("  .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #334155; padding-bottom: 20px; margin-bottom: 25px; }\n");
        sb.append("  .title { font-size: 26px; font-weight: 800; color: #38bdf8; margin: 0; }\n");
        sb.append("  .subtitle { font-size: 13px; color: #94a3b8; margin-top: 4px; }\n");
        sb.append("  .hero-box { background: ").append(statusColor).append("22; border: 2px solid ").append(statusColor).append("; border-radius: 10px; padding: 20px; margin-bottom: 25px; }\n");
        sb.append("  .hero-title { font-size: 20px; font-weight: 800; color: #ffffff; margin-bottom: 6px; }\n");
        sb.append("  .hero-sub { font-size: 14px; color: #e2e8f0; }\n");
        sb.append("  .badge { display: inline-block; padding: 4px 10px; border-radius: 16px; font-size: 12px; font-weight: 700; }\n");
        sb.append("  table { width: 100%; border-collapse: collapse; margin: 15px 0 25px 0; background: #0f172a; border-radius: 8px; overflow: hidden; }\n");
        sb.append("  th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #1e293b; font-size: 13px; }\n");
        sb.append("  th { background: #172554; color: #93c5fd; font-weight: 700; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }\n");
        sb.append("  .step-card { background: #0f172a; padding: 14px; border-radius: 8px; border-left: 4px solid #0284c7; margin-bottom: 12px; }\n");
        sb.append("  .cmd-box { font-family: monospace; background: #000000; color: #38bdf8; padding: 6px 10px; border-radius: 4px; display: inline-block; margin-top: 6px; }\n");
        sb.append("  .print-btn { background: #0284c7; color: white; border: none; padding: 10px 20px; border-radius: 6px; font-weight: 700; cursor: pointer; float: right; }\n");
        sb.append("  @media print { body { background: #ffffff; color: #000000; } .container { border: none; box-shadow: none; padding: 0; background: #ffffff; } .print-btn { display: none; } th { background: #f1f5f9; color: #000000; } table, td { border-color: #e2e8f0; } .cmd-box { background: #f1f5f9; color: #0369a1; } .hero-box { border-color: #000000; } }\n");
        sb.append("</style>\n</head>\n<body>\n<div class=\"container\">\n");

        sb.append("<button class=\"print-btn\" onclick=\"window.print()\">🖨️ Print to PDF</button>\n");
        sb.append("<div class=\"header\">\n  <div>\n    <div class=\"title\">NETSHIELD DIAGNOSTIC REPORT</div>\n");
        sb.append("    <div class=\"subtitle\">Offline-First Internet Failure Detection & Recovery Tool | Capstone Project</div>\n");
        sb.append("  </div>\n  <div style=\"text-align: right; font-size: 12px; color: #94a3b8;\">\n");
        sb.append("    Generated: ").append(timestamp).append("<br>Session: ").append(report.getId().substring(0, 8)).append("\n  </div>\n</div>\n");

        // Hero Verdict
        sb.append("<div class=\"hero-box\">\n");
        sb.append("  <div style=\"display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\">\n");
        sb.append("    <span class=\"badge\" style=\"background: ").append(statusColor).append("; color: #ffffff;\">").append(outcome.getSeverity()).append(" STATUS</span>\n");
        sb.append("    <span style=\"font-size: 12px; font-weight: 600;\">Confidence: ").append(outcome.getConfidence().getDisplayLabel()).append("</span>\n");
        sb.append("  </div>\n");
        sb.append("  <div class=\"hero-title\">").append(outcome.getTitle()).append("</div>\n");
        sb.append("  <div class=\"hero-sub\">").append(outcome.getSummary()).append("</div>\n");
        sb.append("</div>\n");

        // Wi-Fi Telemetry Section
        if (wifi != null && wifi.isConnected()) {
            sb.append("<h3 style=\"color: #38bdf8; margin-bottom: 8px;\">Wireless Link Telemetry (Wi-Fi)</h3>\n");
            sb.append("<table>\n<tr><th>SSID</th><th>BSSID</th><th>Band / Channel</th><th>Signal Quality</th><th>Link Rates</th></tr>\n");
            sb.append("<tr>\n");
            sb.append("  <td><strong>").append(wifi.getSsid()).append("</strong></td>\n");
            sb.append("  <td>").append(wifi.getBssid()).append("</td>\n");
            sb.append("  <td>").append(wifi.getBand()).append(" (Ch ").append(wifi.getChannel()).append(")</td>\n");
            sb.append("  <td><span style=\"color: ").append(wifi.getSignalBadgeColor()).append("; font-weight: 700;\">").append(wifi.getSignalPercent()).append("% (").append(wifi.getSignalDbm()).append(" dBm) - ").append(wifi.getQualityRating()).append("</span></td>\n");
            sb.append("  <td>RX: ").append(wifi.getReceiveRateMbps()).append(" Mbps / TX: ").append(wifi.getTransmitRateMbps()).append(" Mbps</td>\n");
            sb.append("</tr>\n</table>\n");
        }

        // Diagnostic Evidence
        sb.append("<h3 style=\"color: #38bdf8; margin-bottom: 8px;\">Diagnostic Evidence Chain</h3>\n");
        sb.append("<table>\n<tr><th>Tested Layer</th><th>Observation</th><th>Technical Finding</th></tr>\n");
        for (DiagnosticEvidence ev : outcome.getEvidenceList()) {
            sb.append("<tr>\n  <td><strong>").append(ev.getStageSource()).append("</strong></td>\n");
            sb.append("  <td>").append(ev.getObservation()).append("</td>\n");
            sb.append("  <td style=\"color: #94a3b8;\">").append(ev.getTechnicalDetail()).append("</td>\n</tr>\n");
        }
        sb.append("</table>\n");

        // Safe Recovery Actions
        sb.append("<h3 style=\"color: #10b981; margin-bottom: 8px;\">Safe Step-by-Step Recovery Plan</h3>\n");
        for (RecoveryAction act : outcome.getRecoveryActions()) {
            sb.append("<div class=\"step-card\">\n");
            sb.append("  <div style=\"font-weight: 700; color: #f8fafc;\">Step ").append(act.getStepNumber()).append(": ").append(act.getTitle()).append("</div>\n");
            sb.append("  <div style=\"font-size: 13px; color: #cbd5e1; margin-top: 4px;\">").append(act.getDescription()).append("</div>\n");
            if (act.getSafeCommand() != null) {
                sb.append("  <div class=\"cmd-box\">").append(act.getSafeCommand()).append("</div><br>\n");
            }
            sb.append("  <div style=\"font-size: 11px; color: #64748b; font-style: italic; margin-top: 4px;\">Why: ").append(act.getExplanation()).append("</div>\n");
            sb.append("</div>\n");
        }

        // Full 6-Stage Metrics
        sb.append("<h3 style=\"color: #38bdf8; margin-top: 25px; margin-bottom: 8px;\">Multi-Layer Probe Execution Metrics</h3>\n");
        sb.append("<table>\n<tr><th>Stage Name</th><th>Result Status</th><th>Latency (ms)</th><th>Technical Summary</th></tr>\n");
        for (StageResult stg : report.getStageResults().values()) {
            sb.append("<tr>\n  <td><strong>").append(stg.getStageName()).append("</strong></td>\n");
            sb.append("  <td><span style=\"color: ").append(stg.getStatus().getColorHex()).append("; font-weight: 700;\">").append(stg.getStatus().getLabel()).append("</span></td>\n");
            sb.append("  <td>").append(stg.getDurationMs()).append(" ms</td>\n");
            sb.append("  <td>").append(stg.getMessage()).append("</td>\n</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<div style=\"text-align: center; color: #64748b; font-size: 11px; margin-top: 30px; border-top: 1px solid #334155; padding-top: 15px;\">\n");
        sb.append("NetShield 2.0 • Offline-First Internet Failure Detection & Recovery Tool • College CSE Capstone Project\n");
        sb.append("</div>\n</div>\n</body>\n</html>");

        return sb.toString();
    }
}
