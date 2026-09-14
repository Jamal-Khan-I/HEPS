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
 * CORE DSA SERVICE: PRIORITY QUEUE TRIAGE ENGINE
 * ============================================================================
 * Central triage coordinator managing the in-memory Java PriorityQueue&lt;Patient&gt;.
 *
 * Key Design Principles (Viva Relevant):
 * 1. The PriorityQueue is the AUTHORITATIVE active ordering structure in RAM.
 * 2. It uses min-heap semantics backed by PatientPriorityComparator:
 *    - CRITICAL (1) &gt; HIGH (2) &gt; MEDIUM (3) &gt; LOW (4)
 *    - FIFO on tie (earlier arrival time)
 *    - Deterministic tie-breaker (smaller token number)
 * 3. CRITICAL IMPLEMENTATION NOTE:
 *    Iterating over a Java PriorityQueue directly using iterator() or for-each
 *    DOES NOT guarantee sorted order! Therefore, getSortedSnapshot() creates
 *    a copy and sorts it using the comparator before binding to the JavaFX TableView.
 * 4. Queue updates and MySQL database updates are kept strictly synchronized.
 * ============================================================================
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
     *
     * @param doctorName Attending doctor or staff member
     * @return The served Patient
     * @throws PatientNotFoundException if the queue is empty
     * @throws DatabaseOperationException if database update fails
     */
    public synchronized Patient serveNext(String doctorName) throws PatientNotFoundException, DatabaseOperationException {
        Patient head = triageQueue.poll();
        if (head == null) {
            throw new PatientNotFoundException("Emergency triage queue is currently empty. No waiting patients to serve.");
        }

        head.setStatus(PatientStatus.IN_TREATMENT);
        patientDao.updateStatus(head.getPatientId(), PatientStatus.IN_TREATMENT);

        // Record treatment admission
        TreatmentRecord record = new TreatmentRecord(
                head.getPatientId(),
                doctorName != null && !doctorName.trim().isEmpty() ? doctorName.trim() : "ER Physician",
                "Patient called into emergency treatment bay.",
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

    /**
     * For unit tests or resetting state.
     */
    public synchronized void clear() {
        triageQueue.clear();
    }
}
