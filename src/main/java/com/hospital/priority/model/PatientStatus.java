package com.hospital.priority.model;

/**
 * Represents the lifecycle stages of an emergency patient in the hospital.
 */
public enum PatientStatus {
    WAITING("Waiting", "#2563EB", "status-waiting"),
    IN_TREATMENT("In Treatment", "#7C3AED", "status-treatment"),
    COMPLETED("Completed", "#059669", "status-completed"),
    CANCELLED("Cancelled", "#64748B", "status-cancelled");

    private final String displayName;
    private final String colorHex;
    private final String cssClass;

    PatientStatus(String displayName, String colorHex, String cssClass) {
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

    public static PatientStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return WAITING;
        }
        String clean = text.trim().toUpperCase().replace(" ", "_");
        for (PatientStatus s : values()) {
            if (s.name().equals(clean) || s.displayName.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return WAITING;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
