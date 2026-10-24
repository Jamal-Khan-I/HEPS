package com.hospital.priority.service;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.TreatmentRecordDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.dao.impl.TreatmentRecordDaoImpl;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.exception.InvalidPatientDataException;
import com.hospital.priority.exception.PatientNotFoundException;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;
import com.hospital.priority.model.TreatmentRecord;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * HEPS 2.0 TREATMENT SERVICE
 * ============================================================================
 * Manages treatment lifecycle, medical documentation, doctor workload release,
 * and patient discharge.
 */
public class TreatmentService {

    private static TreatmentService instance;
    private final TreatmentRecordDao treatmentRecordDao;
    private final PatientDao patientDao;
    private final QueueService queueService;
    private final DoctorRoutingService routingService;

    private TreatmentService() {
        this.treatmentRecordDao = new TreatmentRecordDaoImpl();
        this.patientDao = new PatientDaoImpl();
        this.queueService = QueueService.getInstance();
        this.routingService = DoctorRoutingService.getInstance();
    }

    public static synchronized TreatmentService getInstance() {
        if (instance == null) {
            instance = new TreatmentService();
        }
        return instance;
    }

    /**
     * Completes consultation/procedure for an in-treatment patient:
     * 1. Records doctor notes and completion timestamp.
     * 2. Sets patient status to COMPLETED in database.
     * 3. Ensures active queue synchronization.
     * 4. Releases assigned doctor's active workload.
     */
    public void completeTreatment(int patientId, String doctorName, String treatmentNotes)
            throws InvalidPatientDataException, PatientNotFoundException, DatabaseOperationException {
        completeTreatment(patientId, doctorName, treatmentNotes, "DISCHARGED_HOME");
    }

    public void completeTreatment(int patientId, String doctorName, String treatmentNotes, String disposition)
            throws InvalidPatientDataException, PatientNotFoundException, DatabaseOperationException {

        if (doctorName == null || doctorName.trim().isEmpty()) {
            throw new InvalidPatientDataException("Attending doctor name is required to complete treatment.");
        }
        if (treatmentNotes == null || treatmentNotes.trim().isEmpty()) {
            throw new InvalidPatientDataException("Clinical treatment notes are required before discharge.");
        }

        Optional<Patient> patientOpt = patientDao.findById(patientId);
        if (patientOpt.isEmpty()) {
            throw new PatientNotFoundException("Patient ID " + patientId + " not found.");
        }

        Patient patient = patientOpt.get();

        // 1. Update or create completed treatment record
        treatmentRecordDao.completeTreatmentByPatientId(patientId, doctorName.trim(), treatmentNotes.trim(), disposition);

        // 2. Mark patient as COMPLETED
        patientDao.updateStatus(patientId, PatientStatus.COMPLETED);

        // 3. Ensure removed from waiting queue if still there
        queueService.removeFromQueue(patient);

        // 4. HEPS 2.0: Release doctor workload
        if (patient.getAssignedDoctorId() != null) {
            routingService.releaseDoctorFromPatient(patient.getAssignedDoctorId());
        }
    }

    public Optional<TreatmentRecord> getTreatmentForPatient(int patientId) throws DatabaseOperationException {
        return treatmentRecordDao.findByPatientId(patientId);
    }

    public List<TreatmentRecord> getAllTreatmentRecords() throws DatabaseOperationException {
        return treatmentRecordDao.findAll();
    }
}
