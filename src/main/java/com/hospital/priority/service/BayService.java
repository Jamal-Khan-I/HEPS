package com.hospital.priority.service;

import com.hospital.priority.model.Bay;
import com.hospital.priority.model.EmergencyLevel;
import com.hospital.priority.model.Patient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * EMERGENCY BAY & BED MANAGEMENT SERVICE (Page 9)
 * ============================================================================
 * Manages the 12 Emergency Department physical bays and beds.
 * Enforces capacity limits (zero double-booking), supports cleaning countdowns,
 * and provides auto-allocation based on triage emergency level.
 */
public class BayService {

    private static BayService instance;

    private final Map<Integer, Bay> bays = new ConcurrentHashMap<>();

    private BayService() {
        initializeBays();
    }

    public static synchronized BayService getInstance() {
        if (instance == null) {
            instance = new BayService();
        }
        return instance;
    }

    /**
     * Initializes the 12 Emergency Department bays as specified in Page 9.
     */
    private void initializeBays() {
        bays.clear();

        // 1. Resus & Trauma (Critical Red Zone)
        Bay b1 = new Bay(1, "Bay 1 · Resus", Bay.BayType.RESUS, Bay.BayStatus.OCCUPIED);
        b1.setCurrentPatientToken("TK-0142");
        b1.setCurrentPatientName("Ramesh Kumar");
        b1.setCurrentPriority(EmergencyLevel.CRITICAL);
        b1.setAssignedDoctorName("Dr. Mehta");
        b1.setOccupiedSince(LocalDateTime.now().minusMinutes(3));
        bays.put(1, b1);

        Bay b2 = new Bay(2, "Bay 2 · Trauma", Bay.BayType.TRAUMA, Bay.BayStatus.OCCUPIED);
        b2.setCurrentPatientToken("TK-0136");
        b2.setCurrentPatientName("Meera Nair");
        b2.setCurrentPriority(EmergencyLevel.CRITICAL);
        b2.setAssignedDoctorName("Dr. Singh");
        b2.setOccupiedSince(LocalDateTime.now().minusMinutes(21));
        bays.put(2, b2);

        // 2. Acute Care (Orange Zone)
        Bay b3 = new Bay(3, "Bay 3 · Acute", Bay.BayType.ACUTE, Bay.BayStatus.FREE);
        bays.put(3, b3);

        Bay b3b = new Bay(4, "Bay 3b · Acute", Bay.BayType.ACUTE, Bay.BayStatus.OCCUPIED);
        b3b.setCurrentPatientToken("TK-0143");
        b3b.setCurrentPatientName("Anita Joshi");
        b3b.setCurrentPriority(EmergencyLevel.HIGH);
        b3b.setAssignedDoctorName("Dr. Rao");
        b3b.setOccupiedSince(LocalDateTime.now().minusMinutes(12));
        bays.put(4, b3b);

        // 3. Monitored Observation (Yellow Zone)
        Bay b4 = new Bay(5, "Bay 4 · Obs", Bay.BayType.OBSERVATION, Bay.BayStatus.OCCUPIED);
        b4.setCurrentPatientToken("TK-0131");
        b4.setCurrentPatientName("Gurpreet Singh");
        b4.setCurrentPriority(EmergencyLevel.MEDIUM);
        b4.setAssignedDoctorName("Dr. Miller");
        b4.setOccupiedSince(LocalDateTime.now().minusMinutes(48));
        bays.put(5, b4);

        Bay b4b = new Bay(6, "Bay 4b · Obs", Bay.BayType.OBSERVATION, Bay.BayStatus.OCCUPIED);
        b4b.setCurrentPatientToken("TK-0133");
        b4b.setCurrentPatientName("Siddharth Bose");
        b4b.setCurrentPriority(EmergencyLevel.MEDIUM);
        b4b.setAssignedDoctorName("Dr. Miller");
        b4b.setOccupiedSince(LocalDateTime.now().minusMinutes(33));
        bays.put(6, b4b);

        // 4. Fast Track & Ambulatory (Green Zone)
        Bay b5 = new Bay(7, "Bay 5 · Fast track", Bay.BayType.FAST_TRACK, Bay.BayStatus.OCCUPIED);
        b5.setCurrentPatientToken("TK-0145");
        b5.setCurrentPatientName("Tanya Kapoor");
        b5.setCurrentPriority(EmergencyLevel.LOW);
        b5.setAssignedDoctorName("Nurse Ana");
        b5.setOccupiedSince(LocalDateTime.now().minusMinutes(5));
        bays.put(7, b5);

        Bay b6 = new Bay(8, "Bay 6 · Ambulatory", Bay.BayType.AMBULATORY, Bay.BayStatus.CLEANING);
        b6.setCleaningUntil(LocalDateTime.now().plusMinutes(4));
        bays.put(8, b6);

        // 5. Extended Observation Beds (Beds 7 through 12)
        Bay b7 = new Bay(9, "Bay 7 · Obs", Bay.BayType.OBSERVATION, Bay.BayStatus.FREE);
        bays.put(9, b7);

        Bay b8 = new Bay(10, "Bed 8 · Obs", Bay.BayType.OBSERVATION, Bay.BayStatus.OCCUPIED);
        b8.setCurrentPatientToken("TK-0128");
        b8.setCurrentPatientName("Harish Patel");
        b8.setCurrentPriority(EmergencyLevel.HIGH);
        b8.setAssignedDoctorName("Dr. Rao");
        b8.setOccupiedSince(LocalDateTime.now().minusMinutes(52));
        bays.put(10, b8);

        Bay b9 = new Bay(11, "Bed 9 · Obs", Bay.BayType.OBSERVATION, Bay.BayStatus.OCCUPIED);
        b9.setCurrentPatientToken("TK-0129");
        b9.setCurrentPatientName("Sunita Reddy");
        b9.setCurrentPriority(EmergencyLevel.MEDIUM);
        b9.setAssignedDoctorName("Dr. Miller");
        b9.setOccupiedSince(LocalDateTime.now().minusMinutes(65));
        bays.put(11, b9);

        Bay b10 = new Bay(12, "Bed 10 · Obs", Bay.BayType.OBSERVATION, Bay.BayStatus.OCCUPIED);
        b10.setCurrentPatientToken("TK-0130");
        b10.setCurrentPatientName("Farhan Akhtar");
        b10.setCurrentPriority(EmergencyLevel.LOW);
        b10.setAssignedDoctorName("Dr. Miller");
        b10.setOccupiedSince(LocalDateTime.now().minusMinutes(18));
        bays.put(12, b10);
    }

    public synchronized List<Bay> getAllBays() {
        // Refresh cleaning bays
        checkCleaningStatus();
        List<Bay> list = new ArrayList<>(bays.values());
        list.sort(Comparator.comparingInt(Bay::getBayId));
        return Collections.unmodifiableList(list);
    }

    public synchronized List<Bay> getAvailableBays() {
        checkCleaningStatus();
        List<Bay> freeBays = new ArrayList<>();
        for (Bay b : bays.values()) {
            if (b.isAvailable()) {
                freeBays.add(b);
            }
        }
        freeBays.sort(Comparator.comparingInt(Bay::getBayId));
        return freeBays;
    }

    public synchronized List<Bay> getAvailableBaysForLevel(EmergencyLevel level) {
        List<Bay> available = getAvailableBays();
        if (level == null) return available;

        List<Bay> matched = new ArrayList<>();
        for (Bay b : available) {
            if (b.getType().getRecommendedLevel() == level) {
                matched.add(b);
            }
        }
        return !matched.isEmpty() ? matched : available;
    }

    public synchronized Optional<Bay> getBayById(int bayId) {
        checkCleaningStatus();
        return Optional.ofNullable(bays.get(bayId));
    }

    public synchronized Optional<Bay> getBayByName(String name) {
        if (name == null) return Optional.empty();
        checkCleaningStatus();
        for (Bay b : bays.values()) {
            if (b.getName().equalsIgnoreCase(name.trim())) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<Bay> findBayForPatient(String patientToken) {
        if (patientToken == null) return Optional.empty();
        for (Bay b : bays.values()) {
            if (patientToken.equalsIgnoreCase(b.getCurrentPatientToken())) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    /**
     * Assigns a patient to a specified bay.
     * Enforces capacity limit: Throws IllegalStateException if the bay is already occupied or cleaning.
     */
    public synchronized void assignPatientToBay(int bayId, Patient patient) {
        Bay bay = bays.get(bayId);
        if (bay == null) {
            throw new IllegalArgumentException("Bay ID " + bayId + " does not exist.");
        }
        if (bay.getStatus() != Bay.BayStatus.FREE) {
            throw new IllegalStateException("Bay " + bay.getName() + " is currently " + bay.getStatus().getDisplayName() +
                    ". Cannot assign patient " + patient.getTokenNo());
        }

        // Release patient's old bay if transferring
        releasePatientBay(patient.getTokenNo(), 10);

        bay.occupy(patient);
        patient.setAssignedBay(bay.getName());
    }

    /**
     * Releases bay assigned to a patient and puts it into CLEANING status.
     */
    public synchronized void releasePatientBay(String patientToken, int cleaningMinutes) {
        if (patientToken == null) return;
        for (Bay b : bays.values()) {
            if (patientToken.equalsIgnoreCase(b.getCurrentPatientToken())) {
                b.releaseToCleaning(cleaningMinutes);
                break;
            }
        }
    }

    /**
     * Immediately marks a cleaning bay as free.
     */
    public synchronized void markBayCleaned(int bayId) {
        Bay bay = bays.get(bayId);
        if (bay != null) {
            bay.markFree();
        }
    }

    /**
     * Transfers an active patient from one bay to an available target bay.
     */
    public synchronized void transferPatient(int fromBayId, int toBayId) {
        Bay from = bays.get(fromBayId);
        Bay to = bays.get(toBayId);
        if (from == null || to == null) return;
        if (!to.isAvailable()) {
            throw new IllegalStateException("Target bay " + to.getName() + " is not currently free.");
        }
        to.occupyFromBay(from);
        from.releaseToCleaning(10);
    }

    /**
     * Auto-advances cleaning bays when their timer expires.
     */
    public synchronized void checkCleaningStatus() {
        LocalDateTime now = LocalDateTime.now();
        for (Bay b : bays.values()) {
            if (b.getStatus() == Bay.BayStatus.CLEANING && b.getCleaningUntil() != null) {
                if (now.isAfter(b.getCleaningUntil())) {
                    b.markFree();
                }
            }
        }
    }

    public synchronized int getOccupiedCount() {
        int count = 0;
        for (Bay b : bays.values()) {
            if (b.getStatus() == Bay.BayStatus.OCCUPIED) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getTotalBaysCount() {
        return bays.size();
    }

    public synchronized int getResusOccupiedCount() {
        int count = 0;
        for (Bay b : bays.values()) {
            if (b.getType() == Bay.BayType.RESUS && b.getStatus() == Bay.BayStatus.OCCUPIED) {
                count++;
            }
        }
        return count;
    }
}
