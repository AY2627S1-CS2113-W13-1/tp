package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a lens in the equipment inventory. */
public class Lens extends Equipment {
    private static final String TYPE = "lens";
    private static final Logger LOGGER = Logger.getLogger(Lens.class.getName());

    /** Damage states that can be recorded for a lens. */
    public enum LensDamage {
        NONE,
        SCRATCHED_GLASS,
        AUTOFOCUS_FAULT
    }

    private final String zoomRange;
    private final LensDamage damage;

    /**
     * Creates a lens with unspecified zoom range and no recorded damage.
     *
     * @param name Name of the lens.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the lens from 1 to 10.
     */
    public Lens(String name, String id, int condition) {
        this(name, id, condition, "unspecified", LensDamage.NONE);
    }

    /**
     * Creates a lens with its type-specific details.
     * Callers must validate user input before construction.
     *
     * @param name Name of the lens.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the lens from 1 to 10.
     * @param zoomRange Zoom range description, from 1 to 100 characters without line breaks.
     * @param damage Damage recorded for the lens.
     */
    public Lens(String name, String id, int condition, String zoomRange, LensDamage damage) {
        this(name, id, condition, zoomRange, damage, "");
    }

    /** Creates a lens with type-specific details and optional remarks. */
    public Lens(String name, String id, int condition, String zoomRange, LensDamage damage, String remarks) {
        super(name, id, condition, remarks);
        assert zoomRange != null && !zoomRange.isBlank() && zoomRange.length() <= 100
                && !zoomRange.matches("(?s).*\\R.*")
                : "Lens zoom range must contain 1 to 100 characters without line breaks.";
        assert damage != null : "Lens damage must be specified.";
        this.zoomRange = zoomRange;
        this.damage = damage;
        assert TYPE.equals(getType()) : "Lens type must remain lens.";
        LOGGER.log(Level.INFO, "Created lens with item ID {0}.", id);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getZoomRange() {
        return zoomRange;
    }

    public LensDamage getDamage() {
        return damage;
    }

    @Override
    public String getDetails() {
        return "zoom/" + zoomRange + " damage/" + damage.name();
    }
}
