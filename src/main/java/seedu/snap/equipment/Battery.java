package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a battery in the equipment inventory. */
public class Battery extends Equipment {
    private static final String TYPE = "battery";
    private static final Logger LOGGER = Logger.getLogger(Battery.class.getName());

    /**
     * Creates a battery.
     *
     * @param name Name of the battery.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the battery from 1 to 10.
     */
    public Battery(String name, String id, int condition) {
        super(name, id, condition);
        assert TYPE.equals(getType()) : "Battery type must remain battery.";
        LOGGER.log(Level.INFO, "Created battery with item ID {0}.", id);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
