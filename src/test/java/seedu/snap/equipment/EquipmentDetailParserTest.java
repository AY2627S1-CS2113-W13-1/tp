package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests shared parsing of subtype-specific equipment fields. */
class EquipmentDetailParserTest {
    private static final List<EquipmentDetailField> CAMERA_FIELDS = List.of(
            EquipmentDetailField.SENSOR,
            EquipmentDetailField.MOUNT,
            EquipmentDetailField.THREAD,
            EquipmentDetailField.BATTERY_TYPE,
            EquipmentDetailField.DAMAGE);

    @Test
    void parse_allAllowedFields_returnsNormalizedValues()
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        "sensor/\u2003APS-C\u2003 mount/Canon RF "
                                + "thread/1/4\"-20 battery/LP-E17 "
                                + "damage/none",
                        "camera",
                        CAMERA_FIELDS);

        assertEquals("APS-C", fields.get(EquipmentDetailField.SENSOR));
        assertEquals("Canon RF", fields.get(EquipmentDetailField.MOUNT));
        assertEquals(
                "1/4\"-20",
                fields.get(EquipmentDetailField.THREAD));
        assertEquals(
                "LP-E17",
                fields.get(EquipmentDetailField.BATTERY_TYPE));
        assertEquals("none", fields.get(EquipmentDetailField.DAMAGE));
    }

    @Test
    void parse_emptyDetails_returnsEmptyMap()
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        "",
                        "camera",
                        CAMERA_FIELDS);

        assertEquals(Map.of(), fields);
    }

    @Test
    void parse_partialFields_returnsOnlySuppliedValues()
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        "mount/Canon RF battery/LP-E17",
                        "camera",
                        CAMERA_FIELDS);

        assertEquals(2, fields.size());
        assertEquals("Canon RF", fields.get(EquipmentDetailField.MOUNT));
        assertEquals(
                "LP-E17",
                fields.get(EquipmentDetailField.BATTERY_TYPE));
    }

    @Test
    void parse_fieldForAnotherSubclass_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        "zoom/24-70 mm",
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_wrongFieldOrder_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        "damage/NONE sensor/APS-C",
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_repeatedField_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        "mount/Canon RF mount/Nikon Z",
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_blankFieldValue_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        "mount/   ",
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_oversizedFieldValue_throwsException() {
        String details = "mount/" + "A".repeat(
                CompatibilityText.MAX_LENGTH + 1);

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        details,
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_controlCharacterInValue_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        "mount/Canon\u0000RF",
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_unrecognizedFieldText_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        "unknown/value",
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_nullDetails_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentDetailParser.parse(
                        null,
                        "camera",
                        CAMERA_FIELDS));
    }

    @Test
    void parse_returnedMap_rejectsExternalModification()
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        "sensor/APS-C",
                        "camera",
                        CAMERA_FIELDS);

        assertThrows(
                UnsupportedOperationException.class,
                () -> fields.put(
                        EquipmentDetailField.MOUNT,
                        "Canon RF"));
    }
}

// @@author
