package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// @@author Rohita-29

/** Tests compatibility comparisons between equipment subclasses. */
class EquipmentCompatibilityTest {
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
    void compare_missingLensInformation_returnsUnknown() {
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
