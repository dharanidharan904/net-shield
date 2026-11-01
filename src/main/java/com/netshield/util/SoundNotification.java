package com.netshield.util;

import java.awt.Toolkit;

/**
 * Audio cue helper for alerting users when network state changes or scans finish.
 */
public class SoundNotification {

    public static void playAlert() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {
            // Audio hardware may be absent in some desktop/server environments
        }
    }
}
