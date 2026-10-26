package seedu.snap.equipment;

import java.util.List;
import java.util.Map;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Converts parsed lens-specific fields into a Lens object. */
final class LensDetailsParser {
    private static final List<EquipmentDetailField> LENS_FIELDS =
            List.of(
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

        String zoomRange =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.ZOOM);
        String lensMount =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.MOUNT);
        String sensorFormat =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.SENSOR);
        Lens.LensDamage damage =
                EquipmentDetailParser.getEnumValue(
                        fields,
                        EquipmentDetailField.DAMAGE,
                        Lens.LensDamage.NONE,
                        Lens.LensDamage.class,
                        "lens");

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
}

// @@author
