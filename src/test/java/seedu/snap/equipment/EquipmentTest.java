package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests the internal construction contract with assertions enabled. */
class EquipmentTest {
    @Test
    void constructor_validBoundaryValues_preservesFields() {
        Equipment first = new Equipment("A", "001", "camera", 1);
        Equipment last = new Equipment("A".repeat(100), "999", "audio", 10);

        assertEquals("A", first.getItemName());
        assertEquals("001", first.getItemId());
        assertEquals("camera", first.getType());
        assertEquals(1, first.getCondition());
        assertEquals(100, last.getItemName().length());
        assertEquals("999", last.getItemId());
        assertEquals(10, last.getCondition());
    }

    @Test
    void constructor_invalidInternalValues_failAssertions() {
        assertTrue(Equipment.class.desiredAssertionStatus(), "Run this test with Java assertions enabled");
        String[] invalidNames = {null, "", "   ", "A".repeat(101)};
        for (String name : invalidNames) {
            assertThrows(AssertionError.class, () -> new Equipment(name, "001", "camera", 1));
        }
        String[] invalidIds = {null, "", "000", "1", "1000", "abc"};
        for (String id : invalidIds) {
            assertThrows(AssertionError.class, () -> new Equipment("Camera", id, "camera", 1));
        }
        String[] invalidTypes = {null, "", "   "};
        for (String type : invalidTypes) {
            assertThrows(AssertionError.class, () -> new Equipment("Camera", "001", type, 1));
        }
        int[] invalidConditions = {Integer.MIN_VALUE, 0, 11, Integer.MAX_VALUE};
        for (int condition : invalidConditions) {
            assertThrows(AssertionError.class, () -> new Equipment("Camera", "001", "camera", condition));
        }
    }
}
