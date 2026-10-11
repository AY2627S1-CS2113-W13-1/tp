package seedu.snap.equipment;

/**
 * Represents an item of equipment in the inventory.
 */
public class Equipment {
    private final String itemName;
    private final String itemId;
    private final String type;
    private final int condition;

    /**
     * Creates an equipment item.
     *
     * @param name Name of the equipment.
     * @param id Three-digit equipment ID.
     * @param type Type of equipment.
     * @param condition Condition of the equipment from 1 to 10.
     */
    public Equipment(String name, String id, String type, int condition) {
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
}
