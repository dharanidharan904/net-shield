package com.netshield;

/**
 * Bootstrap launcher for NetShield.
 * Bypasses JavaFX modular runtime restrictions when running as a fat JAR or direct classpath execution.
 */
public class NetShieldLauncher {
    public static void main(String[] args) {
        NetShieldApp.main(args);
    }
}
