package com.hospital.priority.service;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.exception.InvalidPatientDataException;
import com.hospital.priority.exception.PatientNotFoundException;
import com.hospital.priority.model.*;
import com.hospital.priority.service.ai.AIAgentOrchestrator;
import com.hospital.priority.util.TokenGenerator;
import com.hospital.priority.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * HEPS 2.0 PATIENT SERVICE
 * ============================================================================
 * Service orchestrating patient domain workflows, data validation,
 * persistence, queue updates, AI multi-agent analysis, and doctor routing.
 */
public class PatientService {

    private static PatientService instance;
    private final PatientDao patientDao;
    private final QueueService queueService;
    private final DoctorRoutingService routingService;
    private final AIAgentOrchestrator aiOrchestrator;

    private PatientService() {
        this.patientDao = new PatientDaoImpl();
        this.queueService = QueueService.getInstance();
        this.routingService = DoctorRoutingService.getInstance();
        this.aiOrchestrator = AIAgentOrchestrator.getInstance();
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
     * Runs multi-agent AI assessment + deterministic Java safety check.
     */
    public AIAssessmentResult runClinicalAndAiAssessment(String fullName, int age, String gender,
                                                         String symptoms, Vitals vitals) {
        return aiOrchestrator.assessPatient(fullName, age, gender, symptoms, vitals);
    }

    /**
     * Finds optimal available specialist doctor for a given specialty.
     */
    public DoctorRoutingService.RoutingDecision routeDoctor(Specialty specialty) {
        return routingService.routeDoctor(specialty);
    }

    /**
     * HEPS 2.0 Full Registration Workflow:
     * 1. Validates patient inputs.
     * 2. Auto-generates unique sequential token.
     * 3. Sets vitals, specialty, and doctor assignment.
     * 4. Persists to MySQL.
     * 5. Immediately enqueues into in-memory Min-Heap PriorityQueue.
     * 6. Updates doctor's workload.
     */
    public Patient registerPatient(String fullName, String ageStr, String gender,
                                   String phone, String symptoms, EmergencyLevel level,
                                   Vitals vitals, Specialty specialty, Doctor assignedDoctor,
                                   String assignedBay,
                                   EmergencyLevel aiLevel, EmergencyLevel ruleLevel, String overrideReason)
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
        patient.setVitals(vitals != null ? vitals : new Vitals());
        patient.setAssignedSpecialty(specialty != null ? specialty : Specialty.GENERAL_EMERGENCY);
        patient.setAssignedBay(assignedBay != null && !assignedBay.trim().isEmpty() ? assignedBay : Patient.getDefaultBayForLevel(level));

        if (assignedDoctor != null) {
            patient.setAssignedDoctorId(assignedDoctor.getDoctorId());
            patient.setAssignedDoctorName(assignedDoctor.getName());
        } else {
            // Auto-route if not explicitly specified
            DoctorRoutingService.RoutingDecision route = routingService.routeDoctor(patient.getAssignedSpecialty());
            if (route != null && route.getAssignedDoctor() != null) {
                patient.setAssignedDoctorId(route.getAssignedDoctor().getDoctorId());
                patient.setAssignedDoctorName(route.getAssignedDoctor().getName());
            }
        }

        patient.setAiSuggestedLevel(aiLevel);
        patient.setRuleSuggestedLevel(ruleLevel);
        patient.setOverrideReason(overrideReason);

        // Step 4: Persist to MySQL
        int generatedId = patientDao.insert(patient);
        patient.setPatientId(generatedId);

        // Step 5: Add immediately to in-memory PriorityQueue
        queueService.enqueue(patient);

        return patient;
    }

    /**
     * Registers and persists an already-constructed Patient model object.
     */
    public Patient registerPatient(Patient patient) throws DatabaseOperationException {
        if (patient == null) {
            throw new IllegalArgumentException("Patient cannot be null");
        }
        if (patient.getTokenNo() == null || patient.getTokenNo().isEmpty()) {
            patient.setTokenNo(TokenGenerator.nextToken());
        }
        int generatedId = patientDao.insert(patient);
        patient.setPatientId(generatedId);
        return patient;
    }

    public Patient registerPatient(String fullName, String ageStr, String gender,
                                   String phone, String symptoms, EmergencyLevel level,
                                   Vitals vitals, Specialty specialty, Doctor assignedDoctor,
                                   EmergencyLevel aiLevel, EmergencyLevel ruleLevel, String overrideReason)
            throws InvalidPatientDataException, DatabaseOperationException {
        return registerPatient(fullName, ageStr, gender, phone, symptoms, level,
                vitals, specialty, assignedDoctor, null, aiLevel, ruleLevel, overrideReason);
    }

    /**
     * Backward-compatible registration method for HEPS 1.0 callers.
     */
    public Patient registerPatient(String fullName, String ageStr, String gender,
                                   String phone, String symptoms, EmergencyLevel level)
            throws InvalidPatientDataException, DatabaseOperationException {
        return registerPatient(fullName, ageStr, gender, phone, symptoms, level,
                new Vitals(), Specialty.GENERAL_EMERGENCY, null, null, null, null, null);
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
        int escalatedCount = queueService.getEscalatedCount();
        int inTreatment = patientDao.countByStatus(PatientStatus.IN_TREATMENT);
        int completedToday = patientDao.countCompletedToday();

        stats.put("totalWaiting", totalWaiting);
        stats.put("criticalWaiting", criticalWaiting);
        stats.put("escalatedCount", escalatedCount);
        stats.put("inTreatment", inTreatment);
        stats.put("completedToday", completedToday);
        stats.put("specialistAvailability", routingService.getAvailableDoctorsCountBySpecialty());

        return stats;
    }
}
