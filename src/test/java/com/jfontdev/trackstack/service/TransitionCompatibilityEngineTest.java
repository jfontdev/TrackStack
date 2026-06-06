package com.jfontdev.trackstack.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TransitionCompatibilityEngine}.
 * <p>
 * These tests cover key compatibility (Camelot wheel rules) and BPM difference
 * calculations. No Spring context or database is required — these are pure
 * logic tests.
 */
class TransitionCompatibilityEngineTest {

    private final TransitionCompatibilityEngine engine = new TransitionCompatibilityEngine();

    // --- Key Compatibility Tests ---

    @Test
    void sameKeyIsCompatible() {
        assertThat(engine.calculateKeyCompatibility("4A", "4A")).isTrue();
    }

    @Test
    void differentLetterKeysAreNotCompatible() {
        assertThat(engine.calculateKeyCompatibility("4A", "4B")).isFalse();
    }

    @Test
    void adjacentNumbersSameLetterAreCompatible() {
        assertThat(engine.calculateKeyCompatibility("4A", "5A")).isTrue();
        assertThat(engine.calculateKeyCompatibility("5A", "4A")).isTrue();
    }

    @Test
    void relativeMajorMinorSameLetterDiff7AreCompatible() {
        assertThat(engine.calculateKeyCompatibility("4A", "11A")).isTrue();
        assertThat(engine.calculateKeyCompatibility("11A", "4A")).isTrue();
    }

    @Test
    void nonAdjacentNonRelativeKeysAreNotCompatible() {
        assertThat(engine.calculateKeyCompatibility("4A", "7A")).isFalse();
    }

    @Test
    void nullKeysReturnNull() {
        assertThat(engine.calculateKeyCompatibility(null, "4A")).isNull();
        assertThat(engine.calculateKeyCompatibility("4A", null)).isNull();
    }

    @Test
    void caseInsensitiveMatching() {
        assertThat(engine.calculateKeyCompatibility("4a", "4A")).isTrue();
        assertThat(engine.calculateKeyCompatibility(" 4A ", "4A")).isTrue();
    }

    @Test
    void nonCamelotKeysOnlyExactMatch() {
        assertThat(engine.calculateKeyCompatibility("F# minor", "F# minor")).isTrue();
        assertThat(engine.calculateKeyCompatibility("F# minor", "G minor")).isFalse();
    }

    @Test
    void invalidCamelotKeysFallbackToExactMatch() {
        assertThat(engine.calculateKeyCompatibility("13A", "13A")).isTrue(); // exact match
        assertThat(engine.calculateKeyCompatibility("13A", "1A")).isFalse();  // not exact, not valid Camelot
    }

    // --- BPM Difference Tests ---

    @Test
    void bpmDifferenceIsAbsolute() {
        assertThat(engine.calculateBpmDifference(130.0, 140.0)).isEqualTo(10.0);
        assertThat(engine.calculateBpmDifference(140.0, 130.0)).isEqualTo(10.0);
    }

    @Test
    void sameBpmDifferenceIsZero() {
        assertThat(engine.calculateBpmDifference(130.0, 130.0)).isEqualTo(0.0);
    }

    @Test
    void nullBpmReturnsNull() {
        assertThat(engine.calculateBpmDifference(null, 130.0)).isNull();
        assertThat(engine.calculateBpmDifference(130.0, null)).isNull();
    }

    // --- Camelot Parsing Tests ---

    @Test
    void parseValidCamelotKeys() {
        assertThat(engine.parseCamelotKey("4A"))
                .isEqualTo(new TransitionCompatibilityEngine.CamelotKey(4, "A"));
        assertThat(engine.parseCamelotKey("11B"))
                .isEqualTo(new TransitionCompatibilityEngine.CamelotKey(11, "B"));
        assertThat(engine.parseCamelotKey("12A"))
                .isEqualTo(new TransitionCompatibilityEngine.CamelotKey(12, "A"));
        assertThat(engine.parseCamelotKey("1B"))
                .isEqualTo(new TransitionCompatibilityEngine.CamelotKey(1, "B"));
    }

    @Test
    void parseInvalidCamelotKeysReturnsNull() {
        assertThat(engine.parseCamelotKey("F minor")).isNull();
        assertThat(engine.parseCamelotKey("4")).isNull();
        assertThat(engine.parseCamelotKey("A")).isNull();
        assertThat(engine.parseCamelotKey("13A")).isNull();
        assertThat(engine.parseCamelotKey("0A")).isNull();
        assertThat(engine.parseCamelotKey(null)).isNull();
    }
}
