package com.hospital.priority.util;

import com.hospital.priority.exception.DatabaseOperationException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Thread-safe Database Connection utility.
 * Demonstrates clean JDBC configuration management without hardcoded credentials in code.
 * Reads connection settings dynamically from config.properties in classpath.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;
    private static boolean initialized = false;
    private static boolean autoStartAttempted = false;

    private DBConnection() {
        // Private constructor prevents direct instantiation
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("Warning: config.properties could not be loaded from classpath: " + e.getMessage());
        }

        url = props.getProperty("db.url", "jdbc:mysql://127.0.0.1:3306/hospital_emergency_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        user = props.getProperty("db.user", "root");
        password = props.getProperty("db.password", "");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found in classpath!", e);
        }

        initialized = true;
    }

    /**
     * Obtains a new JDBC connection to MySQL.
     * If the server is offline on localhost, attempts auto-recovery once before failing.
     * Callers must close the returned connection via try-with-resources.
     *
     * @return active java.sql.Connection
     * @throws DatabaseOperationException if connection fails
     */
    public static Connection getConnection() throws DatabaseOperationException {
        if (!initialized) {
            initialize();
        }
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            if (!autoStartAttempted && (url.contains("127.0.0.1") || url.contains("localhost"))) {
                autoStartAttempted = true;
                if (tryAutoStartLocalMySQL()) {
                    try {
                        return DriverManager.getConnection(url, user, password);
                    } catch (SQLException retryEx) {
                        throw new DatabaseOperationException("Failed to connect to MySQL database after auto-recovery: " + retryEx.getMessage(), retryEx);
                    }
                }
            }
            throw new DatabaseOperationException("Failed to connect to MySQL database at " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to automatically launch local MySQL Server daemon if it is stopped after a reboot.
     */
    private static synchronized boolean tryAutoStartLocalMySQL() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("win")) {
                return false;
            }

            java.io.File mysqld = new java.io.File("C:/Program Files/MySQL/MySQL Server 8.4/bin/mysqld.exe");
            java.io.File myIni = new java.io.File("C:/ProgramData/MySQL/MySQL Server 8.4/my.ini");

            if (!mysqld.exists() || !myIni.exists()) {
                return false;
            }

            System.out.println(">>> [Auto-Recovery] MySQL Server is offline. Starting local server...");
            new ProcessBuilder(mysqld.getAbsolutePath(), "--defaults-file=" + myIni.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();

            // Wait up to 6 seconds for port 3306 to accept connections
            for (int i = 0; i < 12; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}

                try (java.net.Socket socket = new java.net.Socket("127.0.0.1", 3306)) {
                    System.out.println(">>> [Auto-Recovery] MySQL Server started successfully.");
                    return true;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ex) {
            System.err.println(">>> [Auto-Recovery] Note: Could not auto-start MySQL: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Helper to test database connectivity during application boot or settings check.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Allows dynamic reconfiguration if settings change at runtime.
     */
    public static synchronized void setCredentials(String customUrl, String customUser, String customPass) {
        url = customUrl;
        user = customUser;
        password = customPass;
        initialized = true;
    }
}
