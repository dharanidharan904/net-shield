package com.netshield.util;

import com.netshield.models.TroubleshootingArticle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory, offline-accessible knowledge base providing detailed troubleshooting handbooks.
 * Accessible even when the host machine has zero internet connectivity.
 */
public class KnowledgeBaseRepository {

    private final List<TroubleshootingArticle> articles;

    public KnowledgeBaseRepository() {
        this.articles = buildArticles();
    }

    private List<TroubleshootingArticle> buildArticles() {
        List<TroubleshootingArticle> list = new ArrayList<>();

        list.add(new TroubleshootingArticle(
                "KB-101",
                "Resolving Physical Link & Wi-Fi Disconnections",
                "Physical & Data Link Layer",
                "Windows shows a red 'X' or globe icon on the taskbar. NetShield reports 'No active network adapter'.",
                "The network interface card (NIC) detected no electrical carrier signal on Ethernet, or the Wi-Fi radio is powered down or out of range.",
                Arrays.asList(
                        "Verify physical RJ-45 cable latch click and check the link LED lights (green/amber) on the network port.",
                        "Press the physical Wi-Fi switch or keyboard function toggle (Fn + F-key) on laptops to ensure airplane mode is OFF.",
                        "Open Windows Network Connections (ncpa.cpl) and verify the adapter is not grayed out / disabled.",
                        "Reinstall or update the network adapter driver in Device Manager if Windows shows a yellow exclamation mark."
                ),
                Arrays.asList("control.exe netconnections", "devmgmt.msc")
        ));

        list.add(new TroubleshootingArticle(
                "KB-102",
                "Fixing DHCP Lease Failures and APIPA (169.254.x.x)",
                "IP Addressing & DHCP",
                "Computer connects to Wi-Fi/Ethernet but has 'No Internet Access'. NetShield detects IP address starting with 169.254.",
                "The Windows DHCP client broadcasted DHCPDISCOVER packets over UDP port 67, but received no DHCPOFFER from any local DHCP server within the 60-second lease window. RFC 3927 APIPA assigned a link-local address.",
                Arrays.asList(
                        "Reboot the home router or access point by unplugging power for 15 seconds. This restarts the router's dnsmasq / ISC-DHCP server daemon.",
                        "Run 'ipconfig /renew' in Command Prompt to initiate a fresh DHCP handshake.",
                        "Verify the router has not exhausted its available DHCP IP lease pool (e.g. 192.168.1.100 - 192.168.1.250 is full of stale leases).",
                        "Check that your adapter IPv4 properties are set to 'Obtain an IP address automatically'."
                ),
                Arrays.asList("ipconfig /renew", "ipconfig /all")
        ));

        list.add(new TroubleshootingArticle(
                "KB-103",
                "Default Gateway Unreachable & Local Router Freezes",
                "Local Routing & Gateway",
                "Valid IP assigned, but gateway IP (e.g. 192.168.1.1) fails to ping or respond.",
                "The router's network processor is unresponsive, the ARP table failed to resolve the gateway MAC address, or an IP address conflict exists on the LAN.",
                Arrays.asList(
                        "Perform a full power cycle of the router/modem: leave off for 30 seconds to discharge capacitors.",
                        "Verify the Ethernet cable is plugged into a LAN switch port (usually ports 1 through 4) and NOT the WAN/Internet port.",
                        "Check for IP conflicts: if two devices were statically assigned the same IP, both will experience intermittent gateway loss.",
                        "Test ARP table resolution by running 'arp -a' to see if the gateway IP maps to a valid MAC address."
                ),
                Arrays.asList("arp -a", "route print 0.0.0.0")
        ));

        list.add(new TroubleshootingArticle(
                "KB-104",
                "DNS Resolution Failures & Stale Cache Remediation",
                "Domain Name System (DNS)",
                "Web browser shows ERR_NAME_NOT_RESOLVED, but direct IP addresses (like 1.1.1.1 or 8.8.8.8) are reachable via ping.",
                "The configured DNS resolver (often the local router or ISP DNS) is failing to answer recursive DNS queries on UDP/TCP port 53, or the local Windows resolver cache contains stale or corrupted records.",
                Arrays.asList(
                        "Flush the local Windows DNS cache using 'ipconfig /flushdns'.",
                        "Change adapter DNS settings to reliable public DNS resolvers: Cloudflare (1.1.1.1 and 1.0.0.1) or Google (8.8.8.8 and 8.8.4.4).",
                        "Test direct DNS resolution using 'nslookup google.com 1.1.1.1' to verify whether upstream port 53 traffic is allowed.",
                        "Enable DNS-over-HTTPS (DoH) in your browser settings to bypass ISP DNS censorship or filtering."
                ),
                Arrays.asList("ipconfig /flushdns", "nslookup google.com 1.1.1.1")
        ));

        list.add(new TroubleshootingArticle(
                "KB-105",
                "Captive Portals & Public Wi-Fi Login Interceptions",
                "Authentication & Captive Portals",
                "Connected to Wi-Fi (hotel, cafe, airport, campus), but apps cannot connect and HTTPS pages show certificate warnings.",
                "The access point firewall is intercepting all outbound HTTP/HTTPS packets and redirecting them to a captive login portal. HTTPS queries fail SSL validation because the portal certificate does not match the requested domain.",
                Arrays.asList(
                        "Temporarily disable any active VPN client. VPNs attempt to establish encrypted tunnels before the network allows traffic.",
                        "Open your browser and navigate to a plaintext HTTP site such as http://neverssl.com or http://1.1.1.1 to trigger the splash page.",
                        "Complete the web login, accept terms of service, or enter room/guest credentials.",
                        "Once authenticated, re-enable your VPN or firewall software."
                ),
                Arrays.asList("explorer.exe http://neverssl.com")
        ));

        list.add(new TroubleshootingArticle(
                "KB-106",
                "High Latency, Jitter, and Bufferbloat Diagnostics",
                "Quality of Service & Latency",
                "Video calls stutter, online games lag, ping spikes from 20ms to 400ms intermittently.",
                "Wireless radio interference on 2.4 GHz, heavy upload traffic saturating router buffers (Bufferbloat), or congested ISP node routing.",
                Arrays.asList(
                        "Switch your PC connection from 2.4 GHz Wi-Fi to a 5 GHz or 6 GHz Wi-Fi SSID, or use a direct Cat6 Ethernet cable.",
                        "Open Task Manager and check for bandwidth-heavy cloud sync processes (OneDrive, Google Drive, iCloud, torrents).",
                        "Enable Smart Queue Management (SQM) or Cake / FQ-CoDel in your router's QoS configuration.",
                        "Change your Wi-Fi channel: in crowded apartments, channels 1, 6, and 11 often suffer co-channel interference."
                ),
                Arrays.asList("taskmgr.exe")
        ));

        list.add(new TroubleshootingArticle(
                "KB-107",
                "Resolving VPN and Proxy Routing Deadlocks",
                "Tunneling & Proxy",
                "Certain websites fail to load after disconnecting from a workplace or university VPN.",
                "The VPN client crashed without cleanly dismantling virtual TAP/TUN adapters or removing split-tunnel routing table entries, leaving default routes pointing to invalid virtual interfaces.",
                Arrays.asList(
                        "Open Windows Settings -> Network & Internet -> Proxy, and ensure 'Use a proxy server' is toggled OFF.",
                        "Open Windows Network Connections and disable any orphan VPN adapter (e.g. WireGuard, TAP-Windows, OpenVPN).",
                        "Check the Windows hosts file (C:\\Windows\\System32\\drivers\\etc\\hosts) to ensure no domain overrides are redirecting traffic.",
                        "Reboot your computer to allow the Windows IP routing engine to flush stale virtual socket bindings."
                ),
                Arrays.asList("control.exe netconnections")
        ));

        list.add(new TroubleshootingArticle(
                "KB-108",
                "Comprehensive Windows Network Stack Reset (Safe Steps)",
                "Operating System Diagnostics",
                "Multiple network adapters show persistent anomalies even after router reboot and cable checks.",
                "Winsock catalog corruption, invalid TCP/IP autotuning parameters, or stale firewall state tables.",
                Arrays.asList(
                        "Open Windows Settings -> Network & Internet -> Advanced network settings -> Network reset.",
                        "Note: A network reset will reinstall network adapters and set networking components back to original defaults.",
                        "You will need to re-enter Wi-Fi passwords after the computer restarts.",
                        "Only perform this step after basic adapter and router reboot steps have been exhausted."
                ),
                Arrays.asList("ms-settings:network")
        ));

        return Collections.unmodifiableList(list);
    }

    public List<TroubleshootingArticle> getAllArticles() {
        return articles;
    }

    public List<TroubleshootingArticle> search(String query) {
        if (query == null || query.isBlank()) {
            return articles;
        }
        String q = query.toLowerCase().trim();
        return articles.stream()
                .filter(a -> a.getTitle().toLowerCase().contains(q) ||
                        a.getCategory().toLowerCase().contains(q) ||
                        a.getSymptoms().toLowerCase().contains(q) ||
                        a.getTechnicalCause().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public TroubleshootingArticle getById(String id) {
        return articles.stream().filter(a -> a.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }
}
