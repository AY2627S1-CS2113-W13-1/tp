package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests the internal construction contract with assertions enabled. */
class EquipmentTest {
    @Test
    void constructor_validBoundaryValues_preservesFields() {
        // @@author yarnieee
        Equipment first = new Camera("A", "E001", 1);
        Equipment last = new Battery("A".repeat(100), "E999", 10);
        // @@author

        assertEquals("A", first.getItemName());
        // @@author yarnieee
        assertEquals("E001", first.getItemId());
        // @@author
        assertEquals("camera", first.getType());
        assertEquals(1, first.getCondition());
        // @@author yarnieee
        Equipment withRemarks = new Lens("Lens", "E002", 3, "unspecified", Lens.LensDamage.NONE,
                // @@author
                "Handle with care");
        assertEquals("Handle with care", withRemarks.getRemarks());
        assertEquals(100, last.getItemName().length());
        // @@author yarnieee
        assertEquals("E999", last.getItemId());
        // @@author
        assertEquals("battery", last.getType());
        assertEquals(10, last.getCondition());
    }

    @Test
    void constructor_invalidInternalValues_failAssertions() {
        assertTrue(Equipment.class.desiredAssertionStatus(), "Run this test with Java assertions enabled");
        String[] invalidNames = {null, "", "   ", "A".repeat(101)};
        for (String name : invalidNames) {
            // @@author yarnieee
            assertThrows(AssertionError.class, () -> new Camera(name, "E001", 1));
            // @@author
        }
        String[] invalidIds = {null, "", "000", "1", "1000", "abc"};
        for (String id : invalidIds) {
            assertThrows(AssertionError.class, () -> new Camera("Camera", id, 1));
        }
        int[] invalidConditions = {Integer.MIN_VALUE, 0, 11, Integer.MAX_VALUE};
        for (int condition : invalidConditions) {
            // @@author yarnieee
            assertThrows(AssertionError.class, () -> new Camera("Camera", "E001", condition));
            // @@author
        }
        assertThrows(AssertionError.class,
                // @@author yarnieee
                () -> new Camera("Camera", "E001", 1, "unspecified", Camera.CameraDamage.NONE,
                        // @@author
                        "r".repeat(201)));
        assertThrows(AssertionError.class,
                // @@author yarnieee
                () -> new Camera("Camera", "E001", 1, "unspecified", Camera.CameraDamage.NONE,
                        // @@author
                        "line one\nline two"));
    }
}
