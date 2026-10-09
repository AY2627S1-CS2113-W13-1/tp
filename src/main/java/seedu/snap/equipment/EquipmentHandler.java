package seedu.snap.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;

/** Parses add commands and stores equipment for the current session. */
public class EquipmentHandler {
    private static final String ADD_COMMAND = "add";
    private static final String ITEM_MARKER = "i/";
    private static final String ID_MARKER = "id/";
    private static final String TYPE_MARKER = "type/";
    private static final String CONDITION_MARKER = "cond/";

    private static final ArrayList<Equipment> inventory = new ArrayList<>();

    /**
     * Adds equipment when the command contains valid details.
     *
     * @param input complete add command
     * @return the added equipment
     * @throws EquipmentAdditionUnsuccessful when the command is invalid
     */
    public Equipment addEquipment(String input) throws EquipmentAdditionUnsuccessful {
        Equipment equipment = parseEquipment(input);
        addEquipment(inventory, equipment);
        return equipment;
    }

    /**
     * Adds an equipment item to the inventory.
     *
     * @param inventory Inventory to add the equipment to.
     * @param equipment Equipment to add.
     */
    public static void addEquipment(ArrayList<Equipment> inventory, Equipment equipment) {
        inventory.add(equipment);
    }

    /**
     * Returns the inventory for listing or inspecting stored equipment.
     *
     * @return an unmodifiable view of the inventory
     */
    public static List<Equipment> getEquipmentList() {
        return Collections.unmodifiableList(inventory);
    }

    /**
     * Parses and validates an add command.
     *
     * @param userInput Complete command entered by the user.
     * @return The parsed equipment.
     * @throws EquipmentAdditionUnsuccessful when the command is invalid
     */
    static Equipment parseEquipment(String userInput) throws EquipmentAdditionUnsuccessful {
        if (userInput == null || !(userInput.equals(ADD_COMMAND) || userInput.startsWith(ADD_COMMAND + " "))) {
            throw new EquipmentAdditionUnsuccessful("Invalid format. Use: add i/[ITEM] id/[ITEM ID] "
                    + "type/[TYPE] cond/[CONDITION]");
        }
        String addDetails = userInput.substring(ADD_COMMAND.length()).trim();
        int itemIndex = addDetails.indexOf(ITEM_MARKER);
        int idIndex = addDetails.indexOf(ID_MARKER);
        int typeIndex = addDetails.indexOf(TYPE_MARKER);
        int conditionIndex = addDetails.indexOf(CONDITION_MARKER);

        boolean markersAreInOrder = itemIndex == 0
                && idIndex > itemIndex
                && typeIndex > idIndex
                && conditionIndex > typeIndex;
        if (!markersAreInOrder) {
            throw new EquipmentAdditionUnsuccessful("Invalid format. Use: add i/[ITEM] id/[ITEM ID] "
                    + "type/[TYPE] cond/[CONDITION]");
        }

        String itemString = addDetails.substring(itemIndex + ITEM_MARKER.length(), idIndex).trim();
        String id = addDetails.substring(idIndex + ID_MARKER.length(), typeIndex).trim();
        String type = addDetails.substring(typeIndex + TYPE_MARKER.length(), conditionIndex).trim();
        String conditionString = addDetails.substring(conditionIndex + CONDITION_MARKER.length()).trim();

        if (itemString.isEmpty() || itemString.length() > 100) {
            throw new EquipmentAdditionUnsuccessful("Item name must contain 1 to 100 characters.");
        }

        if (!id.matches("[0-9]{3}") || id.equals("000")) {
            throw new EquipmentAdditionUnsuccessful("Item ID must be from 001 to 999.");
        }

        if (type.isEmpty()) {
            throw new EquipmentAdditionUnsuccessful("Type cannot be empty.");
        }

        if (!conditionString.matches("10|[1-9]")) {
            throw new EquipmentAdditionUnsuccessful("Condition must be a whole number from 1 to 10.");
        }

        int condition = Integer.parseInt(conditionString);

        return new Equipment(itemString, id, type, condition);
    }

    /**
     * Prints out all equipment in inventory
     */
    public static void viewAllEquipment() {
        System.out.printf(
                "%-4s %-24s | %-6s | %-15s | %-9s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION"
        );

        List<Equipment> equipmentList = getEquipmentList();

        for (int i = 0; i < equipmentList.size(); i++) {
            Equipment item = equipmentList.get(i);

            System.out.printf(
                    "%-4s %-24s | %-6s | %-15s | %-9d%n",
                    (i + 1) + ")", item.getItemName(), item.getItemId(), item.getType(), item.getCondition()
            );
        }
    }
}
