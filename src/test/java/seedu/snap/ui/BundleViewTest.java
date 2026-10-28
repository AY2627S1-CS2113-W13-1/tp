package seedu.snap.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.snap.bundle.Bundle;
import seedu.snap.equipment.EquipmentHandler;

/** Checks composition explanations without adding a second validation or compatibility rule. */
class BundleViewTest {
    @Test
    void composition_missingTypesFollowModelAndTripodRemainsOptional() throws Exception {
        Bundle bundle = new Bundle("Kit", "001");
        EquipmentHandler equipment = new EquipmentHandler();
        assertEquals(List.of("camera", "lens", "battery"), bundle.getMissingEquipmentTypes());
        assertEquals("Incomplete (missing camera, lens, battery)", BundleView.composition(bundle));
        assertThrows(UnsupportedOperationException.class, () -> bundle.getMissingEquipmentTypes().clear());
        bundle.addEquipment(equipment.addEquipment("add i/Camera id/001 type/camera cond/1"));
        assertEquals("Incomplete (missing lens, battery)", BundleView.composition(bundle));
        bundle.addEquipment(equipment.addEquipment("add i/Lens id/002 type/lens cond/2"));
        assertEquals("Incomplete (missing battery)", BundleView.composition(bundle));
        bundle.addEquipment(equipment.addEquipment("add i/Battery id/003 type/battery cond/3"));
        assertEquals("Complete", BundleView.composition(bundle));
        assertTrue(bundle.getMissingEquipmentTypes().isEmpty());
        assertTrue(bundle.isComplete());
    }

    @Test
    void print_emptyAndPopulatedBundles_haveCountsAndAccuratePersistenceGuidance() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            BundleView.print(List.of());
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("0 bundles."));
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("add-bundle name/Camera kit"));
            output.reset();
            Bundle bundle = new Bundle("Kit\t\u001b[31m", "001");
            BundleView.print(List.of(bundle));
            String text = output.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("Bundle 001: Kit\\u0009\\u001b[31m"));
            assertTrue(text.contains("0 equipment items:"));
            assertTrue(text.contains("1 bundle."));
            assertTrue(text.contains("saved bundles are restored at startup"));
            assertTrue(text.contains("Successful membership removals are saved immediately."));
            assertFalse(text.contains("true"));
            assertFalse(text.contains("false"));
            assertFalse(text.contains("session-only"));
            assertTrue(bundle.getEquipmentList().isEmpty());
        } finally {
            System.setOut(original);
        }
    }
}
