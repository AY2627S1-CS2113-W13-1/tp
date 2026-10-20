package seedu.snap.equipment;

/**
 * Represents an item of equipment in the inventory.
 *
 * <p>The concrete equipment type is represented by a subclass rather than by a
 * field on this class. This keeps type-specific behavior separate from the
 * common equipment properties.</p>
 */
public abstract class Equipment {
    private final String itemName;
    private final String itemId;
    private final int condition;

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
