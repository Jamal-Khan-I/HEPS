package com.hospital.priority.service;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.exception.InvalidPatientDataException;
import com.hospital.priority.exception.PatientNotFoundException;
import com.hospital.priority.model.*;
import com.hospital.priority.util.TokenGenerator;
import com.hospital.priority.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service orchestrating patient domain workflows, data validation,
 * persistence, and queue updates.
 */
public class PatientService {

    private static PatientService instance;
    private final PatientDao patientDao;
    private final QueueService queueService;

    private PatientService() {
        this.patientDao = new PatientDaoImpl();
        this.queueService = QueueService.getInstance();
    }

    public static synchronized PatientService getInstance() {
        if (instance == null) {
            instance = new PatientService();
        }
        return instance;
    }

    /**
     * Initializes the token generator sequence from existing database records.
     */
    public void initializeSequence() {
        try {
            int maxSeq = patientDao.getMaxTokenSequence();
            TokenGenerator.initializeSequence(maxSeq);
        } catch (Exception e) {
            System.err.println("Notice: Could not query max token sequence: " + e.getMessage());
        }
    }

    /**
     * Registers a new patient into the Emergency System:
     * 1. Validates inputs.
     * 2. Auto-generates unique token.
     * 3. Sets timestamps and WAITING status.
     * 4. Persists to MySQL.
     * 5. Immediately adds to the active PriorityQueue.
     */
    public Patient registerPatient(String fullName, String ageStr, String gender,
                                   String phone, String symptoms, EmergencyLevel level)
            throws InvalidPatientDataException, DatabaseOperationException {

        // Step 1: Input Validation
        ValidationUtil.validatePatientInput(fullName, ageStr, gender, phone, symptoms);
        int age = Integer.parseInt(ageStr.trim());

        // Step 2: Auto-generate unique token
        String token = TokenGenerator.nextToken();

        // Step 3: Instantiate model
        Patient patient = new Patient(
                token,
                fullName.trim(),
                age,
                gender.trim(),
                phone.trim(),
                symptoms.trim(),
                level != null ? level : EmergencyLevel.LOW,
                LocalDateTime.now()
        );
        patient.setStatus(PatientStatus.WAITING);

        // Step 4: Persist to MySQL
        int generatedId = patientDao.insert(patient);
        patient.setPatientId(generatedId);

        // Step 5: Add immediately to in-memory PriorityQueue
        queueService.enqueue(patient);

        return patient;
    }

    public boolean updatePatient(Patient patient) throws DatabaseOperationException, InvalidPatientDataException {
        if (patient == null) {
            throw new InvalidPatientDataException("Patient object cannot be null.");
        }
        ValidationUtil.validatePatientInput(
                patient.getFullName(),
                String.valueOf(patient.getAge()),
                patient.getGender(),
                patient.getPhone(),
                patient.getSymptoms()
        );

        boolean updated = patientDao.update(patient);
        if (updated && patient.getStatus() == PatientStatus.WAITING) {
            // Re-sync PriorityQueue in case emergency level changed
            queueService.enqueue(patient);
        }
        return updated;
    }

    public Optional<Patient> findByToken(String tokenNo) throws DatabaseOperationException {
        return patientDao.findByToken(tokenNo);
    }

    public Optional<Patient> findById(int id) throws DatabaseOperationException {
        return patientDao.findById(id);
    }

    public List<Patient> getAllPatients() throws DatabaseOperationException {
        return patientDao.findAll();
    }

    public List<Patient> getInTreatmentPatients() throws DatabaseOperationException {
        return patientDao.findByStatus(PatientStatus.IN_TREATMENT);
    }

    public List<Patient> searchPatients(String searchTerm, String level, String status) throws DatabaseOperationException {
        return patientDao.search(searchTerm, level, status);
    }

    public Map<String, Object> getDashboardStatistics() throws DatabaseOperationException {
        Map<String, Object> stats = new HashMap<>();

        int totalWaiting = queueService.getQueueSize();
        int criticalWaiting = queueService.getCriticalCount();
        int inTreatment = patientDao.countByStatus(PatientStatus.IN_TREATMENT);
        int completedToday = patientDao.countCompletedToday();
        Patient nextPatient = queueService.peekNext();

        stats.put("totalWaiting", totalWaiting);
        stats.put("criticalWaiting", criticalWaiting);
        stats.put("inTreatment", inTreatment);
        stats.put("completedToday", completedToday);
        stats.put("nextPatient", nextPatient);

        return stats;
    }

    public void cancelPatient(int patientId) throws DatabaseOperationException, PatientNotFoundException {
        Optional<Patient> pOpt = patientDao.findById(patientId);
        if (pOpt.isEmpty()) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
        Patient p = pOpt.get();
        queueService.removeFromQueue(p);
        patientDao.updateStatus(patientId, PatientStatus.CANCELLED);
    }
}
