package com.hospital.priority;

import com.hospital.priority.exception.InvalidStateTransitionException;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;
import com.hospital.priority.service.PatientStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Page 18 Patient Pipeline State Machine transitions.
 */
public class PatientStateMachineTest {

    @Test
    @DisplayName("Valid lifecycle transition sequence: REGISTERED -> TRIAGED -> WAITING -> ASSIGNED -> IN_TREATMENT -> DISPOSITION -> COMPLETED")
    public void testValidPipelineProgression() {
        Patient patient = new Patient("TK-001", "Test Patient", 45, "Male", "9876543210", "Chest pain", EmergencyLevel.CRITICAL, null);
        patient.setStatus(PatientStatus.REGISTERED);

        // 1. Registered -> Triaged
        PatientStateMachine.transition(patient, PatientStatus.TRIAGED);
        assertEquals(PatientStatus.TRIAGED, patient.getStatus());

        // 2. Triaged -> Waiting
        PatientStateMachine.transition(patient, PatientStatus.WAITING);
        assertEquals(PatientStatus.WAITING, patient.getStatus());

        // 3. Waiting -> Assigned
        PatientStateMachine.transition(patient, PatientStatus.ASSIGNED);
        assertEquals(PatientStatus.ASSIGNED, patient.getStatus());

        // 4. Assigned -> In Treatment
        PatientStateMachine.transition(patient, PatientStatus.IN_TREATMENT);
        assertEquals(PatientStatus.IN_TREATMENT, patient.getStatus());

        // 5. In Treatment -> Disposition
        PatientStateMachine.transition(patient, PatientStatus.DISPOSITION);
        assertEquals(PatientStatus.DISPOSITION, patient.getStatus());

        // 6. Disposition -> Completed
        PatientStateMachine.transition(patient, PatientStatus.COMPLETED);
        assertEquals(PatientStatus.COMPLETED, patient.getStatus());
    }

    @Test
    @DisplayName("Illegal transition from REGISTERED directly to COMPLETED is rejected")
    public void testIllegalTransitionRejection() {
        Patient patient = new Patient("TK-002", "Invalid Jumper", 30, "Female", "9876543210", "Mild cough", EmergencyLevel.LOW, null);
        patient.setStatus(PatientStatus.REGISTERED);

        assertThrows(InvalidStateTransitionException.class, () -> {
            PatientStateMachine.transition(patient, PatientStatus.COMPLETED);
        });
    }

    @Test
    @DisplayName("Any active patient can transition to Left Without Being Seen (LWBS)")
    public void testLwbsTransition() {
        Patient patient = new Patient("TK-003", "Leaving Patient", 25, "Male", "9876543210", "Sprain", EmergencyLevel.LOW, null);
        patient.setStatus(PatientStatus.WAITING);

        PatientStateMachine.transition(patient, PatientStatus.LWBS);
        assertEquals(PatientStatus.LWBS, patient.getStatus());
    }
}
