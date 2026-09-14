package com.hospital.priority.util;

import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique emergency triage token numbers.
 * Format: TK-YYYY-XXXX (e.g. TK-2026-0013)
 */
public class TokenGenerator {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(100);

    private TokenGenerator() {
    }

    /**
     * Initializes the atomic counter based on the maximum existing token index.
     */
    public static void initializeSequence(int maxExistingIndex) {
        if (maxExistingIndex >= SEQUENCE.get()) {
            SEQUENCE.set(maxExistingIndex);
        }
    }

    /**
     * Generates a new unique token number.
     * Thread-safe and monotonically increasing.
     */
    public static String nextToken() {
        int currentYear = Year.now().getValue();
        int seq = SEQUENCE.incrementAndGet();
        return String.format("TK-%d-%04d", currentYear, seq);
    }
}
