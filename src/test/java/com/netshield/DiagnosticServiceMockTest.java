package com.netshield;

import com.netshield.diagnostics.CaptivePortalDiagnostic;
import com.netshield.diagnostics.DnsDiagnostic;
import com.netshield.diagnostics.GatewayDiagnostic;
import com.netshield.diagnostics.HttpDiagnostic;
import com.netshield.diagnostics.InterfaceDiagnostic;
import com.netshield.diagnostics.LatencyDiagnostic;
import com.netshield.diagnostics.NetworkDiagnosticService;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.StageResult;
import com.netshield.models.UserSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticServiceMockTest {

    static class StubInterfaceDiagnostic implements InterfaceDiagnostic {
        boolean called = false;
        int receivedTimeout = -1;

        @Override
        public StageResult checkInterfaces(int timeoutMs) {
            this.called = true;
            this.receivedTimeout = timeoutMs;
            return StageResult.success(DiagnosticReport.STAGE_INTERFACE, 10, "Adapter OK", Map.of("activeInterfacesCount", 1));
        }
    }

    static class StubGatewayDiagnostic implements GatewayDiagnostic {
        boolean called = false;

        @Override
        public StageResult checkGateway(String customGatewayIp, int timeoutMs) {
            this.called = true;
            return StageResult.success(DiagnosticReport.STAGE_GATEWAY, 5, "Gateway OK", Map.of("gatewayIp", "192.168.1.1"));
        }
    }

    static class StubDnsDiagnostic implements DnsDiagnostic {
        boolean called = false;

        @Override
        public StageResult checkDns(List<String> domains, List<String> directDnsServers, int timeoutMs) {
            this.called = true;
            return StageResult.success(DiagnosticReport.STAGE_DNS, 15, "DNS OK", Map.of("systemDnsSuccess", true));
        }
    }

    static class StubHttpDiagnostic implements HttpDiagnostic {
        boolean called = false;

        @Override
        public StageResult checkHttpReachability(List<String> targetUrls, int timeoutMs) {
            this.called = true;
            return StageResult.success(DiagnosticReport.STAGE_HTTP, 25, "HTTPS OK", Map.of("successfulEndpoints", 3));
        }
    }

    static class StubLatencyDiagnostic implements LatencyDiagnostic {
        boolean called = false;

        @Override
        public StageResult measureLatencyAndLoss(String targetHost, int pingCount, int timeoutMs) {
            this.called = true;
            return StageResult.success(DiagnosticReport.STAGE_LATENCY, 30, "Latency OK", Map.of("packetLossPercent", 0.0, "avgLatencyMs", 18.0));
        }
    }

    static class StubCaptivePortalDiagnostic implements CaptivePortalDiagnostic {
        boolean called = false;

        @Override
        public StageResult checkCaptivePortal(String probeUrl, int timeoutMs) {
            this.called = true;
            return StageResult.success(DiagnosticReport.STAGE_CAPTIVE, 20, "No captive portal", Map.of("isCaptivePortal", false));
        }
    }

    @Test
    @DisplayName("Mocked offline pipeline executes all 6 diagnostic stages")
    void testOfflineDiagnosticPipeline() throws Exception {
        StubInterfaceDiagnostic iface = new StubInterfaceDiagnostic();
        StubGatewayDiagnostic gw = new StubGatewayDiagnostic();
        StubDnsDiagnostic dns = new StubDnsDiagnostic();
        StubHttpDiagnostic http = new StubHttpDiagnostic();
        StubLatencyDiagnostic latency = new StubLatencyDiagnostic();
        StubCaptivePortalDiagnostic captive = new StubCaptivePortalDiagnostic();

        NetworkDiagnosticService service = new NetworkDiagnosticService(
                iface, gw, dns, http, latency, captive
        );

        UserSettings settings = new UserSettings();
        List<String> progressStages = new ArrayList<>();

        DiagnosticReport report = service.runFullDiagnostics(settings, (stage, progress) -> {
            progressStages.add(stage);
        });

        assertNotNull(report);
        assertEquals(6, report.getStageResults().size());
        assertTrue(report.allStagesSuccessful());
        assertEquals(0.0, report.getPacketLossPercentage());
        assertEquals(18.0, report.getAverageLatencyMs());

        // Verify all 6 mock stages were invoked
        assertTrue(iface.called);
        assertTrue(gw.called);
        assertTrue(dns.called);
        assertTrue(http.called);
        assertTrue(latency.called);
        assertTrue(captive.called);

        // Verify progress reporting
        assertTrue(progressStages.contains(DiagnosticReport.STAGE_INTERFACE));
        assertTrue(progressStages.contains(DiagnosticReport.STAGE_GATEWAY));
        assertTrue(progressStages.contains("Diagnostics Complete"));
    }

    @Test
    @DisplayName("Pipeline respects cancellation via thread interruption")
    void testPipelineInterruptionCancellation() {
        InterfaceDiagnostic interruptIface = timeoutMs -> {
            Thread.currentThread().interrupt(); // Simulate cancel during stage 1
            return StageResult.success("Interface", 10, "OK", Collections.emptyMap());
        };

        NetworkDiagnosticService service = new NetworkDiagnosticService(
                interruptIface,
                new StubGatewayDiagnostic(),
                new StubDnsDiagnostic(),
                new StubHttpDiagnostic(),
                new StubLatencyDiagnostic(),
                new StubCaptivePortalDiagnostic()
        );

        try {
            assertThrows(InterruptedException.class, () -> {
                service.runFullDiagnostics(new UserSettings(), null);
            });
        } finally {
            Thread.interrupted(); // Clear interrupted status for JUnit thread
        }
    }
}
