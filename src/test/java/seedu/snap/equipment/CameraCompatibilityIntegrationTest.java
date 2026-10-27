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

/** Tests Camera compatibility parsing, display and persistence. */
class CameraCompatibilityIntegrationTest {
    private static final String FULL_CAMERA_COMMAND =
            "add i/Canon R50 id/001 type/camera cond/8 "
                    + "sensor/APS-C mount/Canon RF thread/1/4\"-20 "
                    + "battery/LP-E17 damage/NONE remarks/Ready";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void parseEquipment_allCameraCompatibilityFields_preservesValues()
            throws EquipmentAdditionUnsuccessful {
        Camera camera = assertInstanceOf(
                Camera.class,
                EquipmentHandler.parseEquipment(FULL_CAMERA_COMMAND));

        assertFullCompatibility(camera);
        assertEquals("Ready", camera.getRemarks());
    }

    @Test
    void parseEquipment_omittedCompatibilityFields_usesDefaults()
            throws EquipmentAdditionUnsuccessful {
        Camera camera = assertInstanceOf(
                Camera.class,
                EquipmentHandler.parseEquipment(
                        "add i/Camera id/001 type/camera cond/5 "
                                + "sensor/APS-C damage/NONE"));

        assertEquals("APS-C", camera.getSensorFormat());
        assertEquals(CompatibilityText.UNSPECIFIED, camera.getLensMount());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                camera.getTripodThread());
        assertEquals(CompatibilityText.UNSPECIFIED, camera.getBatteryType());
        assertEquals("sensor/APS-C damage/NONE", camera.getDetails());
    }

    @Test
    void parseEquipment_partialCompatibilityFields_preservesSuppliedValues()
            throws EquipmentAdditionUnsuccessful {
        Camera camera = assertInstanceOf(
                Camera.class,
                EquipmentHandler.parseEquipment(
                        "add i/Camera id/001 type/camera cond/5 "
                                + "mount/Canon RF battery/LP-E17"));

        assertEquals("Canon RF", camera.getLensMount());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                camera.getTripodThread());
        assertEquals("LP-E17", camera.getBatteryType());
        assertEquals(
                "sensor/unspecified mount/Canon RF "
                        + "battery/LP-E17 damage/NONE",
                camera.getDetails());
    }

    @Test
    void parseEquipment_repeatedCompatibilityField_throwsException() {
        String command = "add i/Camera id/001 type/camera cond/5 "
                + "mount/Canon RF mount/Nikon Z";

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(command));
    }

    @Test
    void parseEquipment_wrongCompatibilityFieldOrder_throwsException() {
        String command = "add i/Camera id/001 type/camera cond/5 "
                + "damage/NONE sensor/APS-C";

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(command));
    }

    @Test
    void parseEquipment_fieldsFromOtherSubclasses_throwException() {
        String zoomCommand = "add i/Camera id/001 type/camera cond/5 "
                + "zoom/24-70 mm";
        String capacityCommand = "add i/Camera id/001 type/camera cond/5 "
                + "capacity/2000 mAh";

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(zoomCommand));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(capacityCommand));
    }

    @Test
    void parseEquipment_invalidCompatibilityValues_throwException() {
        String emptyMount = "add i/Camera id/001 type/camera cond/5 mount/";
        String emptyThread = "add i/Camera id/001 type/camera cond/5 thread/";
        String repeatedBattery = "add i/Camera id/001 type/camera cond/5 "
                + "battery/AA battery/AAA";
        String invalidDamage = "add i/Camera id/001 type/camera cond/5 "
                + "damage/SWELLING";

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(emptyMount));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(emptyThread));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(repeatedBattery));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(invalidDamage));
    }

    @Test
    void persistence_fullCameraCompatibility_survivesReload()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler originalHandler = new EquipmentHandler(file);
        originalHandler.addEquipment(FULL_CAMERA_COMMAND);

        String savedRecord = Files.readString(file);
        assertTrue(savedRecord.contains("mount/Canon RF"));
        assertTrue(savedRecord.contains("thread/1/4\"-20"));
        assertTrue(savedRecord.contains("battery/LP-E17"));

        EquipmentHandler reloadedHandler = new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();
        Camera reloadedCamera = assertInstanceOf(
                Camera.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertFullCompatibility(reloadedCamera);
        assertEquals("Ready", reloadedCamera.getRemarks());
    }

    @Test
    void persistence_legacyCameraRecord_usesCompatibilityDefaults()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(
                file,
                "add i/Legacy Camera id/001 type/camera cond/5 "
                        + "sensor/APS-C damage/NONE"
                        + System.lineSeparator());

        EquipmentHandler handler = new EquipmentHandler(file);
        handler.loadFromStorage();
        Camera camera = assertInstanceOf(
                Camera.class,
                handler.getEquipmentList().getFirst());

        assertEquals(CompatibilityText.UNSPECIFIED, camera.getLensMount());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                camera.getTripodThread());
        assertEquals(CompatibilityText.UNSPECIFIED, camera.getBatteryType());
    }

    @Test
    void updateEquipment_cameraCompatibility_preservesValuesAndStorage()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        handler.addEquipment(FULL_CAMERA_COMMAND);

        Camera updatedCamera = assertInstanceOf(
                Camera.class,
                handler.updateEquipment("update id/001 cond/9"));

        assertEquals(9, updatedCamera.getCondition());
        assertFullCompatibility(updatedCamera);

        EquipmentHandler reloadedHandler = new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();
        Camera reloadedCamera = assertInstanceOf(
                Camera.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertEquals(9, reloadedCamera.getCondition());
        assertFullCompatibility(reloadedCamera);
    }

    @Test
    void showEquipment_cameraCompatibility_displaysValues()
            throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment(FULL_CAMERA_COMMAND);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(
                    new PrintStream(
                            output,
                            true,
                            StandardCharsets.UTF_8));
            handler.showEquipment("show id/001");
        } finally {
            System.setOut(originalOutput);
        }

        String displayed = output.toString(StandardCharsets.UTF_8);
        assertTrue(displayed.contains("Lens mount: Canon RF"));
        assertTrue(displayed.contains("Tripod thread: 1/4\"-20"));
        assertTrue(displayed.contains("Battery type: LP-E17"));
    }

    /** Checks all explicit compatibility fields on a Camera. */
    private void assertFullCompatibility(Camera camera) {
        assertEquals("APS-C", camera.getSensorFormat());
        assertEquals("Canon RF", camera.getLensMount());
        assertEquals("1/4\"-20", camera.getTripodThread());
        assertEquals("LP-E17", camera.getBatteryType());
        assertEquals(Camera.CameraDamage.NONE, camera.getDamage());
    }
}

// @@author
