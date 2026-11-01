package com.netshield;

import com.netshield.util.SafeCommandRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SafeCommandRunnerTest {

    private SafeCommandRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SafeCommandRunner();
    }

    @Test
    @DisplayName("Whitelisted read-only command executes safely")
    void testAllowedDiagnosticCommand() {
        SafeCommandRunner.CommandResult result = runner.execute(
                Arrays.asList("ipconfig", "/displaydns"), 5
        );
        assertNotNull(result);
        assertFalse(result.isTimedOut());
    }

    @Test
    @DisplayName("Rejects unauthorized binaries outside whitelist")
    void testRejectUnauthorizedBinary() {
        assertThrows(SecurityException.class, () -> {
            runner.execute(Arrays.asList("powershell.exe", "-Command", "Get-Process"), 5);
        });

        assertThrows(SecurityException.class, () -> {
            runner.execute(Arrays.asList("cmd.exe", "/c", "dir"), 5);
        });

        assertThrows(SecurityException.class, () -> {
            runner.execute(Arrays.asList("reg.exe", "query", "HKLM"), 5);
        });
    }

    @Test
    @DisplayName("Rejects destructive parameter attempts on whitelisted tools")
    void testRejectDestructiveParameters() {
        // netsh set is blocked
        assertThrows(SecurityException.class, () -> {
            runner.execute(Arrays.asList("netsh", "interface", "ipv4", "set", "address", "Wi-Fi", "dhcp"), 5);
        });

        // ipconfig /release is blocked
        assertThrows(SecurityException.class, () -> {
            runner.execute(Arrays.asList("ipconfig", "/release"), 5);
        });

        // format keyword is blocked
        assertThrows(SecurityException.class, () -> {
            runner.execute(Arrays.asList("ping", "format"), 5);
        });
    }

    @Test
    @DisplayName("Rejects null or empty command")
    void testNullOrEmptyCommand() {
        assertThrows(IllegalArgumentException.class, () -> {
            runner.execute(null, 5);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            runner.execute(Collections.emptyList(), 5);
        });
    }
}
