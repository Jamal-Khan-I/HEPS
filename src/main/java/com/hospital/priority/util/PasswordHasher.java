package com.hospital.priority.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for hashing user authentication passwords with SHA-256.
 */
public class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String plainText) {
        if (plainText == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean verify(String plainText, String hashed) {
        if (plainText == null || hashed == null) {
            return false;
        }
        return hash(plainText).equalsIgnoreCase(hashed);
    }
}
