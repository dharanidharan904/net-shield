package com.netshield;

import com.netshield.diagnostics.RealWifiDiagnostic;
import com.netshield.models.WifiNetworkInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WifiDiagnosticTest {

    private final RealWifiDiagnostic diagnostic = new RealWifiDiagnostic();

    @Test
    @DisplayName("Parse connected 5 GHz Wi-Fi 6 telemetry")
    void testParseConnected5Ghz() {
        String mockOutput = """
            There is 1 interface on the system:

                Name                   : Wi-Fi
                Description            : Intel(R) Wi-Fi 6 AX201 160MHz
                State                  : connected
                SSID                   : Campus-Secure-5G
                BSSID                  : 00:11:22:33:44:55
                Radio type             : 802.11ax
                Channel                : 36
                Receive rate (Mbps)    : 866
                Transmit rate (Mbps)   : 866
                Signal                 : 90%
            """;

        WifiNetworkInfo info = diagnostic.parseNetshOutput(mockOutput);

        assertTrue(info.isConnected());
        assertEquals("Wi-Fi", info.getInterfaceName());
        assertEquals("Campus-Secure-5G", info.getSsid());
        assertEquals("00:11:22:33:44:55", info.getBssid());
        assertEquals("802.11ax", info.getRadioType());
        assertEquals("5 GHz", info.getBand());
        assertEquals(36, info.getChannel());
        assertEquals(90, info.getSignalPercent());
        assertEquals(-55, info.getSignalDbm()); // (90/2) - 100
        assertEquals("EXCELLENT", info.getQualityRating());
        assertFalse(info.isCongested24Ghz());
    }

    @Test
    @DisplayName("Parse 2.4 GHz congested Wi-Fi channel")
    void testParseCongested24Ghz() {
        String mockOutput = """
            There is 1 interface on the system:

                Name                   : Wi-Fi
                Description            : Realtek Wireless Adapter
                State                  : connected
                SSID                   : Home-2G
                BSSID                  : aa:bb:cc:dd:ee:ff
                Radio type             : 802.11n
                Channel                : 6
                Receive rate (Mbps)    : 72
                Transmit rate (Mbps)   : 72
                Signal                 : 45%
            """;

        WifiNetworkInfo info = diagnostic.parseNetshOutput(mockOutput);

        assertTrue(info.isConnected());
        assertEquals("2.4 GHz", info.getBand());
        assertEquals(6, info.getChannel());
        assertEquals(45, info.getSignalPercent());
        assertEquals("FAIR", info.getQualityRating());
        assertTrue(info.isCongested24Ghz());
    }

    @Test
    @DisplayName("Parse disconnected Wi-Fi state")
    void testParseDisconnected() {
        String mockOutput = """
            There is 1 interface on the system:

                Name                   : Wi-Fi
                Description            : Intel(R) Wi-Fi Adapter
                State                  : disconnected
            """;

        WifiNetworkInfo info = diagnostic.parseNetshOutput(mockOutput);

        assertFalse(info.isConnected());
        assertEquals("Disconnected", info.getSsid());
        assertEquals("DISCONNECTED", info.getQualityRating());
    }
}
