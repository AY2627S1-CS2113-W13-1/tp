package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a tripod in the equipment inventory. */
public class Tripod extends Equipment {
    private static final String TYPE = "tripod";
    private static final Logger LOGGER = Logger.getLogger(Tripod.class.getName());

    /** Damage states that can be recorded for a tripod. */
    public enum TripodDamage {
        NONE,
        BENT_LEG,
        HEAD_FAULT
    }

    private final String thread;
    private final TripodDamage damage;

    /**
     * Creates a tripod with unspecified thread and no recorded damage.
     *
     * @param name Name of the tripod.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the tripod from 1 to 10.
     */
    public Tripod(String name, String id, int condition) {
        this(name, id, condition, "unspecified", TripodDamage.NONE);
    }

    /**
     * Creates a tripod with its type-specific details.
     * Callers must validate user input before construction.
     *
     * @param name Name of the tripod.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the tripod from 1 to 10.
     * @param thread Thread description, from 1 to 100 characters without line breaks.
     * @param damage Damage recorded for the tripod.
     */
    public Tripod(String name, String id, int condition, String thread, TripodDamage damage) {
        this(name, id, condition, thread, damage, "");
    }

    /** Creates a tripod with type-specific details and optional remarks. */
    public Tripod(String name, String id, int condition, String thread, TripodDamage damage, String remarks) {
        super(name, id, condition, remarks);
        assert thread != null && !thread.isBlank() && thread.length() <= 100
                && !thread.matches("(?s).*\\R.*")
                : "Tripod thread must contain 1 to 100 characters without line breaks.";
        assert damage != null : "Tripod damage must be specified.";
        this.thread = thread;
        this.damage = damage;
        assert TYPE.equals(getType()) : "Tripod type must remain tripod.";
        LOGGER.log(Level.INFO, "Created tripod with item ID {0}.", id);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getThread() {
        return thread;
    }

    public TripodDamage getDamage() {
        return damage;
    }

    @Override
    public String getDetails() {
        return "thread/" + thread + " damage/" + damage.name();
    }
}
