package seedu.snap.equipment;

import java.util.regex.Pattern;

/**
 * Represents an item of equipment in the inventory.
 */
public class Equipment {
    /** Maximum number of characters accepted in an equipment name. */
    public static final int MAX_NAME_LENGTH = 100;
    /** Inclusive lower bound of the equipment condition scale. */
    public static final int MIN_CONDITION = 1;
    /** Inclusive upper bound of the equipment condition scale. */
    public static final int MAX_CONDITION = 10;
    private static final String ITEM_ID_PATTERN = "[0-9]{3}";
    private static final String RESERVED_ITEM_ID = "000";
    private static final Pattern BLANK_TEXT = Pattern.compile("\\s*", Pattern.UNICODE_CHARACTER_CLASS);

    private final String itemName;
    private final String itemId;
    private final String type;
    private final int condition;

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
        assert hasText(name) && name.length() <= MAX_NAME_LENGTH
                : "Equipment name must contain 1 to 100 characters";
        assert isValidItemId(id)
                : "Equipment ID must be from 001 to 999";
        assert hasText(type) : "Equipment type must contain text";
        assert condition >= MIN_CONDITION && condition <= MAX_CONDITION : "Equipment condition must be from 1 to 10";
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
}
