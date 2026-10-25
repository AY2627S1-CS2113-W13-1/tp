package seedu.snap.equipment;

import java.util.List;
import java.util.Map;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Converts parsed camera-specific fields into a Camera object. */
final class CameraDetailsParser {
    private static final List<EquipmentDetailField> CAMERA_FIELDS =
            List.of(
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

        String sensorFormat =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.SENSOR);
        String lensMount =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.MOUNT);
        String tripodThread =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.THREAD);
        String batteryType =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.BATTERY_TYPE);
        Camera.CameraDamage damage =
                EquipmentDetailParser.getEnumValue(
                        fields,
                        EquipmentDetailField.DAMAGE,
                        Camera.CameraDamage.NONE,
                        Camera.CameraDamage.class,
                        "camera");
        EquipmentDetailParser.validateDamageRemarks(
                damage,
                remarks,
                "camera");

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
}

// @@author
