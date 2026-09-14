package com.hospital.priority.model;

/**
 * Represents the medical urgency category of a patient in triage.
 * Evaluated in strict order:
 * 1. CRITICAL (Rank 1 - Immediate life threat)
 * 2. HIGH     (Rank 2 - Urgent, severe suffering/instability)
 * 3. MEDIUM   (Rank 3 - Semi-urgent, acute distress)
 * 4. LOW      (Rank 4 - Non-urgent, standard care)
 */
public enum EmergencyLevel {
    CRITICAL(1, "Critical", "#DC2626", "badge-critical"),
    HIGH(2, "High", "#EA580C", "badge-high"),
    MEDIUM(3, "Medium", "#D97706", "badge-medium"),
    LOW(4, "Low", "#16A34A", "badge-low");

    private final int rank;
    private final String displayName;
    private final String colorHex;
    private final String cssClass;

    EmergencyLevel(int rank, String displayName, String colorHex, String cssClass) {
        this.rank = rank;
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.cssClass = cssClass;
    }

    public int getRank() {
        return rank;
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

    public static EmergencyLevel fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return LOW;
        }
        String clean = text.trim().toUpperCase();
        for (EmergencyLevel level : values()) {
            if (level.name().equals(clean) || level.displayName.equalsIgnoreCase(clean)) {
                return level;
            }
        }
        return LOW;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
