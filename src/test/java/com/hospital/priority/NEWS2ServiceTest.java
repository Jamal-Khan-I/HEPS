package com.hospital.priority;

import com.hospital.priority.model.Vitals;
import com.hospital.priority.service.NEWS2Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Page 5 NEWS2 Early Warning Score calculation.
 */
public class NEWS2ServiceTest {

    @Test
    @DisplayName("Verify exact Ramesh Kumar vitals from PDF Page 5 calculate NEWS2 = 9")
    public void testPage5RameshKumarNews2() {
        // From Page 5:
        // SpO2 88% (<=91 -> 3 pts)
        // Heart rate 134 bpm (>=131 -> 3 pts)
        // Temperature 98.6 F (96.9-100.4 -> 0 pts)
        // Systolic BP 150 mmHg (111-219 -> 0 pts)
        // Diastolic BP 95 mmHg
        // Respiratory rate 26 /min (>=25 -> 3 pts)
        // GCS 15 (Alert -> 0 pts)
        // Total expected: 3 + 3 + 0 + 0 + 3 + 0 = 9!
        Vitals vitals = new Vitals(88, 134, 98.6, 150, 95, 26);
        int score = NEWS2Service.calculateScore(vitals, 15);

        assertEquals(9, score, "NEWS2 score for Ramesh Kumar on Page 5 must be exactly 9");
        assertEquals("HIGH", NEWS2Service.getRiskTier(score));
    }

    @Test
    @DisplayName("Normal physiological vitals result in NEWS2 = 0 (Low risk)")
    public void testNormalVitalsNews2Zero() {
        Vitals normal = new Vitals(99, 72, 98.4, 120, 80, 16);
        int score = NEWS2Service.calculateScore(normal, 15);

        assertEquals(0, score);
        assertEquals("LOW", NEWS2Service.getRiskTier(score));
    }
}
