package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// @@author Rohita-29

/** Tests immutable equipment condition snapshots. */
class EquipmentConditionSnapshotTest {
    @Test
    void constructor_validValues_normalizesAndPreservesFields() {
        EquipmentConditionSnapshot snapshot =
                new EquipmentConditionSnapshot(
                        " Camera ",
                        6,
                        " sensor_damage ",
                        " Handle carefully ");

        assertEquals("camera", snapshot.getEquipmentType());
        assertEquals(6, snapshot.getCondition());
        assertEquals("SENSOR_DAMAGE", snapshot.getDamage());
        assertEquals("Handle carefully", snapshot.getRemarks());
    }

    @Test
    void constructor_boundaryConditions_acceptsValues() {
        EquipmentConditionSnapshot pristine =
                new EquipmentConditionSnapshot(
                        "camera",
                        1,
                        "NONE",
                        "");
        EquipmentConditionSnapshot unusable =
                new EquipmentConditionSnapshot(
                        "camera",
                        10,
                        "SHUTTER_FAULT",
                        "");

        assertEquals(1, pristine.getCondition());
        assertEquals(10, unusable.getCondition());
    }

    @Test
    void constructor_invalidConditions_throwException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera",
                        0,
                        "NONE",
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera",
                        11,
                        "NONE",
                        ""));
    }

    @Test
    void constructor_missingType_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        null,
                        5,
                        "NONE",
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "   ",
                        5,
                        "NONE",
                        ""));
    }

    @Test
    void constructor_invalidDamage_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera",
                        5,
                        "",
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera",
                        5,
                        "SENSOR\nDAMAGE",
                        ""));
    }

    @Test
    void constructor_invalidRemarks_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera",
                        5,
                        "NONE",
                        "A".repeat(
                                EquipmentConditionSnapshot
                                        .MAX_REMARKS_LENGTH
                                        + 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera",
                        5,
                        "NONE",
                        "First line\nSecond line"));
    }

    @Test
    void equals_sameFields_returnsTrue() {
        EquipmentConditionSnapshot first =
                new EquipmentConditionSnapshot(
                        "camera",
                        5,
                        "NONE",
                        "Ready");
        EquipmentConditionSnapshot second =
                new EquipmentConditionSnapshot(
                        "CAMERA",
                        5,
                        "none",
                        "Ready");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equals_differentCondition_returnsFalse() {
        EquipmentConditionSnapshot first =
                new EquipmentConditionSnapshot(
                        "camera",
                        5,
                        "NONE",
                        "Ready");
        EquipmentConditionSnapshot second =
                new EquipmentConditionSnapshot(
                        "camera",
                        6,
                        "NONE",
                        "Ready");

        assertNotEquals(first, second);
    }
}

// @@author
