package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a battery in the equipment inventory. */
public class Battery extends Equipment {
    private static final String TYPE = "battery";
    private static final Logger LOGGER =
            Logger.getLogger(Battery.class.getName());

    /** Damage states that can be recorded for a battery. */
    public enum BatteryDamage {
        NONE,
        SWELLING,
        CONTACT_DAMAGE,

        // @@author Rohita-29

        WATER_DAMAGE,
        LEAKING,
        OTHER

        // @@author
    }

    private final String capacity;
    private BatteryDamage damage;

    // @@author Rohita-29

    private final String batteryType;

    // @@author

    /**
     * Creates a battery with unspecified details and no recorded damage.
     *
     * @param name name of the battery
     * @param id three-digit equipment ID
     * @param condition condition of the battery from 1 to 10
     */
    public Battery(String name, String id, int condition) {
        this(
                name,
                id,
                condition,
                CompatibilityText.UNSPECIFIED,
                BatteryDamage.NONE);
    }

    /**
     * Creates a battery with its existing type-specific details.
     *
     * @param name name of the battery
     * @param id three-digit equipment ID
     * @param condition condition of the battery from 1 to 10
     * @param capacity battery capacity description
     * @param damage damage recorded for the battery
     */
    public Battery(
            String name,
            String id,
            int condition,
            String capacity,
            BatteryDamage damage) {
        this(name, id, condition, capacity, damage, "");
    }

    /**
     * Creates a battery with type-specific details and optional remarks.
     *
     * @param name name of the battery
     * @param id three-digit equipment ID
     * @param condition condition of the battery from 1 to 10
     * @param capacity battery capacity description
     * @param damage damage recorded for the battery
     * @param remarks optional equipment remarks
     */
    public Battery(
            String name,
            String id,
            int condition,
            String capacity,
            BatteryDamage damage,
            String remarks) {
        this(
                name,
                id,
                condition,
                capacity,
                damage,
                remarks,
                CompatibilityText.UNSPECIFIED);
    }

    // @@author Rohita-29

    /**
     * Creates a battery with all compatibility attributes.
     *
     * @param name name of the battery
     * @param id three-digit equipment ID
     * @param condition condition of the battery from 1 to 10
     * @param capacity battery capacity description
     * @param damage damage recorded for the battery
     * @param remarks optional equipment remarks
     * @param batteryType battery chemistry or format
     */
    public Battery(
            String name,
            String id,
            int condition,
            String capacity,
            BatteryDamage damage,
            String remarks,
            String batteryType) {
        super(name, id, condition, remarks);

        assert capacity != null
                && !capacity.isBlank()
                && capacity.length() <= CompatibilityText.MAX_LENGTH
                && !capacity.matches("(?s).*\\R.*")
                : "Battery capacity must contain 1 to 100 "
                + "characters without line breaks.";
        assert damage != null : "Battery damage must be specified.";

        this.capacity = capacity;
        this.damage = damage;
        this.batteryType =
                CompatibilityText.normalize(batteryType);

        assert TYPE.equals(getType())
                : "Battery type must remain battery.";
        LOGGER.log(
                Level.INFO,
                "Created battery with item ID {0}.",
                id);
    }

    // @@author

    @Override
    public String getType() {
        return TYPE;
    }

    public String getCapacity() {
        return capacity;
    }

    public BatteryDamage getDamage() {
        return damage;
    }

    // @@author Rohita-29

    /** Returns the compatibility type of this battery. */
    public String getBatteryType() {
        return batteryType;
    }

    @Override
    protected void applyDamageAssessment(Equipment assessed) {
        Battery battery = (Battery) assessed;
        if (!capacity.equals(battery.capacity) || battery.damage == null) {
            throw new IllegalArgumentException("A return assessment cannot change the battery capacity.");
        }
        damage = battery.damage;
    }

    @Override
    public String getDetails() {
        StringBuilder details = new StringBuilder("capacity/")
                .append(capacity);

        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.BATTERY_TYPE,
                batteryType);

        return details.append(' ')
                .append(EquipmentDetailField.DAMAGE.getMarker())
                .append(damage.name())
                .toString();
    }

    // @@author
}
