package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a battery in the equipment inventory. */
public class Battery extends Equipment {
    private static final String TYPE = "battery";
    private static final Logger LOGGER = Logger.getLogger(Battery.class.getName());

    /** Damage states that can be recorded for a battery. */
    public enum BatteryDamage {
        NONE,
        SWELLING,
        CONTACT_DAMAGE
    }

    private final String capacity;
    private final BatteryDamage damage;

    /**
     * Creates a battery with unspecified capacity and no recorded damage.
     *
     * @param name Name of the battery.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the battery from 1 to 10.
     */
    public Battery(String name, String id, int condition) {
        this(name, id, condition, "unspecified", BatteryDamage.NONE);
    }

    /**
     * Creates a battery with its type-specific details.
     * Callers must validate user input before construction.
     *
     * @param name Name of the battery.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the battery from 1 to 10.
     * @param capacity Capacity description, from 1 to 100 characters without line breaks.
     * @param damage Damage recorded for the battery.
     */
    public Battery(String name, String id, int condition, String capacity, BatteryDamage damage) {
        this(name, id, condition, capacity, damage, "");
    }

    /** Creates a battery with type-specific details and optional remarks. */
    public Battery(String name, String id, int condition, String capacity, BatteryDamage damage, String remarks) {
        super(name, id, condition, remarks);
        assert capacity != null && !capacity.isBlank() && capacity.length() <= 100
                && !capacity.matches("(?s).*\\R.*")
                : "Battery capacity must contain 1 to 100 characters without line breaks.";
        assert damage != null : "Battery damage must be specified.";
        this.capacity = capacity;
        this.damage = damage;
        assert TYPE.equals(getType()) : "Battery type must remain battery.";
        LOGGER.log(Level.INFO, "Created battery with item ID {0}.", id);
    }

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

    @Override
    public String getDetails() {
        return "capacity/" + capacity + " damage/" + damage.name();
    }
}
