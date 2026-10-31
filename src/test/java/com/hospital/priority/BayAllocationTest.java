package com.hospital.priority;

import com.hospital.priority.model.Bay;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;
import com.hospital.priority.service.BayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BayAllocationTest {

    private BayService bayService;

    @BeforeEach
    public void setup() {
        bayService = BayService.getInstance();
        // Clear all bays to FREE state
        for (Bay b : bayService.getAllBays()) {
            b.markFree();
        }
    }

    @Test
    public void testBayNameNormalizationAndLookup() {
        // Various string formats nurses or UI may supply
        Optional<Bay> b1 = bayService.getBayByName("Bay 1 - Resus");
        assertTrue(b1.isPresent());
        assertEquals(1, b1.get().getBayId());

        Optional<Bay> b1Dot = bayService.getBayByName("Bay 1 · Resus");
        assertTrue(b1Dot.isPresent());
        assertEquals(1, b1Dot.get().getBayId());

        Optional<Bay> b1Paren = bayService.getBayByName("Bay 1 - Resus (free)");
        assertTrue(b1Paren.isPresent());
        assertEquals(1, b1Paren.get().getBayId());

        Optional<Bay> b1Bracket = bayService.getBayByName("Bay 1 [Resuscitation - Red]");
        assertTrue(b1Bracket.isPresent());
        assertEquals(1, b1Bracket.get().getBayId());

        Optional<Bay> b3b = bayService.getBayByName("Bay 3b - Acute");
        assertTrue(b3b.isPresent());
        assertEquals(4, b3b.get().getBayId());
    }

    @Test
    public void testAllocateBayForPatientSuccess() {
        Patient p = new Patient();
        p.setTokenNo("TK-TEST-001");
        p.setFullName("John Doe");
        p.setEmergencyLevel(EmergencyLevel.CRITICAL);
        p.setAssignedDoctorName("Dr. Rao");
        p.setAssignedBay("Bay 1 - Resus");

        Bay allocated = bayService.allocateBayForPatient(p.getAssignedBay(), p);
        assertNotNull(allocated);
        assertEquals(Bay.BayStatus.OCCUPIED, allocated.getStatus());
        assertEquals("TK-TEST-001", allocated.getCurrentPatientToken());
        assertEquals("John Doe", allocated.getCurrentPatientName());
        assertEquals(EmergencyLevel.CRITICAL, allocated.getCurrentPriority());
        assertEquals("Bay 1 - Resus", p.getAssignedBay());

        // Verify bay is reported occupied in bayService
        Optional<Bay> lookup = bayService.findBayForPatient("TK-TEST-001");
        assertTrue(lookup.isPresent());
        assertEquals(1, lookup.get().getBayId());
    }

    @Test
    public void testAllocateBayFallsBackWhenAlreadyOccupied() {
        // First patient takes Bay 1 - Resus
        Patient p1 = new Patient();
        p1.setTokenNo("TK-TEST-001");
        p1.setFullName("Patient One");
        p1.setEmergencyLevel(EmergencyLevel.CRITICAL);
        p1.setAssignedBay("Bay 1 - Resus");
        bayService.allocateBayForPatient("Bay 1 - Resus", p1);

        // Second critical patient also asks for Bay 1 - Resus, should fallback to Bay 2 - Trauma (also Critical)
        Patient p2 = new Patient();
        p2.setTokenNo("TK-TEST-002");
        p2.setFullName("Patient Two");
        p2.setEmergencyLevel(EmergencyLevel.CRITICAL);
        p2.setAssignedBay("Bay 1 - Resus");

        Bay allocatedP2 = bayService.allocateBayForPatient(p2.getAssignedBay(), p2);
        assertNotNull(allocatedP2);
        assertEquals(2, allocatedP2.getBayId());
        assertEquals(Bay.BayStatus.OCCUPIED, allocatedP2.getStatus());
        assertEquals("TK-TEST-002", allocatedP2.getCurrentPatientToken());
        assertEquals("Bay 2 - Trauma", p2.getAssignedBay());
    }
}

