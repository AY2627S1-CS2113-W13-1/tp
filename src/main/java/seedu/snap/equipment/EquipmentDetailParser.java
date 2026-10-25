package seedu.snap.equipment;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/**
 * Parses ordered optional fields belonging to an equipment subclass.
 * The caller supplies the fields supported by that subclass in their required order.
 */
final class EquipmentDetailParser {
    private static final Pattern FIELD_PATTERN = createFieldPattern();

    private EquipmentDetailParser() {
    }

    /**
     * Parses and validates subtype-specific equipment fields.
     *
     * @param details text containing the optional fields
     * @param equipmentType equipment type used in error messages
     * @param allowedOrder supported fields in their required order
     * @return immutable map of fields explicitly supplied by the user
     * @throws EquipmentAdditionUnsuccessful if the fields are invalid
     */
    static Map<EquipmentDetailField, String> parse(
            String details,
            String equipmentType,
            List<EquipmentDetailField> allowedOrder)
            throws EquipmentAdditionUnsuccessful {
        validateConfiguration(equipmentType, allowedOrder);

        if (details == null) {
            throw createInvalidFormatException(
                    equipmentType,
                    allowedOrder);
        }

        String normalizedDetails =
                EquipmentFields.trimWhitespace(details);
        if (normalizedDetails.isEmpty()) {
            return Map.of();
        }

        return scanFields(
                normalizedDetails,
                equipmentType,
                allowedOrder);
    }

    /**
     * Returns a supplied detail value or the common unspecified value.
     *
     * @param fields parsed equipment fields
     * @param field field whose value is required
     * @return supplied value or unspecified
     */
    static String getTextValue(
            Map<EquipmentDetailField, String> fields,
            EquipmentDetailField field) {
        assert fields != null : "Parsed equipment fields must exist.";
        assert field != null : "Requested equipment field must exist.";

        return fields.getOrDefault(
                field,
                CompatibilityText.UNSPECIFIED);
    }

    /**
     * Converts a supplied field value into its corresponding enum.
     *
     * @param fields parsed equipment fields
     * @param field enum-backed field being read
     * @param defaultValue value used when the field was omitted
     * @param enumClass enum class used for conversion
     * @param equipmentType equipment type used in error messages
     * @param <T> enum type
     * @return parsed or default enum value
     * @throws EquipmentAdditionUnsuccessful if the value is invalid
     */
    static <T extends Enum<T>> T getEnumValue(
            Map<EquipmentDetailField, String> fields,
            EquipmentDetailField field,
            T defaultValue,
            Class<T> enumClass,
            String equipmentType)
            throws EquipmentAdditionUnsuccessful {
        assert fields != null : "Parsed equipment fields must exist.";
        assert field != null : "Requested equipment field must exist.";
        assert defaultValue != null : "Default enum value must exist.";
        assert enumClass != null : "Enum class must exist.";
        assert equipmentType != null && !equipmentType.isBlank()
                : "Equipment type must be supplied internally.";

        String value = fields.getOrDefault(
                field,
                defaultValue.name());

        return parseEnumValue(
                value,
                field,
                enumClass,
                equipmentType);
    }

    /**
     * Converts one supplied detail value into its corresponding enum.
     *
     * @param value supplied field value
     * @param field enum-backed field being parsed
     * @param enumClass enum class used for conversion
     * @param equipmentType equipment type used in error messages
     * @param <T> enum type
     * @return parsed enum value
     * @throws EquipmentAdditionUnsuccessful if the value is invalid
     */
    static <T extends Enum<T>> T parseEnumValue(
            String value,
            EquipmentDetailField field,
            Class<T> enumClass,
            String equipmentType)
            throws EquipmentAdditionUnsuccessful {
        try {
            return Enum.valueOf(
                    enumClass,
                    EquipmentFields.trimWhitespace(value)
                            .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid " + field.getFieldName()
                            + " for " + equipmentType + ". Use: "
                            + Arrays.toString(enumClass.getEnumConstants())
                            + ".");
        }
    }

    /**
     * Ensures that an OTHER damage classification includes an explanation.
     *
     * @param damage parsed equipment-specific damage value
     * @param remarks optional equipment remarks
     * @param equipmentType equipment type used in the error message
     * @throws EquipmentAdditionUnsuccessful if OTHER has no remarks
     */
    static void validateDamageRemarks(
            Enum<?> damage,
            String remarks,
            String equipmentType)
            throws EquipmentAdditionUnsuccessful {
        assert damage != null : "Damage must be supplied internally.";
        assert remarks != null : "Remarks must be supplied internally.";
        assert equipmentType != null && !equipmentType.isBlank()
                : "Equipment type must be supplied internally.";

        if ("OTHER".equals(damage.name()) && remarks.isBlank()) {
            throw new EquipmentAdditionUnsuccessful(
                    "Damage OTHER for " + equipmentType
                            + " requires non-empty remarks.");
        }
    }

    /** Checks the parser configuration supplied by application code. */
    private static void validateConfiguration(
            String equipmentType,
            List<EquipmentDetailField> allowedOrder) {
        assert equipmentType != null && !equipmentType.isBlank()
                : "Equipment type must be supplied internally.";
        assert allowedOrder != null && !allowedOrder.isEmpty()
                : "At least one supported detail field must be configured.";
        assert allowedOrder.stream().distinct().count()
                == allowedOrder.size()
                : "Supported detail fields must not contain duplicates.";
    }

    /** Scans supplied text and collects its validated fields. */
    private static Map<EquipmentDetailField, String> scanFields(
            String details,
            String equipmentType,
            List<EquipmentDetailField> allowedOrder)
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                new EnumMap<>(EquipmentDetailField.class);
        Matcher matcher = FIELD_PATTERN.matcher(details);
        EquipmentDetailField currentField = null;
        int valueStart = 0;
        int previousFieldOrder = -1;

        while (matcher.find()) {
            EquipmentDetailField field =
                    findField(matcher.group(1));

            if (currentField == null && matcher.start() != 0) {
                throw createInvalidFormatException(
                        equipmentType,
                        allowedOrder);
            }

            int currentFieldOrder = validateFieldPosition(
                    field,
                    equipmentType,
                    allowedOrder,
                    previousFieldOrder);

            if (currentField != null) {
                addField(
                        fields,
                        currentField,
                        details.substring(
                                valueStart,
                                matcher.start()));
            }

            currentField = field;
            previousFieldOrder = currentFieldOrder;
            valueStart = matcher.end();
        }

        if (currentField == null) {
            throw createInvalidFormatException(
                    equipmentType,
                    allowedOrder);
        }

        addField(
                fields,
                currentField,
                details.substring(valueStart));
        return Map.copyOf(fields);
    }

    /**
     * Checks that a field belongs to the selected subclass and appears in order.
     *
     * @return position of the field in the required order
     */
    private static int validateFieldPosition(
            EquipmentDetailField field,
            String equipmentType,
            List<EquipmentDetailField> allowedOrder,
            int previousFieldOrder)
            throws EquipmentAdditionUnsuccessful {
        int currentFieldOrder = allowedOrder.indexOf(field);
        if (currentFieldOrder == -1) {
            throw new EquipmentAdditionUnsuccessful(
                    "Field " + field.getMarker()
                            + " is not valid for "
                            + equipmentType + ".");
        }

        if (currentFieldOrder < previousFieldOrder) {
            throw new EquipmentAdditionUnsuccessful(
                    capitalize(equipmentType)
                            + " fields must appear in this order: "
                            + formatMarkers(allowedOrder) + ".");
        }

        return currentFieldOrder;
    }

    /** Validates and stores one field without allowing repetition. */
    private static void addField(
            Map<EquipmentDetailField, String> fields,
            EquipmentDetailField field,
            String value)
            throws EquipmentAdditionUnsuccessful {
        String normalizedValue;
        try {
            normalizedValue = CompatibilityText.normalize(value);
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid " + field.getMarker()
                            + " value: "
                            + exception.getMessage());
        }

        if (fields.putIfAbsent(field, normalizedValue) != null) {
            throw new EquipmentAdditionUnsuccessful(
                    "Field " + field.getMarker()
                            + " must appear only once.");
        }
    }

    /** Builds one field-detection pattern from the enum definitions. */
    private static Pattern createFieldPattern() {
        String alternatives =
                Arrays.stream(EquipmentDetailField.values())
                        .map(EquipmentDetailField::getFieldName)
                        .map(Pattern::quote)
                        .collect(Collectors.joining("|"));

        return Pattern.compile(
                "(?<!\\S)(" + alternatives + ")/",
                Pattern.UNICODE_CHARACTER_CLASS);
    }

    /** Resolves a field name already matched by the generated pattern. */
    private static EquipmentDetailField findField(
            String fieldName) {
        for (EquipmentDetailField field
                : EquipmentDetailField.values()) {
            if (field.getFieldName().equals(fieldName)) {
                return field;
            }
        }

        throw new AssertionError(
                "Matched equipment detail field must exist in the enum.");
    }

    /** Creates a format error listing the supported markers. */
    private static EquipmentAdditionUnsuccessful createInvalidFormatException(
            String equipmentType,
            List<EquipmentDetailField> allowedOrder) {
        return new EquipmentAdditionUnsuccessful(
                capitalize(equipmentType)
                        + " details must use these fields: "
                        + formatMarkers(allowedOrder) + ".");
    }

    /** Formats markers in their required command order. */
    private static String formatMarkers(
            List<EquipmentDetailField> allowedOrder) {
        return allowedOrder.stream()
                .map(EquipmentDetailField::getMarker)
                .collect(Collectors.joining(", "));
    }

    /** Capitalises the first character for user-facing error messages. */
    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0))
                + value.substring(1);
    }
}

// @@author
