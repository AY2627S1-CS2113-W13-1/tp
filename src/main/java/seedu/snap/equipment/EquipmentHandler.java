package seedu.snap.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;

/** Parses add and delete commands and stores equipment for the current session. */
public class EquipmentHandler {
    /** Diagnostic logger; the application sends outcomes and detailed traces to local files. */
    private static final Logger logger = Logger.getLogger(EquipmentHandler.class.getName());
    private static final String ADD_COMMAND = "add";
    private static final Pattern DELETE_PATTERN = Pattern.compile("delete\\s+item/([0-9]{3})");
    private static final String ITEM_MARKER = "i/";
    private static final String ID_MARKER = "id/";
    private static final String TYPE_MARKER = "type/";
    private static final String CONDITION_MARKER = "cond/";

    /** Equipment stored by this handler during the current application session. */
    private final ArrayList<Equipment> inventory = new ArrayList<>();

    /**
     * Adds equipment when the command contains valid details.
     *
     * @param input complete add command
     * @return the added equipment
     * @throws EquipmentAdditionUnsuccessful when the command is invalid
     */
    public Equipment addEquipment(String input) throws EquipmentAdditionUnsuccessful {
        int previousSize = inventory.size();
        Equipment equipment;
        try {
            equipment = parseEquipment(input);
        } catch (EquipmentAdditionUnsuccessful e) {
            assert inventory.size() == previousSize : "Rejected additions must not change inventory size";
            logger.log(Level.FINE, "Equipment addition rejected: {0}", e.getMessage());
            throw e;
        }
        assert equipment != null : "Successful parsing must return equipment";
        inventory.add(equipment);
        assert inventory.size() == previousSize + 1 : "Adding equipment must increase inventory size by one";
        assert inventory.getLast() == equipment : "The newly added equipment must be last in the inventory";
        logger.log(Level.FINE, "Added equipment with item ID {0}", equipment.getItemId());
        return equipment;
    }

    /**
     * Updates supplied details after validating every value; the ID, object, and inventory order stay the same.
     *
     * @param input update command with an ID and at least one detail
     * @return the updated equipment
     * @throws EquipmentUpdateUnsuccessful when the command or new details are invalid
     */
    public Equipment updateEquipment(String input) throws EquipmentUpdateUnsuccessful {
        try {
            Map<String, String> fields = EquipmentFields.parse(input, "update", "id", "i", "type", "cond");
            String id = fields.get("id");
            if (id == null || !id.matches("[0-9]{3}") || id.equals("000")) {
                throw new EquipmentUpdateUnsuccessful("Item ID must be from 001 to 999.");
            }
            if (fields.size() == 1) {
                throw new EquipmentUpdateUnsuccessful("Supply a name, type, or condition to update.");
            }
            Equipment target = findEquipmentForUpdate(id);
            String name = fields.getOrDefault("i", target.getItemName());
            String type = fields.getOrDefault("type", target.getType());
            String condition = fields.getOrDefault("cond", Integer.toString(target.getCondition()));
            validateEquipmentDetails(name, id, type, condition);
            int previousSize = inventory.size();
            target.updateDetails(name, type, Integer.parseInt(condition));
            assert inventory.size() == previousSize : "Updates must not add or remove equipment";
            assert target.getItemId().equals(id) : "Updates must preserve Item ID";
            logger.log(Level.FINE, "Updated equipment with item ID {0}", id);
            return target;
        } catch (IllegalArgumentException | EquipmentAdditionUnsuccessful | EquipmentUpdateUnsuccessful e) {
            logger.log(Level.FINE, "Equipment update rejected: {0}", e.getMessage());
            throw new EquipmentUpdateUnsuccessful(e.getMessage());
        }
    }

    /** Finds exactly one target so malformed inventories cannot cause an update to the wrong item. */
    private Equipment findEquipmentForUpdate(String id) throws EquipmentUpdateUnsuccessful {
        Equipment match = null;
        for (Equipment item : inventory) {
            if (item.getItemId().equals(id)) {
                if (match != null) {
                    throw new EquipmentUpdateUnsuccessful("Item ID " + id + " is not unique.");
                }
                match = item;
            }
        }
        if (match == null) {
            throw new EquipmentUpdateUnsuccessful("Item ID " + id + " does not exist.");
        }
        return match;
    }

    /**
     * Returns the inventory for listing or inspecting stored equipment.
     *
     * @return an unmodifiable view of the inventory
     */
    public List<Equipment> getEquipmentList() {
        return Collections.unmodifiableList(inventory);
    }

    /**
     * Finds the equipment targeted by a delete command without changing the inventory.
     *
     * @param input complete delete command
     * @return the equipment that matches the command's item ID
     * @throws EquipmentDeletionUnsuccessful when the command is invalid or the ID is missing or duplicated
     */
    public Equipment getEquipmentForDeletion(String input) throws EquipmentDeletionUnsuccessful {
        String itemId = parseDeleteItemId(input);
        Equipment match = null;
        for (Equipment equipment : inventory) {
            if (equipment.getItemId().equals(itemId)) {
                if (match != null) {
                    logger.log(Level.WARNING, "Equipment deletion rejected: duplicate ID {0}.", itemId);
                    throw new EquipmentDeletionUnsuccessful(
                            "Equipment ID " + itemId + " is not unique. No equipment was deleted.");
                }
                match = equipment;
            }
        }
        if (match == null) {
            logger.log(Level.FINE, "Equipment deletion rejected: ID {0} does not exist.", itemId);
            throw new EquipmentDeletionUnsuccessful("Equipment ID " + itemId + " does not exist.");
        }
        return match;
    }

    /**
     * Removes the equipment identified by a valid delete command.
     *
     * @param input complete delete command
     * @return the removed equipment
     * @throws EquipmentDeletionUnsuccessful when the command is invalid or the ID is missing or duplicated
     */
    public Equipment deleteEquipment(String input) throws EquipmentDeletionUnsuccessful {
        Equipment equipment = getEquipmentForDeletion(input);
        int previousSize = inventory.size();
        // Removal must still run when assertions are disabled.
        boolean removed = inventory.remove(equipment);
        assert removed : "Equipment found for deletion must still be in the inventory";
        assert inventory.size() == previousSize - 1 : "Deletion must remove exactly one equipment record";
        logger.log(Level.INFO, "Deleted equipment with ID {0}.", equipment.getItemId());
        return equipment;
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

        validateEquipmentDetails(itemString, id, type, conditionString);
        return new Equipment(itemString, id, type, Integer.parseInt(conditionString));
    }

    /** Shares field rules between adding and updating, before either operation changes the inventory. */
    private static void validateEquipmentDetails(String itemName, String id, String type, String conditionString)
            throws EquipmentAdditionUnsuccessful {
        if (itemName.isBlank() || itemName.length() > 100) {
            throw new EquipmentAdditionUnsuccessful("Item name must contain 1 to 100 characters.");
        }

        if (!id.matches("[0-9]{3}") || id.equals("000")) {
            throw new EquipmentAdditionUnsuccessful("Item ID must be from 001 to 999.");
        }

        if (type.isBlank()) {
            throw new EquipmentAdditionUnsuccessful("Type cannot be empty.");
        }

        if (!conditionString.matches("10|[1-9]")) {
            throw new EquipmentAdditionUnsuccessful("Condition must be a whole number from 1 to 10.");
        }
    }

    /**
     * Extracts and validates the item ID from a delete command.
     *
     * @param input complete delete command
     * @return the validated item ID
     * @throws EquipmentDeletionUnsuccessful when the command does not match the delete syntax
     */
    private String parseDeleteItemId(String input) throws EquipmentDeletionUnsuccessful {
        Matcher matcher = input == null ? null : DELETE_PATTERN.matcher(input.trim());
        if (matcher == null || !matcher.matches() || matcher.group(1).equals("000")) {
            logger.fine("Equipment deletion rejected: invalid command syntax.");
            throw new EquipmentDeletionUnsuccessful(
                    "Invalid delete syntax. ITEM ID must be from 001 to 999.");
        }
        return matcher.group(1);
    }

    /**
     * Prints out all equipment in inventory
     */
    public void viewAllEquipment() {
        System.out.printf(
                "%-4s %-24s | %-6s | %-15s | %-9s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION"
        );

        List<Equipment> equipmentList = getEquipmentList();
        logger.log(Level.INFO, "Viewing {0} equipment item(s)", equipmentList.size());

        for (int i = 0; i < equipmentList.size(); i++) {
            Equipment item = equipmentList.get(i);
            assert item != null : "Equipment list must not contain null items";

            System.out.printf(
                    "%-4s %-24s | %-6s | %-15s | %-9d%n",
                    (i + 1) + ")", item.getItemName(), item.getItemId(), item.getType(), item.getCondition()
            );
        }
    }
}
