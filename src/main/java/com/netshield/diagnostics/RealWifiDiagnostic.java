package com.netshield.diagnostics;

import com.netshield.models.WifiNetworkInfo;
import com.netshield.util.SafeCommandRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete implementation that inspects Wi-Fi adapters using Windows netsh wlan show interfaces.
 */
public class RealWifiDiagnostic implements WifiDiagnostic {
    private static final SafeCommandRunner RUNNER = new SafeCommandRunner();
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)");

    @Override
    public WifiNetworkInfo getWifiInfo() {
        try {
            SafeCommandRunner.CommandResult res = RUNNER.execute(
                    Arrays.asList("netsh", "wlan", "show", "interfaces"), 4
            );
            return parseNetshOutput(res.getRawOutput());
        } catch (Exception e) {
            return WifiNetworkInfo.disconnected();
        }
    }

    /**
     * Parses raw netsh wlan show interfaces output.
     * Package-private for unit testing with mocked output.
     */
    public WifiNetworkInfo parseNetshOutput(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return WifiNetworkInfo.disconnected();
        }

        Map<String, String> map = new HashMap<>();
        String[] lines = rawOutput.split("\r?\n");

        for (String line : lines) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim().toLowerCase();
                String val = line.substring(colonIndex + 1).trim();
                map.put(key, val);
            }
        }

        String state = map.getOrDefault("state", "").toLowerCase();
        boolean connected = state.contains("connected") && !state.contains("disconnected");

        if (!connected) {
            String ifaceName = map.getOrDefault("name", "Wi-Fi");
            return new WifiNetworkInfo(false, ifaceName, "Disconnected", null, "N/A", "N/A", 0, 0, 0, 0);
        }

        String ifaceName = map.getOrDefault("name", "Wi-Fi");
        String ssid = map.getOrDefault("ssid", "Unknown Network");
        String bssid = map.getOrDefault("bssid", "N/A");
        String radio = map.getOrDefault("radio type", "802.11");

        int channel = parseInteger(map.get("channel"), 0);
        int signal = parseInteger(map.get("signal"), 0);
        double rxRate = parseDouble(map.get("receive rate (mbps)"), 0.0);
        double txRate = parseDouble(map.get("transmit rate (mbps)"), 0.0);

        String band;
        if (map.containsKey("band")) {
            band = map.get("band");
        } else if (channel > 14) {
            band = "5 GHz";
        } else if (channel > 0) {
            band = "2.4 GHz";
        } else {
            band = "N/A";
        }

        return new WifiNetworkInfo(true, ifaceName, ssid, bssid, radio, band, channel, rxRate, txRate, signal);
    }

    private int parseInteger(String val, int def) {
        if (val == null) return def;
        Matcher m = NUMERIC_PATTERN.matcher(val);
        if (m.find()) {
            try {
                return (int) Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private double parseDouble(String val, double def) {
        if (val == null) return def;
        Matcher m = NUMERIC_PATTERN.matcher(val);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
