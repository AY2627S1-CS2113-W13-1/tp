package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an item of equipment in the inventory.
 */
public class Equipment {
    private static final Logger logger = Logger.getLogger(Equipment.class.getName());
    private final String itemName;
    private final String itemId;
    private final String type;
    private final int condition;
    private boolean isLoaned = false;

    /**
     * Creates an equipment item.
     * Callers must validate user input before construction; assertions detect programming mistakes.
     *
     * @param name Name of the equipment.
     * @param id Three-digit equipment ID.
     * @param type Type of equipment.
     * @param condition Condition of the equipment from 1 to 10.
     */
    public Equipment(String name, String id, String type, int condition) {
        assert name != null && !name.isBlank() && name.length() <= 100
                : "Equipment name must contain 1 to 100 characters";
        assert id != null && id.matches("[0-9]{3}") && !id.equals("000")
                : "Equipment ID must be from 001 to 999";
        assert type != null && !type.isBlank() : "Equipment type must contain text";
        assert condition >= 1 && condition <= 10 : "Equipment condition must be from 1 to 10";
        this.itemName = name;
        this.itemId = id;
        this.type = type;
        this.condition = condition;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemId() {
        return itemId;
    }

    public String getType() {
        return type;
    }

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
}
