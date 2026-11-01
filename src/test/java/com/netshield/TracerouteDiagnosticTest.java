package com.netshield;

import com.netshield.diagnostics.RealTracerouteDiagnostic;
import com.netshield.models.RouteHop;
import com.netshield.models.TracerouteReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TracerouteDiagnosticTest {

    private final RealTracerouteDiagnostic diagnostic = new RealTracerouteDiagnostic();

    @Test
    @DisplayName("Parse valid traceroute output with bottleneck jump detection")
    void testParseTracertOutputWithBottleneck() {
        String mockOutput = """
            Tracing route to 1.1.1.1 over a maximum of 15 hops:

              1     2 ms     2 ms     2 ms  192.168.1.1
              2    12 ms    11 ms    13 ms  10.20.0.1
              3    85 ms    88 ms    82 ms  142.250.60.1
              4    86 ms    87 ms    85 ms  1.1.1.1

            Trace complete.
            """;

        TracerouteReport report = diagnostic.parseTracertOutput("1.1.1.1", mockOutput, 3500);

        assertTrue(report.isDestinationReached());
        assertEquals(4, report.getHops().size());

        // Hop 1 check
        RouteHop hop1 = report.getHops().get(0);
        assertEquals(1, hop1.getHopNumber());
        assertEquals("192.168.1.1", hop1.getIpAddress());
        assertEquals(2.0, hop1.getAvgRttMs(), 0.1);
        assertFalse(hop1.isBottleneck());

        // Hop 3 check: jumped from ~12ms to ~85ms (+73ms jump) -> bottleneck!
        RouteHop hop3 = report.getHops().get(2);
        assertEquals(3, hop3.getHopNumber());
        assertTrue(hop3.isBottleneck());
        assertEquals(3, report.getBottleneckHop());
    }

    @Test
    @DisplayName("Parse traceroute with intermediate timeout hops (*)")
    void testParseTracertTimeoutHops() {
        String mockOutput = """
            Tracing route to 8.8.8.8 over a maximum of 15 hops:

              1     1 ms     1 ms     1 ms  192.168.1.1
              2     *        *        *     Request timed out.
              3    15 ms    14 ms    16 ms  8.8.8.8

            Trace complete.
            """;

        TracerouteReport report = diagnostic.parseTracertOutput("8.8.8.8", mockOutput, 4200);

        assertEquals(3, report.getHops().size());
        assertTrue(report.getHops().get(1).isTimedOut());
        assertEquals("*", report.getHops().get(1).getIpAddress());
        assertEquals("* (Timed Out)", report.getHops().get(1).getDisplayLatency());
        assertTrue(report.isDestinationReached());
    }
}
