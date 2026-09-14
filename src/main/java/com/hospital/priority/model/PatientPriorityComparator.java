package com.hospital.priority.model;

import java.io.Serializable;
import java.util.Comparator;

/**
 * ============================================================================
 * CORE DATA STRUCTURE & ALGORITHM (DSA) COMPONENT
 * ============================================================================
 * Dedicated Comparator for PriorityQueue&lt;Patient&gt; in the Emergency Triage System.
 *
 * Triage Priority Rules:
 * ----------------------------------------------------------------------------
 * 1. Urgency Level (Highest to Lowest):
 *    CRITICAL (Rank 1) &gt; HIGH (Rank 2) &gt; MEDIUM (Rank 3) &gt; LOW (Rank 4)
 *    Lower integer rank value = Higher medical urgency.
 *
 * 2. Tie-Breaker 1 - First-Come, First-Served (FIFO) on equal urgency:
 *    Earlier arrival timestamp takes precedence over later arrival.
 *
 * 3. Tie-Breaker 2 - Deterministic Unique Ordering:
 *    If urgency level and arrival timestamp are identical, smaller token number
 *    comes first (lexicographically or numerically).
 *
 * Time Complexity:
 * - compare(p1, p2): O(1) constant time comparison.
 * - Insertion in PriorityQueue&lt;Patient&gt; (Min-Heap): O(log N).
 * - Peek head (highest priority patient): O(1).
 * - Serve/poll head: O(log N).
 * ============================================================================
 */
public class PatientPriorityComparator implements Comparator<Patient>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Patient p1, Patient p2) {
        if (p1 == p2) {
            return 0;
        }
        if (p1 == null) {
            return 1;
        }
        if (p2 == null) {
            return -1;
        }

        // STEP 1: Compare by Emergency Urgency Rank
        // Lower rank number means higher emergency (CRITICAL=1, HIGH=2, MEDIUM=3, LOW=4)
        int rank1 = p1.getEmergencyLevel() != null ? p1.getEmergencyLevel().getRank() : EmergencyLevel.LOW.getRank();
        int rank2 = p2.getEmergencyLevel() != null ? p2.getEmergencyLevel().getRank() : EmergencyLevel.LOW.getRank();

        int urgencyComparison = Integer.compare(rank1, rank2);
        if (urgencyComparison != 0) {
            return urgencyComparison;
        }

        // STEP 2: Tie-breaker 1 - Earlier arrival time comes first
        if (p1.getArrivalTime() != null && p2.getArrivalTime() != null) {
            int arrivalComparison = p1.getArrivalTime().compareTo(p2.getArrivalTime());
            if (arrivalComparison != 0) {
                return arrivalComparison;
            }
        } else if (p1.getArrivalTime() != null) {
            return -1;
        } else if (p2.getArrivalTime() != null) {
            return 1;
        }

        // STEP 3: Tie-breaker 2 - Smaller token number comes first
        String token1 = p1.getTokenNo() != null ? p1.getTokenNo() : "";
        String token2 = p2.getTokenNo() != null ? p2.getTokenNo() : "";
        return token1.compareTo(token2);
    }
}
