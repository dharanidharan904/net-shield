package com.netshield.persistence;

import com.netshield.models.UserSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for persisting and loading user configuration settings.
 */
public class SettingsDao {
    private static final Logger LOGGER = Logger.getLogger(SettingsDao.class.getName());
    private final DatabaseManager dbManager;

    public SettingsDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public synchronized UserSettings loadSettings() {
        UserSettings settings = new UserSettings();
        Map<String, String> map = new HashMap<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT setting_key, setting_value FROM app_settings;")) {

            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }

            if (map.containsKey("dnsTestDomains")) {
                settings.setDnsTestDomains(Arrays.asList(map.get("dnsTestDomains").split(",")));
            }
            if (map.containsKey("directDnsServers")) {
                settings.setDirectDnsServers(Arrays.asList(map.get("directDnsServers").split(",")));
            }
            if (map.containsKey("httpsTargetUrls")) {
                settings.setHttpsTargetUrls(Arrays.asList(map.get("httpsTargetUrls").split(",")));
            }
            if (map.containsKey("captiveCheckUrl")) {
                settings.setCaptiveCheckUrl(map.get("captiveCheckUrl"));
            }
            if (map.containsKey("timeoutMs")) {
                settings.setTimeoutMs(Integer.parseInt(map.get("timeoutMs")));
            }
            if (map.containsKey("pingCount")) {
                settings.setPingCount(Integer.parseInt(map.get("pingCount")));
            }
            if (map.containsKey("continuousIntervalSec")) {
                settings.setContinuousIntervalSec(Integer.parseInt(map.get("continuousIntervalSec")));
            }
            if (map.containsKey("soundAlertsEnabled")) {
                settings.setSoundAlertsEnabled(Boolean.parseBoolean(map.get("soundAlertsEnabled")));
            }
            if (map.containsKey("autoHealingEnabled")) {
                settings.setAutoHealingEnabled(Boolean.parseBoolean(map.get("autoHealingEnabled")));
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load settings, using defaults: " + e.getMessage(), e);
        }
        return settings;
    }

    public synchronized void saveSettings(UserSettings settings) {
        String sql = "INSERT OR REPLACE INTO app_settings (setting_key, setting_value) VALUES (?, ?);";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            upsert(stmt, "dnsTestDomains", String.join(",", settings.getDnsTestDomains()));
            upsert(stmt, "directDnsServers", String.join(",", settings.getDirectDnsServers()));
            upsert(stmt, "httpsTargetUrls", String.join(",", settings.getHttpsTargetUrls()));
            upsert(stmt, "captiveCheckUrl", settings.getCaptiveCheckUrl());
            upsert(stmt, "timeoutMs", String.valueOf(settings.getTimeoutMs()));
            upsert(stmt, "pingCount", String.valueOf(settings.getPingCount()));
            upsert(stmt, "continuousIntervalSec", String.valueOf(settings.getContinuousIntervalSec()));
            upsert(stmt, "soundAlertsEnabled", String.valueOf(settings.isSoundAlertsEnabled()));
            upsert(stmt, "autoHealingEnabled", String.valueOf(settings.isAutoHealingEnabled()));

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist user settings: " + e.getMessage(), e);
        }
    }

    private void upsert(PreparedStatement stmt, String key, String value) throws SQLException {
        stmt.setString(1, key);
        stmt.setString(2, value);
        stmt.executeUpdate();
    }
}
