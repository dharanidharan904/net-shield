package com.netshield;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.HistoricalSession;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import com.netshield.models.UserSettings;
import com.netshield.persistence.DatabaseManager;
import com.netshield.persistence.HistoryDao;
import com.netshield.persistence.SettingsDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabasePersistenceTest {

    private DatabaseManager dbManager;
    private HistoryDao historyDao;
    private SettingsDao settingsDao;

    @BeforeEach
    void setUp() {
        dbManager = DatabaseManager.inMemory();
        historyDao = new HistoryDao(dbManager);
        settingsDao = new SettingsDao(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    @DisplayName("Persist and retrieve diagnostic sessions in SQLite")
    void testSaveAndRetrieveSession() {
        Map<String, StageResult> stages = new HashMap<>();
        Map<String, Object> gwDetails = new HashMap<>();
        gwDetails.put("latencyMs", 12.5);
        stages.put(DiagnosticReport.STAGE_GATEWAY, new StageResult(DiagnosticReport.STAGE_GATEWAY, StageStatus.SUCCESS, 12, "Gateway reached", gwDetails, null));

        Map<String, Object> httpDetails = new HashMap<>();
        httpDetails.put("avgHttpLatencyMs", 35.0);
        stages.put(DiagnosticReport.STAGE_HTTP, new StageResult(DiagnosticReport.STAGE_HTTP, StageStatus.SUCCESS, 35, "HTTPS ok", httpDetails, null));

        Map<String, Object> latencyDetails = new HashMap<>();
        latencyDetails.put("packetLossPercent", 0.0);
        latencyDetails.put("jitterMs", 3.0);
        stages.put(DiagnosticReport.STAGE_LATENCY, new StageResult(DiagnosticReport.STAGE_LATENCY, StageStatus.SUCCESS, 20, "Low jitter", latencyDetails, null));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = new DiagnosisOutcome(
                DiagnosisCategory.HEALTHY_ONLINE,
                "Healthy Online Connection",
                "All tests passed smoothly.",
                ConfidenceLevel.CERTAIN,
                Severity.HEALTHY,
                Collections.emptyList(),
                Collections.emptyList()
        );

        historyDao.saveSession(report, outcome);

        List<HistoricalSession> sessions = historyDao.getRecentSessions(10);
        assertEquals(1, sessions.size());

        HistoricalSession saved = sessions.get(0);
        assertEquals(DiagnosisCategory.HEALTHY_ONLINE, saved.getCategory());
        assertEquals(ConfidenceLevel.CERTAIN, saved.getConfidence());
        assertEquals(12.5, saved.getGatewayLatencyMs(), 0.1);
        assertEquals(35.0, saved.getHttpsLatencyMs(), 0.1);
        assertEquals(0.0, saved.getPacketLossPercent(), 0.1);
    }

    @Test
    @DisplayName("Compute uptime percentage and total outages accurately")
    void testUptimeAndOutageMetrics() {
        DiagnosticReport report = new DiagnosticReport(Collections.emptyMap());

        // 1 Healthy
        DiagnosisOutcome healthy = new DiagnosisOutcome(DiagnosisCategory.HEALTHY_ONLINE, "Healthy", "Ok", ConfidenceLevel.CERTAIN, Severity.HEALTHY, Collections.emptyList(), Collections.emptyList());
        historyDao.saveSession(report, healthy);

        // 1 Outage
        DiagnosisOutcome outage = new DiagnosisOutcome(DiagnosisCategory.ISP_OUTAGE, "ISP Outage", "Down", ConfidenceLevel.HIGH, Severity.CRITICAL, Collections.emptyList(), Collections.emptyList());
        historyDao.saveSession(report, outage);

        assertEquals(50.0, historyDao.getUptimePercentage(), 0.01);
        assertEquals(1, historyDao.getTotalOutagesCount());
        assertEquals(2, historyDao.getRecentSessions(10).size());

        // Clear history
        historyDao.clearHistory();
        assertEquals(0, historyDao.getRecentSessions(10).size());
    }

    @Test
    @DisplayName("Save and reload user configuration settings")
    void testUserSettingsPersistence() {
        UserSettings settings = new UserSettings();
        settings.setTimeoutMs(4500);
        settings.setPingCount(8);
        settings.setContinuousIntervalSec(45);
        settings.setSoundAlertsEnabled(false);
        settings.setAutoHealingEnabled(true);
        settings.setDnsTestDomains(List.of("example.com", "myrouter.local"));

        settingsDao.saveSettings(settings);

        UserSettings reloaded = settingsDao.loadSettings();
        assertEquals(4500, reloaded.getTimeoutMs());
        assertEquals(8, reloaded.getPingCount());
        assertEquals(45, reloaded.getContinuousIntervalSec());
        assertFalse(reloaded.isSoundAlertsEnabled());
        assertTrue(reloaded.isAutoHealingEnabled());
        assertTrue(reloaded.getDnsTestDomains().contains("example.com"));
    }
}
