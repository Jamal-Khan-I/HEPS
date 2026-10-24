package com.hospital.priority.service;

import com.hospital.priority.exception.InvalidStateTransitionException;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;

import java.util.*;

/**
 * ============================================================================
 * PATIENT STATE MACHINE
 * ============================================================================
 * Enforces valid lifecycle status transitions as specified in Page 18:
 * Registered → Triaged → Waiting → Assigned (bay + doctor) → In treatment →
 * Observation or results pending → Disposition → Completed
 *
 * Rejects invalid transitions and logs violations for clinical audit trail.
 */
public class PatientStateMachine {

    private static final Map<PatientStatus, Set<PatientStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PatientStatus.class);

    static {
        // REGISTERED can transition to TRIAGED, WAITING (direct bypass in rapid triage), or LWBS/CANCELLED
        ALLOWED_TRANSITIONS.put(PatientStatus.REGISTERED, EnumSet.of(
                PatientStatus.TRIAGED,
                PatientStatus.WAITING,
                PatientStatus.LWBS,
                PatientStatus.CANCELLED
        ));

        // TRIAGED can transition to WAITING, ASSIGNED (immediate bay reservation), IN_TREATMENT (critical resus), or LWBS
        ALLOWED_TRANSITIONS.put(PatientStatus.TRIAGED, EnumSet.of(
                PatientStatus.WAITING,
                PatientStatus.ASSIGNED,
                PatientStatus.IN_TREATMENT,
                PatientStatus.LWBS,
                PatientStatus.CANCELLED
        ));

        // WAITING can transition to ASSIGNED, IN_TREATMENT, LWBS, CANCELLED, or back to TRIAGED (re-triage)
        ALLOWED_TRANSITIONS.put(PatientStatus.WAITING, EnumSet.of(
                PatientStatus.ASSIGNED,
                PatientStatus.IN_TREATMENT,
                PatientStatus.TRIAGED,
                PatientStatus.LWBS,
                PatientStatus.CANCELLED
        ));

        // ASSIGNED can transition to IN_TREATMENT, WAITING (unassigned/requeued), LWBS, CANCELLED
        ALLOWED_TRANSITIONS.put(PatientStatus.ASSIGNED, EnumSet.of(
                PatientStatus.IN_TREATMENT,
                PatientStatus.WAITING,
                PatientStatus.OBSERVATION,
                PatientStatus.LWBS,
                PatientStatus.CANCELLED
        ));

        // IN_TREATMENT can transition to OBSERVATION, LAB_PENDING, DISPOSITION, DECEASED, or directly COMPLETED/DISCHARGED
        ALLOWED_TRANSITIONS.put(PatientStatus.IN_TREATMENT, EnumSet.of(
                PatientStatus.OBSERVATION,
                PatientStatus.LAB_PENDING,
                PatientStatus.DISPOSITION,
                PatientStatus.COMPLETED,
                PatientStatus.DISCHARGED,
                PatientStatus.ADMITTED,
                PatientStatus.TRANSFERRED,
                PatientStatus.DECEASED
        ));

        // OBSERVATION can transition to LAB_PENDING, IN_TREATMENT, DISPOSITION, COMPLETED, DECEASED
        ALLOWED_TRANSITIONS.put(PatientStatus.OBSERVATION, EnumSet.of(
                PatientStatus.IN_TREATMENT,
                PatientStatus.LAB_PENDING,
                PatientStatus.DISPOSITION,
                PatientStatus.COMPLETED,
                PatientStatus.DISCHARGED,
                PatientStatus.ADMITTED,
                PatientStatus.TRANSFERRED,
                PatientStatus.DECEASED
        ));

        // LAB_PENDING can transition to IN_TREATMENT, OBSERVATION, DISPOSITION, COMPLETED
        ALLOWED_TRANSITIONS.put(PatientStatus.LAB_PENDING, EnumSet.of(
                PatientStatus.IN_TREATMENT,
                PatientStatus.OBSERVATION,
                PatientStatus.DISPOSITION,
                PatientStatus.COMPLETED,
                PatientStatus.DISCHARGED,
                PatientStatus.ADMITTED,
                PatientStatus.TRANSFERRED
        ));

        // DISPOSITION can transition to COMPLETED, DISCHARGED, ADMITTED, TRANSFERRED, DECEASED, or back to IN_TREATMENT
        ALLOWED_TRANSITIONS.put(PatientStatus.DISPOSITION, EnumSet.of(
                PatientStatus.COMPLETED,
                PatientStatus.DISCHARGED,
                PatientStatus.ADMITTED,
                PatientStatus.TRANSFERRED,
                PatientStatus.DECEASED,
                PatientStatus.IN_TREATMENT
        ));

        // Terminal states - no further automatic transitions allowed unless reopened by an admin
        ALLOWED_TRANSITIONS.put(PatientStatus.COMPLETED, EnumSet.noneOf(PatientStatus.class));
        ALLOWED_TRANSITIONS.put(PatientStatus.DISCHARGED, EnumSet.noneOf(PatientStatus.class));
        ALLOWED_TRANSITIONS.put(PatientStatus.ADMITTED, EnumSet.noneOf(PatientStatus.class));
        ALLOWED_TRANSITIONS.put(PatientStatus.TRANSFERRED, EnumSet.noneOf(PatientStatus.class));
        ALLOWED_TRANSITIONS.put(PatientStatus.LWBS, EnumSet.noneOf(PatientStatus.class));
        ALLOWED_TRANSITIONS.put(PatientStatus.DECEASED, EnumSet.noneOf(PatientStatus.class));
        ALLOWED_TRANSITIONS.put(PatientStatus.CANCELLED, EnumSet.noneOf(PatientStatus.class));
    }

    /**
     * Checks if a transition from current to target status is valid.
     */
    public static boolean canTransition(PatientStatus from, PatientStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return true; // Idempotent transition is allowed

        Set<PatientStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Validates and applies transition on patient.
     * Throws InvalidStateTransitionException if illegal.
     */
    public static void transition(Patient patient, PatientStatus targetStatus) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient cannot be null for state transition.");
        }
        PatientStatus current = patient.getStatus();
        if (!canTransition(current, targetStatus)) {
            throw new InvalidStateTransitionException(
                    "Invalid clinical state transition for patient " + patient.getTokenNo() +
                            ": Cannot move from " + current.getDisplayName() + " to " + targetStatus.getDisplayName()
            );
        }
        patient.setStatus(targetStatus);
    }

    /**
     * Returns allowed next statuses for the given status.
     */
    public static Set<PatientStatus> getNextAllowedStatuses(PatientStatus current) {
        if (current == null) return Collections.emptySet();
        Set<PatientStatus> set = ALLOWED_TRANSITIONS.get(current);
        return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }
}
