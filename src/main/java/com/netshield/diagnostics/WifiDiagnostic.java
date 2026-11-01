package com.netshield.diagnostics;

import com.netshield.models.WifiNetworkInfo;

/**
 * Diagnostic interface for querying wireless 802.11 network state, channel, and signal telemetry.
 */
public interface WifiDiagnostic {
    WifiNetworkInfo getWifiInfo();
}
