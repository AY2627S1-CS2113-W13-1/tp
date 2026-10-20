package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a lens in the equipment inventory. */
public class Lens extends Equipment {
    private static final String TYPE = "lens";
    private static final Logger LOGGER = Logger.getLogger(Lens.class.getName());

    /**
     * Creates a lens.
     *
     * @param name Name of the lens.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the lens from 1 to 10.
     */
    public Lens(String name, String id, int condition) {
        super(name, id, condition);
        assert TYPE.equals(getType()) : "Lens type must remain lens.";
        LOGGER.log(Level.INFO, "Created lens with item ID {0}.", id);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
