package com.netshield;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.HistoricalSession;
import com.netshield.models.RecoveryAction;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import com.netshield.util.ReportExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportExporterTest {

    @Test
    @DisplayName("Export history sessions to valid CSV format")
    void testExportCsv(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("test_export.csv").toFile();

        HistoricalSession session = new HistoricalSession(
                1L, "uuid-1234", Instant.now(),
                DiagnosisCategory.HEALTHY_ONLINE, Severity.HEALTHY,
                ConfidenceLevel.CERTAIN, "Connection is pristine",
                2.5, 12.0, 30.0, 0.0, 1.2
        );

        ReportExporter.exportHistoryToCsv(List.of(session), csvFile);

        assertTrue(csvFile.exists());
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("ID,Session_UUID,Timestamp"));
        assertTrue(content.contains("uuid-1234"));
        assertTrue(content.contains("HEALTHY_ONLINE"));
    }

    @Test
    @DisplayName("Export history sessions to valid JSON format")
    void testExportJson(@TempDir Path tempDir) throws Exception {
        File jsonFile = tempDir.resolve("test_export.json").toFile();

        HistoricalSession session = new HistoricalSession(
                2L, "uuid-5678", Instant.now(),
                DiagnosisCategory.ISP_OUTAGE, Severity.CRITICAL,
                ConfidenceLevel.HIGH, "Modem WAN disconnected",
                1.5, 0.0, 0.0, 100.0, 0.0
        );

        ReportExporter.exportHistoryToJson(List.of(session), jsonFile);

        assertTrue(jsonFile.exists());
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("\"sessionUuid\": \"uuid-5678\""));
        assertTrue(content.contains("\"category\": \"ISP_OUTAGE\""));
        assertTrue(content.contains("\"packetLossPercent\": 100.0"));
    }

    @Test
    @DisplayName("Generate comprehensive plain text report")
    void testGenerateTextReport() {
        StageResult iface = new StageResult(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, 10, "Adapter OK", Map.of("primaryIpv4", "192.168.1.5"), null);
        DiagnosticReport report = new DiagnosticReport(Map.of(DiagnosticReport.STAGE_INTERFACE, iface));

        DiagnosisOutcome outcome = new DiagnosisOutcome(
                DiagnosisCategory.HEALTHY_ONLINE,
                "Healthy Online Connection",
                "All systems operational.",
                ConfidenceLevel.CERTAIN,
                Severity.HEALTHY,
                List.of(new DiagnosticEvidence("Interface", "Link active", Severity.HEALTHY, "IPv4 192.168.1.5")),
                List.of(new RecoveryAction(1, "No Action Needed", "Maintain regular monitoring", null, null, false, "Network is healthy"))
        );

        String textReport = ReportExporter.generateTextReport(report, outcome);
        assertNotNull(textReport);
        assertTrue(textReport.contains("NETSHIELD - INTERNET FAILURE DIAGNOSTIC REPORT"));
        assertTrue(textReport.contains("Healthy Online Connection"));
        assertTrue(textReport.contains("RECOMMENDED RECOVERY ACTIONS"));
        assertTrue(textReport.contains("DIAGNOSTIC EVIDENCE"));
    }
}
