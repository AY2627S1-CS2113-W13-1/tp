package seedu.snap.equipment;

/**
 * Represents an item of equipment in the inventory.
 */
public class Equipment {
    private String itemName;
    private final String itemId;
    private String type;
    private int condition;

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

    /** Applies already validated details together, preserving this item's identity and other state. */
    public void updateDetails(String itemName, String type, int condition) {
        assert itemName != null && !itemName.isBlank() && itemName.length() <= 100
                : "Equipment name must contain 1 to 100 characters";
        assert type != null && !type.isBlank() : "Equipment type must contain text";
        assert condition >= 1 && condition <= 10 : "Equipment condition must be from 1 to 10";
        this.itemName = itemName;
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
