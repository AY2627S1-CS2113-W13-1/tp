package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests the common equipment state and type supplied by each concrete subclass. */
class EquipmentSubclassTest {
    @Test
    void camera_hasCameraTypeAndCommonFields() {
        Equipment equipment = new Camera("Canon EOS R10", "101", 8);

        assertEquals("camera", equipment.getType());
        assertCommonFields(equipment, "Canon EOS R10", "101", 8);
    }

    @Test
    void tripod_hasTripodTypeAndCommonFields() {
        Equipment equipment = new Tripod("Manfrotto Tripod", "102", 7);

        assertEquals("tripod", equipment.getType());
        assertCommonFields(equipment, "Manfrotto Tripod", "102", 7);
    }

    @Test
    void lens_hasLensTypeAndCommonFields() {
        Equipment equipment = new Lens("Sigma 30mm Lens", "103", 9);

        assertEquals("lens", equipment.getType());
        assertCommonFields(equipment, "Sigma 30mm Lens", "103", 9);
    }

    @Test
    void battery_hasBatteryTypeAndCommonFields() {
        Equipment equipment = new Battery("Canon Battery", "104", 6);

        assertEquals("battery", equipment.getType());
        assertCommonFields(equipment, "Canon Battery", "104", 6);
    }

    private void assertCommonFields(Equipment equipment, String expectedName, String expectedId,
            int expectedCondition) {
        assertEquals(expectedName, equipment.getItemName());
        assertEquals(expectedId, equipment.getItemId());
        assertEquals(expectedCondition, equipment.getCondition());
    }
}
