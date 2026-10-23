package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests the common state and type-specific details stored by equipment subclasses. */
class EquipmentSubclassTest {
    @Test
    void camera_hasCameraTypeAndCommonFields() {
        Camera equipment = new Camera("Canon EOS R10", "101", 8, "APS-C", Camera.CameraDamage.SENSOR_DAMAGE);

        assertEquals("camera", equipment.getType());
        assertCommonFields(equipment, "Canon EOS R10", "101", 8);
        assertEquals("APS-C", equipment.getSensorFormat());
        assertEquals(Camera.CameraDamage.SENSOR_DAMAGE, equipment.getDamage());
        assertEquals("sensor/APS-C damage/SENSOR_DAMAGE", equipment.getDetails());
    }

    @Test
    void tripod_hasTripodTypeAndCommonFields() {
        Tripod equipment = new Tripod("Manfrotto Tripod", "102", 7, "1/4 inch", Tripod.TripodDamage.BENT_LEG);

        assertEquals("tripod", equipment.getType());
        assertCommonFields(equipment, "Manfrotto Tripod", "102", 7);
        assertEquals("1/4 inch", equipment.getThread());
        assertEquals(Tripod.TripodDamage.BENT_LEG, equipment.getDamage());
        assertEquals("thread/1/4 inch damage/BENT_LEG", equipment.getDetails());
    }

    @Test
    void lens_hasLensTypeAndCommonFields() {
        Lens equipment = new Lens("Sigma Zoom Lens", "103", 9, "18-50 mm", Lens.LensDamage.SCRATCHED_GLASS);

        assertEquals("lens", equipment.getType());
        assertCommonFields(equipment, "Sigma Zoom Lens", "103", 9);
        assertEquals("18-50 mm", equipment.getZoomRange());
        assertEquals(Lens.LensDamage.SCRATCHED_GLASS, equipment.getDamage());
        assertEquals("zoom/18-50 mm damage/SCRATCHED_GLASS", equipment.getDetails());
    }

    @Test
    void battery_hasBatteryTypeAndCommonFields() {
        Battery equipment = new Battery("Canon Battery", "104", 6, "2130 mAh", Battery.BatteryDamage.SWELLING);

        assertEquals("battery", equipment.getType());
        assertCommonFields(equipment, "Canon Battery", "104", 6);
        assertEquals("2130 mAh", equipment.getCapacity());
        assertEquals(Battery.BatteryDamage.SWELLING, equipment.getDamage());
        assertEquals("capacity/2130 mAh damage/SWELLING", equipment.getDetails());
    }

    @Test
    void constructors_withoutDetailsUseUnspecifiedAndNoDamage() {
        Camera camera = new Camera("Camera", "101", 8);
        Lens lens = new Lens("Lens", "102", 8);
        Battery battery = new Battery("Battery", "103", 8);
        Tripod tripod = new Tripod("Tripod", "104", 8);

        assertEquals("unspecified", camera.getSensorFormat());
        assertEquals("unspecified", lens.getZoomRange());
        assertEquals("unspecified", battery.getCapacity());
        assertEquals("unspecified", tripod.getThread());
        assertEquals(Camera.CameraDamage.NONE, camera.getDamage());
        assertEquals(Lens.LensDamage.NONE, lens.getDamage());
        assertEquals(Battery.BatteryDamage.NONE, battery.getDamage());
        assertEquals(Tripod.TripodDamage.NONE, tripod.getDamage());
        assertEquals("sensor/unspecified damage/NONE", camera.getDetails());
        assertEquals("zoom/unspecified damage/NONE", lens.getDetails());
        assertEquals("capacity/unspecified damage/NONE", battery.getDetails());
        assertEquals("thread/unspecified damage/NONE", tripod.getDetails());
    }

    @ParameterizedTest
    @EnumSource(Camera.CameraDamage.class)
    void camera_keepsEachDamageState(Camera.CameraDamage damage) {
        Camera camera = new Camera("Camera", "101", 8, "Full frame", damage);

        assertEquals(damage, camera.getDamage());
        assertEquals("sensor/Full frame damage/" + damage.name(), camera.getDetails());
    }

    @ParameterizedTest
    @EnumSource(Lens.LensDamage.class)
    void lens_keepsEachDamageState(Lens.LensDamage damage) {
        Lens lens = new Lens("Lens", "102", 8, "24-70 mm", damage);

        assertEquals(damage, lens.getDamage());
        assertEquals("zoom/24-70 mm damage/" + damage.name(), lens.getDetails());
    }

    @ParameterizedTest
    @EnumSource(Battery.BatteryDamage.class)
    void battery_keepsEachDamageState(Battery.BatteryDamage damage) {
        Battery battery = new Battery("Battery", "103", 8, "2130 mAh", damage);

        assertEquals(damage, battery.getDamage());
        assertEquals("capacity/2130 mAh damage/" + damage.name(), battery.getDetails());
    }

    @ParameterizedTest
    @EnumSource(Tripod.TripodDamage.class)
    void tripod_keepsEachDamageState(Tripod.TripodDamage damage) {
        Tripod tripod = new Tripod("Tripod", "104", 8, "3/8 inch", damage);

        assertEquals(damage, tripod.getDamage());
        assertEquals("thread/3/8 inch damage/" + damage.name(), tripod.getDetails());
    }

    @ParameterizedTest
    @MethodSource("invalidAttributes")
    void constructors_invalidAttributeThrowsAssertionError(String attribute) {
        assertThrows(AssertionError.class,
                () -> new Camera("Camera", "101", 8, attribute, Camera.CameraDamage.NONE));
        assertThrows(AssertionError.class,
                () -> new Lens("Lens", "102", 8, attribute, Lens.LensDamage.NONE));
        assertThrows(AssertionError.class,
                () -> new Battery("Battery", "103", 8, attribute, Battery.BatteryDamage.NONE));
        assertThrows(AssertionError.class,
                () -> new Tripod("Tripod", "104", 8, attribute, Tripod.TripodDamage.NONE));
    }

    @Test
    void constructors_nullDamageThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new Camera("Camera", "101", 8, "APS-C", null));
        assertThrows(AssertionError.class, () -> new Lens("Lens", "102", 8, "24-70 mm", null));
        assertThrows(AssertionError.class, () -> new Battery("Battery", "103", 8, "2130 mAh", null));
        assertThrows(AssertionError.class, () -> new Tripod("Tripod", "104", 8, "3/8 inch", null));
    }

    @Test
    void constructors_attributeAtLengthLimitIsPreserved() {
        String attribute = "a".repeat(100);

        assertEquals(attribute, new Camera("Camera", "101", 8, attribute,
                Camera.CameraDamage.NONE).getSensorFormat());
        assertEquals(attribute, new Lens("Lens", "102", 8, attribute, Lens.LensDamage.NONE).getZoomRange());
        assertEquals(attribute, new Battery("Battery", "103", 8, attribute, Battery.BatteryDamage.NONE).getCapacity());
        assertEquals(attribute, new Tripod("Tripod", "104", 8, attribute, Tripod.TripodDamage.NONE).getThread());
    }

    private static Stream<String> invalidAttributes() {
        return Stream.of(null, "", " ", "\t", "a".repeat(101), "full\nframe", "full\rframe",
                "full\u0085frame", "full\u2028frame", "full\u2029frame");
    }

    private void assertCommonFields(Equipment equipment, String expectedName, String expectedId,
            int expectedCondition) {
        assertEquals(expectedName, equipment.getItemName());
        assertEquals(expectedId, equipment.getItemId());
        assertEquals(expectedCondition, equipment.getCondition());
    }
}
