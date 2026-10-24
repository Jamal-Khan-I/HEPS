package com.hospital.priority.service;

import com.hospital.priority.dao.DoctorDao;
import com.hospital.priority.dao.impl.DoctorDaoImpl;
import com.hospital.priority.exception.DatabaseOperationException;
import com.hospital.priority.model.Doctor;
import com.hospital.priority.model.DoctorStatus;
import com.hospital.priority.model.Patient;
import com.hospital.priority.model.Specialty;

import java.util.*;

/**
 * ============================================================================
 * HEPS 2.0 DOCTOR ROUTING SERVICE
 * ============================================================================
 * Implements deterministic clinical routing:
 * 1. AI or Java Rule suggests the Specialty required.
 * 2. Java selects the actual Doctor based on:
 *    Specialty Match -> Doctor Availability -> Lowest Workload (Load Balancing).
 * 3. Fallback: If no specialist is available, routes to available General Emergency doctor.
 */
public class DoctorRoutingService {

    private static DoctorRoutingService instance;
    private final DoctorDao doctorDao;

    private DoctorRoutingService() {
        this.doctorDao = new DoctorDaoImpl();
    }

    public static synchronized DoctorRoutingService getInstance() {
        if (instance == null) {
            instance = new DoctorRoutingService();
        }
        return instance;
    }

    public static class RoutingDecision {
        private final Doctor assignedDoctor;
        private final Specialty matchedSpecialty;
        private final boolean exactSpecialtyMatch;
        private final String routingRationale;

        public RoutingDecision(Doctor assignedDoctor, Specialty matchedSpecialty,
                               boolean exactSpecialtyMatch, String routingRationale) {
            this.assignedDoctor = assignedDoctor;
            this.matchedSpecialty = matchedSpecialty;
            this.exactSpecialtyMatch = exactSpecialtyMatch;
            this.routingRationale = routingRationale;
        }

        public Doctor getAssignedDoctor() {
            return assignedDoctor;
        }

        public Specialty getMatchedSpecialty() {
            return matchedSpecialty;
        }

        public boolean isExactSpecialtyMatch() {
            return exactSpecialtyMatch;
        }

        public String getRoutingRationale() {
            return routingRationale;
        }

        @Override
        public String toString() {
            return assignedDoctor.getName() + " [" + matchedSpecialty.getDisplayName() + "] - " + routingRationale;
        }
    }

    /**
     * Determines the optimal doctor for a patient based on required specialty.
     */
    public RoutingDecision routeDoctor(Specialty targetSpecialty) {
        if (targetSpecialty == null) {
            targetSpecialty = Specialty.GENERAL_EMERGENCY;
        }

        try {
            // STEP 1: Search for AVAILABLE doctors in target specialty
            List<Doctor> specialists = doctorDao.findAvailableBySpecialty(targetSpecialty);

            if (!specialists.isEmpty()) {
                // Pick doctor with lowest current workload
                Doctor bestDoctor = pickDoctorWithLowestWorkload(specialists);
                return new RoutingDecision(bestDoctor, targetSpecialty, true,
                        "Assigned to available " + targetSpecialty.getDisplayName() + " specialist with lowest active workload (" + bestDoctor.getCurrentWorkload() + " patients).");
            }

            // STEP 2: Fallback to AVAILABLE General Emergency doctors
            List<Doctor> generalEr = doctorDao.findAvailableBySpecialty(Specialty.GENERAL_EMERGENCY);
            if (!generalEr.isEmpty()) {
                Doctor bestGeneral = pickDoctorWithLowestWorkload(generalEr);
                return new RoutingDecision(bestGeneral, Specialty.GENERAL_EMERGENCY, false,
                        targetSpecialty.getDisplayName() + " specialists are currently busy/off-duty. Routed to General Emergency physician with lowest workload (" + bestGeneral.getCurrentWorkload() + " patients).");
            }

            // STEP 3: Fallback to any AVAILABLE doctor across hospital
            List<Doctor> allAvailable = doctorDao.findAvailable();
            if (!allAvailable.isEmpty()) {
                Doctor anyDoctor = pickDoctorWithLowestWorkload(allAvailable);
                return new RoutingDecision(anyDoctor, anyDoctor.getSpecialty(), false,
                        "High hospital load. Routed to next available physician: " + anyDoctor.getName() + " (" + anyDoctor.getSpecialty().getDisplayName() + ").");
            }

            // STEP 4: All doctors are BUSY! Pick target specialty doctor with least backlog
            List<Doctor> allSpecialists = doctorDao.findBySpecialty(targetSpecialty);
            if (!allSpecialists.isEmpty()) {
                Doctor leastBusy = pickDoctorWithLowestWorkload(allSpecialists);
                return new RoutingDecision(leastBusy, targetSpecialty, true,
                        "All specialists busy. Queued for " + leastBusy.getName() + " (Least backlog: " + leastBusy.getCurrentWorkload() + " patients).");
            }

            // Absolute fallback
            List<Doctor> allDoctors = doctorDao.findAll();
            Doctor fallback = pickDoctorWithLowestWorkload(allDoctors);
            return new RoutingDecision(fallback, fallback.getSpecialty(), false,
                    "Emergency queue assignment: " + fallback.getName());

        } catch (DatabaseOperationException e) {
            System.err.println("Error during doctor routing: " + e.getMessage());
            Doctor defaultDoc = new Doctor(1, "Dr. Emergency On-Duty", Specialty.GENERAL_EMERGENCY, DoctorStatus.AVAILABLE, 0, "ER Station 1", "");
            return new RoutingDecision(defaultDoc, Specialty.GENERAL_EMERGENCY, false, "Fallback to default on-duty emergency physician.");
        }
    }

    private Doctor pickDoctorWithLowestWorkload(List<Doctor> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .min(Comparator.comparingInt(Doctor::getCurrentWorkload)
                        .thenComparingInt(Doctor::getDoctorId))
                .orElse(candidates.get(0));
    }

    public void assignPatientToDoctor(Doctor doctor, Patient patient) {
        if (doctor != null && patient != null) {
            patient.setAssignedDoctorId(doctor.getDoctorId());
            patient.setAssignedDoctorName(doctor.getName());
            patient.setAssignedSpecialty(doctor.getSpecialty());

            doctor.incrementWorkload();
            try {
                doctorDao.updateWorkload(doctor.getDoctorId(), doctor.getCurrentWorkload());
                doctorDao.updateStatus(doctor.getDoctorId(), doctor.getStatus());
            } catch (DatabaseOperationException e) {
                System.err.println("Notice: Could not persist doctor workload update: " + e.getMessage());
            }
        }
    }

    public void releaseDoctorFromPatient(Integer doctorId) {
        if (doctorId != null && doctorId > 0) {
            try {
                Optional<Doctor> docOpt = doctorDao.findById(doctorId);
                if (docOpt.isPresent()) {
                    Doctor doc = docOpt.get();
                    doc.decrementWorkload();
                    doctorDao.updateWorkload(doc.getDoctorId(), doc.getCurrentWorkload());
                    doctorDao.updateStatus(doc.getDoctorId(), doc.getStatus());
                }
            } catch (DatabaseOperationException e) {
                System.err.println("Notice: Could not release doctor workload: " + e.getMessage());
            }
        }
    }

    public List<Doctor> getAllDoctors() {
        try {
            return doctorDao.findAll();
        } catch (DatabaseOperationException e) {
            return Collections.emptyList();
        }
    }

    public Map<Specialty, Integer> getAvailableDoctorsCountBySpecialty() {
        Map<Specialty, Integer> counts = new EnumMap<>(Specialty.class);
        for (Specialty s : Specialty.values()) {
            counts.put(s, 0);
        }
        try {
            List<Doctor> available = doctorDao.findAvailable();
            for (Doctor d : available) {
                counts.put(d.getSpecialty(), counts.getOrDefault(d.getSpecialty(), 0) + 1);
            }
        } catch (DatabaseOperationException e) {
            // fallback counts
        }
        return counts;
    }
}
