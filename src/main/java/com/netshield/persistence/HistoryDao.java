package com.netshield.persistence;

import com.netshield.models.ConfidenceLevel;
import com.netshield.models.DiagnosisCategory;
import com.netshield.models.DiagnosisOutcome;
import com.netshield.models.DiagnosticReport;
import com.netshield.models.HistoricalSession;
import com.netshield.models.Severity;
import com.netshield.models.StageResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for persisting and querying diagnostic scan history in SQLite.
 */
public class HistoryDao {
    private static final Logger LOGGER = Logger.getLogger(HistoryDao.class.getName());
    private final DatabaseManager dbManager;

    public HistoryDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public synchronized void saveSession(DiagnosticReport report, DiagnosisOutcome outcome) {
        String sessionSql = """
            INSERT INTO diagnostic_sessions (
                session_uuid, timestamp, category, severity, confidence, summary,
                gateway_latency, dns_latency, http_latency, packet_loss, jitter
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        String stageSql = """
            INSERT INTO diagnostic_stages (session_uuid, stage_name, status, duration_ms, message)
            VALUES (?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement sStmt = conn.prepareStatement(sessionSql);
                 PreparedStatement stgStmt = conn.prepareStatement(stageSql)) {

                // Extract metric values
                double gatewayLatency = 0.0;
                StageResult gw = report.getGatewayResult();
                if (gw != null && gw.getDetails().containsKey("latencyMs")) {
                    Object val = gw.getDetails().get("latencyMs");
                    if (val instanceof Number n) gatewayLatency = n.doubleValue();
                }

                double dnsLatency = 0.0;
                StageResult dns = report.getDnsResult();
                if (dns != null && dns.getDetails().containsKey("avgSystemLatencyMs")) {
                    Object val = dns.getDetails().get("avgSystemLatencyMs");
                    if (val instanceof Number n) dnsLatency = n.doubleValue();
                }

                double httpLatency = 0.0;
                StageResult http = report.getHttpResult();
                if (http != null && http.getDetails().containsKey("avgHttpLatencyMs")) {
                    Object val = http.getDetails().get("avgHttpLatencyMs");
                    if (val instanceof Number n) httpLatency = n.doubleValue();
                }

                double packetLoss = report.getPacketLossPercentage();
                double jitter = 0.0;
                StageResult latency = report.getLatencyResult();
                if (latency != null && latency.getDetails().containsKey("jitterMs")) {
                    Object val = latency.getDetails().get("jitterMs");
                    if (val instanceof Number n) jitter = n.doubleValue();
                }

                sStmt.setString(1, report.getId());
                sStmt.setString(2, report.getTimestamp().toString());
                sStmt.setString(3, outcome.getCategory().name());
                sStmt.setString(4, outcome.getSeverity().name());
                sStmt.setString(5, outcome.getConfidence().name());
                sStmt.setString(6, outcome.getSummary());
                sStmt.setDouble(7, gatewayLatency);
                sStmt.setDouble(8, dnsLatency);
                sStmt.setDouble(9, httpLatency);
                sStmt.setDouble(10, packetLoss);
                sStmt.setDouble(11, jitter);
                sStmt.executeUpdate();

                // Save individual stage results
                for (StageResult stage : report.getStageResults().values()) {
                    stgStmt.setString(1, report.getId());
                    stgStmt.setString(2, stage.getStageName());
                    stgStmt.setString(3, stage.getStatus().name());
                    stgStmt.setLong(4, stage.getDurationMs());
                    stgStmt.setString(5, stage.getMessage());
                    stgStmt.addBatch();
                }
                stgStmt.executeBatch();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist diagnostic session: " + e.getMessage(), e);
        }
    }

    public List<HistoricalSession> getRecentSessions(int limit) {
        String sql = "SELECT * FROM diagnostic_sessions ORDER BY id DESC LIMIT ?;";
        List<HistoricalSession> list = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error querying diagnostic history: " + e.getMessage(), e);
        }
        return list;
    }

    public List<HistoricalSession> getAllSessions() {
        return getRecentSessions(1000);
    }

    public double getUptimePercentage() {
        String sql = """
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN category = 'HEALTHY_ONLINE' THEN 1 ELSE 0 END) as healthy
            FROM diagnostic_sessions;
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int total = rs.getInt("total");
                int healthy = rs.getInt("healthy");
                if (total == 0) return 100.0;
                return ((double) healthy / total) * 100.0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error calculating uptime percentage: " + e.getMessage(), e);
        }
        return 100.0;
    }

    public double getAverageLatency() {
        String sql = "SELECT AVG(http_latency) as avg_lat FROM diagnostic_sessions WHERE http_latency > 0;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("avg_lat");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error calculating average latency: " + e.getMessage(), e);
        }
        return 0.0;
    }

    public int getTotalOutagesCount() {
        String sql = "SELECT COUNT(*) as count FROM diagnostic_sessions WHERE category != 'HEALTHY_ONLINE';";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error counting outages: " + e.getMessage(), e);
        }
        return 0;
    }

    public void clearHistory() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM diagnostic_stages;");
            stmt.execute("DELETE FROM diagnostic_sessions;");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to clear history: " + e.getMessage(), e);
        }
    }

    private HistoricalSession mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String uuid = rs.getString("session_uuid");
        Instant timestamp = Instant.parse(rs.getString("timestamp"));

        DiagnosisCategory cat;
        try {
            cat = DiagnosisCategory.valueOf(rs.getString("category"));
        } catch (Exception e) {
            cat = DiagnosisCategory.INCONCLUSIVE;
        }

        Severity sev;
        try {
            sev = Severity.valueOf(rs.getString("severity"));
        } catch (Exception e) {
            sev = Severity.INFO;
        }

        ConfidenceLevel conf;
        try {
            conf = ConfidenceLevel.valueOf(rs.getString("confidence"));
        } catch (Exception e) {
            conf = ConfidenceLevel.MEDIUM;
        }

        String summary = rs.getString("summary");
        double gwLat = rs.getDouble("gateway_latency");
        double dnsLat = rs.getDouble("dns_latency");
        double httpLat = rs.getDouble("http_latency");
        double loss = rs.getDouble("packet_loss");
        double jitter = rs.getDouble("jitter");

        return new HistoricalSession(id, uuid, timestamp, cat, sev, conf, summary, gwLat, dnsLat, httpLat, loss, jitter);
    }
}
