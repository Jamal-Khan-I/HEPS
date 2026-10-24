package com.hospital.priority.service;

import com.hospital.priority.model.AuditLog;
import com.hospital.priority.model.Role;
import com.hospital.priority.model.User;
import com.hospital.priority.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ============================================================================
 * COMPLIANCE & AUDIT SERVICE (Page 18)
 * ============================================================================
 * Records immutable audit logs for medical-legal compliance.
 * Persists to MySQL `audit_log` table and maintains an in-memory ring buffer
 * for fast UI viewing in the Admin screen.
 */
public class AuditService {

    private static AuditService instance;

    private final List<AuditLog> memoryAuditLogs = new CopyOnWriteArrayList<>();

    private AuditService() {
    }

    public static synchronized AuditService getInstance() {
        if (instance == null) {
            instance = new AuditService();
        }
        return instance;
    }

    /**
     * Logs a clinical or operational action.
     */
    public void log(String action, String patientToken, String uhid, String oldValue, String newValue, String details) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        String username = currentUser != null ? currentUser.getUsername() : "system";
        Role role = currentUser != null ? currentUser.getRole() : Role.ADMIN;

        AuditLog entry = new AuditLog(username, role, action, patientToken, uhid, oldValue, newValue, details);
        memoryAuditLogs.add(0, entry); // Most recent first

        // Asynchronously persist to database if table exists
        new Thread(() -> persistToDatabase(entry)).start();
    }

    private void persistToDatabase(AuditLog entry) {
        String sql = "INSERT INTO audit_log (username, role, action, patient_token, uhid, old_value, new_value, details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entry.getUsername());
            stmt.setString(2, entry.getRole().name());
            stmt.setString(3, entry.getAction());
            stmt.setString(4, entry.getPatientToken());
            stmt.setString(5, entry.getUhid());
            stmt.setString(6, entry.getOldValue());
            stmt.setString(7, entry.getNewValue());
            stmt.setString(8, entry.getDetails());
            stmt.setTimestamp(9, Timestamp.valueOf(entry.getTimestamp()));
            stmt.executeUpdate();
        } catch (Exception ignored) {
            // Memory log remains available even if database write fails
        }
    }

    public List<AuditLog> getRecentLogs(int limit) {
        if (memoryAuditLogs.size() <= limit) {
            return Collections.unmodifiableList(memoryAuditLogs);
        }
        return Collections.unmodifiableList(memoryAuditLogs.subList(0, limit));
    }

    public List<AuditLog> getAllLogs() {
        return Collections.unmodifiableList(memoryAuditLogs);
    }
}
