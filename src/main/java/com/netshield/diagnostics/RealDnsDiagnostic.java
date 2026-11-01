package com.netshield.diagnostics;

import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete diagnostic that verifies system DNS resolution and tests direct UDP DNS queries to public resolvers.
 */
public class RealDnsDiagnostic implements DnsDiagnostic {

    @Override
    public StageResult checkDns(List<String> domains, List<String> directDnsServers, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> details = new LinkedHashMap<>();

        List<String> testDomains = (domains != null && !domains.isEmpty())
                ? domains : List.of("google.com", "cloudflare.com");
        List<String> publicDnsList = (directDnsServers != null && !directDnsServers.isEmpty())
                ? directDnsServers : List.of("1.1.1.1", "8.8.8.8");

        int systemSuccessCount = 0;
        long totalSystemLatency = 0;
        List<String> resolvedSampleIps = new ArrayList<>();

        // 1. System Resolver Check
        for (String domain : testDomains) {
            long domainStart = System.currentTimeMillis();
            try {
                InetAddress[] addresses = InetAddress.getAllByName(domain);
                long latency = System.currentTimeMillis() - domainStart;
                if (addresses.length > 0) {
                    systemSuccessCount++;
                    totalSystemLatency += latency;
                    resolvedSampleIps.add(domain + " -> " + addresses[0].getHostAddress() + " (" + latency + "ms)");
                }
            } catch (Exception ignored) {
                // Resolution failed for this domain
            }
        }

        boolean systemDnsOk = systemSuccessCount > 0;
        double avgSystemLatency = systemSuccessCount > 0 ? (double) totalSystemLatency / systemSuccessCount : -1;

        details.put("systemDnsSuccess", systemDnsOk);
        details.put("systemDomainsResolved", systemSuccessCount + "/" + testDomains.size());
        details.put("avgSystemLatencyMs", avgSystemLatency);
        details.put("resolvedDomains", resolvedSampleIps);

        // 2. Direct Public DNS UDP Query Check (RFC 1035 packet)
        int directSuccessCount = 0;
        long totalDirectLatency = 0;
        List<String> directDnsResults = new ArrayList<>();

        for (String dnsServer : publicDnsList) {
            long queryStart = System.currentTimeMillis();
            try {
                boolean queried = queryDnsUdp(dnsServer, "google.com", timeoutMs);
                long latency = System.currentTimeMillis() - queryStart;
                if (queried) {
                    directSuccessCount++;
                    totalDirectLatency += latency;
                    directDnsResults.add(dnsServer + ": Responded in " + latency + "ms");
                } else {
                    directDnsResults.add(dnsServer + ": No response");
                }
            } catch (Exception e) {
                directDnsResults.add(dnsServer + ": Failed (" + e.getMessage() + ")");
            }
        }

        boolean directDnsOk = directSuccessCount > 0;
        double avgDirectLatency = directSuccessCount > 0 ? (double) totalDirectLatency / directSuccessCount : -1;

        details.put("directDnsSuccess", directDnsOk);
        details.put("directServersResponded", directSuccessCount + "/" + publicDnsList.size());
        details.put("avgDirectLatencyMs", avgDirectLatency);
        details.put("directDnsDetails", directDnsResults);

        long duration = System.currentTimeMillis() - startTime;

        if (systemDnsOk) {
            return StageResult.success("DNS Resolution", duration,
                    "DNS operational: Resolved " + systemSuccessCount + "/" + testDomains.size() +
                            " domains (avg " + String.format("%.1f", avgSystemLatency) + " ms)",
                    details);
        } else if (directDnsOk) {
            // Local system resolver failed, but upstream DNS is reachable!
            return StageResult.failed("DNS Resolution", duration,
                    "Local DNS resolver failed, but direct public DNS (" + directSuccessCount + "/" + publicDnsList.size() + ") is functional.",
                    details, "System DNS failure / Upstream DNS intact");
        } else {
            return StageResult.failed("DNS Resolution", duration,
                    "Total DNS Failure: Neither system resolver nor direct public DNS resolvers responded.",
                    details, "All DNS resolution paths timed out or unreachable");
        }
    }

    /**
     * Sends an RFC 1035 standard A-record DNS query over UDP directly to the target DNS server.
     */
    private boolean queryDnsUdp(String dnsServerIp, String queryDomain, int timeoutMs) throws IOException {
        byte[] requestData = buildDnsQuery(queryDomain);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(Math.min(timeoutMs, 2500));
            InetAddress address = InetAddress.getByName(dnsServerIp);
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, address, 53);
            socket.send(requestPacket);

            byte[] responseBuffer = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            if (responsePacket.getLength() >= 12) {
                // Header check: Flags byte (byte 3) RCODE is lower 4 bits (0 = No error)
                int rcode = responseBuffer[3] & 0x0F;
                return rcode == 0 || rcode == 3; // 0 = NOERROR, 3 = NXDOMAIN (server is replying!)
            }
            return false;
        }
    }

    private byte[] buildDnsQuery(String domain) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Transaction ID: 0x1234
        dos.writeShort(0x1234);
        // Flags: Standard query with recursion desired (0x0100)
        dos.writeShort(0x0100);
        // Questions count: 1
        dos.writeShort(0x0001);
        // Answer RRs: 0
        dos.writeShort(0x0000);
        // Authority RRs: 0
        dos.writeShort(0x0000);
        // Additional RRs: 0
        dos.writeShort(0x0000);

        // Domain name parts (e.g. 3www6google3com0)
        String[] parts = domain.split("\\.");
        for (String part : parts) {
            byte[] partBytes = part.getBytes();
            dos.writeByte(partBytes.length);
            dos.write(partBytes);
        }
        dos.writeByte(0x00); // End of domain name

        // Type: A (0x0001)
        dos.writeShort(0x0001);
        // Class: IN (0x0001)
        dos.writeShort(0x0001);

        dos.flush();
        return baos.toByteArray();
    }
}
