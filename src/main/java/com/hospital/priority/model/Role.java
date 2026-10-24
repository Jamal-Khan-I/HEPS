package com.hospital.priority.model;

/**
 * Access control roles for hospital staff.
 */
public enum Role {
    ADMIN("Administrator"),
    RECEPTIONIST("Receptionist"),
    TRIAGE_NURSE("Triage nurse"),
    DOCTOR("Doctor"),
    CHARGE_NURSE("Charge nurse");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static Role fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return RECEPTIONIST;
        }
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(text.trim())) {
                return r;
            }
        }
        return RECEPTIONIST;
    }
}
