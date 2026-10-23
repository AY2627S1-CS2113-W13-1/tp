package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Represents an item of equipment in the inventory.
 *
 * <p>The concrete equipment type is represented by a subclass rather than by a
 * field on this class. This keeps type-specific behaviour separate from the
 * common equipment properties.</p>
 */
public abstract class Equipment {
    /** Maximum number of characters accepted in an equipment name. */
    public static final int MAX_NAME_LENGTH = 100;
    /** Inclusive lower bound of the equipment condition scale. */
    public static final int MIN_CONDITION = 1;
    /** Inclusive upper bound of the equipment condition scale. */
    public static final int MAX_CONDITION = 10;
    private static final String ITEM_ID_PATTERN = "[0-9]{3}";
    private static final String RESERVED_ITEM_ID = "000";
    private static final Pattern BLANK_TEXT = Pattern.compile("\\s*", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Logger logger = Logger.getLogger(Equipment.class.getName());
    private String itemName;
    private final String itemId;
    private int condition;
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

    /** Checks required text consistently for ordinary and nonbreaking whitespace. */
    static boolean hasText(String value) {
        return value != null && !BLANK_TEXT.matcher(value).matches();
    }

    /** Checks the three-digit Item ID range, excluding the reserved all-zero ID. */
    static boolean isValidItemId(String id) {
        return id != null && id.matches(ITEM_ID_PATTERN) && !id.equals(RESERVED_ITEM_ID);
    }

    /** Applies validated name and condition together, preserving subtype, details, ID, and loan state. */
    public final void updateDetails(String itemName, int condition) {
        assert hasText(itemName) && itemName.length() <= MAX_NAME_LENGTH
                : "Equipment name must contain 1 to 100 characters";
        assert condition >= MIN_CONDITION && condition <= MAX_CONDITION
                : "Equipment condition must be from 1 to 10";
        this.itemName = itemName;
        this.condition = condition;
    }

    /**
     * Checks the invariant shared by all equipment subclasses.
     *
     * @return true when all common equipment fields are valid
     */
    private boolean hasValidState() {
        return hasText(itemName) && itemName.length() <= MAX_NAME_LENGTH
                && isValidItemId(itemId)
                && condition >= MIN_CONDITION && condition <= MAX_CONDITION;
    }
}
