package seedu.snap.equipment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/**
 * Converts parsed camera-specific fields into a Camera object.
 * General field parsing and validation are delegated to EquipmentDetailParser.
 */
final class CameraDetailsParser {
    private static final List<EquipmentDetailField> CAMERA_FIELDS = List.of(
            EquipmentDetailField.SENSOR,
            EquipmentDetailField.MOUNT,
            EquipmentDetailField.THREAD,
            EquipmentDetailField.BATTERY_TYPE,
            EquipmentDetailField.DAMAGE);

    private CameraDetailsParser() {
    }

    /**
     * Creates a camera using its optional subtype-specific fields.
     *
     * @param name camera name
     * @param id camera item ID
     * @param condition camera condition
     * @param details camera-specific command fields
     * @param remarks optional remarks
     * @return camera containing the parsed compatibility information
     * @throws EquipmentAdditionUnsuccessful if a field is invalid
     */
    static Camera createCamera(
            String name,
            String id,
            int condition,
            String details,
            String remarks)
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        details,
                        "camera",
                        CAMERA_FIELDS);

        String sensorFormat = getValue(
                fields,
                EquipmentDetailField.SENSOR);
        String lensMount = getValue(
                fields,
                EquipmentDetailField.MOUNT);
        String tripodThread = getValue(
                fields,
                EquipmentDetailField.THREAD);
        String batteryType = getValue(
                fields,
                EquipmentDetailField.BATTERY_TYPE);
        Camera.CameraDamage damage = parseDamage(
                fields.getOrDefault(
                        EquipmentDetailField.DAMAGE,
                        Camera.CameraDamage.NONE.name()));

        return new Camera(
                name,
                id,
                condition,
                sensorFormat,
                damage,
                remarks,
                lensMount,
                tripodThread,
                batteryType);
    }

    /** Returns a supplied field value or the common unspecified value. */
    private static String getValue(
            Map<EquipmentDetailField, String> fields,
            EquipmentDetailField field) {
        return fields.getOrDefault(
                field,
                CompatibilityText.UNSPECIFIED);
    }

    /** Converts a validated damage value into the camera damage enum. */
    private static Camera.CameraDamage parseDamage(String value)
            throws EquipmentAdditionUnsuccessful {
        try {
            return Camera.CameraDamage.valueOf(
                    value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid damage for camera. Use: "
                            + Arrays.toString(
                            Camera.CameraDamage.values())
                            + ".");
        }
    }
}

// @@author
