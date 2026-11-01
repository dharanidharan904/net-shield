package com.netshield.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes strictly whitelisted, non-destructive diagnostic system commands.
 * Adheres to safety constraints: no administrator execution, no adapter modifications,
 * no firewall alterations, and strict argument validation.
 */
public class SafeCommandRunner {
    private static final Logger LOGGER = Logger.getLogger(SafeCommandRunner.class.getName());

    // Permitted read-only or harmless diagnostic binaries
    private static final Set<String> ALLOWED_COMMANDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "ping", "ping.exe",
                    "ipconfig", "ipconfig.exe",
                    "route", "route.exe",
                    "nslookup", "nslookup.exe",
                    "netsh.exe", "netsh",
                    "tracert.exe", "tracert"
            ))
    );

    // Permitted harmless subcommands for netsh (strictly read-only show commands)
    private static final List<String> BLOCKED_KEYWORD_PATTERNS = Arrays.asList(
            "set", "delete", "add", "reset", "uninstall", "disable", "firewall",
            "format", "del", "rmdir", "shutdown", "powershell", "cmd.exe", "reg",
            "takeown", "icacls", "sc.exe", "stop"
    );

    public static class CommandResult {
        private final int exitCode;
        private final List<String> outputLines;
        private final boolean timedOut;
        private final String rawOutput;

        public CommandResult(int exitCode, List<String> outputLines, boolean timedOut) {
            this.exitCode = exitCode;
            this.outputLines = outputLines != null ? Collections.unmodifiableList(outputLines) : Collections.emptyList();
            this.timedOut = timedOut;
            this.rawOutput = String.join("\n", this.outputLines);
        }

        public int getExitCode() {
            return exitCode;
        }

        public List<String> getOutputLines() {
            return outputLines;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public String getRawOutput() {
            return rawOutput;
        }

        public boolean isSuccess() {
            return exitCode == 0 && !timedOut;
        }
    }

    /**
     * Executes a safe command with explicit argument array to prevent shell injection.
     */
    public CommandResult execute(List<String> command, int timeoutSeconds) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }

        String baseBinary = command.get(0).toLowerCase();
        // Remove path prefix if any
        if (baseBinary.contains("\\")) {
            baseBinary = baseBinary.substring(baseBinary.lastIndexOf('\\') + 1);
        } else if (baseBinary.contains("/")) {
            baseBinary = baseBinary.substring(baseBinary.lastIndexOf('/') + 1);
        }

        if (!ALLOWED_COMMANDS.contains(baseBinary)) {
            LOGGER.warning("Blocked unpermitted command execution attempt: " + baseBinary);
            throw new SecurityException("Command rejected: '" + baseBinary + "' is not in the safe diagnostic whitelist.");
        }

        // Check for any destructive parameter
        for (String arg : command) {
            String lower = arg.toLowerCase();
            for (String blocked : BLOCKED_KEYWORD_PATTERNS) {
                // Allow "ipconfig /all", "ipconfig /flushdns", "ipconfig /displaydns"
                if (baseBinary.startsWith("ipconfig")) {
                    if (lower.equals("/all") || lower.equals("/flushdns") || lower.equals("/displaydns")) {
                        continue;
                    }
                    if (lower.equals("/release") || lower.equals("/renew")) {
                        LOGGER.warning("Blocked automatic IP release/renew - guidance only");
                        throw new SecurityException("Automatic adapter reconfiguration is disallowed by safety policy.");
                    }
                }

                // If netsh is invoked, ONLY "interface ipv4 show ..." or "wlan show ..." is safe
                if (baseBinary.startsWith("netsh")) {
                    if (lower.equals("set") || lower.equals("add") || lower.equals("delete") || lower.equals("reset")) {
                        throw new SecurityException("Modifying network configuration via netsh is disallowed by safety policy.");
                    }
                }

                if (lower.contains(blocked)) {
                    LOGGER.warning("Blocked unpermitted command parameter: " + arg);
                    throw new SecurityException("Parameter rejected: '" + arg + "' contains restricted keyword '" + blocked + "'");
                }
            }
        }

        List<String> lines = new ArrayList<>();
        Process process = null;
        boolean timedOut = false;
        int exitCode = -1;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                timedOut = true;
                process.destroyForcibly();
            } else {
                exitCode = process.exitValue();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            timedOut = true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error running diagnostic command: " + command, e);
            lines.add("Command execution error: " + e.getMessage());
        }

        return new CommandResult(exitCode, lines, timedOut);
    }
}
