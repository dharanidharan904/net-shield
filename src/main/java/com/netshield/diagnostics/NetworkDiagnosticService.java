package com.netshield.diagnostics;

import com.netshield.models.DiagnosticReport;
import com.netshield.models.StageResult;
import com.netshield.models.UserSettings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Service that orchestrates the execution of all diagnostic stages in order.
 * Fully decoupled via dependency injection for seamless offline mocking in unit tests.
 */
public class NetworkDiagnosticService {

    private final InterfaceDiagnostic interfaceDiagnostic;
    private final GatewayDiagnostic gatewayDiagnostic;
    private final DnsDiagnostic dnsDiagnostic;
    private final HttpDiagnostic httpDiagnostic;
    private final LatencyDiagnostic latencyDiagnostic;
    private final CaptivePortalDiagnostic captivePortalDiagnostic;

    public NetworkDiagnosticService() {
        this(
                new RealInterfaceDiagnostic(),
                new RealGatewayDiagnostic(),
                new RealDnsDiagnostic(),
                new RealHttpDiagnostic(),
                new RealLatencyDiagnostic(),
                new RealCaptivePortalDiagnostic()
        );
    }

    public NetworkDiagnosticService(InterfaceDiagnostic interfaceDiagnostic,
                                    GatewayDiagnostic gatewayDiagnostic,
                                    DnsDiagnostic dnsDiagnostic,
                                    HttpDiagnostic httpDiagnostic,
                                    LatencyDiagnostic latencyDiagnostic,
                                    CaptivePortalDiagnostic captivePortalDiagnostic) {
        this.interfaceDiagnostic = interfaceDiagnostic;
        this.gatewayDiagnostic = gatewayDiagnostic;
        this.dnsDiagnostic = dnsDiagnostic;
        this.httpDiagnostic = httpDiagnostic;
        this.latencyDiagnostic = latencyDiagnostic;
        this.captivePortalDiagnostic = captivePortalDiagnostic;
    }

    /**
     * Executes all diagnostic stages sequentially, reporting progress to the given listener.
     * Respects thread interruption for immediate background task cancellation.
     */
    public DiagnosticReport runFullDiagnostics(UserSettings settings, BiConsumer<String, Double> progressListener) throws InterruptedException {
        Map<String, StageResult> results = new LinkedHashMap<>();
        int timeoutMs = settings.getTimeoutMs();

        // Stage 1: Network Interfaces
        checkInterrupted();
        notifyProgress(progressListener, DiagnosticReport.STAGE_INTERFACE, 0.1);
        StageResult interfaceResult = interfaceDiagnostic.checkInterfaces(timeoutMs);
        results.put(DiagnosticReport.STAGE_INTERFACE, interfaceResult);

        // Stage 2: Default Gateway
        checkInterrupted();
        notifyProgress(progressListener, DiagnosticReport.STAGE_GATEWAY, 0.3);
        StageResult gatewayResult = gatewayDiagnostic.checkGateway(null, timeoutMs);
        results.put(DiagnosticReport.STAGE_GATEWAY, gatewayResult);

        // Stage 3: DNS Resolution
        checkInterrupted();
        notifyProgress(progressListener, DiagnosticReport.STAGE_DNS, 0.5);
        StageResult dnsResult = dnsDiagnostic.checkDns(settings.getDnsTestDomains(), settings.getDirectDnsServers(), timeoutMs);
        results.put(DiagnosticReport.STAGE_DNS, dnsResult);

        // Stage 4: HTTPS Reachability
        checkInterrupted();
        notifyProgress(progressListener, DiagnosticReport.STAGE_HTTP, 0.7);
        StageResult httpResult = httpDiagnostic.checkHttpReachability(settings.getHttpsTargetUrls(), timeoutMs);
        results.put(DiagnosticReport.STAGE_HTTP, httpResult);

        // Stage 5: Latency & Packet Loss
        checkInterrupted();
        notifyProgress(progressListener, DiagnosticReport.STAGE_LATENCY, 0.85);
        StageResult latencyResult = latencyDiagnostic.measureLatencyAndLoss("1.1.1.1", settings.getPingCount(), timeoutMs);
        results.put(DiagnosticReport.STAGE_LATENCY, latencyResult);

        // Stage 6: Captive Portal Detection
        checkInterrupted();
        notifyProgress(progressListener, DiagnosticReport.STAGE_CAPTIVE, 0.95);
        StageResult captiveResult = captivePortalDiagnostic.checkCaptivePortal(settings.getCaptiveCheckUrl(), timeoutMs);
        results.put(DiagnosticReport.STAGE_CAPTIVE, captiveResult);

        notifyProgress(progressListener, "Diagnostics Complete", 1.0);
        return new DiagnosticReport(results);
    }

    private void notifyProgress(BiConsumer<String, Double> listener, String stageName, double progress) {
        if (listener != null) {
            listener.accept(stageName, progress);
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Diagnostic execution cancelled by user");
        }
    }
}
