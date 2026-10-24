package com.hospital.priority.service;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.TreatmentRecordDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.dao.impl.TreatmentRecordDaoImpl;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.exception.PatientNotFoundException;
import com.hospital.priority.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * ============================================================================
 * HEPS 2.0 DSA SERVICE: PRIORITY QUEUE TRIAGE ENGINE & ESCALATION CONTROLLER
 * ============================================================================
 * Central triage coordinator managing the in-memory Java PriorityQueue<Patient>.
 *
 * HEPS 2.0 Upgrades:
 * 1. Automatic Priority Escalation: Patients waiting excessively long without care
 *    are automatically escalated to prevent clinical neglect.
 * 2. Specialty & Doctor Integration: Coordinates with DoctorRoutingService upon admission.
 * 3. Heap State Synchronization: Guarantees Min-Heap invariant is restored after escalations.
 */
public class QueueService {

    private static QueueService instance;

    // The central Data Structure: Java Collections PriorityQueue with custom Comparator
    private final PriorityQueue<Patient> triageQueue;

    private final PatientDao patientDao;
    private final TreatmentRecordDao treatmentRecordDao;

    // Singleton pattern for centralized triage queue state
    private QueueService() {
        this.triageQueue = new PriorityQueue<>(new PatientPriorityComparator());
        this.patientDao = new PatientDaoImpl();
        this.treatmentRecordDao = new TreatmentRecordDaoImpl();
    }

    public static synchronized QueueService getInstance() {
        if (instance == null) {
            instance = new QueueService();
        }
        return instance;
    }

    /**
     * Bootstraps the in-memory PriorityQueue on application launch by loading
     * all WAITING patients from MySQL.
     */
    public synchronized void initializeQueueFromDatabase() throws DatabaseOperationException {
        triageQueue.clear();
        List<Patient> waitingPatients = patientDao.findByStatus(PatientStatus.WAITING);
        for (Patient p : waitingPatients) {
            triageQueue.offer(p);
        }
    }

    /**
     * Enqueues a newly registered waiting patient.
     * Time Complexity: O(log N) heap insertion.
     */
    public synchronized void enqueue(Patient patient) {
        if (patient != null && patient.getStatus() == PatientStatus.WAITING) {
            // Remove existing instance if already present (e.g. priority update)
            triageQueue.remove(patient);
            triageQueue.offer(patient);
        }
    }

    /**
     * Inspects the highest-priority patient waiting at the front of the queue
     * without removing them.
     * Time Complexity: O(1).
     */
    public synchronized Patient peekNext() {
        return triageQueue.peek();
    }

    /**
     * Serves the highest priority patient:
     * 1. Removes the patient from the head of the PriorityQueue (poll, O(log N)).
     * 2. Changes status from WAITING to IN_TREATMENT.
     * 3. Persists status in MySQL.
     * 4. Creates an active treatment record in MySQL.
     */
    public synchronized Patient serveNext(String doctorName) throws PatientNotFoundException, DatabaseOperationException {
        Patient head = triageQueue.poll();
        if (head == null) {
            throw new PatientNotFoundException("Emergency triage queue is currently empty. No waiting patients to serve.");
        }

        head.setStatus(PatientStatus.IN_TREATMENT);
        patientDao.updateStatus(head.getPatientId(), PatientStatus.IN_TREATMENT);

        String effectiveDoctor = doctorName != null && !doctorName.trim().isEmpty()
                ? doctorName.trim()
                : (head.getAssignedDoctorName() != null && !head.getAssignedDoctorName().equals("Unassigned")
                ? head.getAssignedDoctorName() : "ER Physician");

        head.setAssignedDoctorName(effectiveDoctor);

        // Record treatment admission
        TreatmentRecord record = new TreatmentRecord(
                head.getPatientId(),
                effectiveDoctor,
                "Patient admitted to emergency bay for " + head.getAssignedSpecialty().getDisplayName() + " evaluation.",
                LocalDateTime.now()
        );
        treatmentRecordDao.insert(record);

        return head;
    }

    /**
     * Removes a patient from the active queue (e.g. if cancelled or transferred).
     */
    public synchronized boolean removeFromQueue(Patient patient) {
        if (patient == null) {
            return false;
        }
        return triageQueue.remove(patient);
    }

    /**
     * HEPS 2.0 AUTOMATIC WAIT-TIME PRIORITY ESCALATION ENGINE
     * Scans all waiting patients in the PriorityQueue.
     * Rules:
     * - LOW waiting >= 90 mins -> Escalated to MEDIUM
     * - MEDIUM waiting >= 45 mins -> Escalated to HIGH
     * - HIGH waiting >= 25 mins -> Marked for Critical Attention
     * Rebuilds the PriorityQueue to ensure Min-Heap invariant is restored!
     *
     * @return List of newly escalated patients for UI notification
     */
    public synchronized List<Patient> checkAndApplyEscalations() {
        List<Patient> escalatedList = new ArrayList<>();
        boolean heapModified = false;

        List<Patient> currentList = new ArrayList<>(triageQueue);

        for (Patient p : currentList) {
            long waitMins = p.getWaitingMinutes();
            boolean changed = false;

            if (p.getEmergencyLevel() == EmergencyLevel.LOW && waitMins >= 90) {
                p.setEmergencyLevel(EmergencyLevel.MEDIUM);
                p.setEscalated(true);
                p.setEscalationReason("Wait time reached " + waitMins + "m (Exceeded 90m threshold for Low priority)");
                changed = true;
            } else if (p.getEmergencyLevel() == EmergencyLevel.MEDIUM && waitMins >= 45) {
                p.setEmergencyLevel(EmergencyLevel.HIGH);
                p.setEscalated(true);
                p.setEscalationReason("Wait time reached " + waitMins + "m (Exceeded 45m threshold for Medium priority)");
                changed = true;
            } else if (p.getEmergencyLevel() == EmergencyLevel.HIGH && waitMins >= 25 && !p.isEscalated()) {
                p.setEscalated(true);
                p.setEscalationReason("Prolonged High wait (" + waitMins + "m) - Immediate treatment recommended");
                changed = true;
            }

            if (changed) {
                escalatedList.add(p);
                heapModified = true;
                try {
                    patientDao.update(p);
                } catch (Exception e) {
                    System.err.println("Notice: Could not persist patient escalation: " + e.getMessage());
                }
            }
        }

        if (heapModified) {
            // Re-heapify: Clear and re-insert all elements
            triageQueue.clear();
            for (Patient p : currentList) {
                triageQueue.offer(p);
            }
        }

        return escalatedList;
    }

    /**
     * Returns a sorted, thread-safe snapshot of all waiting patients.
     * Injects 1-based queue position numbers for clear UI table presentation.
     */
    public synchronized List<Patient> getSortedSnapshot() {
        List<Patient> snapshot = new ArrayList<>(triageQueue);
        snapshot.sort(new PatientPriorityComparator());

        int pos = 1;
        for (Patient p : snapshot) {
            p.setQueuePosition(pos++);
        }

        return Collections.unmodifiableList(snapshot);
    }

    public synchronized int getQueueSize() {
        return triageQueue.size();
    }

    public synchronized int getCriticalCount() {
        int count = 0;
        for (Patient p : triageQueue) {
            if (p.getEmergencyLevel() == EmergencyLevel.CRITICAL) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getEscalatedCount() {
        int count = 0;
        for (Patient p : triageQueue) {
            if (p.isEscalated()) {
                count++;
            }
        }
        return count;
    }

    /**
     * For unit tests or resetting state.
     */
    public synchronized void clear() {
        triageQueue.clear();
    }
}
