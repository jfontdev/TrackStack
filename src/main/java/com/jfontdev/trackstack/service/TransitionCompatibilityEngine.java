package com.jfontdev.trackstack.service;

import org.springframework.stereotype.Component;

/**
 * Shared engine for calculating musical compatibility between two tracks.
 * <p>
 * This component is responsible for two core DJ-mixing metrics:
 * <ul>
 *   <li><b>Harmonic key compatibility</b> — based on the Camelot wheel system</li>
 *   <li><b>BPM difference</b> — absolute tempo gap between two tracks</li>
 * </ul>
 * <p>
 * It is intentionally decoupled from {@link com.jfontdev.trackstack.service.impl.TransitionServiceImpl}
 * so that other services (e.g. setlist validation) can reuse the same rules without
 * duplicating the logic. The methods are pure functions: they accept raw key/BPM strings
 * and return calculated values, with no database interaction.
 * <p>
 * <b>Key Compatibility Rules (Camelot Wheel):</b>
 * <ul>
 *   <li>Same key → compatible</li>
 *   <li>Adjacent numbers, same letter (e.g. 4A ↔ 5A) → compatible</li>
 *   <li>Same letter, number ±7 (e.g. 4A ↔ 11A) → compatible (relative minor/major)</li>
 *   <li>Everything else → not compatible</li>
 * </ul>
 *
 * @see com.jfontdev.trackstack.service.impl.TransitionServiceImpl
 */
@Component
public class TransitionCompatibilityEngine {

    /**
     * Calculates whether two musical keys are harmonically compatible.
     * <p>
     * Uses the Camelot wheel system. Keys that are not in Camelot notation
     * (e.g. traditional "F# minor") are only considered compatible if they are
     * an exact string match after normalization.
     *
     * @param sourceKey the source track's key string (may be null or non-Camelot)
     * @param targetKey the target track's key string (may be null or non-Camelot)
     * @return {@code true} if compatible, {@code false} if not compatible,
     *         {@code null} if either key is missing (unknown)
     */
    public Boolean calculateKeyCompatibility(String sourceKey, String targetKey) {
        if (sourceKey == null || targetKey == null) {
            return null;
        }

        String normalizedSource = normalizeKey(sourceKey);
        String normalizedTarget = normalizeKey(targetKey);

        // Exact match
        if (normalizedSource.equals(normalizedTarget)) {
            return true;
        }

        // Try Camelot parsing
        CamelotKey sourceCamelot = parseCamelotKey(normalizedSource);
        CamelotKey targetCamelot = parseCamelotKey(normalizedTarget);

        if (sourceCamelot == null || targetCamelot == null) {
            return false;
        }

        if (sourceCamelot.letter.equals(targetCamelot.letter)) {
            int numDiff = Math.abs(sourceCamelot.number - targetCamelot.number);

            if (numDiff == 1) {
                return true;
            }

            if (numDiff == 7) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates the absolute BPM difference between two tracks.
     *
     * @param sourceBpm the source track's BPM (may be null)
     * @param targetBpm the target track's BPM (may be null)
     * @return the absolute BPM difference, or {@code null} if either BPM is missing
     */
    public Double calculateBpmDifference(Double sourceBpm, Double targetBpm) {
        if (sourceBpm == null || targetBpm == null) {
            return null;
        }
        return Math.abs(sourceBpm - targetBpm);
    }

    /**
     * Normalizes a key string for consistent comparison.
     * <p>
     * Trims whitespace and converts to uppercase.
     *
     * @param key the raw key string
     * @return the normalized key string
     */
    public String normalizeKey(String key) {
        return key.trim().toUpperCase();
    }

    /**
     * Attempts to parse a key string in Camelot notation.
     * <p>
     * Camelot notation consists of a number (1-12) followed by a letter:
     * <ul>
     *   <li><b>A</b> = minor key</li>
     *   <li><b>B</b> = major key</li>
     * </ul>
     *
     * @param key the normalized key string to parse
     * @return a {@link CamelotKey} if parsing succeeds, {@code null} otherwise
     */
    public CamelotKey parseCamelotKey(String key) {
        if (key == null || key.length() < 2) {
            return null;
        }

        char letter = key.charAt(key.length() - 1);
        if (letter != 'A' && letter != 'B') {
            return null;
        }

        String numberStr = key.substring(0, key.length() - 1);
        try {
            int number = Integer.parseInt(numberStr);
            if (number < 1 || number > 12) {
                return null;
            }
            return new CamelotKey(number, String.valueOf(letter));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Immutable data holder for a parsed Camelot key.
     *
     * @param number the Camelot wheel position (1-12)
     * @param letter the key quality ("A" for minor, "B" for major)
     */
    public record CamelotKey(int number, String letter) {
    }
}
