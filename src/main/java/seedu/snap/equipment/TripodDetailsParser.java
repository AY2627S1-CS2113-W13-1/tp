package seedu.snap.equipment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Converts parsed tripod-specific fields into a Tripod object. */
final class TripodDetailsParser {
    private static final List<EquipmentDetailField> TRIPOD_FIELDS =
            List.of(
                    EquipmentDetailField.THREAD,
                    EquipmentDetailField.DAMAGE);

    private TripodDetailsParser() {
    }

    /**
     * Creates a tripod using its optional subtype-specific fields.
     *
     * @param name tripod name
     * @param id tripod item ID
     * @param condition tripod condition
     * @param details tripod-specific command fields
     * @param remarks optional remarks
     * @return tripod containing the parsed compatibility information
     * @throws EquipmentAdditionUnsuccessful if a field is invalid
     */
    static Tripod createTripod(
            String name,
            String id,
            int condition,
            String details,
            String remarks)
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        details,
                        "tripod",
                        TRIPOD_FIELDS);

        String thread = fields.getOrDefault(
                EquipmentDetailField.THREAD,
                CompatibilityText.UNSPECIFIED);
        Tripod.TripodDamage damage = parseDamage(
                fields.getOrDefault(
                        EquipmentDetailField.DAMAGE,
                        Tripod.TripodDamage.NONE.name()));

        return new Tripod(
                name,
                id,
                condition,
                thread,
                damage,
                remarks);
    }

    /** Converts a validated damage value into the tripod damage enum. */
    private static Tripod.TripodDamage parseDamage(String value)
            throws EquipmentAdditionUnsuccessful {
        try {
            return Tripod.TripodDamage.valueOf(
                    value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid damage for tripod. Use: "
                            + Arrays.toString(
                            Tripod.TripodDamage.values())
                            + ".");
        }
    }
}

// @@author

