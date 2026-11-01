package com.netshield.models;

/**
 * High-level root cause categories identified by the NetShield Diagnosis Engine.
 */
public enum DiagnosisCategory {
    HEALTHY_ONLINE("Healthy Online Connection", Severity.HEALTHY),
    NO_ACTIVE_ADAPTER("No Active Network Interface", Severity.CRITICAL),
    DHCP_APIPA_FAILURE("DHCP Lease Failure (APIPA 169.254.x.x)", Severity.CRITICAL),
    GATEWAY_UNREACHABLE("Default Gateway Unreachable", Severity.CRITICAL),
    CAPTIVE_PORTAL_DETECTED("Captive Portal / Wi-Fi Login Required", Severity.WARNING),
    LOCAL_DNS_FAILURE("Local Router DNS Resolution Failure", Severity.WARNING),
    COMPLETE_DNS_FAILURE("Total DNS Infrastructure Failure", Severity.CRITICAL),
    ISP_OUTAGE("Upstream Internet Service Provider (ISP) Outage", Severity.CRITICAL),
    HIGH_LATENCY_PACKET_LOSS("Severe Network Degradation / Packet Loss", Severity.WARNING),
    PARTIAL_CONNECTIVITY("Partial Internet Connectivity", Severity.WARNING),
    INCONCLUSIVE("Inconclusive Diagnostic Findings", Severity.INFO);

    private final String defaultTitle;
    private final Severity baseSeverity;

    DiagnosisCategory(String defaultTitle, Severity baseSeverity) {
        this.defaultTitle = defaultTitle;
        this.baseSeverity = baseSeverity;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public Severity getBaseSeverity() {
        return baseSeverity;
    }
}
