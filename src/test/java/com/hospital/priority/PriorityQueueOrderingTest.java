package com.hospital.priority;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientPriorityComparator;
import com.hospital.priority.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying PriorityQueue behaviour:
 * - Critical patient served before all others
 * - Correct Min-Heap extraction order
 * - Queue head peek vs poll removal
 * - Queue snapshot ordering
 */
public class PriorityQueueOrderingTest {

    private PriorityQueue<Patient> queue;

    @BeforeEach
    public void setUp() {
        queue = new PriorityQueue<>(new PatientPriorityComparator());
    }

    @Test
    @DisplayName("PriorityQueue extracts patients in strict urgency order regardless of insertion sequence")
    public void testStrictUrgencyOrder() {
        LocalDateTime t1 = LocalDateTime.of(2026, 9, 14, 8, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 9, 14, 8, 10, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 9, 14, 8, 20, 0);
        LocalDateTime t4 = LocalDateTime.of(2026, 9, 14, 8, 30, 0);

        // Insert in reverse priority order (Low first, Critical last)
        Patient low = new Patient("TK-2026-0001", "Low Patient", 20, "Male", "9876543210", "Cough", EmergencyLevel.LOW, t1);
        Patient med = new Patient("TK-2026-0002", "Medium Patient", 30, "Female", "9876543211", "Sprain", EmergencyLevel.MEDIUM, t2);
        Patient high = new Patient("TK-2026-0003", "High Patient", 40, "Male", "9876543212", "Fracture", EmergencyLevel.HIGH, t3);
        Patient critical = new Patient("TK-2026-0004", "Critical Patient", 50, "Female", "9876543213", "Cardiac Arrest", EmergencyLevel.CRITICAL, t4);

        queue.offer(low);
        queue.offer(med);
        queue.offer(high);
        queue.offer(critical);

        assertEquals(4, queue.size());

        // Polling MUST return Critical first, despite being inserted last!
        assertEquals("Critical Patient", queue.poll().getFullName());
        assertEquals("High Patient", queue.poll().getFullName());
        assertEquals("Medium Patient", queue.poll().getFullName());
        assertEquals("Low Patient", queue.poll().getFullName());

        assertTrue(queue.isEmpty(), "Queue should be empty after polling all patients");
    }

    @Test
    @DisplayName("Earlier arrival patient is served first when priority levels are identical")
    public void testEarlierArrivalWithEqualPriority() {
        LocalDateTime tEarly = LocalDateTime.of(2026, 9, 14, 9, 0, 0);
        LocalDateTime tLate = LocalDateTime.of(2026, 9, 14, 9, 30, 0);

        Patient lateCritical = new Patient("TK-2026-0002", "Late Stroke Patient", 60, "Male", "9876543210", "Stroke", EmergencyLevel.CRITICAL, tLate);
        Patient earlyCritical = new Patient("TK-2026-0001", "Early STEMI Patient", 55, "Female", "9876543211", "STEMI", EmergencyLevel.CRITICAL, tEarly);

        queue.offer(lateCritical);
        queue.offer(earlyCritical);

        Patient firstServed = queue.poll();
        assertEquals("Early STEMI Patient", firstServed.getFullName(),
                "Earlier arrival patient must be polled first when emergency levels match");
    }

    @Test
    @DisplayName("Queue correctly removes served patient")
    public void testQueueRemovesServedPatient() {
        Patient p1 = new Patient("TK-2026-0001", "Alice", 25, "Female", "9876543210", "Allergy", EmergencyLevel.HIGH, LocalDateTime.now());
        Patient p2 = new Patient("TK-2026-0002", "Bob", 30, "Male", "9876543211", "Asthma", EmergencyLevel.MEDIUM, LocalDateTime.now());

        queue.offer(p1);
        queue.offer(p2);

        assertEquals(2, queue.size());
        Patient headPeeked = queue.peek();
        assertEquals("Alice", headPeeked.getFullName());
        assertEquals(2, queue.size(), "Peek must not remove element");

        Patient headPolled = queue.poll();
        assertEquals("Alice", headPolled.getFullName());
        assertEquals(1, queue.size(), "Poll must remove element from queue");
        assertEquals("Bob", queue.peek().getFullName(), "Next in line is now Bob");
    }
}
