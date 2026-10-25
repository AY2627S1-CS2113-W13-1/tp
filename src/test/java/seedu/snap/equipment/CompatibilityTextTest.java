package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// @@author Rohita-29

/**
 * Tests validation of shared equipment compatibility text.
 */
class CompatibilityTextTest {
    @ParameterizedTest
    @ValueSource(strings = {"Canon RF", "APS-C", "1/4\"-20", "LP-E17",
        "unspecified", "Mount  with  spaces", "Canon\tRF"})

    void normalize_validText_preservesValue(String value) {
        assertEquals(value, CompatibilityText.normalize(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {" Canon RF ", "\tCanon RF\t", "\u2003Canon RF\u2003", "\u00A0Canon RF\u00A0"})

    void normalize_surroundingWhitespace_removesWhitespace(String value) {
        assertEquals("Canon RF", CompatibilityText.normalize(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\u2003", "\u00A0"})
    void normalize_missingValue_throwsException(String value) {
        assertThrows(
                IllegalArgumentException.class,
                () -> CompatibilityText.normalize(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Canon\nRF", "Canon\rRF", "Canon\r\nRF", "Canon\u2028RF", "\nCanon RF", "Canon RF\n"})
    void normalize_lineBreak_throwsException(String value) {
        assertThrows(
                IllegalArgumentException.class,
                () -> CompatibilityText.normalize(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Canon\u0000RF", "Canon\u001BRF"})
    void normalize_controlCharacter_throwsException(String value) {
        assertThrows(
                IllegalArgumentException.class,
                () -> CompatibilityText.normalize(value));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void normalize_validLength_acceptsValue(int length) {
        String value = "A".repeat(length);

        assertEquals(value, CompatibilityText.normalize(value));
    }

    @Test
    void normalize_longValue_throwsException() {
        String value = "A".repeat(101);

        assertThrows(
                IllegalArgumentException.class,
                () -> CompatibilityText.normalize(value));
    }

    @Test
    void normalize_paddedMaximumLength_acceptsTrimmedValue() {
        String value = "A".repeat(100);

        assertEquals(value, CompatibilityText.normalize(" " + value + " "));
    }
}

// @@author
