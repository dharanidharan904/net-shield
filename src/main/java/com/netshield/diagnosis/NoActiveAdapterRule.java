package com.netshield.diagnosis;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticEvidence;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.RecoveryAction;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.StageStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnosis Rule: Detects when no active physical or Wi-Fi adapter is connected.
 */
public class NoActiveAdapterRule implements DiagnosisRule {

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean matches(DiagnosticReport report) {
        StageResult iface = report.getInterfaceResult();
        if (iface == null) return false;
        boolean hasApipa = iface.getDetail("hasApipa", false);
        int activeCount = iface.getDetail("activeInterfacesCount", 0);
        return (iface.getStatus() == StageStatus.FAILED && !hasApipa) || activeCount == 0;
    }

    @Override
    public DiagnosisOutcome evaluate(DiagnosticReport report) {
        StageResult iface = report.getInterfaceResult();
        List<DiagnosticEvidence> evidence = new ArrayList<>();
        evidence.add(new DiagnosticEvidence(
                DiagnosticReport.STAGE_INTERFACE,
                "No connected network interface with an active IPv4 configuration was detected.",
                Severity.CRITICAL,
                iface != null ? iface.getMessage() : "Interface check missing"
        ));

        List<RecoveryAction> actions = new ArrayList<>();
        actions.add(new RecoveryAction(1,
                "Inspect Physical Cabling or Wi-Fi Switch",
                "Ensure Ethernet cable is firmly plugged into both PC and router port, or verify Wi-Fi is switched ON in Windows Quick Settings.",
                null, null, false,
                "Physical Layer (Layer 1) connection is required before any IP communication can occur."));

        actions.add(new RecoveryAction(2,
                "Check Windows Network Connections",
                "Open Windows Network Connections to check if your Wi-Fi or Ethernet adapter is currently disabled.",
                null, "OPEN_NETWORK_CONNECTIONS", false,
                "Adapters can sometimes be manually disabled or put into power-saving sleep by the OS."));

        actions.add(new RecoveryAction(3,
                "Inspect Device Manager for Driver Faults",
                "If no adapters show up in Windows settings, open Device Manager to verify your network interface card (NIC) driver is installed properly.",
                "devmgmt.msc", null, false,
                "Missing or corrupted network drivers prevent Windows from recognizing the hardware."));

        return new DiagnosisOutcome(
                DiagnosisCategory.NO_ACTIVE_ADAPTER,
                "No Active Network Interface Detected",
                "Your computer is disconnected from the local network. No active Wi-Fi or Ethernet adapters were found.",
                ConfidenceLevel.CERTAIN,
                Severity.CRITICAL,
                evidence,
                actions
        );
    }
}
