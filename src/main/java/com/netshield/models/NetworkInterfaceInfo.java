package com.netshield.models;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates physical and logical attributes of a host network interface.
 */
public class NetworkInterfaceInfo {
    private final String name;
    private final String displayName;
    private final String macAddress;
    private final int mtu;
    private final boolean isUp;
    private final boolean isLoopback;
    private final boolean isVirtual;
    private final List<String> ipv4Addresses;
    private final List<String> ipv6Addresses;
    private final boolean hasApipaAddress;

    public NetworkInterfaceInfo(String name, String displayName, String macAddress, int mtu,
                                boolean isUp, boolean isLoopback, boolean isVirtual,
                                List<String> ipv4Addresses, List<String> ipv6Addresses) {
        this.name = name;
        this.displayName = displayName;
        this.macAddress = macAddress != null ? macAddress : "N/A";
        this.mtu = mtu;
        this.isUp = isUp;
        this.isLoopback = isLoopback;
        this.isVirtual = isVirtual;
        this.ipv4Addresses = ipv4Addresses != null ? List.copyOf(ipv4Addresses) : Collections.emptyList();
        this.ipv6Addresses = ipv6Addresses != null ? List.copyOf(ipv6Addresses) : Collections.emptyList();
        this.hasApipaAddress = this.ipv4Addresses.stream().anyMatch(ip -> ip.startsWith("169.254."));
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public int getMtu() {
        return mtu;
    }

    public boolean isUp() {
        return isUp;
    }

    public boolean isLoopback() {
        return isLoopback;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public List<String> getIpv4Addresses() {
        return ipv4Addresses;
    }

    public List<String> getIpv6Addresses() {
        return ipv6Addresses;
    }

    public boolean hasApipaAddress() {
        return hasApipaAddress;
    }

    public boolean isUsablePhysicalAdapter() {
        return isUp && !isLoopback && !ipv4Addresses.isEmpty();
    }

    @Override
    public String toString() {
        return displayName + " (" + name + ") [Up=" + isUp + ", IPv4=" + ipv4Addresses + "]";
    }
}
