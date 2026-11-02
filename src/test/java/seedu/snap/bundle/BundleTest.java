// @@author yarnieee
package seedu.snap.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
// @@author
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
// @@author yarnieee
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Lens;
// @@author
import seedu.snap.equipment.Tripod;
// @@author yarnieee

/** Tests bundle identity, contents, and completion state. */
class BundleTest {
    @Test
    void newBundle_isIncompleteWithNoContents() {
        Bundle bundle = new Bundle("Travel Kit", "B001");

        assertEquals("Travel Kit", bundle.getBundleName());
        assertEquals("B001", bundle.getBundleId());
        assertTrue(bundle.getEquipmentList().isEmpty());
        assertFalse(bundle.isComplete());
    }

    @Test
    void addEquipment_meetsRequirements_marksBundleComplete() {
        Bundle bundle = new Bundle("Travel Kit", "B001");

        bundle.addEquipment(new Camera("Camera", "E001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "E002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        bundle.addEquipment(new Lens("Lens", "E003", 8, "24 to 70 mm", Lens.LensDamage.NONE));

        assertTrue(bundle.isComplete());
        assertEquals(3, bundle.getEquipmentList().size());
    }

    @Test
    void addEquipment_afterCompletion_keepsBundleComplete() {
        Bundle bundle = new Bundle("Travel Kit", "B001");
        bundle.addEquipment(new Camera("Camera", "E001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "E002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        bundle.addEquipment(new Lens("Lens", "E003", 8, "24 to 70 mm", Lens.LensDamage.NONE));

        bundle.addEquipment(new Battery("Spare Battery", "E004", 8, "1800 mAh", Battery.BatteryDamage.NONE));

        assertTrue(bundle.isComplete());
        assertEquals(4, bundle.getEquipmentList().size());
    }
    // @@author

    @Test
    void removeEquipmentById_updatesContentsAndCompletion() {
        // @@author yarnieee
        Bundle bundle = new Bundle("Travel Kit", "B001");
        bundle.addEquipment(new Camera("Camera", "E001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "E002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        bundle.addEquipment(new Lens("Lens", "E003", 8, "24 to 70 mm", Lens.LensDamage.NONE));
        assertTrue(bundle.isComplete());

        assertTrue(bundle.removeEquipmentById("E003"));
        // @@author

        assertEquals(2, bundle.getEquipmentList().size());
        // @@author yarnieee
        assertFalse(bundle.containsEquipmentId("E003"));
        assertFalse(bundle.isComplete());
    }

    @Test
    // @@author
    void removeEquipmentById_unknownItem_leavesBundleUnchanged() {
        // @@author yarnieee
        Bundle bundle = new Bundle("Travel Kit", "B001");
        bundle.addEquipment(new Camera("Camera", "E001", 8, "Full frame", Camera.CameraDamage.NONE));

        assertFalse(bundle.removeEquipmentById("E999"));
        // @@author

        assertEquals(1, bundle.getEquipmentList().size());
        // @@author yarnieee
        assertTrue(bundle.containsEquipmentId("E001"));
        // @@author
    }

    @Test
    void addEquipment_optionalTripod_keepsBundleComplete() {
        // @@author yarnieee
        Bundle bundle = new Bundle("Travel Kit", "B001");
        bundle.addEquipment(new Camera("Camera", "E001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "E002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        assertFalse(bundle.isComplete());
        bundle.addEquipment(new Lens("Lens", "E003", 8, "24 to 70 mm", Lens.LensDamage.NONE));
        assertTrue(bundle.isComplete());

        bundle.addEquipment(new Tripod("Tripod", "E004", 8, "Quarter inch", Tripod.TripodDamage.NONE));

        assertTrue(bundle.isComplete());
        // @@author
    }

    @Test
    void getEquipmentList_preservesObjectsAndRejectsExternalChanges() {
        // @@author yarnieee
        Bundle bundle = new Bundle("Travel Kit", "B001");
        Camera camera = new Camera("Camera", "E001", 8, "Full frame", Camera.CameraDamage.SHUTTER_FAULT);
        // @@author
        bundle.addEquipment(camera);

        assertSame(camera, bundle.getEquipmentList().getFirst());
        assertEquals("sensor/Full frame damage/SHUTTER_FAULT", bundle.getEquipmentList().getFirst().getDetails());
        assertThrows(UnsupportedOperationException.class, () -> bundle.getEquipmentList().clear());
        // @@author yarnieee
        assertTrue(bundle.containsEquipmentId("E001"));
        assertFalse(bundle.containsEquipmentId("E002"));
        // @@author
    }
    // @@author yarnieee
}
// @@author
