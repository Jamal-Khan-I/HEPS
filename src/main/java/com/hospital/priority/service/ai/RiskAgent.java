package com.hospital.priority.service.ai;

import com.hospital.priority.model.Vitals;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * HEPS 2.0 AI AGENT 4: RISK AGENT
 * ============================================================================
 * Role: Identifies subtle clinical red flags, physiological deterioration markers,
 * and high-risk demographic vulnerability factors.
 */
public class RiskAgent {

    public List<String> detectRedFlags(int age, String gender, String symptoms, Vitals vitals) {
        List<String> risks = new ArrayList<>();
        String s = symptoms != null ? symptoms.toLowerCase() : "";

        // Geriatric vulnerability
        if (age >= 65) {
            risks.add("Geriatric vulnerability (Age >= 65): Elevated risk of atypical clinical presentation.");
            if (s.contains("dizziness") || s.contains("fall") || s.contains("weakness")) {
                risks.add("Warning: Geriatric collapse/dizziness may mask occult cardiac or intracranial hemorrhage.");
            }
        }

        // Silent MI / Atypical Cardiac Presentations
        if (s.contains("indigestion") || s.contains("jaw pain") || s.contains("nausea") || s.contains("sweating")) {
            if (age >= 50 || s.contains("diabetic") || s.contains("hypertension")) {
                risks.add("Atypical Cardiac Risk: Non-classical angina symptoms in high-risk demographic.");
            }
        }

        // Combined physiological warning signs
        if (vitals != null) {
            if (vitals.getSpo2() != null && vitals.getSpo2() < 94 && vitals.getHeartRate() != null && vitals.getHeartRate() > 105) {
                risks.add("Cardiopulmonary Strain: Concurrent hypoxia (SpO₂ < 94%) and compensatory tachycardia.");
            }
            if (vitals.getSystolicBp() != null && vitals.getSystolicBp() < 95 && vitals.getHeartRate() != null && vitals.getHeartRate() > 110) {
                risks.add("Pre-Shock Warning: Borderline hypotension combined with elevated heart rate indicates systemic hypoperfusion.");
            }
            if (vitals.getTemperature() != null && vitals.getTemperature() >= 103.0) {
                risks.add("Severe Febrile Risk: High pyrexia posing seizure and metabolic decompensation risk.");
            }
        }

        // Rapid Progression Warnings
        if (s.contains("allergic") || s.contains("bee sting") || s.contains("peanut") || s.contains("swelling in throat")) {
            risks.add("Airway Anaphylaxis Danger: High probability of explosive airway closure.");
        }

        if (risks.isEmpty()) {
            risks.add("No critical occult red flags detected; standard monitoring advised.");
        }

        return risks;
    }
}
