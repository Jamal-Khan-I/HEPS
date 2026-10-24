package com.hospital.priority;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.model.Vitals;
import com.hospital.priority.service.JavaTriageRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HEPS 2.0 Deterministic Java Triage Rule Engine Tests")
class JavaTriageRuleEngineTest {

    private JavaTriageRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = JavaTriageRuleEngine.getInstance();
    }

    @Test
    @DisplayName("SpO2 < 90% must always trigger forced CRITICAL level")
    void testCriticalHypoxiaThreshold() {
        Vitals vitals = new Vitals();
        vitals.setSpo2(87); // Severely low oxygen saturation
        vitals.setHeartRate(80);

        JavaTriageRuleEngine.RuleEvaluation eval = ruleEngine.evaluate(45, "Mild headache", vitals);

        assertEquals(EmergencyLevel.CRITICAL, eval.getRecommendedLevel());
        assertTrue(eval.isCriticalVitalTriggered());
        assertTrue(eval.getTriggeredRules().stream().anyMatch(r -> r.contains("Severe Hypoxemia")));
    }

    @Test
    @DisplayName("Extreme heart rate (<40 or >140) must force CRITICAL level")
    void testCriticalHeartRateThreshold() {
        Vitals vitals = new Vitals();
        vitals.setHeartRate(155); // Tachycardia
        vitals.setSpo2(98);

        JavaTriageRuleEngine.RuleEvaluation eval = ruleEngine.evaluate(50, "Feeling dizzy", vitals);

        assertEquals(EmergencyLevel.CRITICAL, eval.getRecommendedLevel());
        assertTrue(eval.isCriticalVitalTriggered());
    }

    @Test
    @DisplayName("Systolic BP > 200 mmHg must force CRITICAL hypertensive crisis")
    void testCriticalBloodPressureThreshold() {
        Vitals vitals = new Vitals();
        vitals.setSystolicBp(215);
        vitals.setDiastolicBp(120);

        JavaTriageRuleEngine.RuleEvaluation eval = ruleEngine.evaluate(60, "Vision blur", vitals);

        assertEquals(EmergencyLevel.CRITICAL, eval.getRecommendedLevel());
        assertTrue(eval.isCriticalVitalTriggered());
    }

    @Test
    @DisplayName("Symptoms with chest pain must map to CARDIOLOGY specialty")
    void testCardiologySpecialtyMapping() {
        Vitals normalVitals = new Vitals(97, 76, 98.6, 120, 80, 16);
        JavaTriageRuleEngine.RuleEvaluation eval = ruleEngine.evaluate(55, "Crushing substernal chest pain radiating to left arm", normalVitals);

        assertEquals(Specialty.CARDIOLOGY, eval.getRecommendedSpecialty());
        assertEquals(EmergencyLevel.HIGH, eval.getRecommendedLevel());
    }

    @Test
    @DisplayName("Pediatric patients (Age <= 14) must map to PEDIATRICS specialty")
    void testPediatricSpecialtyMapping() {
        Vitals vitals = new Vitals(99, 90, 101.5, 100, 65, 20);
        JavaTriageRuleEngine.RuleEvaluation eval = ruleEngine.evaluate(8, "High fever and persistent cough", vitals);

        assertEquals(Specialty.PEDIATRICS, eval.getRecommendedSpecialty());
    }

    @Test
    @DisplayName("Fracture symptoms must map to ORTHOPEDICS specialty")
    void testOrthopedicsSpecialtyMapping() {
        Vitals vitals = new Vitals(98, 80, 98.4, 125, 82, 16);
        JavaTriageRuleEngine.RuleEvaluation eval = ruleEngine.evaluate(28, "Suspected radius fracture and severe wrist pain after fall", vitals);

        assertEquals(Specialty.ORTHOPEDICS, eval.getRecommendedSpecialty());
        assertEquals(EmergencyLevel.MEDIUM, eval.getRecommendedLevel());
    }
}
