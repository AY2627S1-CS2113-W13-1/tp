package seedu.snap.equipment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Converts parsed battery-specific fields into a Battery object. */
final class BatteryDetailsParser {
    private static final List<EquipmentDetailField> BATTERY_FIELDS =
            List.of(
                    EquipmentDetailField.CAPACITY,
                    EquipmentDetailField.BATTERY_TYPE,
                    EquipmentDetailField.DAMAGE);

    private BatteryDetailsParser() {
    }

    /**
     * Creates a battery using its optional subtype-specific fields.
     *
     * @param name battery name
     * @param id battery item ID
     * @param condition battery condition
     * @param details battery-specific command fields
     * @param remarks optional remarks
     * @return battery containing the parsed compatibility information
     * @throws EquipmentAdditionUnsuccessful if a field is invalid
     */
    static Battery createBattery(
            String name,
            String id,
            int condition,
            String details,
            String remarks)
            throws EquipmentAdditionUnsuccessful {
        Map<EquipmentDetailField, String> fields =
                EquipmentDetailParser.parse(
                        details,
                        "battery",
                        BATTERY_FIELDS);

        String capacity = getValue(
                fields,
                EquipmentDetailField.CAPACITY);
        String batteryType = getValue(
                fields,
                EquipmentDetailField.BATTERY_TYPE);
        Battery.BatteryDamage damage = parseDamage(
                fields.getOrDefault(
                        EquipmentDetailField.DAMAGE,
                        Battery.BatteryDamage.NONE.name()));

        return new Battery(
                name,
                id,
                condition,
                capacity,
                damage,
                remarks,
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

    /** Converts a validated damage value into the battery damage enum. */
    private static Battery.BatteryDamage parseDamage(String value)
            throws EquipmentAdditionUnsuccessful {
        try {
            return Battery.BatteryDamage.valueOf(
                    value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful(
                    "Invalid damage for battery. Use: "
                            + Arrays.toString(
                            Battery.BatteryDamage.values())
                            + ".");
        }
    }
}

// @@author

