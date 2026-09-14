package com.hospital.priority;

import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.PatientPriorityComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying the 3-tier emergency triage ordering:
 * 1. Urgency Level (CRITICAL > HIGH > MEDIUM > LOW)
 * 2. Earlier Arrival Time (FIFO on equal urgency)
 * 3. Smaller Token Number (Deterministic tie-breaker)
 */
public class PatientPriorityComparatorTest {

    private PatientPriorityComparator comparator;

    @BeforeEach
    public void setUp() {
        comparator = new PatientPriorityComparator();
    }

    @Test
    @DisplayName("Critical patient must be prioritized before High, Medium, and Low")
    public void testCriticalPatientServedBeforeOthers() {
        LocalDateTime now = LocalDateTime.now();

        Patient critical = new Patient("TK-2026-0001", "Cardiac Patient", 65, "Male", "9876543210", "Chest Pain", EmergencyLevel.CRITICAL, now);
        Patient high = new Patient("TK-2026-0002", "Fracture Patient", 30, "Female", "9876543211", "Broken Arm", EmergencyLevel.HIGH, now);
        Patient medium = new Patient("TK-2026-0003", "Asthma Patient", 25, "Male", "9876543212", "Wheezing", EmergencyLevel.MEDIUM, now);
        Patient low = new Patient("TK-2026-0004", "Fever Patient", 20, "Female", "9876543213", "Mild Fever", EmergencyLevel.LOW, now);

        assertTrue(comparator.compare(critical, high) < 0, "CRITICAL should come before HIGH");
        assertTrue(comparator.compare(high, medium) < 0, "HIGH should come before MEDIUM");
        assertTrue(comparator.compare(medium, low) < 0, "MEDIUM should come before LOW");
        assertTrue(comparator.compare(critical, low) < 0, "CRITICAL should come before LOW");
    }

    @Test
    @DisplayName("Earlier arrival is served first when priority levels match")
    public void testEarlierArrivalServedFirstWhenPriorityMatches() {
        LocalDateTime timeEarlier = LocalDateTime.of(2026, 9, 14, 9, 0, 0);
        LocalDateTime timeLater = LocalDateTime.of(2026, 9, 14, 9, 30, 0);

        Patient patientEarlier = new Patient("TK-2026-0005", "First Arrival", 40, "Male", "9876543210", "High Grade Fever", EmergencyLevel.HIGH, timeEarlier);
        Patient patientLater = new Patient("TK-2026-0006", "Second Arrival", 45, "Female", "9876543211", "Severe Abdominal Pain", EmergencyLevel.HIGH, timeLater);

        assertTrue(comparator.compare(patientEarlier, patientLater) < 0,
                "Earlier arrival patient must be prioritized ahead of later arrival for the same priority level");
        assertTrue(comparator.compare(patientLater, patientEarlier) > 0,
                "Later arrival patient must yield to earlier arrival");
    }

    @Test
    @DisplayName("Smaller token number breaks ties when priority and arrival time are identical")
    public void testDeterministicTieBreakerSmallerToken() {
        LocalDateTime sameTime = LocalDateTime.of(2026, 9, 14, 10, 0, 0);

        Patient p1 = new Patient("TK-2026-0001", "Patient Alpha", 35, "Male", "9876543210", "Sore throat", EmergencyLevel.LOW, sameTime);
        Patient p2 = new Patient("TK-2026-0002", "Patient Beta", 38, "Female", "9876543211", "Headache", EmergencyLevel.LOW, sameTime);

        assertTrue(comparator.compare(p1, p2) < 0, "TK-2026-0001 should come before TK-2026-0002 when all other fields match");
    }

    @Test
    @DisplayName("Comparing a patient to itself returns zero")
    public void testSamePatientComparison() {
        Patient p = new Patient("TK-2026-0010", "Patient Self", 50, "Male", "9876543210", "Sprain", EmergencyLevel.MEDIUM, LocalDateTime.now());
        assertEquals(0, comparator.compare(p, p), "Comparing same instance must return 0");
    }
}
