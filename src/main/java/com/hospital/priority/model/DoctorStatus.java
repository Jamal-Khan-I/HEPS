package com.hospital.priority.model;

/**
 * Represents doctor availability status in the emergency department.
 */
public enum DoctorStatus {
    AVAILABLE("Available", "#16A34A", "status-doctor-available"),
    BUSY("Busy (With Patient)", "#EA580C", "status-doctor-busy"),
    OFF_DUTY("Off Duty", "#64748B", "status-doctor-offduty");

    private final String displayName;
    private final String colorHex;
    private final String cssClass;

    DoctorStatus(String displayName, String colorHex, String cssClass) {
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static DoctorStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return AVAILABLE;
        }
        String clean = text.trim().toUpperCase().replace(" ", "_");
        for (DoctorStatus s : values()) {
            if (s.name().equals(clean) || s.displayName.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return AVAILABLE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
