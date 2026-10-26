package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests retrieval and conversion of parsed equipment detail values. */
class EquipmentDetailValueTest {
    @Test
    void getTextValue_suppliedField_returnsStoredValue() {
        Map<EquipmentDetailField, String> fields = Map.of(
                EquipmentDetailField.BATTERY_TYPE,
                "Li-ion");

        assertEquals(
                "Li-ion",
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.BATTERY_TYPE));
    }

    @Test
    void getTextValue_missingField_returnsUnspecified() {
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                EquipmentDetailParser.getTextValue(
                        Map.of(),
                        EquipmentDetailField.MOUNT));
    }

    @Test
    void getEnumValue_suppliedValue_returnsMatchingEnum()
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields = Map.of(
                EquipmentDetailField.DAMAGE,
                "sensor_damage");

        assertEquals(
                Camera.CameraDamage.SENSOR_DAMAGE,
                EquipmentDetailParser.getEnumValue(
                        fields,
                        EquipmentDetailField.DAMAGE,
                        Camera.CameraDamage.NONE,
                        Camera.CameraDamage.class,
                        "camera"));
    }

    @Test
    void getEnumValue_missingValue_returnsDefault()
            throws EquipmentAdditionUnsuccessful {
        assertEquals(
                Camera.CameraDamage.NONE,
                EquipmentDetailParser.getEnumValue(
                        Map.of(),
                        EquipmentDetailField.DAMAGE,
                        Camera.CameraDamage.NONE,
                        Camera.CameraDamage.class,
                        "camera"));
    }

    @Test
    void getEnumValue_invalidValue_throwsException() {
        Map<EquipmentDetailField, String> fields = Map.of(
                EquipmentDetailField.DAMAGE,
                "cracked");

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.getEnumValue(
                        fields,
                        EquipmentDetailField.DAMAGE,
                        Camera.CameraDamage.NONE,
                        Camera.CameraDamage.class,
                        "camera"));
    }
}

// @@author
