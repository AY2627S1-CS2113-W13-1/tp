package seedu.snap.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Lens;
import seedu.snap.equipment.Tripod;

/** Tests bundle identity, contents, and completion state. */
class BundleTest {
    @Test
    void newBundle_isIncompleteWithNoContents() {
        Bundle bundle = new Bundle("Travel Kit", "001");

        assertEquals("Travel Kit", bundle.getBundleName());
        assertEquals("001", bundle.getBundleId());
        assertTrue(bundle.getEquipmentList().isEmpty());
        assertFalse(bundle.isComplete());
    }

    @Test
    void addEquipment_meetsRequirements_marksBundleComplete() {
        Bundle bundle = new Bundle("Travel Kit", "001");

        bundle.addEquipment(new Camera("Camera", "001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        bundle.addEquipment(new Lens("Lens", "003", 8, "24 to 70 mm", Lens.LensDamage.NONE));

        assertTrue(bundle.isComplete());
        assertEquals(3, bundle.getEquipmentList().size());
    }

    @Test
    void addEquipment_afterCompletion_keepsBundleComplete() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        bundle.addEquipment(new Camera("Camera", "001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        bundle.addEquipment(new Lens("Lens", "003", 8, "24 to 70 mm", Lens.LensDamage.NONE));

        bundle.addEquipment(new Battery("Spare Battery", "004", 8, "1800 mAh", Battery.BatteryDamage.NONE));

        assertTrue(bundle.isComplete());
        assertEquals(4, bundle.getEquipmentList().size());
    }

    @Test
    void removeEquipmentById_updatesContentsAndCompletion() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        bundle.addEquipment(new Camera("Camera", "001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        bundle.addEquipment(new Lens("Lens", "003", 8, "24 to 70 mm", Lens.LensDamage.NONE));
        assertTrue(bundle.isComplete());

        assertTrue(bundle.removeEquipmentById("003"));

        assertEquals(2, bundle.getEquipmentList().size());
        assertFalse(bundle.containsEquipmentId("003"));
        assertFalse(bundle.isComplete());
    }

    @Test
    void removeEquipmentById_unknownItem_leavesBundleUnchanged() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        bundle.addEquipment(new Camera("Camera", "001", 8, "Full frame", Camera.CameraDamage.NONE));

        assertFalse(bundle.removeEquipmentById("999"));

        assertEquals(1, bundle.getEquipmentList().size());
        assertTrue(bundle.containsEquipmentId("001"));
    }

    @Test
    void addEquipment_optionalTripod_keepsBundleComplete() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        bundle.addEquipment(new Camera("Camera", "001", 8, "Full frame", Camera.CameraDamage.NONE));
        bundle.addEquipment(new Battery("Battery", "002", 8, "2000 mAh", Battery.BatteryDamage.NONE));
        assertFalse(bundle.isComplete());
        bundle.addEquipment(new Lens("Lens", "003", 8, "24 to 70 mm", Lens.LensDamage.NONE));
        assertTrue(bundle.isComplete());

        bundle.addEquipment(new Tripod("Tripod", "004", 8, "Quarter inch", Tripod.TripodDamage.NONE));

        assertTrue(bundle.isComplete());
    }

    @Test
    void getEquipmentList_preservesObjectsAndRejectsExternalChanges() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        Camera camera = new Camera("Camera", "001", 8, "Full frame", Camera.CameraDamage.SHUTTER_FAULT);
        bundle.addEquipment(camera);

        assertSame(camera, bundle.getEquipmentList().getFirst());
        assertEquals("sensor/Full frame damage/SHUTTER_FAULT", bundle.getEquipmentList().getFirst().getDetails());
        assertThrows(UnsupportedOperationException.class, () -> bundle.getEquipmentList().clear());
        assertTrue(bundle.containsEquipmentId("001"));
        assertFalse(bundle.containsEquipmentId("002"));
    }
}
