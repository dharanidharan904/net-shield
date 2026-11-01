package com.netshield;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import com.netshield.models.WifiNetworkInfo;
import com.netshield.util.NotificationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationManagerTest {

    private final NotificationManager manager = new NotificationManager(null, null);

    private DiagnosticReport createReport(String ip) {
        StageResult iface = new StageResult(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, 10, "OK", Map.of("primaryIpv4", ip), null);
        return new DiagnosticReport(Map.of(DiagnosticReport.STAGE_INTERFACE, iface));
    }

    private DiagnosisOutcome createOutcome(DiagnosisCategory cat, Severity sev) {
        return new DiagnosisOutcome(cat, cat.getDefaultTitle(), "Summary", ConfidenceLevel.CERTAIN, sev, Collections.emptyList(), Collections.emptyList());
    }

    @Test
    @DisplayName("State change detector alerts host device on network outage and restoration")
    void testStateChangeDetection() {
        WifiNetworkInfo wifi1 = new WifiNetworkInfo(true, "Wi-Fi", "Campus-5G", "00:11:22:33:44:55", "802.11ax", "5 GHz", 36, 866, 866, 90);

        // 1. Baseline scan
        DiagnosticReport rep1 = createReport("192.168.1.10");
        DiagnosisOutcome healthy = createOutcome(DiagnosisCategory.HEALTHY_ONLINE, Severity.HEALTHY);
        String event1 = manager.checkAndNotifyStateChange(rep1, healthy, wifi1);
        assertNull(event1, "Initial scan should set baseline without firing alert");

        // 2. Outage occurs (HEALTHY -> CRITICAL)
        DiagnosisOutcome outage = createOutcome(DiagnosisCategory.ISP_OUTAGE, Severity.CRITICAL);
        String event2 = manager.checkAndNotifyStateChange(rep1, outage, wifi1);
        assertNotNull(event2);
        assertTrue(event2.contains("Outage Detected"));

        // 3. Steady state outage (no duplicate event)
        String event3 = manager.checkAndNotifyStateChange(rep1, outage, wifi1);
        assertNull(event3, "No duplicate alert for steady state outage");

        // 4. Connection restored (CRITICAL -> HEALTHY)
        String event4 = manager.checkAndNotifyStateChange(rep1, healthy, wifi1);
        assertNotNull(event4);
        assertTrue(event4.contains("Connection Restored"));

        // 5. IP Address Change on this device
        DiagnosticReport rep2 = createReport("192.168.1.150");
        String event5 = manager.checkAndNotifyStateChange(rep2, healthy, wifi1);
        assertNotNull(event5);
        assertTrue(event5.contains("IPv4 Address Changed"));

        // 6. Wi-Fi AP Roaming event
        WifiNetworkInfo wifi2 = new WifiNetworkInfo(true, "Wi-Fi", "Campus-Library-5G", "00:11:22:33:44:99", "802.11ax", "5 GHz", 44, 866, 866, 88);
        String event6 = manager.checkAndNotifyStateChange(rep2, healthy, wifi2);
        assertNotNull(event6);
        assertTrue(event6.contains("Access Point Changed"));
    }
}
