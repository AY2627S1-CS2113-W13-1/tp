package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests the internal construction contract with assertions enabled. */
class EquipmentTest {
    @Test
    void constructor_validBoundaryValues_preservesFields() {
        Equipment first = new Camera("A", "001", 1);
        Equipment last = new Battery("A".repeat(100), "999", 10);

        assertEquals("A", first.getItemName());
        assertEquals("001", first.getItemId());
        assertEquals("camera", first.getType());
        assertEquals(1, first.getCondition());
        Equipment withRemarks = new Lens("Lens", "002", 3, "unspecified", Lens.LensDamage.NONE,
                "Handle with care");
        assertEquals("Handle with care", withRemarks.getRemarks());
        assertEquals(100, last.getItemName().length());
        assertEquals("999", last.getItemId());
        assertEquals("battery", last.getType());
        assertEquals(10, last.getCondition());
    }

    @Test
    void constructor_invalidInternalValues_failAssertions() {
        assertTrue(Equipment.class.desiredAssertionStatus(), "Run this test with Java assertions enabled");
        String[] invalidNames = {null, "", "   ", "A".repeat(101)};
        for (String name : invalidNames) {
            assertThrows(AssertionError.class, () -> new Camera(name, "001", 1));
        }
        String[] invalidIds = {null, "", "000", "1", "1000", "abc"};
        for (String id : invalidIds) {
            assertThrows(AssertionError.class, () -> new Camera("Camera", id, 1));
        }
        int[] invalidConditions = {Integer.MIN_VALUE, 0, 11, Integer.MAX_VALUE};
        for (int condition : invalidConditions) {
            assertThrows(AssertionError.class, () -> new Camera("Camera", "001", condition));
        }
        assertThrows(AssertionError.class,
                () -> new Camera("Camera", "001", 1, "unspecified", Camera.CameraDamage.NONE,
                        "r".repeat(201)));
        assertThrows(AssertionError.class,
                () -> new Camera("Camera", "001", 1, "unspecified", Camera.CameraDamage.NONE,
                        "line one\nline two"));
        assertThrows(AssertionError.class,
                () -> new Camera("Camera", "001", 1, "unspecified", Camera.CameraDamage.NONE, null));
        assertThrows(AssertionError.class,
                () -> new Camera("Camera", "001", 1, "unspecified", Camera.CameraDamage.NONE,
                        "line one\rline two"));
    }

    @Test
    void returnAssessmentAndLoanId_validateAllReachableIdentityFields() {
        Camera original = new Camera("Camera", "001", 5);
        assertThrows(IllegalArgumentException.class, () -> original.applyReturnAssessment(null));
        assertThrows(IllegalArgumentException.class,
                () -> original.applyReturnAssessment(new Camera("Camera", "002", 5)));
        Camera differentRecord = new Camera("Camera", "001", 5);
        differentRecord.restoreRecordId("e21c9254-ab91-4624-82eb-9b4423e5a795");
        assertThrows(IllegalArgumentException.class, () -> original.applyReturnAssessment(differentRecord));
        assertThrows(IllegalArgumentException.class,
                () -> original.applyReturnAssessment(new Camera("Camera", "001", 6)));
        assertThrows(AssertionError.class, () -> original.setLoanId("000"));
        original.setLoanId(null);
    }
}
