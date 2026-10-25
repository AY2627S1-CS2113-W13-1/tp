package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// @@author Rohita-29

/** Tests compatibility comparisons between equipment subclasses. */
class EquipmentCompatibilityTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void compare_matchingCameraAndLens_returnsCompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Lens lens = createLens("canon rf", "full frame");

        assertEquals(
                CompatibilityResult.COMPATIBLE,
                EquipmentCompatibility.compare(camera, lens));
    }

    @Test
    void compare_cameraAndLensInReverseOrder_returnsSameResult() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Lens lens = createLens("Canon RF", "Full frame");

        assertEquals(
                EquipmentCompatibility.compare(camera, lens),
                EquipmentCompatibility.compare(lens, camera));
    }

    @Test
    void compare_differentLensMount_returnsIncompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Lens lens = createLens("Nikon Z", "Full frame");

        assertEquals(
                CompatibilityResult.INCOMPATIBLE,
                EquipmentCompatibility.compare(camera, lens));
    }

    @Test
    void compare_differentSensorFormat_returnsIncompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "APS-C",
                "1/4\"-20",
                "Li-ion");
        Lens lens = createLens("Canon RF", "Full frame");

        assertEquals(
                CompatibilityResult.INCOMPATIBLE,
                EquipmentCompatibility.compare(camera, lens));
    }

    @Test
    void compare_compatibleAndUnknownAttributes_returnsUnknown() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Lens lens = createLens(
                CompatibilityText.UNSPECIFIED,
                "Full frame");

        assertEquals(
                CompatibilityResult.UNKNOWN,
                EquipmentCompatibility.compare(camera, lens));
    }

    @Test
    void compare_matchingCameraAndBattery_returnsCompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Battery battery = createBattery("li-ION");

        assertEquals(
                CompatibilityResult.COMPATIBLE,
                EquipmentCompatibility.compare(camera, battery));
    }

    @Test
    void compare_differentCameraAndBattery_returnsIncompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Battery battery = createBattery("AA");

        assertEquals(
                CompatibilityResult.INCOMPATIBLE,
                EquipmentCompatibility.compare(camera, battery));
    }

    @Test
    void compare_matchingCameraAndTripod_returnsCompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Tripod tripod = createTripod("1/4\"-20");

        assertEquals(
                CompatibilityResult.COMPATIBLE,
                EquipmentCompatibility.compare(camera, tripod));
    }

    @Test
    void compare_differentCameraAndTripod_returnsIncompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Tripod tripod = createTripod("3/8\"-16");

        assertEquals(
                CompatibilityResult.INCOMPATIBLE,
                EquipmentCompatibility.compare(camera, tripod));
    }

    @Test
    void compare_unrelatedEquipmentTypes_returnsNotApplicable() {
        Lens lens = createLens("Canon RF", "Full frame");
        Battery battery = createBattery("Li-ion");

        assertEquals(
                CompatibilityResult.NOT_APPLICABLE,
                EquipmentCompatibility.compare(lens, battery));
    }

    @Test
    void compare_nullEquipment_throwsException() {
        Battery battery = createBattery("Li-ion");

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentCompatibility.compare(null, battery));
        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentCompatibility.compare(battery, null));
    }

    @Test
    void compare_reloadedEquipment_preservesCompatibilityResults()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler originalHandler = new EquipmentHandler(file);

        originalHandler.addEquipment(
                "add i/Camera id/001 type/camera cond/5 "
                        + "sensor/Full frame mount/Canon RF");
        originalHandler.addEquipment(
                "add i/Compatible Lens id/002 type/lens cond/5 "
                        + "mount/Canon RF sensor/Full frame");
        originalHandler.addEquipment(
                "add i/Incompatible Lens id/003 type/lens cond/5 "
                        + "mount/Nikon Z sensor/Full frame");

        EquipmentHandler reloadedHandler = new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();

        Camera camera = assertInstanceOf(
                Camera.class,
                reloadedHandler.getEquipmentById("001"));
        Lens compatibleLens = assertInstanceOf(
                Lens.class,
                reloadedHandler.getEquipmentById("002"));
        Lens incompatibleLens = assertInstanceOf(
                Lens.class,
                reloadedHandler.getEquipmentById("003"));

        assertEquals(
                CompatibilityResult.COMPATIBLE,
                EquipmentCompatibility.compare(camera, compatibleLens));
        assertEquals(
                CompatibilityResult.INCOMPATIBLE,
                EquipmentCompatibility.compare(camera, incompatibleLens));
    }

    @Test
    void compare_incompatibleAndUnknownAttributes_returnsIncompatible() {
        Camera camera = createCamera(
                "Canon RF",
                "Full frame",
                "1/4\"-20",
                "Li-ion");
        Lens lens = createLens(
                "Nikon Z",
                CompatibilityText.UNSPECIFIED);

        assertEquals(
                CompatibilityResult.INCOMPATIBLE,
                EquipmentCompatibility.compare(camera, lens));
    }

    /** Creates a camera with explicit comparison attributes. */
    private Camera createCamera(
            String lensMount,
            String sensorFormat,
            String tripodThread,
            String batteryType) {
        return new Camera(
                "Camera",
                "001",
                8,
                sensorFormat,
                Camera.CameraDamage.NONE,
                "",
                lensMount,
                tripodThread,
                batteryType);
    }

    /** Creates a lens with explicit comparison attributes. */
    private Lens createLens(
            String lensMount,
            String sensorFormat) {
        return new Lens(
                "Lens",
                "101",
                8,
                "24-70 mm",
                Lens.LensDamage.NONE,
                "",
                lensMount,
                sensorFormat);
    }

    /** Creates a battery with an explicit comparison type. */
    private Battery createBattery(String batteryType) {
        return new Battery(
                "Battery",
                "201",
                8,
                "2000 mAh",
                Battery.BatteryDamage.NONE,
                "",
                batteryType);
    }

    /** Creates a tripod with an explicit thread. */
    private Tripod createTripod(String thread) {
        return new Tripod(
                "Tripod",
                "301",
                8,
                thread,
                Tripod.TripodDamage.NONE,
                "");
    }
}

// @@author
