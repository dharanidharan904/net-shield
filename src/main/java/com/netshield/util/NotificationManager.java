package com.netshield.util;

import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;
import com.netshield.models.WifiNetworkInfo;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * System Tray and Automated State-Change Notification Manager.
 * Immediately alerts the host device when network connectivity, adapter IP, or Wi-Fi state shifts.
 */
public class NotificationManager {
    private static final Logger LOGGER = Logger.getLogger(NotificationManager.class.getName());

    private TrayIcon trayIcon;
    private boolean traySupported;

    // Previous state tracking
    private DiagnosisCategory previousCategory = null;
    private Severity previousSeverity = null;
    private String previousIp = null;
    private String previousSsid = null;
    private boolean initialized = false;

    public NotificationManager(Runnable onOpenAppRequested, Runnable onRunDiagnosticsRequested) {
        initSystemTray(onOpenAppRequested, onRunDiagnosticsRequested);
    }

    private void initSystemTray(Runnable onOpenApp, Runnable onRunDiag) {
        if (!SystemTray.isSupported()) {
            LOGGER.info("SystemTray is not supported on this host environment.");
            this.traySupported = false;
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image iconImage = createShieldIcon(Color.BLUE);

            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("Open NetShield Dashboard");
            openItem.addActionListener(e -> { if (onOpenApp != null) onOpenApp.run(); });

            MenuItem diagItem = new MenuItem("Run Network Diagnostics");
            diagItem.addActionListener(e -> { if (onRunDiag != null) onRunDiag.run(); });

            MenuItem exitItem = new MenuItem("Exit NetShield");
            exitItem.addActionListener(e -> System.exit(0));

            popup.add(openItem);
            popup.add(diagItem);
            popup.addSeparator();
            popup.add(exitItem);

            this.trayIcon = new TrayIcon(iconImage, "NetShield — Internet Failure & Recovery Tool", popup);
            this.trayIcon.setImageAutoSize(true);
            this.trayIcon.addActionListener(e -> { if (onOpenApp != null) onOpenApp.run(); });

            tray.add(this.trayIcon);
            this.traySupported = true;
            LOGGER.info("System Tray icon registered successfully.");
        } catch (AWTException e) {
            LOGGER.log(Level.WARNING, "Failed to register System Tray icon: " + e.getMessage());
            this.traySupported = false;
        }
    }

    /**
     * Checks if a network status change occurred and automatically triggers notifications on this device.
     * Returns a human-readable event description if a change was detected, or null if steady state.
     */
    public synchronized String checkAndNotifyStateChange(DiagnosticReport report, DiagnosisOutcome outcome, WifiNetworkInfo wifiInfo) {
        DiagnosisCategory currentCategory = outcome.getCategory();
        Severity currentSeverity = outcome.getSeverity();

        StageResult iface = report.getInterfaceResult();
        String currentIp = (iface != null) ? iface.getDetail("primaryIpv4", "N/A") : "N/A";
        String currentSsid = (wifiInfo != null && wifiInfo.isConnected()) ? wifiInfo.getSsid() : "Ethernet / None";

        // Update Tray Icon Color
        updateTrayIconColor(currentSeverity);

        if (!initialized) {
            this.previousCategory = currentCategory;
            this.previousSeverity = currentSeverity;
            this.previousIp = currentIp;
            this.previousSsid = currentSsid;
            this.initialized = true;
            return null; // Initial scan baseline
        }

        boolean categoryChanged = currentCategory != previousCategory;
        boolean severityChanged = currentSeverity != previousSeverity;
        boolean ipChanged = currentIp != null && !currentIp.equals("N/A") && !currentIp.equals(previousIp);
        boolean ssidChanged = wifiInfo != null && wifiInfo.isConnected() && !currentSsid.equals(previousSsid);

        String changeMessage = null;

        if (severityChanged || categoryChanged) {
            if (currentSeverity == Severity.HEALTHY) {
                changeMessage = "Internet Connection Restored! (" + outcome.getTitle() + ")";
                showToast("Connection Restored", changeMessage, TrayIcon.MessageType.INFO);
            } else if (currentSeverity == Severity.CRITICAL) {
                changeMessage = "Internet Outage Detected: " + outcome.getTitle();
                showToast("Network Outage Alert", changeMessage, TrayIcon.MessageType.ERROR);
            } else if (currentSeverity == Severity.WARNING) {
                changeMessage = "Network Quality Degraded: " + outcome.getTitle();
                showToast("Network Degradation", changeMessage, TrayIcon.MessageType.WARNING);
            }
            SoundNotification.playAlert();
        } else if (ipChanged) {
            changeMessage = "Device IPv4 Address Changed: " + previousIp + " -> " + currentIp;
            showToast("Network Configuration Change", changeMessage, TrayIcon.MessageType.INFO);
        } else if (ssidChanged) {
            changeMessage = "Wi-Fi Access Point Changed: " + previousSsid + " -> " + currentSsid;
            showToast("Wi-Fi Roaming Event", changeMessage, TrayIcon.MessageType.INFO);
        }

        // Update tracking state
        this.previousCategory = currentCategory;
        this.previousSeverity = currentSeverity;
        this.previousIp = currentIp;
        this.previousSsid = currentSsid;

        return changeMessage;
    }

    public void showToast(String title, String message, TrayIcon.MessageType type) {
        if (traySupported && trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        }
    }

    private void updateTrayIconColor(Severity severity) {
        if (!traySupported || trayIcon == null) return;

        Color color = switch (severity) {
            case HEALTHY -> new Color(16, 185, 129); // Emerald
            case CRITICAL -> new Color(239, 68, 68); // Red
            case WARNING -> new Color(245, 158, 11); // Amber
            default -> new Color(59, 130, 246);      // Blue
        };

        trayIcon.setImage(createShieldIcon(color));
    }

    private Image createShieldIcon(Color color) {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw shield polygon
        int[] xPoints = {8, 14, 14, 8, 2, 2};
        int[] yPoints = {1, 3, 9, 15, 9, 3};

        g.setColor(color);
        g.fillPolygon(xPoints, yPoints, 6);

        g.setColor(Color.WHITE);
        g.drawPolygon(xPoints, yPoints, 6);

        g.dispose();
        return img;
    }
}
