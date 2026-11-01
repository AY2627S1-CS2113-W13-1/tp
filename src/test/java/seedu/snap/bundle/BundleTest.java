package seedu.snap.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

    @Test
    void addEquipment_rejectsNullAndDuplicateIds() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        Camera camera = new Camera("Camera", "001", 8);

        assertThrows(IllegalArgumentException.class, () -> bundle.addEquipment(null));
        bundle.addEquipment(camera);
        assertThrows(AssertionError.class, () -> bundle.addEquipment(new Camera("Other", "001", 8)));
    }

    @Test
    void removeEquipmentById_nullId_returnsFalse() {
        Bundle bundle = new Bundle("Travel Kit", "001");

        assertFalse(bundle.removeEquipmentById(null));
    }

    @Test
    void restoreEquipment_replacesContentsAndRejectsNull() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        Camera camera = new Camera("Camera", "001", 8);
        Battery battery = new Battery("Battery", "002", 8);
        Lens lens = new Lens("Lens", "003", 8);
        bundle.addEquipment(camera);

        bundle.restoreEquipment(List.of(camera, battery, lens));

        assertEquals(List.of(camera, battery, lens), bundle.getEquipmentList());
        assertTrue(bundle.isComplete());
        assertThrows(IllegalArgumentException.class, () -> bundle.restoreEquipment(null));
    }

    @Test
    void completionState_checksEachCompositionLimit() {
        Bundle missingCamera = new Bundle("Missing camera", "001");
        missingCamera.addEquipment(new Battery("Battery", "001", 8));
        missingCamera.addEquipment(new Lens("Lens", "002", 8));
        assertFalse(missingCamera.isComplete());

        Bundle missingBattery = new Bundle("Missing battery", "002");
        missingBattery.addEquipment(new Camera("Camera", "003", 8));
        missingBattery.addEquipment(new Lens("Lens", "004", 8));
        assertFalse(missingBattery.isComplete());

        Bundle tooManyTripods = new Bundle("Many tripods", "003");
        tooManyTripods.addEquipment(new Camera("Camera", "005", 8));
        tooManyTripods.addEquipment(new Battery("Battery", "006", 8));
        tooManyTripods.addEquipment(new Lens("Lens", "007", 8));
        tooManyTripods.addEquipment(new Tripod("Tripod", "008", 8));
        tooManyTripods.addEquipment(new Tripod("Tripod 2", "009", 8));
        assertFalse(tooManyTripods.isComplete());

        Bundle missingLens = new Bundle("Missing lens", "004");
        missingLens.addEquipment(new Camera("Camera", "010", 8));
        missingLens.addEquipment(new Battery("Battery", "011", 8));
        assertFalse(missingLens.isComplete());
    }
}
