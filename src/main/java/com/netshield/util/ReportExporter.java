package com.netshield.util;

import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.HistoricalSession;
import com.netshield.models.RecoveryAction;
import com.netshield.models.StageResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility for exporting diagnostic reports and session history into CSV, JSON, and text formats.
 */
public class ReportExporter {

    public static void exportHistoryToCsv(List<HistoricalSession> sessions, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
            writer.write("ID,Session_UUID,Timestamp,Category,Severity,Confidence,Summary,Gateway_Latency_ms,DNS_Latency_ms,HTTP_Latency_ms,Packet_Loss_Percent,Jitter_ms\n");
            for (HistoricalSession s : sessions) {
                writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.1f,%.1f,%.1f,%.1f,%.1f\n",
                        s.getId(),
                        s.getSessionUuid(),
                        s.getFormattedTimestamp(),
                        s.getCategory().name(),
                        s.getSeverity().name(),
                        s.getConfidence().name(),
                        escapeCsv(s.getSummary()),
                        s.getGatewayLatencyMs(),
                        s.getDnsLatencyMs(),
                        s.getHttpsLatencyMs(),
                        s.getPacketLossPercent(),
                        s.getJitterMs()
                ));
            }
        }
    }

    public static void exportHistoryToJson(List<HistoricalSession> sessions, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
            writer.write("[\n");
            for (int i = 0; i < sessions.size(); i++) {
                HistoricalSession s = sessions.get(i);
                writer.write("  {\n");
                writer.write(String.format("    \"id\": %d,\n", s.getId()));
                writer.write(String.format("    \"sessionUuid\": \"%s\",\n", s.getSessionUuid()));
                writer.write(String.format("    \"timestamp\": \"%s\",\n", s.getFormattedTimestamp()));
                writer.write(String.format("    \"category\": \"%s\",\n", s.getCategory().name()));
                writer.write(String.format("    \"severity\": \"%s\",\n", s.getSeverity().name()));
                writer.write(String.format("    \"confidence\": \"%s\",\n", s.getConfidence().name()));
                writer.write(String.format("    \"summary\": \"%s\",\n", escapeJson(s.getSummary())));
                writer.write(String.format("    \"gatewayLatencyMs\": %.1f,\n", s.getGatewayLatencyMs()));
                writer.write(String.format("    \"dnsLatencyMs\": %.1f,\n", s.getDnsLatencyMs()));
                writer.write(String.format("    \"httpLatencyMs\": %.1f,\n", s.getHttpsLatencyMs()));
                writer.write(String.format("    \"packetLossPercent\": %.1f,\n", s.getPacketLossPercent()));
                writer.write(String.format("    \"jitterMs\": %.1f\n", s.getJitterMs()));
                writer.write("  }" + (i < sessions.size() - 1 ? "," : "") + "\n");
            }
            writer.write("]\n");
        }
    }

    public static String generateTextReport(DiagnosticReport report, DiagnosisOutcome outcome) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                NETSHIELD - INTERNET FAILURE DIAGNOSTIC REPORT                  \n");
        sb.append("================================================================================\n\n");
        sb.append("Session ID : ").append(report.getId()).append("\n");
        sb.append("Timestamp  : ").append(report.getTimestamp()).append("\n");
        sb.append("Diagnosis  : ").append(outcome.getTitle()).append(" [").append(outcome.getCategory()).append("]\n");
        sb.append("Severity   : ").append(outcome.getSeverity().getLabel()).append("\n");
        sb.append("Confidence : ").append(outcome.getConfidence().getDisplayLabel()).append("\n");
        sb.append("Summary    : ").append(outcome.getSummary()).append("\n\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("                             DIAGNOSTIC EVIDENCE                                \n");
        sb.append("--------------------------------------------------------------------------------\n");
        for (DiagnosticEvidence ev : outcome.getEvidenceList()) {
            sb.append(" * [").append(ev.getStageSource()).append("] ").append(ev.getObservation())
                    .append(" (").append(ev.getTechnicalDetail()).append(")\n");
        }
        sb.append("\n");

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("                         RECOMMENDED RECOVERY ACTIONS                           \n");
        sb.append("--------------------------------------------------------------------------------\n");
        for (RecoveryAction act : outcome.getRecoveryActions()) {
            sb.append(act.getStepNumber()).append(". ").append(act.getTitle()).append("\n");
            sb.append("   Instructions: ").append(act.getDescription()).append("\n");
            if (act.getSafeCommand() != null) {
                sb.append("   Command     : ").append(act.getSafeCommand()).append("\n");
            }
            sb.append("   Why It Helps: ").append(act.getExplanation()).append("\n\n");
        }

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("                            STAGE-BY-STAGE METRICS                              \n");
        sb.append("--------------------------------------------------------------------------------\n");
        for (StageResult stg : report.getStageResults().values()) {
            sb.append(String.format(" %-26s | %-12s | %4d ms | %s\n",
                    stg.getStageName(), stg.getStatus().getLabel(), stg.getDurationMs(), stg.getMessage()));
        }
        sb.append("================================================================================\n");
        return sb.toString();
    }

    private static String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
