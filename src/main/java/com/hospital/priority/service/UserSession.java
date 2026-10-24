package com.hospital.priority.service;

import com.hospital.priority.model.Role;
import com.hospital.priority.model.User;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * USER SESSION & ROLE-BASED ACCESS CONTROL (RBAC) MANAGER
 * ============================================================================
 * Maintains active authenticated user session and enforces screen visibility
 * according to the Page 18 Permission Matrix.
 */
public class UserSession {

    private static UserSession instance;

    private User currentUser;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;

    private UserSession() {
        // Default demo session for immediate development/testing (Triage nurse Priya S.)
        this.currentUser = new User(1, "nurse.priya", "", "Priya S.", Role.TRIAGE_NURSE, LocalDateTime.now());
        this.loginTime = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public synchronized void setLoggedInUser(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
    }

    public synchronized void logout() {
        this.currentUser = null;
        this.loginTime = null;
        this.lastActivityTime = null;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized Role getRole() {
        return currentUser != null ? currentUser.getRole() : Role.RECEPTIONIST;
    }

    public synchronized String getStaffName() {
        return currentUser != null ? currentUser.getFullName() : "Guest";
    }

    public synchronized String getStaffInitials() {
        if (currentUser == null || currentUser.getFullName() == null || currentUser.getFullName().isEmpty()) {
            return "??";
        }
        String[] parts = currentUser.getFullName().trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public synchronized void touchActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }

    public synchronized boolean isInactiveOver(long minutes) {
        if (lastActivityTime == null) return false;
        return java.time.Duration.between(lastActivityTime, LocalDateTime.now()).toMinutes() >= minutes;
    }

    // =========================================================================
    // PAGE 18 PERMISSION MATRIX: "WHO SEES WHICH SCREEN"
    // =========================================================================

    public boolean canAccessCommandCenter() {
        return true; // All roles
    }

    public boolean canAccessRegisterPatient() {
        Role r = getRole();
        return r == Role.RECEPTIONIST || r == Role.TRIAGE_NURSE || r == Role.CHARGE_NURSE || r == Role.ADMIN;
    }

    public boolean canAccessEmergencyQueue() {
        Role r = getRole();
        return r == Role.TRIAGE_NURSE || r == Role.DOCTOR || r == Role.CHARGE_NURSE || r == Role.ADMIN;
    }

    public boolean canAccessBayBoard() {
        Role r = getRole();
        return r == Role.TRIAGE_NURSE || r == Role.CHARGE_NURSE || r == Role.ADMIN;
    }

    public boolean canAccessDoctorWorkspace() {
        Role r = getRole();
        return r == Role.DOCTOR || r == Role.ADMIN;
    }

    public boolean canAccessOrdersAndResults() {
        Role r = getRole();
        return r == Role.TRIAGE_NURSE || r == Role.DOCTOR || r == Role.CHARGE_NURSE || r == Role.ADMIN;
    }

    public boolean canAccessOrdersResults() {
        return canAccessOrdersAndResults();
    }

    public boolean canAccessDisposition() {
        Role r = getRole();
        return r == Role.DOCTOR || r == Role.CHARGE_NURSE || r == Role.ADMIN;
    }

    public boolean canAccessPatientRecords() {
        return true; // All roles
    }

    public boolean canAccessReports() {
        Role r = getRole();
        return r == Role.CHARGE_NURSE || r == Role.ADMIN;
    }

    public boolean canAccessAlerts() {
        Role r = getRole();
        return r != Role.RECEPTIONIST; // Triage nurse, Doctor, Charge nurse, Admin
    }

    public boolean canAccessAdmin() {
        return getRole() == Role.ADMIN;
    }

    public boolean canAccessMassCasualty() {
        Role r = getRole();
        return r == Role.CHARGE_NURSE || r == Role.ADMIN;
    }
}
