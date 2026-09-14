package com.hospital.priority;

import com.hospital.priority.dao.PatientDao;
import com.hospital.priority.dao.TreatmentRecordDao;
import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.dao.impl.TreatmentRecordDaoImpl;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientStatus;
import com.hospital.priority.model.TreatmentRecord;
import com.hospital.priority.util.DBConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test verifying JDBC persistence against MySQL:
 * 1. Insert patient via PreparedStatement with RETURN_GENERATED_KEYS.
 * 2. Query patient back by ID and verify column mapping.
 * 3. Update patient status to IN_TREATMENT and verify persistence.
 * 4. Insert and complete treatment record with doctor and notes.
 * 5. Verify status transition to COMPLETED and completed_at timestamp.
 * 6. Clean up test records.
 */
public class JdbcPersistenceIntegrationTest {

    @Test
    @DisplayName("Full JDBC round-trip: Insert, query, update status, complete treatment, and verify persistence in MySQL")
    public void testFullJdbcLifecycle() {
        if (!DBConnection.testConnection()) {
            System.out.println("Notice: MySQL database not reachable, skipping live JDBC test.");
            return;
        }

        PatientDao patientDao = new PatientDaoImpl();
        TreatmentRecordDao treatmentDao = new TreatmentRecordDaoImpl();

        String testToken = "TK-TEST-" + System.currentTimeMillis();
        Patient testPatient = new Patient(
                testToken,
                "Test Patient (Audit)",
                42,
                "Male",
                "+91 98000 99999",
                "Severe chest pressure and acute shortness of breath",
                EmergencyLevel.CRITICAL,
                LocalDateTime.now()
        );

        // 1. Insert Patient
        int patientId = assertDoesNotThrow(() -> patientDao.insert(testPatient));
        assertTrue(patientId > 0, "Generated patient ID should be positive");
        assertEquals(patientId, testPatient.getPatientId());

        // 2. Query back by ID
        Optional<Patient> fetchedOpt = assertDoesNotThrow(() -> patientDao.findById(patientId));
        assertTrue(fetchedOpt.isPresent(), "Inserted patient should be retrievable from MySQL");
        Patient fetched = fetchedOpt.get();
        assertEquals(testToken, fetched.getTokenNo());
        assertEquals("Test Patient (Audit)", fetched.getFullName());
        assertEquals(42, fetched.getAge());
        assertEquals(EmergencyLevel.CRITICAL, fetched.getEmergencyLevel());
        assertEquals(PatientStatus.WAITING, fetched.getStatus());

        // 3. Update Status to IN_TREATMENT
        boolean statusUpdated = assertDoesNotThrow(() -> patientDao.updateStatus(patientId, PatientStatus.IN_TREATMENT));
        assertTrue(statusUpdated, "Status update should succeed");

        Optional<Patient> inTreatmentOpt = assertDoesNotThrow(() -> patientDao.findById(patientId));
        assertTrue(inTreatmentOpt.isPresent());
        assertEquals(PatientStatus.IN_TREATMENT, inTreatmentOpt.get().getStatus());

        // 4. Create Treatment Record
        TreatmentRecord record = new TreatmentRecord(
                patientId,
                "Dr. Audit Specialist",
                "Admitted to resuscitation bay for immediate stabilization.",
                LocalDateTime.now()
        );
        int recordId = assertDoesNotThrow(() -> treatmentDao.insert(record));
        assertTrue(recordId > 0, "Generated treatment record ID should be positive");

        // 5. Complete Treatment
        boolean treatmentCompleted = assertDoesNotThrow(() -> 
                treatmentDao.completeTreatmentByPatientId(patientId, "Dr. Audit Specialist", "Patient stabilized and transferred to CCU."));
        assertTrue(treatmentCompleted, "Treatment completion should succeed");

        // Verify Treatment Record in MySQL has completed_at
        Optional<TreatmentRecord> completedRecOpt = assertDoesNotThrow(() -> treatmentDao.findByPatientId(patientId));
        assertTrue(completedRecOpt.isPresent());
        assertNotNull(completedRecOpt.get().getCompletedAt(), "completed_at timestamp must be stored in MySQL");
        assertEquals("Dr. Audit Specialist", completedRecOpt.get().getDoctorName());

        // 6. Clean up test records (treatment record first, then patient)
        assertDoesNotThrow(() -> {
            try (java.sql.Connection conn = DBConnection.getConnection();
                 java.sql.PreparedStatement stmt1 = conn.prepareStatement("DELETE FROM treatment_records WHERE patient_id = ?");
                 java.sql.PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM patients WHERE patient_id = ?")) {
                stmt1.setInt(1, patientId);
                stmt1.executeUpdate();
                stmt2.setInt(1, patientId);
                stmt2.executeUpdate();
            }
        });

        // Verify clean up
        Optional<Patient> postClean = assertDoesNotThrow(() -> patientDao.findById(patientId));
        assertTrue(postClean.isEmpty(), "Test patient should be deleted after test cleanup");
    }
}
