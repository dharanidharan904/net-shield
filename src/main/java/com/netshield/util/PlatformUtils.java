package com.netshield.util;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Platform helper utilities with focus on safe Windows desktop integration.
 */
public class PlatformUtils {
    private static final Logger LOGGER = Logger.getLogger(PlatformUtils.class.getName());
    private static final SafeCommandRunner COMMAND_RUNNER = new SafeCommandRunner();

    private static final Pattern ROUTE_PRINT_GATEWAY_PATTERN = Pattern.compile(
            "^\\s*0\\.0\\.0\\.0\\s+0\\.0\\.0\\.0\\s+([0-9]{1,3}(?:\\.[0-9]{1,3}){3})\\s+([0-9]{1,3}(?:\\.[0-9]{1,3}){3})",
            Pattern.MULTILINE
    );

    /**
     * Determines the IPv4 Default Gateway address on Windows using route print 0.0.0.0.
     * Returns null if not detected.
     */
    public static String detectDefaultGatewayWindows() {
        try {
            SafeCommandRunner.CommandResult res = COMMAND_RUNNER.execute(
                    Arrays.asList("route", "print", "0.0.0.0"), 5
            );
            if (res.isSuccess()) {
                Matcher matcher = ROUTE_PRINT_GATEWAY_PATTERN.matcher(res.getRawOutput());
                if (matcher.find()) {
                    String gatewayIp = matcher.group(1);
                    if (!gatewayIp.equals("0.0.0.0") && !gatewayIp.equals("127.0.0.1")) {
                        return gatewayIp;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read gateway via route print: " + e.getMessage());
        }
        return null;
    }

    /**
     * Opens Windows Network Connections (ncpa.cpl) safely.
     */
    public static void openWindowsNetworkConnections() {
        try {
            new ProcessBuilder("control.exe", "netconnections").start();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not open ncpa.cpl", e);
        }
    }

    /**
     * Opens Windows 10/11 Network Settings page (ms-settings:network).
     */
    public static void openWindowsNetworkSettings() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("ms-settings:network"));
            } else {
                new ProcessBuilder("explorer.exe", "ms-settings:network").start();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not open ms-settings:network", e);
        }
    }

    /**
     * Opens a URL in the user's default web browser (used for Captive Portal login).
     */
    public static void openWebBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                new ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", url).start();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to launch browser for url: " + url, e);
        }
    }

    /**
     * Safely clears DNS cache via ipconfig /flushdns (requires no elevation, safe).
     */
    public static SafeCommandRunner.CommandResult flushDnsCache() {
        return COMMAND_RUNNER.execute(Arrays.asList("ipconfig", "/flushdns"), 5);
    }
}
