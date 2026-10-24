package com.hospital.priority.model;

/**
 * ============================================================================
 * HEPS 2.0 DOMAIN MODEL: MEDICAL SPECIALTY
 * ============================================================================
 * Represents clinical departments used by Doctor Routing Service and AI Specialty Agent.
 */
public enum Specialty {
    CARDIOLOGY("Cardiology", "badge-specialty-cardio", "#E11D48", "Chest pain, acute coronary syndrome, arrhythmias, palpitations"),
    NEUROLOGY("Neurology", "badge-specialty-neuro", "#9333EA", "Acute stroke, seizures, severe sudden headache, altered consciousness"),
    PULMONOLOGY("Pulmonology", "badge-specialty-pulmo", "#0284C7", "Severe asthma, respiratory failure, acute bronchitis, low SpO₂"),
    ORTHOPEDICS("Orthopedics", "badge-specialty-ortho", "#D97706", "Open/closed fractures, joint dislocations, severe musculoskeletal trauma"),
    PEDIATRICS("Pediatrics", "badge-specialty-pedia", "#059669", "Infants, toddlers and children under 14 with acute conditions"),
    TRAUMA_SURGERY("Trauma Surgery", "badge-specialty-trauma", "#DC2626", "Major hemorrhage, penetrating wounds, vehicular accidents, critical shock"),
    GENERAL_EMERGENCY("General Emergency", "badge-specialty-general", "#4B5563", "General acute illnesses, infections, abdominal pain, stable triage cases");

    private final String displayName;
    private final String cssClass;
    private final String colorHex;
    private final String clinicalFocus;

    Specialty(String displayName, String cssClass, String colorHex, String clinicalFocus) {
        this.displayName = displayName;
        this.cssClass = cssClass;
        this.colorHex = colorHex;
        this.clinicalFocus = clinicalFocus;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getClinicalFocus() {
        return clinicalFocus;
    }

    public static Specialty fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return GENERAL_EMERGENCY;
        }
        String clean = text.trim().toUpperCase().replace(" ", "_");
        for (Specialty s : values()) {
            if (s.name().equals(clean) || s.displayName.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return GENERAL_EMERGENCY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
