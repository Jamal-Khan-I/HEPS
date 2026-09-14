package com.hospital.priority.util;

import com.hospital.priority.exception.InvalidPatientDataException;

import java.util.regex.Pattern;

/**
 * Validates triage patient data according to hospital registration rules.
 */
public class ValidationUtil {

    // Accepts international or local phone numbers: e.g. +91 98765 43210, 9876543210, 011-23456789
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?\\d{1,3}[- ]?)?\\d{10}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    private ValidationUtil() {
    }

    /**
     * Validates all registration fields for a patient.
     * Throws InvalidPatientDataException with a clear descriptive message on failure.
     */
    public static void validatePatientInput(String fullName, String ageStr, String gender,
                                           String phone, String symptoms) throws InvalidPatientDataException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new InvalidPatientDataException("Patient full name is required.");
        }
        if (fullName.trim().length() < 2) {
            throw new InvalidPatientDataException("Patient name must be at least 2 characters long.");
        }
        if (DIGITS_ONLY.matcher(fullName.trim()).matches()) {
            throw new InvalidPatientDataException("Patient name cannot consist solely of numbers.");
        }

        if (ageStr == null || ageStr.trim().isEmpty()) {
            throw new InvalidPatientDataException("Patient age is required.");
        }
        int age;
        try {
            age = Integer.parseInt(ageStr.trim());
        } catch (NumberFormatException e) {
            throw new InvalidPatientDataException("Age must be a valid whole number.");
        }
        if (age < 0 || age > 125) {
            throw new InvalidPatientDataException("Age must be between 0 and 125 years.");
        }

        if (gender == null || gender.trim().isEmpty()) {
            throw new InvalidPatientDataException("Gender selection is required.");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new InvalidPatientDataException("Contact phone number is required.");
        }
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        if (cleanPhone.length() < 10 || cleanPhone.length() > 15) {
            throw new InvalidPatientDataException("Contact phone number must contain 10 to 15 digits.");
        }

        if (symptoms == null || symptoms.trim().isEmpty()) {
            throw new InvalidPatientDataException("Symptoms / triage clinical notes are required.");
        }
        if (symptoms.trim().length() < 3) {
            throw new InvalidPatientDataException("Please provide a more descriptive clinical symptom note.");
        }
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String clean = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return clean.length() >= 10 && clean.length() <= 15 && clean.matches("^\\+?\\d+$");
    }

    public static boolean isValidAge(int age) {
        return age >= 0 && age <= 125;
    }

    /**
     * Masks contact phone numbers for display in public/triage tables.
     * Keeps first few and last few characters, replacing middle with ****.
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "---";
        }
        String clean = phone.trim();
        if (clean.length() <= 5) {
            return "***";
        }
        int len = clean.length();
        int prefixLen = Math.min(6, len / 2);
        int suffixLen = Math.min(3, len - prefixLen);
        return clean.substring(0, prefixLen) + "****" + clean.substring(len - suffixLen);
    }
}
