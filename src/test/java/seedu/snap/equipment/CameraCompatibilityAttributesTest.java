package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// @@author Rohita-29

/** Tests compatibility attributes belonging to cameras. */
public class CameraCompatibilityAttributesTest {

    @Test
    void existingConstructor_missingCompatibilityFields_usesUnspecifiedDefaults() {
        Camera camera = new Camera(
                "Canon R50", "001", 8,
                "APS-C", Camera.CameraDamage.NONE, "");

        assertEquals(CompatibilityText.UNSPECIFIED, camera.getLensMount());
        assertEquals(CompatibilityText.UNSPECIFIED, camera.getTripodThread());
        assertEquals(CompatibilityText.UNSPECIFIED, camera.getBatteryType());
    }

    @Test
    void fullConstructor_validCompatibilityFields_preservesValues() {
        Camera camera = createCamera(
                "Canon RF", "1/4\"-20", "LP-E17");

        assertEquals("Canon RF", camera.getLensMount());
        assertEquals("1/4\"-20", camera.getTripodThread());
        assertEquals("LP-E17", camera.getBatteryType());
        assertEquals("Checked before issue", camera.getRemarks());
    }

    @Test
    void fullConstructor_surroundingWhitespace_trimsValues() {
        Camera camera = createCamera(
                "\u2003Canon RF\u2003",
                "\t1/4\"-20\t",
                "  LP-E17  ");

        assertEquals("Canon RF", camera.getLensMount());
        assertEquals("1/4\"-20", camera.getTripodThread());
        assertEquals("LP-E17", camera.getBatteryType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n", "\t", "\u2003"})
    void fullConstructor_invalidLensMount_throwsException(String lensMount) {
        assertThrows(
                IllegalArgumentException.class,
                () -> createCamera(lensMount, "1/4\"-20", "LP-E17"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n", "\t", "\u2003"})
    void fullConstructor_invalidTripodThread_throwsException(String tripodThread) {
        assertThrows(
                IllegalArgumentException.class,
                () -> createCamera("Canon RF", tripodThread, "LP-E17"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n", "\t", "\u2003"})
    void fullConstructor_invalidBatteryType_throwsException(String batteryType) {
        assertThrows(
                IllegalArgumentException.class,
                () -> createCamera("Canon RF", "1/4\"-20", batteryType));
    }

    @Test
    void fullConstructor_nullCompatibilityFields_throwException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createCamera(null, "1/4\"-20", "LP-E17"));
        assertThrows(
                IllegalArgumentException.class,
                () -> createCamera("Canon RF", null, "LP-E17"));
        assertThrows(
                IllegalArgumentException.class,
                () -> createCamera("Canon RF", "1/4\"-20", null));
    }

    /** Creates a camera with the supplied compatibility values. */
    private Camera createCamera(
            String lensMount, String tripodThread, String batteryType) {
        return new Camera(
                "Canon R50",
                "001",
                8,
                "APS-C",
                Camera.CameraDamage.NONE,
                "Checked before issue",
                lensMount,
                tripodThread,
                batteryType);
    }
}

// @@author
