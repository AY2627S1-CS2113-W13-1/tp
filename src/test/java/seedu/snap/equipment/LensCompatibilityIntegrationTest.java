package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests Lens compatibility parsing, display and persistence. */
class LensCompatibilityIntegrationTest {
    private static final String FULL_LENS_COMMAND =
            "add i/Standard Zoom id/101 type/lens cond/8 "
                    + "zoom/24-70 mm mount/Canon RF "
                    + "sensor/Full frame damage/NONE remarks/Ready";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void parseEquipment_allLensCompatibilityFields_preservesValues()
            throws EquipmentAdditionUnsuccessful {
        Lens lens = assertInstanceOf(
                Lens.class,
                EquipmentHandler.parseEquipment(FULL_LENS_COMMAND));

        assertFullCompatibility(lens);
        assertEquals("Ready", lens.getRemarks());
    }

    @Test
    void parseEquipment_omittedCompatibilityFields_usesDefaults()
            throws EquipmentAdditionUnsuccessful {
        Lens lens = assertInstanceOf(
                Lens.class,
                EquipmentHandler.parseEquipment(
                        "add i/Prime Lens id/102 type/lens cond/9"));

        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getZoomRange());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getLensMount());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getSensorFormat());
        assertEquals(Lens.LensDamage.NONE, lens.getDamage());
    }

    @Test
    void parseEquipment_partialCompatibilityFields_preservesValues()
            throws EquipmentAdditionUnsuccessful {
        Lens lens = assertInstanceOf(
                Lens.class,
                EquipmentHandler.parseEquipment(
                        "add i/Portrait Lens id/103 type/lens cond/7 "
                                + "mount/Nikon Z sensor/Full frame"));

        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getZoomRange());
        assertEquals("Nikon Z", lens.getLensMount());
        assertEquals("Full frame", lens.getSensorFormat());
        assertEquals(
                "zoom/unspecified mount/Nikon Z "
                        + "sensor/Full frame damage/NONE",
                lens.getDetails());
    }

    @Test
    void parseEquipment_invalidLensFields_throwException() {
        String repeatedMount =
                "add i/Lens id/104 type/lens cond/5 "
                        + "mount/Canon RF mount/Nikon Z";
        String wrongOrder =
                "add i/Lens id/105 type/lens cond/5 "
                        + "sensor/APS-C mount/Canon RF";
        String batteryField =
                "add i/Lens id/106 type/lens cond/5 "
                        + "battery/LP-E17";

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(repeatedMount));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(wrongOrder));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(batteryField));
    }

    @Test
    void persistence_fullLensCompatibility_survivesReload()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler originalHandler =
                new EquipmentHandler(file);
        originalHandler.addEquipment(FULL_LENS_COMMAND);

        String savedRecord = Files.readString(file);
        assertTrue(savedRecord.contains("zoom/24-70 mm"));
        assertTrue(savedRecord.contains("mount/Canon RF"));
        assertTrue(savedRecord.contains("sensor/Full frame"));

        EquipmentHandler reloadedHandler =
                new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();

        Lens reloadedLens = assertInstanceOf(
                Lens.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertFullCompatibility(reloadedLens);
        assertEquals("Ready", reloadedLens.getRemarks());
    }

    @Test
    void persistence_legacyLensRecord_usesCompatibilityDefaults()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(
                file,
                "add i/Legacy Lens id/101 type/lens cond/5 "
                        + "zoom/24-70 mm damage/NONE"
                        + System.lineSeparator());

        EquipmentHandler handler = new EquipmentHandler(file);
        handler.loadFromStorage();

        Lens lens = assertInstanceOf(
                Lens.class,
                handler.getEquipmentList().getFirst());

        assertEquals("24-70 mm", lens.getZoomRange());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getLensMount());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getSensorFormat());
    }

    @Test
    void updateEquipment_lensCompatibility_preservesValuesAndStorage()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        handler.addEquipment(FULL_LENS_COMMAND);

        Lens updatedLens = assertInstanceOf(
                Lens.class,
                handler.updateEquipment("update id/101 cond/9"));

        assertEquals(9, updatedLens.getCondition());
        assertFullCompatibility(updatedLens);

        EquipmentHandler reloadedHandler =
                new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();

        Lens reloadedLens = assertInstanceOf(
                Lens.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertEquals(9, reloadedLens.getCondition());
        assertFullCompatibility(reloadedLens);
    }

    @Test
    void showEquipment_lensCompatibility_displaysValues()
            throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment(FULL_LENS_COMMAND);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(
                    new PrintStream(
                            output,
                            true,
                            StandardCharsets.UTF_8));
            handler.showEquipment("show id/101");
        } finally {
            System.setOut(originalOutput);
        }

        String displayed =
                output.toString(StandardCharsets.UTF_8);
        assertTrue(displayed.contains("Zoom range: 24-70 mm"));
        assertTrue(displayed.contains("Lens mount: Canon RF"));
        assertTrue(displayed.contains("Sensor format: Full frame"));
    }

    /** Checks every explicit compatibility field on a Lens. */
    private void assertFullCompatibility(Lens lens) {
        assertEquals("24-70 mm", lens.getZoomRange());
        assertEquals("Canon RF", lens.getLensMount());
        assertEquals("Full frame", lens.getSensorFormat());
        assertEquals(Lens.LensDamage.NONE, lens.getDamage());
    }
}

// @@author
