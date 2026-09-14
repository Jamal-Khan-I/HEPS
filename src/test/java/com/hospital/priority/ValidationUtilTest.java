package com.hospital.priority;

import com.hospital.priority.exception.InvalidPatientDataException;
import com.hospital.priority.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for input validation rules.
 */
public class ValidationUtilTest {

    @Test
    @DisplayName("Valid patient input passes validation successfully")
    public void testValidInputPasses() {
        assertDoesNotThrow(() -> {
            ValidationUtil.validatePatientInput("Sunita Verma", "34", "Female", "+91 98765 43210", "Severe headache and nausea");
        });
    }

    @Test
    @DisplayName("Invalid patient data is rejected: negative age")
    public void testNegativeAgeRejected() {
        InvalidPatientDataException ex = assertThrows(InvalidPatientDataException.class, () -> {
            ValidationUtil.validatePatientInput("John Doe", "-5", "Male", "9876543210", "Fever");
        });
        assertTrue(ex.getMessage().contains("between 0 and 125"));
    }

    @Test
    @DisplayName("Invalid patient data is rejected: non-numeric age")
    public void testNonNumericAgeRejected() {
        InvalidPatientDataException ex = assertThrows(InvalidPatientDataException.class, () -> {
            ValidationUtil.validatePatientInput("John Doe", "forty", "Male", "9876543210", "Fever");
        });
        assertTrue(ex.getMessage().contains("whole number"));
    }

    @Test
    @DisplayName("Invalid patient data is rejected: blank patient name")
    public void testBlankNameRejected() {
        InvalidPatientDataException ex = assertThrows(InvalidPatientDataException.class, () -> {
            ValidationUtil.validatePatientInput("   ", "25", "Female", "9876543210", "Pain");
        });
        assertTrue(ex.getMessage().contains("name is required"));
    }

    @Test
    @DisplayName("Invalid patient data is rejected: invalid contact phone")
    public void testInvalidPhoneRejected() {
        InvalidPatientDataException ex = assertThrows(InvalidPatientDataException.class, () -> {
            ValidationUtil.validatePatientInput("John Doe", "25", "Male", "123", "Pain");
        });
        assertTrue(ex.getMessage().contains("10 to 15 digits"));
    }

    @Test
    @DisplayName("Invalid patient data is rejected: blank symptoms")
    public void testBlankSymptomsRejected() {
        InvalidPatientDataException ex = assertThrows(InvalidPatientDataException.class, () -> {
            ValidationUtil.validatePatientInput("John Doe", "25", "Male", "9876543210", "  ");
        });
        assertTrue(ex.getMessage().contains("Symptoms"));
    }
    @Test
    @DisplayName("Phone masking properly masks digits outside detailed view")
    public void testMaskPhone() {
        assertEquals("+91 98****001", ValidationUtil.maskPhone("+91 98000 00001"));
        assertEquals("98765****210", ValidationUtil.maskPhone("9876543210"));
        assertEquals("***", ValidationUtil.maskPhone("123"));
        assertEquals("---", ValidationUtil.maskPhone(null));
    }
}
