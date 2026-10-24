package com.hospital.priority;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.service.QueueService;
import com.hospital.priority.util.DBConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertDoesNotThrow(() -> {
            queueService.initializeQueueFromDatabase();
        });

        int size = queueService.getQueueSize();
        assertTrue(size > 0, "Database should contain seeded waiting patients");

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
    }
}
