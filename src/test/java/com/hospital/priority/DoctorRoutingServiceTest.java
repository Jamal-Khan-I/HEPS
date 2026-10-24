package com.hospital.priority;

import com.hospital.priority.model.Doctor;
import com.hospital.priority.model.DoctorStatus;
import com.hospital.priority.model.Specialty;
import com.hospital.priority.service.DoctorRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HEPS 2.0 Doctor Routing Service Tests")
class DoctorRoutingServiceTest {

    private DoctorRoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = DoctorRoutingService.getInstance();
    }

    @Test
    @DisplayName("Should successfully route to available cardiologist for cardiology specialty")
    void testCardiologyRouting() {
        DoctorRoutingService.RoutingDecision decision = routingService.routeDoctor(Specialty.CARDIOLOGY);

        assertNotNull(decision);
        assertNotNull(decision.getAssignedDoctor());
        assertTrue(decision.isExactSpecialtyMatch());
        assertEquals(Specialty.CARDIOLOGY, decision.getAssignedDoctor().getSpecialty());
    }

    @Test
    @DisplayName("Should route to available neurologist for neurology specialty")
    void testNeurologyRouting() {
        DoctorRoutingService.RoutingDecision decision = routingService.routeDoctor(Specialty.NEUROLOGY);

        assertNotNull(decision);
        assertNotNull(decision.getAssignedDoctor());
        assertTrue(decision.isExactSpecialtyMatch());
        assertEquals(Specialty.NEUROLOGY, decision.getAssignedDoctor().getSpecialty());
    }

    @Test
    @DisplayName("Doctor workload increment and decrement lifecycle")
    void testDoctorWorkloadLifecycle() {
        Doctor doc = new Doctor(99, "Dr. Test Specialist", Specialty.CARDIOLOGY, DoctorStatus.AVAILABLE, 0, "Suite 99", "12345");

        assertEquals(0, doc.getCurrentWorkload());
        assertEquals(DoctorStatus.AVAILABLE, doc.getStatus());

        doc.incrementWorkload();
        assertEquals(1, doc.getCurrentWorkload());

        doc.incrementWorkload();
        doc.incrementWorkload();
        assertEquals(3, doc.getCurrentWorkload());
        assertEquals(DoctorStatus.BUSY, doc.getStatus());

        doc.decrementWorkload();
        assertEquals(2, doc.getCurrentWorkload());
        assertEquals(DoctorStatus.AVAILABLE, doc.getStatus());
    }
}
