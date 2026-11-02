package seedu.snap.equipment;

import java.util.UUID;
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
    /** Maximum number of characters allowed in a single-line equipment remark. */
    public static final int MAX_REMARKS_LENGTH = 200;
    private static final String ITEM_ID_PATTERN = "[0-9]{3}";
    private static final String RESERVED_ITEM_ID = "000";
    private static final Pattern BLANK_TEXT = Pattern.compile("\\s*", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Logger logger = Logger.getLogger(Equipment.class.getName());
    private String itemName;
    private final String itemId;
    /** Permanent identity of this physical item; the visible three-digit Item ID may later be reused. */
    private String recordId = UUID.randomUUID().toString();
    private int condition;
    private boolean isLoaned = false;
    /** Whether this item has passed the latest return assessment and can be loaned again. */
    private boolean isServiceable = true;
    /** Current active loan identifier, rebuilt from saved loan records rather than duplicated in equipment storage. */
    private String loanId;
    /** Optional single-line remarks retained with this equipment record. */
    private String remarks;

    /**
     * Creates an equipment item.
     * Callers must validate user input before construction; assertions detect programming mistakes.
     *
     * @param name Name of the equipment.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the equipment from 1 to 10.
     */
    protected Equipment(String name, String id, int condition) {
        this(name, id, condition, "");
    }

    /**
     * Creates an equipment item with optional remarks.
     *
     * @param name Name of the equipment.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the equipment from 1 to 10.
     * @param remarks Optional single-line remarks, up to 200 characters.
     */
    protected Equipment(String name, String id, int condition, String remarks) {
        assert hasText(name) && name.length() <= MAX_NAME_LENGTH
                : "Equipment name must contain 1 to 100 characters";
        assert isValidItemId(id) : "Equipment ID must be from 001 to 999";
        assert condition >= MIN_CONDITION && condition <= MAX_CONDITION
                : "Equipment condition must be from 1 to 10";
        assert remarks != null && remarks.length() <= MAX_REMARKS_LENGTH
                && !remarks.contains("\n") && !remarks.contains("\r")
                : "Equipment remarks must be a single line of at most 200 characters";
        this.itemName = name;
        this.itemId = id;
        this.condition = condition;
        this.remarks = remarks.trim();
        assert hasValidState() : "Equipment fields must contain valid values.";
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemId() {
        return itemId;
    }

    /** Returns this physical item's permanent identity, retained when its visible details change. */
    public String getRecordId() {
        return recordId;
    }

    /** Restores a canonical saved identity or retains it when creating a detached update proposal. */
    final void restoreRecordId(String savedRecordId) {
        try {
            if (savedRecordId == null || !UUID.fromString(savedRecordId).toString().equals(savedRecordId)) {
                throw new IllegalArgumentException("Equipment record ID must be a canonical UUID.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Equipment record ID must be a canonical UUID.", exception);
        }
        recordId = savedRecordId;
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

    /** Returns whether this equipment can be loaned after any active loan has ended. */
    public boolean isServiceable() {
        return isServiceable;
    }

    /** Sets serviceability on a detached proposal or equipment freshly read from storage. */
    final void setServiceable(boolean serviceable) {
        isServiceable = serviceable;
    }

    /** Copies subtype damage after the common return-assessment identity checks have passed. */
    protected abstract void applyDamageAssessment(Equipment assessed);

    /**
     * Commits a validated return assessment without replacing references held by loans and bundles.
     * The assessment can change condition, damage, remarks and serviceability, preserving item identity.
     *
     * @param assessed detached proposal whose persistent values have already been saved
     * @throws IllegalArgumentException if the proposal belongs to different equipment
     */
    public final void applyReturnAssessment(Equipment assessed) {
        if (assessed == null || assessed.getClass() != getClass()
                || !itemId.equals(assessed.itemId) || !recordId.equals(assessed.recordId)
                || !itemName.equals(assessed.itemName)
                || assessed.condition < MIN_CONDITION || assessed.condition > MAX_CONDITION
                || assessed.remarks == null
                || assessed.remarks.length() > MAX_REMARKS_LENGTH
                || assessed.remarks.matches("(?s).*\\R.*")) {
            throw new IllegalArgumentException("The return assessment must belong to the same equipment.");
        }
        applyDamageAssessment(assessed);
        condition = assessed.condition;
        remarks = assessed.remarks;
        isServiceable = assessed.isServiceable;
    }

    /** Returns the current active loan identifier, or null when no identified loan owns this item. */
    public String getLoanId() {
        return loanId;
    }

    /** Associates this item with a validated loan identifier and updates availability together. */
    public void setLoanId(String loanId) {
        assert loanId == null || isValidItemId(loanId) : "Loan ID must be from 001 to 999";
        setLoaned(loanId != null);
        this.loanId = loanId;
    }

    /** Updates availability when an associated loan changes status. */
    public void setLoaned(boolean isLoaned) {
        if (!isLoaned) {
            loanId = null;
        }
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
                && condition >= MIN_CONDITION && condition <= MAX_CONDITION
                && remarks != null && remarks.length() <= MAX_REMARKS_LENGTH
                && !remarks.contains("\n") && !remarks.contains("\r");
    }

    /**
     * Returns the optional remarks associated with this equipment.
     *
     * @return remarks, or an empty string when none were provided
     */
    public String getRemarks() {
        return remarks;
    }
}
