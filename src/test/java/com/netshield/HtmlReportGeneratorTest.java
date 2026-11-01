package com.netshield;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.RecoveryAction;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import com.netshield.models.WifiNetworkInfo;
import com.netshield.util.HtmlReportGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HtmlReportGeneratorTest {

    @Test
    @DisplayName("Generate styled printable HTML executive diagnostic report")
    void testGenerateHtmlReport(@TempDir Path tempDir) throws Exception {
        File htmlFile = tempDir.resolve("test_report.html").toFile();

        StageResult iface = new StageResult(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, 8, "Adapter OK", Map.of("primaryIpv4", "192.168.1.100"), null);
        DiagnosticReport report = new DiagnosticReport(Map.of(DiagnosticReport.STAGE_INTERFACE, iface));

        DiagnosisOutcome outcome = new DiagnosisOutcome(
                DiagnosisCategory.HEALTHY_ONLINE,
                "Healthy Online Connection",
                "Full internet connectivity confirmed across all tested layers.",
                ConfidenceLevel.CERTAIN,
                Severity.HEALTHY,
                List.of(new DiagnosticEvidence("Interface", "Link active", Severity.HEALTHY, "IPv4 192.168.1.100")),
                List.of(new RecoveryAction(1, "No Action Needed", "Maintain regular monitoring", null, null, false, "System healthy"))
        );

        WifiNetworkInfo wifi = new WifiNetworkInfo(true, "Wi-Fi", "Office-5G", "00:11:22:33:44:55", "802.11ax", "5 GHz", 36, 866, 866, 92);

        HtmlReportGenerator.generateReport(report, outcome, wifi, htmlFile);

        assertTrue(htmlFile.exists());
        String html = Files.readString(htmlFile.toPath());

        assertTrue(html.contains("NETSHIELD DIAGNOSTIC REPORT"));
        assertTrue(html.contains("Healthy Online Connection"));
        assertTrue(html.contains("Office-5G"));
        assertTrue(html.contains("Print to PDF"));
        assertTrue(html.contains("Diagnostic Evidence Chain"));
        assertTrue(html.contains("Multi-Layer Probe Execution Metrics"));
    }
}
