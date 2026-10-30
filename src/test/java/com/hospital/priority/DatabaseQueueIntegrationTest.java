package com.hospital.priority;

import com.hospital.priority.dao.impl.PatientDaoImpl;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.service.QueueService;
import com.hospital.priority.util.DBConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that loading waiting patients from MySQL database
 * populates the PriorityQueue with the correct priority ordering.
 */
public class DatabaseQueueIntegrationTest {

    @Test
    @DisplayName("Loading waiting patients from database produces correct triage priority order")
    public void testDatabaseBootstrapProducesCorrectPriorityOrder() {
        // Skip if database is not reachable in current test environment
        if (!DBConnection.testConnection()) {
            System.out.println("Notice: Database connection not available, skipping DB integration test.");
            return;
        }

        QueueService queueService = QueueService.getInstance();
        PatientDaoImpl patientDao = new PatientDaoImpl();

        // If database starts clean, seed 2 test patients for integration verification
        int pLowId = -1;
        int pCritId = -1;
        try {
            queueService.initializeQueueFromDatabase();
        } catch (Exception ignored) {}

        if (queueService.getQueueSize() == 0) {
            Patient pLow = new Patient("TK-TEST-LOW", "Test Low Priority", 25, "Female", "9999900001", "Mild rash", EmergencyLevel.LOW, LocalDateTime.now().minusMinutes(5));
            Patient pCrit = new Patient("TK-TEST-CRIT", "Test Crit Priority", 65, "Male", "9999900002", "Severe chest pain", EmergencyLevel.CRITICAL, LocalDateTime.now().minusMinutes(2));
            try {
                pLowId = patientDao.insert(pLow);
                pCritId = patientDao.insert(pCrit);
            } catch (Exception e) {
                fail("Failed to setup test patients: " + e.getMessage());
            }
        }

        try {
            assertDoesNotThrow(() -> {
                queueService.initializeQueueFromDatabase();
            });

            int size = queueService.getQueueSize();
            assertTrue(size > 0, "Database should contain waiting patients");

            List<Patient> sortedSnapshot = queueService.getSortedSnapshot();
            assertEquals(size, sortedSnapshot.size());

            // Verify that priority rank is monotonically non-decreasing (Rank 1 <= Rank 2 <= Rank 3 <= Rank 4)
            for (int i = 0; i < sortedSnapshot.size() - 1; i++) {
                Patient current = sortedSnapshot.get(i);
                Patient next = sortedSnapshot.get(i + 1);

                int rank1 = current.getEmergencyLevel().getRank();
                int rank2 = next.getEmergencyLevel().getRank();

                assertTrue(rank1 <= rank2, "Patient at pos " + (i + 1) + " (" + current.getEmergencyLevel() +
                        ") must have urgency >= patient at pos " + (i + 2) + " (" + next.getEmergencyLevel() + ")");
            }

            // Verify that the head of the queue is the top patient from sorted snapshot
            Patient head = queueService.peekNext();
            assertNotNull(head);
            assertEquals(sortedSnapshot.get(0).getPatientId(), head.getPatientId(),
                    "Head of queue must match the highest priority patient in the sorted snapshot");
            assertEquals(sortedSnapshot.get(0).getEmergencyLevel(), head.getEmergencyLevel(),
                    "Head of queue urgency level must match the first patient in sorted snapshot");
        } finally {
            // Clean up test patients
            if (pLowId != -1 || pCritId != -1) {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM patients WHERE patient_id IN (?, ?)")) {
                    stmt.setInt(1, pLowId);
                    stmt.setInt(2, pCritId);
                    stmt.executeUpdate();
                } catch (Exception ignored) {}
            }
            try {
                queueService.initializeQueueFromDatabase();
            } catch (Exception ignored) {}
        }
    }
}
