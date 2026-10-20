package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a tripod in the equipment inventory. */
public class Tripod extends Equipment {
    private static final String TYPE = "tripod";
    private static final Logger LOGGER = Logger.getLogger(Tripod.class.getName());

    /**
     * Creates a tripod.
     *
     * @param name Name of the tripod.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the tripod from 1 to 10.
     */
    public Tripod(String name, String id, int condition) {
        super(name, id, condition);
        assert TYPE.equals(getType()) : "Tripod type must remain tripod.";
        LOGGER.log(Level.INFO, "Created tripod with item ID {0}.", id);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
