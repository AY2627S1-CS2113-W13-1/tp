package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an item of equipment in the inventory.
 *
 * <p>The concrete equipment type is represented by a subclass rather than by a
 * field on this class. This keeps type-specific behaviour separate from the
 * common equipment properties.</p>
 */
public abstract class Equipment {
    private static final Logger logger = Logger.getLogger(Equipment.class.getName());
    private final String itemName;
    private final String itemId;
    private final int condition;
    private boolean isLoaned = false;

    /**
     * Creates an equipment item.
     * Callers must validate user input before construction; assertions detect programming mistakes.
     *
     * @param name Name of the equipment.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the equipment from 1 to 10.
     */
    protected Equipment(String name, String id, int condition) {
        this.itemName = name;
        this.itemId = id;
        this.condition = condition;
        assert hasValidState() : "Equipment fields must contain valid values.";
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

    /**
     * Returns the type-specific fields in add-command format for listing and saving.
     *
     * @return the equipment's attribute and damage fields
     */
    public abstract String getDetails();

    /** Returns whether this item currently has an active loan. */
    public boolean isLoaned() {
        return isLoaned;
    }

    /** Updates availability when an associated loan changes status. */
    public void setLoaned(boolean isLoaned) {
        assert itemId != null : "Equipment must have an ID before its availability changes";
        if (this.isLoaned == isLoaned) {
            logger.log(Level.FINE, "Equipment with item ID {0} remains {1}.",
                    new Object[]{itemId, isLoaned ? "loaned" : "available"});
            return;
        }
        boolean previousState = this.isLoaned;
        this.isLoaned = isLoaned;
        logger.log(Level.INFO, "Equipment with item ID {0} changed from {1} to {2}.",
                new Object[]{itemId, previousState ? "loaned" : "available",
                    isLoaned ? "loaned" : "available"});
    }

    public int getCondition() {
        return condition;
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
