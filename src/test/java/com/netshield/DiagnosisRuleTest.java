package com.netshield;

import com.netshield.diagnosis.ApipaDhcpFailureRule;
import com.netshield.diagnosis.CaptivePortalRule;
import com.netshield.diagnosis.CompleteDnsFailureRule;
import com.netshield.diagnosis.DiagnosisEngine;
import com.netshield.diagnosis.GatewayUnreachableRule;
import com.netshield.diagnosis.HealthyNetworkRule;
import com.netshield.diagnosis.HighLatencyPacketLossRule;
import com.netshield.diagnosis.InconclusiveRule;
import com.netshield.diagnosis.IspOutageRule;
import com.netshield.diagnosis.LocalDnsFailureRule;
import com.netshield.diagnosis.NoActiveAdapterRule;
import com.netshield.diagnosis.PartialConnectivityRule;
import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosisRuleTest {

    private DiagnosisEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DiagnosisEngine();
    }

    private StageResult createMockStage(String name, StageStatus status, Map<String, Object> details) {
        return new StageResult(name, status, 15, "Stage " + name + " finished", details, null);
    }

    @Test
    @DisplayName("Rule 1: No Active Adapter detection")
    void testNoActiveAdapterRule() {
        Map<String, StageResult> stages = new HashMap<>();
        Map<String, Object> ifaceDetails = new HashMap<>();
        ifaceDetails.put("activeInterfacesCount", 0);
        ifaceDetails.put("hasApipa", false);
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.FAILED, ifaceDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.NO_ACTIVE_ADAPTER, outcome.getCategory());
        assertEquals(ConfidenceLevel.CERTAIN, outcome.getConfidence());
        assertEquals(Severity.CRITICAL, outcome.getSeverity());
        assertFalse(outcome.getRecoveryActions().isEmpty());
    }

    @Test
    @DisplayName("Rule 2: DHCP Lease Failure & APIPA (169.254.x.x)")
    void testApipaDhcpFailureRule() {
        Map<String, StageResult> stages = new HashMap<>();
        Map<String, Object> ifaceDetails = new HashMap<>();
        ifaceDetails.put("activeInterfacesCount", 1);
        ifaceDetails.put("hasApipa", true);
        ifaceDetails.put("primaryIpv4", "169.254.120.45");
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.FAILED, ifaceDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.DHCP_APIPA_FAILURE, outcome.getCategory());
        assertEquals(ConfidenceLevel.CERTAIN, outcome.getConfidence());
        assertTrue(outcome.getSummary().contains("DHCP server"));
        assertTrue(outcome.getRecoveryActions().stream().anyMatch(a -> a.getSafeCommand() != null && a.getSafeCommand().contains("ipconfig /renew")));
    }

    @Test
    @DisplayName("Rule 3: Captive Portal Interception Detection")
    void testCaptivePortalRule() {
        Map<String, StageResult> stages = new HashMap<>();
        Map<String, Object> ifaceDetails = new HashMap<>();
        ifaceDetails.put("activeInterfacesCount", 1);
        ifaceDetails.put("primaryIpv4", "10.0.0.12");
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, ifaceDetails));

        Map<String, Object> captiveDetails = new HashMap<>();
        captiveDetails.put("isCaptivePortal", true);
        captiveDetails.put("portalUrl", "https://wifi.hotel.com/splash");
        stages.put(DiagnosticReport.STAGE_CAPTIVE, createMockStage(DiagnosticReport.STAGE_CAPTIVE, StageStatus.FAILED, captiveDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.CAPTIVE_PORTAL_DETECTED, outcome.getCategory());
        assertEquals(ConfidenceLevel.HIGH, outcome.getConfidence());
        assertEquals(Severity.WARNING, outcome.getSeverity());
        assertTrue(outcome.getRecoveryActions().stream().anyMatch(a -> "LAUNCH_BROWSER_LOGIN".equals(a.getHelperActionId())));
    }

    @Test
    @DisplayName("Rule 4: Default Gateway Unreachable")
    void testGatewayUnreachableRule() {
        Map<String, StageResult> stages = new HashMap<>();
        Map<String, Object> ifaceDetails = new HashMap<>();
        ifaceDetails.put("activeInterfacesCount", 1);
        ifaceDetails.put("primaryIpv4", "192.168.1.100");
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, ifaceDetails));

        Map<String, Object> gwDetails = new HashMap<>();
        gwDetails.put("gatewayIp", "192.168.1.1");
        stages.put(DiagnosticReport.STAGE_GATEWAY, createMockStage(DiagnosticReport.STAGE_GATEWAY, StageStatus.TIMED_OUT, gwDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.GATEWAY_UNREACHABLE, outcome.getCategory());
        assertEquals(ConfidenceLevel.HIGH, outcome.getConfidence());
        assertEquals(Severity.CRITICAL, outcome.getSeverity());
    }

    @Test
    @DisplayName("Rule 5: Local DNS Failure with Public DNS Functional")
    void testLocalDnsFailureRule() {
        Map<String, StageResult> stages = new HashMap<>();
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, Map.of("activeInterfacesCount", 1)));
        stages.put(DiagnosticReport.STAGE_GATEWAY, createMockStage(DiagnosticReport.STAGE_GATEWAY, StageStatus.SUCCESS, Map.of("gatewayIp", "192.168.1.1")));

        Map<String, Object> dnsDetails = new HashMap<>();
        dnsDetails.put("systemDnsSuccess", false);
        dnsDetails.put("directDnsSuccess", true);
        stages.put(DiagnosticReport.STAGE_DNS, createMockStage(DiagnosticReport.STAGE_DNS, StageStatus.FAILED, dnsDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.LOCAL_DNS_FAILURE, outcome.getCategory());
        assertEquals(ConfidenceLevel.HIGH, outcome.getConfidence());
        assertTrue(outcome.getRecoveryActions().stream().anyMatch(a -> "FLUSH_DNS".equals(a.getHelperActionId())));
    }

    @Test
    @DisplayName("Rule 6: Total DNS Failure (system and direct DNS blocked)")
    void testCompleteDnsFailureRule() {
        Map<String, StageResult> stages = new HashMap<>();
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, Map.of("activeInterfacesCount", 1)));
        stages.put(DiagnosticReport.STAGE_GATEWAY, createMockStage(DiagnosticReport.STAGE_GATEWAY, StageStatus.SUCCESS, Map.of("gatewayIp", "192.168.1.1")));

        Map<String, Object> dnsDetails = new HashMap<>();
        dnsDetails.put("systemDnsSuccess", false);
        dnsDetails.put("directDnsSuccess", false);
        stages.put(DiagnosticReport.STAGE_DNS, createMockStage(DiagnosticReport.STAGE_DNS, StageStatus.FAILED, dnsDetails));

        Map<String, Object> latencyDetails = new HashMap<>();
        latencyDetails.put("packetLossPercent", 0.0);
        latencyDetails.put("avgLatencyMs", 18.0);
        stages.put(DiagnosticReport.STAGE_LATENCY, createMockStage(DiagnosticReport.STAGE_LATENCY, StageStatus.SUCCESS, latencyDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.COMPLETE_DNS_FAILURE, outcome.getCategory());
        assertEquals(ConfidenceLevel.HIGH, outcome.getConfidence());
    }

    @Test
    @DisplayName("Rule 7: Upstream ISP Outage")
    void testIspOutageRule() {
        Map<String, StageResult> stages = new HashMap<>();
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, Map.of("activeInterfacesCount", 1)));
        stages.put(DiagnosticReport.STAGE_GATEWAY, createMockStage(DiagnosticReport.STAGE_GATEWAY, StageStatus.SUCCESS, Map.of("gatewayIp", "192.168.1.1")));
        stages.put(DiagnosticReport.STAGE_HTTP, createMockStage(DiagnosticReport.STAGE_HTTP, StageStatus.FAILED, Map.of("successfulEndpoints", 0)));
        stages.put(DiagnosticReport.STAGE_LATENCY, createMockStage(DiagnosticReport.STAGE_LATENCY, StageStatus.FAILED, Map.of("packetLossPercent", 100.0)));
        stages.put(DiagnosticReport.STAGE_DNS, createMockStage(DiagnosticReport.STAGE_DNS, StageStatus.FAILED, Map.of("systemDnsSuccess", false)));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.ISP_OUTAGE, outcome.getCategory());
        assertEquals(ConfidenceLevel.HIGH, outcome.getConfidence());
        assertEquals(Severity.CRITICAL, outcome.getSeverity());
    }

    @Test
    @DisplayName("Rule 8: High Latency & Packet Loss Degradation")
    void testHighLatencyPacketLossRule() {
        Map<String, StageResult> stages = new HashMap<>();
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, Map.of("activeInterfacesCount", 1)));
        stages.put(DiagnosticReport.STAGE_GATEWAY, createMockStage(DiagnosticReport.STAGE_GATEWAY, StageStatus.SUCCESS, Map.of("gatewayIp", "192.168.1.1")));
        stages.put(DiagnosticReport.STAGE_DNS, createMockStage(DiagnosticReport.STAGE_DNS, StageStatus.SUCCESS, Map.of("systemDnsSuccess", true)));
        stages.put(DiagnosticReport.STAGE_HTTP, createMockStage(DiagnosticReport.STAGE_HTTP, StageStatus.SUCCESS, Map.of("successfulEndpoints", 3)));

        Map<String, Object> latencyDetails = new HashMap<>();
        latencyDetails.put("packetLossPercent", 40.0);
        latencyDetails.put("avgLatencyMs", 320.0);
        latencyDetails.put("jitterMs", 85.0);
        stages.put(DiagnosticReport.STAGE_LATENCY, createMockStage(DiagnosticReport.STAGE_LATENCY, StageStatus.INCONCLUSIVE, latencyDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.HIGH_LATENCY_PACKET_LOSS, outcome.getCategory());
        assertEquals(ConfidenceLevel.HIGH, outcome.getConfidence());
        assertEquals(Severity.WARNING, outcome.getSeverity());
    }

    @Test
    @DisplayName("Rule 9: Healthy Network Connection")
    void testHealthyNetworkRule() {
        Map<String, StageResult> stages = new HashMap<>();
        stages.put(DiagnosticReport.STAGE_INTERFACE, createMockStage(DiagnosticReport.STAGE_INTERFACE, StageStatus.SUCCESS, Map.of("activeInterfacesCount", 1, "primaryIpv4", "192.168.1.50")));
        stages.put(DiagnosticReport.STAGE_GATEWAY, createMockStage(DiagnosticReport.STAGE_GATEWAY, StageStatus.SUCCESS, Map.of("gatewayIp", "192.168.1.1", "latencyMs", 2L)));
        stages.put(DiagnosticReport.STAGE_DNS, createMockStage(DiagnosticReport.STAGE_DNS, StageStatus.SUCCESS, Map.of("systemDnsSuccess", true, "avgSystemLatencyMs", 14.0)));
        stages.put(DiagnosticReport.STAGE_HTTP, createMockStage(DiagnosticReport.STAGE_HTTP, StageStatus.SUCCESS, Map.of("successfulEndpoints", 3, "totalEndpointsTested", 3, "avgHttpLatencyMs", 25.0)));

        Map<String, Object> latencyDetails = new HashMap<>();
        latencyDetails.put("packetLossPercent", 0.0);
        latencyDetails.put("avgLatencyMs", 15.0);
        latencyDetails.put("jitterMs", 2.0);
        stages.put(DiagnosticReport.STAGE_LATENCY, createMockStage(DiagnosticReport.STAGE_LATENCY, StageStatus.SUCCESS, latencyDetails));

        DiagnosticReport report = new DiagnosticReport(stages);
        DiagnosisOutcome outcome = engine.diagnose(report);

        assertEquals(DiagnosisCategory.HEALTHY_ONLINE, outcome.getCategory());
        assertEquals(ConfidenceLevel.CERTAIN, outcome.getConfidence());
        assertEquals(Severity.HEALTHY, outcome.getSeverity());
    }
}
