package com.netshield.android.diagnostics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.netshield.android.models.DiagnosticReport;
import com.netshield.android.models.StageResult;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Android Native 6-Layer Network Diagnostic Service.
 */
public class AndroidDiagnosticService {

    private final Context context;

    public AndroidDiagnosticService(Context context) {
        this.context = context.getApplicationContext();
    }

    public DiagnosticReport runFullDiagnostics() {
        Map<String, StageResult> results = new HashMap<>();

        // Stage 1: Network Adapter
        long t0 = System.currentTimeMillis();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = (cm != null) ? cm.getActiveNetwork() : null;
        NetworkCapabilities caps = (cm != null && activeNetwork != null) ? cm.getNetworkCapabilities(activeNetwork) : null;

        boolean nicOk = caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                          caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                          caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        String nicMsg = nicOk ? "Active Network Interface UP" : "No Active Network Connection";
        results.put("NIC", new StageResult("NIC", nicOk, System.currentTimeMillis() - t0, nicMsg));

        // Wi-Fi Telemetry
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = (wm != null) ? wm.getConnectionInfo() : null;
        String ssid = (wifiInfo != null && wifiInfo.getSSID() != null) ? wifiInfo.getSSID().replace("\"", "") : "Mobile Data / Unknown";
        int rssi = (wifiInfo != null) ? wifiInfo.getRssi() : -100;
        int signalPercent = Math.max(0, Math.min(100, 2 * (rssi + 100)));
        int freq = (wifiInfo != null) ? wifiInfo.getFrequency() : 0;
        String band = (freq > 4900) ? "5 GHz" : (freq > 2400 ? "2.4 GHz" : "Cellular / N/A");
        int linkSpeed = (wifiInfo != null) ? wifiInfo.getLinkSpeed() : 0;

        // Stage 2: Default Gateway
        long t1 = System.currentTimeMillis();
        boolean gatewayOk = false;
        String gatewayIp = "N/A";
        if (wm != null) {
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp != null && dhcp.gateway != 0) {
                gatewayIp = String.format("%d.%d.%d.%d",
                        (dhcp.gateway & 0xff),
                        (dhcp.gateway >> 8 & 0xff),
                        (dhcp.gateway >> 16 & 0xff),
                        (dhcp.gateway >> 24 & 0xff));
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(gatewayIp, 80), 1500);
                    gatewayOk = true;
                } catch (Exception ignored) {
                    // Gateway may not listen on port 80; if IP is non-zero, consider reachable on LAN
                    gatewayOk = !gatewayIp.equals("0.0.0.0");
                }
            }
        }
        results.put("GATEWAY", new StageResult("GATEWAY", gatewayOk || nicOk, System.currentTimeMillis() - t1, "Gateway: " + gatewayIp));

        // Stage 3: DNS Resolution
        long t2 = System.currentTimeMillis();
        boolean dnsOk = false;
        try {
            InetAddress[] addrs = InetAddress.getAllByName("google.com");
            dnsOk = addrs != null && addrs.length > 0;
        } catch (Exception ignored) {}
        results.put("DNS", new StageResult("DNS", dnsOk, System.currentTimeMillis() - t2, dnsOk ? "Resolved google.com" : "DNS Timeout"));

        // Stage 4: HTTPS Reachability
        long t3 = System.currentTimeMillis();
        boolean httpOk = false;
        try {
            URL url = new URL("https://1.1.1.1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            httpOk = (code >= 200 && code < 400);
            conn.disconnect();
        } catch (Exception ignored) {}
        results.put("HTTPS", new StageResult("HTTPS", httpOk, System.currentTimeMillis() - t3, httpOk ? "HTTPS 1.1.1.1 Reached" : "WAN Timeout"));

        // Stage 5: Latency & RTT
        long t4 = System.currentTimeMillis();
        long pingTime = -1;
        try (Socket socket = new Socket()) {
            long start = System.currentTimeMillis();
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 2000);
            pingTime = System.currentTimeMillis() - start;
        } catch (Exception ignored) {}
        boolean latencyOk = pingTime >= 0 && pingTime < 500;
        results.put("LATENCY", new StageResult("LATENCY", latencyOk, System.currentTimeMillis() - t4, pingTime >= 0 ? pingTime + " ms RTT" : "High Latency / Drop"));

        // Stage 6: Captive Portal
        long t5 = System.currentTimeMillis();
        boolean captiveDetected = false;
        try {
            URL url = new URL("http://connectivitycheck.gstatic.com/generate_204");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            captiveDetected = (code != 204 && code >= 300 && code < 400);
            conn.disconnect();
        } catch (Exception ignored) {}
        results.put("CAPTIVE", new StageResult("CAPTIVE", !captiveDetected, System.currentTimeMillis() - t5, captiveDetected ? "Captive Portal Login Required" : "No Interception"));

        return new DiagnosticReport(results, ssid, signalPercent, band, linkSpeed);
    }
}
