package com.hospital.priority.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Compliance audit log entity (Page 18).
 * Tracks user, role, action, patient, old value, new value, and timestamp.
 */
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private int logId;
    private String username;
    private Role role;
    private String action;
    private String patientToken;
    private String uhid;
    private String oldValue;
    private String newValue;
    private String details;
    private LocalDateTime timestamp;

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(String username, Role role, String action, String patientToken,
                    String uhid, String oldValue, String newValue, String details) {
        this.username = username != null ? username : "system";
        this.role = role != null ? role : Role.ADMIN;
        this.action = action;
        this.patientToken = patientToken;
        this.uhid = uhid;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPatientToken() {
        return patientToken;
    }

    public void setPatientToken(String patientToken) {
        this.patientToken = patientToken;
    }

    public String getUhid() {
        return uhid;
    }

    public void setUhid(String uhid) {
        this.uhid = uhid;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        return timestamp.format(TIME_FMT);
    }
}
