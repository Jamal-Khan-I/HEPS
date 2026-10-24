package com.hospital.priority;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HEPS 2.0 PriorityQueue Escalation Tests")
class PriorityQueueEscalationTest {

    private QueueService queueService;

    @BeforeEach
    void setUp() {
        queueService = QueueService.getInstance();
        queueService.clear();
    }

    @Test
    @DisplayName("A MEDIUM patient waiting over 45 minutes must automatically escalate to HIGH")
    void testWaitTimeEscalationMediumToHigh() {
        // Patient arrived 50 minutes ago
        Patient patient = new Patient("TK-TEST-001", "Mohan Lal", 45, "Male", "9876543210",
                "Sprained ankle with moderate pain", EmergencyLevel.MEDIUM, LocalDateTime.now().minusMinutes(50));

        queueService.enqueue(patient);
        assertEquals(1, queueService.getQueueSize());
        assertEquals(0, queueService.getEscalatedCount());

        List<Patient> escalated = queueService.checkAndApplyEscalations();

        assertEquals(1, escalated.size());
        assertEquals(EmergencyLevel.HIGH, patient.getEmergencyLevel());
        assertTrue(patient.isEscalated());
        assertEquals(1, queueService.getEscalatedCount());
    }

    @Test
    @DisplayName("An escalated patient moves ahead of standard MEDIUM patients in the queue")
    void testEscalatedPatientMovesAheadInQueue() {
        // Patient 1 arrived recently with MEDIUM
        Patient recentMedium = new Patient("TK-TEST-002", "Recent Patient", 30, "Female", "9876543211",
                "Minor burn", EmergencyLevel.MEDIUM, LocalDateTime.now().minusMinutes(5));

        // Patient 2 arrived 55 mins ago with MEDIUM (will be escalated to HIGH)
        Patient delayedPatient = new Patient("TK-TEST-003", "Delayed Patient", 62, "Male", "9876543212",
                "Deep cut", EmergencyLevel.MEDIUM, LocalDateTime.now().minusMinutes(55));

        queueService.enqueue(recentMedium);
        queueService.enqueue(delayedPatient);

        // Before escalation, delayed arrived earlier so it's already ahead on tie-break
        // After escalation, delayed becomes HIGH and recentMedium remains MEDIUM
        queueService.checkAndApplyEscalations();

        List<Patient> snapshot = queueService.getSortedSnapshot();
        assertEquals(2, snapshot.size());
        assertEquals("TK-TEST-003", snapshot.get(0).getTokenNo());
        assertEquals(EmergencyLevel.HIGH, snapshot.get(0).getEmergencyLevel());
        assertEquals("TK-TEST-002", snapshot.get(1).getTokenNo());
        assertEquals(EmergencyLevel.MEDIUM, snapshot.get(1).getEmergencyLevel());
    }
}
