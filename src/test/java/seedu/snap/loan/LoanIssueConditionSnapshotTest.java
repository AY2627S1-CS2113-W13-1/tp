package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Camera.CameraDamage;

// @@author Rohita-29

/** Tests the condition snapshot captured when a loan is issued. */
class LoanIssueConditionSnapshotTest {
    private static final LocalDateTime ISSUE_DATE =
            LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime DUE_DATE =
            LocalDateTime.of(2026, 10, 20, 0, 0);

    @Test
    void constructor_identifiedEquipment_capturesConditionSnapshot() {
        Camera camera = createCamera();

        Loan loan =
                new Loan(
                        "001",
                        camera,
                        "101",
                        ISSUE_DATE,
                        DUE_DATE);

        EquipmentConditionSnapshot snapshot =
                loan.getIssueConditionSnapshot();

        assertNotNull(snapshot);
        assertEquals("camera", snapshot.getEquipmentType());
        assertEquals(6, snapshot.getCondition());
        assertEquals("SENSOR_DAMAGE", snapshot.getDamage());
        assertEquals("Handle carefully", snapshot.getRemarks());
    }

    @Test
    void constructor_legacyEquipment_capturesConditionSnapshot() {
        Camera camera = createCamera();

        Loan loan =
                new Loan(
                        camera,
                        "101",
                        ISSUE_DATE,
                        DUE_DATE);

        EquipmentConditionSnapshot snapshot =
                loan.getIssueConditionSnapshot();

        assertNotNull(snapshot);
        assertEquals("camera", snapshot.getEquipmentType());
        assertEquals(6, snapshot.getCondition());
    }

    @Test
    void equipmentUpdatedAfterIssue_snapshotRemainsUnchanged() {
        Camera camera = createCamera();
        Loan loan =
                new Loan(
                        "001",
                        camera,
                        "101",
                        ISSUE_DATE,
                        DUE_DATE);

        camera.updateDetails("Updated Camera", 9);

        EquipmentConditionSnapshot snapshot =
                loan.getIssueConditionSnapshot();

        assertNotNull(snapshot);
        assertEquals(6, snapshot.getCondition());
        assertEquals("SENSOR_DAMAGE", snapshot.getDamage());
        assertEquals("Handle carefully", snapshot.getRemarks());
    }

    @Test
    void returnedCopy_preservesIssueConditionSnapshot() {
        Loan loan =
                new Loan(
                        "001",
                        createCamera(),
                        "101",
                        ISSUE_DATE,
                        DUE_DATE);

        Loan returned = loan.returnedCopy();

        assertTrue(returned.isReturned());
        assertEquals(
                loan.getIssueConditionSnapshot(),
                returned.getIssueConditionSnapshot());
    }

    @Test
    void constructor_detachedRecord_hasNoConditionSnapshot() {
        Loan loan =
                new Loan(
                        "001",
                        "002",
                        "101",
                        ISSUE_DATE,
                        DUE_DATE);

        assertNull(loan.getIssueConditionSnapshot());
    }

    /** Creates an available camera with known condition information. */
    private Camera createCamera() {
        return new Camera(
                "Test Camera",
                "002",
                6,
                "Full frame",
                CameraDamage.SENSOR_DAMAGE,
                "Handle carefully");
    }
}

// @@author
