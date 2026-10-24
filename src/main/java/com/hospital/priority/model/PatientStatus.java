package com.hospital.priority.model;

/**
 * Represents the lifecycle stages of an emergency patient in the hospital.
 * Enforces the complete clinical pipeline from Page 18:
 * Registered → Triaged → Waiting → Assigned (bay + doctor) → In treatment →
 * Observation or results pending → Disposition → Completed
 */
public enum PatientStatus {
    REGISTERED("Registered", "#0D9488", "status-registered"),
    TRIAGED("Triaged", "#0284C7", "status-triaged"),
    WAITING("Waiting", "#D97706", "status-waiting"),
    ASSIGNED("Assigned", "#7C3AED", "status-assigned"),
    IN_TREATMENT("In treatment", "#2563EB", "status-treatment"),
    OBSERVATION("Observation", "#8B5CF6", "status-observation"),
    LAB_PENDING("Results pending", "#EA580C", "status-lab-pending"),
    DISPOSITION("Disposition", "#0D9488", "status-disposition"),
    COMPLETED("Completed", "#059669", "status-completed"),
    DISCHARGED("Discharged", "#16A34A", "status-discharged"),
    ADMITTED("Admitted", "#8B5CF6", "status-admitted"),
    TRANSFERRED("Transferred", "#6366F1", "status-transferred"),
    LWBS("Left unseen", "#64748B", "status-lwbs"),
    DECEASED("Deceased", "#1E293B", "status-deceased"),
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

    public boolean isQueueEligible() {
        return this == WAITING || this == REGISTERED || this == TRIAGED || this == ASSIGNED;
    }

    public boolean isActiveInED() {
        return this != COMPLETED && this != DISCHARGED && this != ADMITTED && this != TRANSFERRED && this != LWBS && this != DECEASED && this != CANCELLED;
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
