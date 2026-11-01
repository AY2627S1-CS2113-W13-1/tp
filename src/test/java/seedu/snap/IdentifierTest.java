package seedu.snap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests the shared format and migration rules for every visible identifier type. */
class IdentifierTest {
    @Test
    void isValid_acceptsCanonicalIdentifiersAndBorrowerZero() {
        assertTrue(Identifier.isValid("E001", Identifier.Type.EQUIPMENT));
        assertTrue(Identifier.isValid("L999", Identifier.Type.LOAN));
        assertTrue(Identifier.isValid("B010", Identifier.Type.BUNDLE));
        assertTrue(Identifier.isValid("U000", Identifier.Type.BORROWER));
        assertTrue(Identifier.isValid("U999", Identifier.Type.BORROWER));
    }

    @Test
    void isValid_rejectsBareMalformedLowercaseZeroAndWrongTypeValues() {
        for (String value : new String[]{"001", "E000", "E1000", "e001", "L001"}) {
            assertFalse(Identifier.isValid(value, Identifier.Type.EQUIPMENT), value);
        }
        assertFalse(Identifier.isValid("U1000", Identifier.Type.BORROWER));
        assertFalse(Identifier.isValid("B000", Identifier.Type.BUNDLE));
        assertTrue(Identifier.isMismatched("L001", Identifier.Type.EQUIPMENT));
        assertFalse(Identifier.isMismatched("001", Identifier.Type.EQUIPMENT));
    }

    @Test
    void migrate_convertsLegacyValuesOnlyForSavedData() {
        assertEquals("E001", Identifier.migrate("001", Identifier.Type.EQUIPMENT));
        assertEquals("L002", Identifier.migrate("002", Identifier.Type.LOAN));
        assertEquals("B003", Identifier.migrate("003", Identifier.Type.BUNDLE));
        assertEquals("U000", Identifier.migrate("000", Identifier.Type.BORROWER));
        assertEquals("E004", Identifier.migrate("E004", Identifier.Type.EQUIPMENT));
    }

    @Test
    void migrate_rejectsReservedZeroAndWrongTypeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> Identifier.migrate("000", Identifier.Type.EQUIPMENT));
        assertThrows(IllegalArgumentException.class,
                () -> Identifier.migrate("L001", Identifier.Type.EQUIPMENT));
        assertThrows(IllegalArgumentException.class,
                () -> Identifier.migrate("001", null));
    }

    @Test
    void formatAndNumber_roundTripSupportedRanges() {
        assertEquals("U000", Identifier.format(0, Identifier.Type.BORROWER));
        assertEquals("E001", Identifier.format(1, Identifier.Type.EQUIPMENT));
        assertEquals(999, Identifier.number("L999", Identifier.Type.LOAN));
        assertThrows(IllegalArgumentException.class,
                () -> Identifier.format(0, Identifier.Type.BUNDLE));
        assertThrows(IllegalArgumentException.class,
                () -> Identifier.format(1000, Identifier.Type.BORROWER));
        assertThrows(IllegalArgumentException.class,
                () -> Identifier.number("E001", null));
    }
}
