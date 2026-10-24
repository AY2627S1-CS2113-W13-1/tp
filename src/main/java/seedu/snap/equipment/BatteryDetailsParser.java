package seedu.snap.equipment;

import java.util.List;
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

        String capacity =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.CAPACITY);
        String batteryType =
                EquipmentDetailParser.getTextValue(
                        fields,
                        EquipmentDetailField.BATTERY_TYPE);
        Battery.BatteryDamage damage =
                EquipmentDetailParser.getEnumValue(
                        fields,
                        EquipmentDetailField.DAMAGE,
                        Battery.BatteryDamage.NONE,
                        Battery.BatteryDamage.class,
                        "battery");

        return new Battery(
                name,
                id,
                condition,
                capacity,
                damage,
                remarks,
                batteryType);
    }
}

// @@author
