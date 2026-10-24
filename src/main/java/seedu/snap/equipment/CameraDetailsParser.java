package seedu.snap.equipment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Parses and validates optional camera-specific command fields. */
final class CameraDetailsParser {
    static final String MOUNT_MARKER = "mount/";
    static final String BATTERY_MARKER = "battery/";

    private static final String SENSOR_FIELD = "sensor";
    private static final String MOUNT_FIELD = "mount";
    private static final String THREAD_FIELD = "thread";
    private static final String BATTERY_FIELD = "battery";
    private static final String DAMAGE_FIELD = "damage";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            SENSOR_FIELD,
            MOUNT_FIELD,
            THREAD_FIELD,
            BATTERY_FIELD,
            DAMAGE_FIELD);

    /*
     * Zoom and capacity are recognised so that fields belonging to another
     * equipment subclass can be rejected instead of becoming part of a value.
     */
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(?<!\\S)(sensor|mount|thread|battery|damage|zoom|capacity)/",
            Pattern.UNICODE_CHARACTER_CLASS);

    private CameraDetailsParser() {
    }

    /**
     * Creates a camera using its optional compatibility fields.
     *
     * @param name camera name
     * @param id camera Item ID
     * @param condition camera condition
     * @param details camera-specific command fields
     * @param remarks optional remarks
     * @return camera containing the parsed compatibility information
     * @throws EquipmentAdditionUnsuccessful if a field is invalid
     */
    static Camera createCamera(String name, String id, int condition,
                               String details, String remarks)
            throws EquipmentAdditionUnsuccessful {
        Map<String, String> fields = parseFields(details);

        String sensorFormat = fields.getOrDefault(
                SENSOR_FIELD, CompatibilityText.UNSPECIFIED);
        String lensMount = fields.getOrDefault(
                MOUNT_FIELD, CompatibilityText.UNSPECIFIED);
        String tripodThread = fields.getOrDefault(
                THREAD_FIELD, CompatibilityText.UNSPECIFIED);
        String batteryType = fields.getOrDefault(
                BATTERY_FIELD, CompatibilityText.UNSPECIFIED);
        Camera.CameraDamage damage = parseDamage(
                fields.getOrDefault(DAMAGE_FIELD, Camera.CameraDamage.NONE.name()));

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

    /** Reads each supplied camera field once without requiring optional fields. */
    private static Map<String, String> parseFields(String details)
            throws EquipmentAdditionUnsuccessful {
        String normalizedDetails = EquipmentFields.trimWhitespace(details);
        if (normalizedDetails.isEmpty()) {
            return Map.of();
        }

        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = FIELD_PATTERN.matcher(normalizedDetails);
        String currentField = null;
        int valueStart = 0;
        int previousFieldOrder = -1;

        while (matcher.find()) {
            String field = matcher.group(1);
            if (currentField == null && matcher.start() != 0) {
                throw invalidFormat();
            }
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new EquipmentAdditionUnsuccessful(
                        "Field " + field + "/ is not valid for a camera.");
            }
            int currentFieldOrder = getFieldOrder(field);
            if (currentFieldOrder < previousFieldOrder) {
                throw new EquipmentAdditionUnsuccessful(
                        "Camera fields must appear in this order: sensor/, mount/, "
                                + "thread/, battery/, damage/.");
            }
            previousFieldOrder = currentFieldOrder;
            if (currentField != null) {
                addField(
                        fields,
                        currentField,
                        normalizedDetails.substring(valueStart, matcher.start()));
            }
            currentField = field;
            valueStart = matcher.end();
        }

        if (currentField == null) {
            throw invalidFormat();
        }

        addField(
                fields,
                currentField,
                normalizedDetails.substring(valueStart));
        return fields;
    }

    /** Validates and stores one field without allowing it to be repeated. */
    private static void addField(
            Map<String, String> fields, String field, String value)
            throws EquipmentAdditionUnsuccessful {
        String normalizedValue;
        try {
            normalizedValue = CompatibilityText.normalize(value);
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid " + field + "/ value: " + exception.getMessage());
        }

        if (fields.putIfAbsent(field, normalizedValue) != null) {
            throw new EquipmentAdditionUnsuccessful(
                    "Field " + field + "/ must appear only once.");
        }
    }

    /** Returns the required position of a camera-specific field. */
    private static int getFieldOrder(String field) {
        if (field.equals(SENSOR_FIELD)) {
            return 0;
        }
        if (field.equals(MOUNT_FIELD)) {
            return 1;
        }
        if (field.equals(THREAD_FIELD)) {
            return 2;
        }
        if (field.equals(BATTERY_FIELD)) {
            return 3;
        }
        return 4;
    }

    /** Converts the supplied damage value to the camera damage enum. */
    private static Camera.CameraDamage parseDamage(String value)
            throws EquipmentAdditionUnsuccessful {
        try {
            return Camera.CameraDamage.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid damage for camera. Use: "
                            + Arrays.toString(Camera.CameraDamage.values()) + ".");
        }
    }

    /** Creates the common camera-field format error. */
    private static EquipmentAdditionUnsuccessful invalidFormat() {
        return new EquipmentAdditionUnsuccessful(
                "Camera details must use sensor/, mount/, thread/, "
                        + "battery/ or damage/ fields.");
    }
}

// @@author
