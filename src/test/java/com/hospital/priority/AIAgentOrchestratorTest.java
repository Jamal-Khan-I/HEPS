package com.hospital.priority;

import com.hospital.priority.model.AIAssessmentResult;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.model.Vitals;
import com.hospital.priority.service.ai.AIAgentOrchestrator;
import com.hospital.priority.service.ai.AIClientService;
import com.hospital.priority.service.ai.AIClinicalExtraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HEPS 2.0 AI Multi-Agent Orchestrator Tests")
class AIAgentOrchestratorTest {

    private AIAgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = AIAgentOrchestrator.getInstance();
    }

    @Test
    @DisplayName("Orchestrator coordinates 4 agents and synthesizes assessment")
    void testAgentOrchestration() {
        Vitals vitals = new Vitals(97, 82, 98.6, 128, 84, 16);
        AIAssessmentResult result = orchestrator.assessPatient(
                "Anita Bose", 48, "Female",
                "Crushing chest pressure radiating to neck and cold sweat", vitals
        );

        assertNotNull(result);
        assertEquals(Specialty.CARDIOLOGY, result.getSuggestedSpecialty());
        assertTrue(result.getSuggestedLevel() == EmergencyLevel.HIGH || result.getSuggestedLevel() == EmergencyLevel.CRITICAL);
        assertNotNull(result.getIntakeSummary());
        assertTrue(result.getConfidenceScore() > 0.7);
        assertFalse(result.getRiskFlags().isEmpty());
    }

    @Test
    @DisplayName("Disagreement detected when Java rules identify critical hypertensive crisis not caught by basic triage")
    void testDisagreementDetection() {
        // Patient has subtle complaint but critically elevated blood pressure (hypertensive crisis)
        Vitals criticalVitals = new Vitals();
        criticalVitals.setSystolicBp(215); // Critical hypertensive crisis > 200 mmHg

        AIAssessmentResult result = orchestrator.assessPatient(
                "Suresh Kumar", 58, "Male",
                "Feeling slightly tired after walking", criticalVitals
        );

        assertNotNull(result);
        // Java rule engine will force CRITICAL due to Systolic BP > 200 mmHg
        assertEquals(EmergencyLevel.CRITICAL, result.getRuleSuggestedLevel());
        assertEquals(EmergencyLevel.CRITICAL, result.getFinalRecommendedLevel());
        assertTrue(result.isDisagreementDetected());
        assertTrue(result.getDisagreementNotes().contains("Java Rule Engine"));
    }

    @Test
    @DisplayName("AI Narrative extraction correctly extracts GCS and infers clinical pain score")
    void testClinicalNarrativeGcsAndPainExtraction() {
        String narrative = "chest pain radiating to left jaw, diaphoresis, SpO2 91%, HR 112, BP 160/95, 98.6 F , 16/min ,15 GCS. History of hypertension, allergic to penicillin.";
        AIClinicalExtraction ext = AIClientService.getInstance().extractClinicalNarrative(narrative);

        assertNotNull(ext);
        assertEquals(15, ext.getGcs(), "GCS should be extracted as 15");
        assertNotNull(ext.getPainScore(), "Pain score should not be null");
        assertTrue(ext.getPainScore() >= 7, "Severe radiating chest pain should have acute pain score >= 7");
        assertEquals(91, ext.getSpo2(), "SpO2 should be 91%");
        assertEquals(112, ext.getHeartRate(), "HR should be 112");
        assertEquals(160, ext.getSystolicBp(), "Systolic BP should be 160");
        assertEquals(95, ext.getDiastolicBp(), "Diastolic BP should be 95");
        assertEquals(98.6, ext.getTemperature(), 0.1, "Temperature should be 98.6");
        assertEquals(16, ext.getRespiratoryRate(), "Respiratory rate should be 16");
    }
}
