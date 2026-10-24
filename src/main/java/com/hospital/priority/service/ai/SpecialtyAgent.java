package com.hospital.priority.service.ai;

import com.hospital.priority.model.Specialty;
import com.hospital.priority.model.Vitals;

/**
 * ============================================================================
 * HEPS 2.0 AI AGENT 3: SPECIALTY AGENT
 * ============================================================================
 * Role: Analyzes organ system, pathology, and determines clinical specialty requirement.
 */
public class SpecialtyAgent {

    public static class SpecialtyRecommendation {
        private final Specialty specialty;
        private final double confidence;
        private final String departmentReason;

        public SpecialtyRecommendation(Specialty specialty, double confidence, String departmentReason) {
            this.specialty = specialty;
            this.confidence = confidence;
            this.departmentReason = departmentReason;
        }

        public Specialty getSpecialty() {
            return specialty;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getDepartmentReason() {
            return departmentReason;
        }

        @Override
        public String toString() {
            return specialty.getDisplayName() + " (" + (int)(confidence * 100) + "%) - " + departmentReason;
        }
    }

    public SpecialtyRecommendation recommend(int age, String symptoms, Vitals vitals) {
        if (age <= 14) {
            return new SpecialtyRecommendation(Specialty.PEDIATRICS, 0.95,
                    "Pediatric patient profile (Age <= 14). Requires pediatric emergency specialist.");
        }

        String s = symptoms != null ? symptoms.toLowerCase() : "";

        if (s.contains("chest") || s.contains("heart") || s.contains("cardiac") || s.contains("palpitation") || s.contains("angina")) {
            return new SpecialtyRecommendation(Specialty.CARDIOLOGY, 0.96,
                    "Cardiovascular presentation: Evaluated for acute coronary syndrome and cardiac monitoring.");
        }

        if (s.contains("stroke") || s.contains("numbness") || s.contains("paralysis") || s.contains("seizure")
                || s.contains("slurred speech") || s.contains("altered mental")) {
            return new SpecialtyRecommendation(Specialty.NEUROLOGY, 0.94,
                    "Neurological presentation: Evaluated for acute focal neurological deficit and brain injury.");
        }

        if (s.contains("asthma") || s.contains("breathless") || s.contains("wheezing")
                || (vitals != null && vitals.getSpo2() != null && vitals.getSpo2() < 94)) {
            return new SpecialtyRecommendation(Specialty.PULMONOLOGY, 0.91,
                    "Respiratory system compromise: Oxygenation support and pulmonary evaluation needed.");
        }

        if (s.contains("fracture") || s.contains("dislocation") || s.contains("bone") || s.contains("joint sprain")) {
            return new SpecialtyRecommendation(Specialty.ORTHOPEDICS, 0.93,
                    "Musculoskeletal trauma: Requires radiological review and orthopedic alignment.");
        }

        if (s.contains("gunshot") || s.contains("stab") || s.contains("heavy bleed") || s.contains("vehicular accident")) {
            return new SpecialtyRecommendation(Specialty.TRAUMA_SURGERY, 0.97,
                    "High-velocity or severe penetrative trauma: Immediate surgical resuscitation triage.");
        }

        return new SpecialtyRecommendation(Specialty.GENERAL_EMERGENCY, 0.88,
                "General acute condition: Managed by primary emergency medicine team.");
    }
}
