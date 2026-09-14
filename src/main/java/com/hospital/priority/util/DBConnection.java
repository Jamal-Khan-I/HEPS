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
            throw new DatabaseOperationException("Failed to connect to MySQL database at " + url + ": " + e.getMessage(), e);
        }
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
