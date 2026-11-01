package com.netshield.diagnostics;

import com.netshield.models.NetworkInterfaceInfo;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete diagnostic that discovers network adapters, link status, MAC, MTU, and IP configuration.
 */
public class RealInterfaceDiagnostic implements InterfaceDiagnostic {

    @Override
    public StageResult checkInterfaces(int timeoutMs) {
        long startTime = System.currentTimeMillis();
        List<NetworkInterfaceInfo> interfaceList = new ArrayList<>();
        List<NetworkInterfaceInfo> activeAdapters = new ArrayList<>();
        boolean foundApipa = false;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return StageResult.failed("Network Interfaces", System.currentTimeMillis() - startTime,
                        "Unable to enumerate network interfaces from OS", Collections.emptyMap(), "Null interface list");
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                try {
                    boolean isUp = ni.isUp();
                    boolean isLoopback = ni.isLoopback();
                    boolean isVirtual = ni.isVirtual() || isVirtualByName(ni.getDisplayName());
                    int mtu = ni.getMTU();

                    byte[] mac = ni.getHardwareAddress();
                    String macStr = formatMac(mac);

                    List<String> ipv4 = new ArrayList<>();
                    List<String> ipv6 = new ArrayList<>();

                    Enumeration<InetAddress> addrs = ni.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet4Address) {
                            String hostAddress = addr.getHostAddress();
                            ipv4.add(hostAddress);
                            if (hostAddress.startsWith("169.254.")) {
                                foundApipa = true;
                            }
                        } else if (addr instanceof Inet6Address) {
                            ipv6.add(addr.getHostAddress());
                        }
                    }

                    NetworkInterfaceInfo info = new NetworkInterfaceInfo(
                            ni.getName(), ni.getDisplayName(), macStr, mtu, isUp, isLoopback, isVirtual, ipv4, ipv6
                    );
                    interfaceList.add(info);

                    if (isUp && !isLoopback && !ipv4.isEmpty()) {
                        activeAdapters.add(info);
                    }
                } catch (Exception e) {
                    // Skip interfaces that refuse attribute queries
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("totalInterfaces", interfaceList.size());
            details.put("activeInterfacesCount", activeAdapters.size());
            details.put("hasApipa", foundApipa);

            if (activeAdapters.isEmpty()) {
                details.put("error", "No active physical or Wi-Fi adapter has an assigned IPv4 address");
                return StageResult.failed("Network Interfaces", duration,
                        "No connected network interface detected. Ethernet cable disconnected or Wi-Fi off.",
                        details, "No active network adapters with IPv4");
            }

            // Pick primary active adapter
            NetworkInterfaceInfo primary = activeAdapters.get(0);
            details.put("primaryAdapterName", primary.getDisplayName());
            details.put("primaryIpv4", primary.getIpv4Addresses().isEmpty() ? "None" : primary.getIpv4Addresses().get(0));
            details.put("primaryMac", primary.getMacAddress());
            details.put("primaryMtu", primary.getMtu());

            if (foundApipa) {
                return new StageResult("Network Interfaces", StageStatus.FAILED, duration,
                        "Interface configured with APIPA address (169.254.x.x) - DHCP lease failed.",
                        details, "APIPA self-assigned link-local address detected");
            }

            return StageResult.success("Network Interfaces", duration,
                    "Active adapter '" + primary.getDisplayName() + "' connected (" + details.get("primaryIpv4") + ")",
                    details);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return StageResult.failed("Network Interfaces", duration,
                    "Interface discovery failed: " + e.getMessage(), Collections.emptyMap(), e.getMessage());
        }
    }

    private boolean isVirtualByName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("virtual") || lower.contains("vethernet") ||
                lower.contains("vmware") || lower.contains("vbox") ||
                lower.contains("hyper-v") || lower.contains("teredo") ||
                lower.contains("pseudo");
    }

    private String formatMac(byte[] mac) {
        if (mac == null || mac.length == 0) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        return sb.toString();
    }
}
