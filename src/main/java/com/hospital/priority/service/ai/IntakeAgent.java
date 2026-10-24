package com.hospital.priority.service.ai;

import com.hospital.priority.model.Vitals;

/**
 * ============================================================================
 * HEPS 2.0 AI AGENT 1: INTAKE AGENT
 * ============================================================================
 * Role: Clinical Information Extraction & Symptom Synthesis.
 * Extracts chief complaints, clinical indicators, and structures narrative notes.
 */
public class IntakeAgent {

    public String analyze(String fullName, int age, String gender, String symptoms, Vitals vitals) {
        StringBuilder sb = new StringBuilder();
        sb.append(age).append("-year-old ").append(gender.toLowerCase()).append(" presenting with: ");

        if (symptoms != null && !symptoms.trim().isEmpty()) {
            sb.append("\"").append(symptoms.trim()).append("\". ");
        } else {
            sb.append("unspecified distress. ");
        }

        if (vitals != null && (vitals.getSpo2() != null || vitals.getHeartRate() != null)) {
            sb.append("Physiological Vitals Recorded: [").append(vitals.toSummaryString()).append("]. ");
        } else {
            sb.append("Initial vitals pending confirmation. ");
        }

        return sb.toString();
    }
}
