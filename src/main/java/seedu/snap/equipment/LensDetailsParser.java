package seedu.snap.equipment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Converts parsed lens-specific fields into a Lens object. */
final class LensDetailsParser {
    private static final List<EquipmentDetailField> LENS_FIELDS = List.of(
            EquipmentDetailField.ZOOM,
            EquipmentDetailField.MOUNT,
            EquipmentDetailField.SENSOR,
            EquipmentDetailField.DAMAGE);

    private LensDetailsParser() {
    }

    /**
     * Creates a lens using its optional subtype-specific fields.
     *
     * @param name lens name
     * @param id lens item ID
     * @param condition lens condition
     * @param details lens-specific command fields
     * @param remarks optional remarks
     * @return lens containing the parsed compatibility information
     * @throws EquipmentAdditionUnsuccessful if a field is invalid
     */
    static Lens createLens(
            String name,
            String id,
            int condition,
            String details,
            String remarks)
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        details,
                        "lens",
                        LENS_FIELDS);

        String zoomRange = getValue(
                fields,
                EquipmentDetailField.ZOOM);
        String lensMount = getValue(
                fields,
                EquipmentDetailField.MOUNT);
        String sensorFormat = getValue(
                fields,
                EquipmentDetailField.SENSOR);
        Lens.LensDamage damage = parseDamage(
                fields.getOrDefault(
                        EquipmentDetailField.DAMAGE,
                        Lens.LensDamage.NONE.name()));

        return new Lens(
                name,
                id,
                condition,
                zoomRange,
                damage,
                remarks,
                lensMount,
                sensorFormat);
    }

    /** Returns a supplied field value or the common unspecified value. */
    private static String getValue(
            Map<EquipmentDetailField, String> fields,
            EquipmentDetailField field) {
        return fields.getOrDefault(
                field,
                CompatibilityText.UNSPECIFIED);
    }

    /** Converts a validated damage value into the lens damage enum. */
    private static Lens.LensDamage parseDamage(String value)
            throws EquipmentAdditionUnsuccessful {
        try {
            return Lens.LensDamage.valueOf(
                    value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid damage for lens. Use: "
                            + Arrays.toString(
                            Lens.LensDamage.values())
                            + ".");
        }
    }
}

// @@author

