package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Battery.BatteryDamage;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Camera.CameraDamage;
import seedu.snap.equipment.Lens;
import seedu.snap.equipment.Lens.LensDamage;
import seedu.snap.equipment.Tripod;
import seedu.snap.equipment.Tripod.TripodDamage;

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
    void constructor_unsupportedEquipmentType_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "drone", 5, "NONE", ""));
    }

    @Test
    void constructor_damageNotValidForEquipmentType_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera", 5, "SWELLING", ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new EquipmentConditionSnapshot(
                        "camera", 5, "UNKNOWN_DAMAGE", ""));
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
    void fromEquipment_camera_capturesCameraCondition() {
        Camera camera =
                new Camera(
                        "Test Camera",
                        "001",
                        6,
                        "Full frame",
                        CameraDamage.SENSOR_DAMAGE,
                        "Handle carefully");

        EquipmentConditionSnapshot snapshot =
                EquipmentConditionSnapshot.fromEquipment(camera);

        assertEquals("camera", snapshot.getEquipmentType());
        assertEquals(6, snapshot.getCondition());
        assertEquals("SENSOR_DAMAGE", snapshot.getDamage());
        assertEquals("Handle carefully", snapshot.getRemarks());
    }

    @Test
    void fromEquipment_lens_capturesLensCondition() {
        Lens lens =
                new Lens(
                        "Test Lens",
                        "002",
                        5,
                        "24-70mm",
                        LensDamage.SCRATCHED_GLASS,
                        "Small scratch");

        EquipmentConditionSnapshot snapshot =
                EquipmentConditionSnapshot.fromEquipment(lens);

        assertEquals("lens", snapshot.getEquipmentType());
        assertEquals(5, snapshot.getCondition());
        assertEquals("SCRATCHED_GLASS", snapshot.getDamage());
        assertEquals("Small scratch", snapshot.getRemarks());
    }

    @Test
    void fromEquipment_battery_capturesBatteryCondition() {
        Battery battery =
                new Battery(
                        "Test Battery",
                        "003",
                        4,
                        "2400mAh",
                        BatteryDamage.SWELLING,
                        "Do not use");

        EquipmentConditionSnapshot snapshot =
                EquipmentConditionSnapshot.fromEquipment(battery);

        assertEquals("battery", snapshot.getEquipmentType());
        assertEquals(4, snapshot.getCondition());
        assertEquals("SWELLING", snapshot.getDamage());
        assertEquals("Do not use", snapshot.getRemarks());
    }

    @Test
    void fromEquipment_tripod_capturesTripodCondition() {
        Tripod tripod =
                new Tripod(
                        "Test Tripod",
                        "004",
                        7,
                        "Quarter inch",
                        TripodDamage.BENT_LEG,
                        "Check before use");

        EquipmentConditionSnapshot snapshot =
                EquipmentConditionSnapshot.fromEquipment(tripod);

        assertEquals("tripod", snapshot.getEquipmentType());
        assertEquals(7, snapshot.getCondition());
        assertEquals("BENT_LEG", snapshot.getDamage());
        assertEquals("Check before use", snapshot.getRemarks());
    }

    @Test
    void fromEquipment_nullEquipment_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentConditionSnapshot.fromEquipment(null));
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
