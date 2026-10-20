package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an item of equipment in the inventory.
 *
 * <p>The concrete equipment type is represented by a subclass rather than by a
 * field on this class. This keeps type-specific behavior separate from the
 * common equipment properties.</p>
 */
public abstract class Equipment {
    private static final Logger LOGGER = Logger.getLogger(Equipment.class.getName());
    private final String itemName;
    private final String itemId;
    private final int condition;
    private boolean isLoaned;

    /**
     * Creates an equipment item.
     *
     * @param name Name of the equipment.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the equipment from 1 to 10.
     */
    protected Equipment(String name, String id, int condition) {
        this.itemName = name;
        this.itemId = id;
        this.condition = condition;
        this.isLoaned = false;
        assert hasValidState() : "Equipment fields must contain valid values.";
        assert !isLoaned : "New equipment must not be loaned.";
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemId() {
        return itemId;
    }

    /**
     * Returns the canonical type represented by this equipment subclass.
     *
     * @return the equipment type
     */
    public abstract String getType();

    public int getCondition() {
        return condition;
    }

    /**
     * Returns whether this equipment currently has an active loan.
     *
     * @return true when the equipment is currently loaned
     */
    public boolean isLoaned() {
        return isLoaned;
    }

    /** Marks this equipment as currently loaned. */
    public void markLoaned() {
        assert !isLoaned : "Equipment cannot be loaned twice without being returned.";
        isLoaned = true;
        LOGGER.log(Level.INFO, "Marked equipment with item ID {0} as loaned.", itemId);
    }

    /** Marks this equipment as returned and available for another loan. */
    public void markReturned() {
        assert isLoaned : "Only loaned equipment can be marked as returned.";
        isLoaned = false;
        LOGGER.log(Level.INFO, "Marked equipment with item ID {0} as returned.", itemId);
    }

    /**
     * Checks the invariant shared by all equipment subclasses.
     *
     * @return true when all common equipment fields are valid
     */
    private boolean hasValidState() {
        return itemName != null && !itemName.isBlank() && itemName.length() <= 100
                && itemId != null && itemId.matches("[0-9]{3}") && !itemId.equals("000")
                && condition >= 1 && condition <= 10;
    }
}
