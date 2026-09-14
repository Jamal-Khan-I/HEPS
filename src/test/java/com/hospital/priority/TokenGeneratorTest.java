package com.hospital.priority;

import com.hospital.priority.util.TokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying unique token generation and formatting.
 */
public class TokenGeneratorTest {

    @Test
    @DisplayName("Generated tokens match expected format TK-YYYY-XXXX")
    public void testTokenFormat() {
        String token = TokenGenerator.nextToken();
        assertNotNull(token);
        int currentYear = Year.now().getValue();
        String prefix = "TK-" + currentYear + "-";

        assertTrue(token.startsWith(prefix), "Token should start with " + prefix + " but was: " + token);
        assertEquals(prefix.length() + 4, token.length(), "Token length should match TK-YYYY-XXXX pattern");
    }

    @Test
    @DisplayName("Generated tokens must be unique across 1,000 successive generations")
    public void testTokenUniqueness() {
        Set<String> tokenSet = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            String t = TokenGenerator.nextToken();
            boolean added = tokenSet.add(t);
            assertTrue(added, "Duplicate token detected: " + t);
        }

        assertEquals(count, tokenSet.size(), "All 1,000 tokens must be distinct");
    }
}
