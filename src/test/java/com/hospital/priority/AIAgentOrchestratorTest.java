package com.hospital.priority;

import com.hospital.priority.model.AIAssessmentResult;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.model.Vitals;
import com.hospital.priority.service.ai.AIAgentOrchestrator;
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
}
