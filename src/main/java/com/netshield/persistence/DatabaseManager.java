package com.netshield.persistence;

import java.io.File;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages SQLite database connection and automated schema initialization.
 * Supports file-based local persistence as well as in-memory databases for tests.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private final String jdbcUrl;
    private final boolean isInMemory;
    private Connection physicalInMemoryConnection;

    public DatabaseManager() {
        this(resolveDefaultDbPath());
    }

    public DatabaseManager(String customJdbcUrl) {
        if (customJdbcUrl.startsWith("jdbc:sqlite:")) {
            this.jdbcUrl = customJdbcUrl;
        } else {
            this.jdbcUrl = "jdbc:sqlite:" + customJdbcUrl;
        }
        this.isInMemory = this.jdbcUrl.equals("jdbc:sqlite::memory:") || this.jdbcUrl.contains("mode=memory");
        initDatabase();
    }

    public static DatabaseManager inMemory() {
        return new DatabaseManager("jdbc:sqlite::memory:");
    }

    private static String resolveDefaultDbPath() {
        String appData = System.getenv("APPDATA");
        File dir;
        if (appData != null && !appData.isBlank()) {
            dir = new File(appData, "NetShield");
        } else {
            dir = new File(System.getProperty("user.home"), ".netshield");
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dbFile = new File(dir, "netshield.db");
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public synchronized Connection getConnection() throws SQLException {
        if (isInMemory) {
            if (physicalInMemoryConnection == null || physicalInMemoryConnection.isClosed()) {
                physicalInMemoryConnection = DriverManager.getConnection(jdbcUrl);
            }
            return wrapConnection(physicalInMemoryConnection);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    private Connection wrapConnection(Connection target) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        // Prevent try-with-resources from destroying in-memory database
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return target.isClosed();
                    }
                    return method.invoke(target, args);
                }
        );
    }

    private void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Sessions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS diagnostic_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_uuid TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    category TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    confidence TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    gateway_latency REAL DEFAULT 0,
                    dns_latency REAL DEFAULT 0,
                    http_latency REAL DEFAULT 0,
                    packet_loss REAL DEFAULT 0,
                    jitter REAL DEFAULT 0
                );
            """);

            // Stages detail table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS diagnostic_stages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_uuid TEXT NOT NULL,
                    stage_name TEXT NOT NULL,
                    status TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    message TEXT NOT NULL
                );
            """);

            // Settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL
                );
            """);

            LOGGER.info("Database schema initialized successfully at " + jdbcUrl);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite database schema: " + e.getMessage(), e);
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void close() {
        if (physicalInMemoryConnection != null) {
            try {
                physicalInMemoryConnection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
