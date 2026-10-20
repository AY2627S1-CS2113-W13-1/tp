package seedu.snap.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Lens;

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

        bundle.addEquipment(new Camera("Camera", "001", 8));
        bundle.addEquipment(new Battery("Battery", "002", 8));
        bundle.addEquipment(new Lens("Lens", "003", 8));

        assertTrue(bundle.isComplete());
        assertEquals(3, bundle.getEquipmentList().size());
    }

    @Test
    void addEquipment_afterCompletion_keepsBundleComplete() {
        Bundle bundle = new Bundle("Travel Kit", "001");
        bundle.addEquipment(new Camera("Camera", "001", 8));
        bundle.addEquipment(new Battery("Battery", "002", 8));
        bundle.addEquipment(new Lens("Lens", "003", 8));

        bundle.addEquipment(new Battery("Spare Battery", "004", 8));

        assertTrue(bundle.isComplete());
        assertEquals(4, bundle.getEquipmentList().size());
    }
}
