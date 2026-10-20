package seedu.snap.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;

/** Parses add commands and stores equipment for the current session. */
public class EquipmentHandler {
    private static final String ADD_COMMAND = "add";
    private static final Pattern DELETE_PATTERN = Pattern.compile("delete\\s+item/([0-9]{3})");
    private static final String ITEM_MARKER = "i/";
    private static final String ID_MARKER = "id/";
    private static final String TYPE_MARKER = "type/";
    private static final String CONDITION_MARKER = "cond/";
    private static final Logger LOGGER = Logger.getLogger(EquipmentHandler.class.getName());

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
        Equipment equipment = parseEquipment(input);
        assert equipment != null : "Parsed equipment must not be null.";
        inventory.add(equipment);
        assert inventory.contains(equipment) : "Added equipment must be present in inventory.";
        LOGGER.log(Level.INFO, "Added {0} with item ID {1}.",
                new Object[] {equipment.getType(), equipment.getItemId()});
        return equipment;
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
     * @throws EquipmentDeletionUnsuccessful when the command or item ID is invalid
     */
    public Equipment getEquipmentForDeletion(String input) throws EquipmentDeletionUnsuccessful {
        String itemId = parseDeleteItemId(input);
        for (Equipment equipment : inventory) {
            if (equipment.getItemId().equals(itemId)) {
                LOGGER.log(Level.INFO, "Found {0} with item ID {1} for deletion.",
                        new Object[] {equipment.getType(), itemId});
                return equipment;
            }
        }
        LOGGER.log(Level.WARNING, "No equipment found for item ID {0}.", itemId);
        throw new EquipmentDeletionUnsuccessful("Equipment ID " + itemId + " does not exist.");
    }

    /**
     * Removes the equipment identified by a valid delete command.
     *
     * @param input complete delete command
     * @return the removed equipment
     * @throws EquipmentDeletionUnsuccessful when the command or item ID is invalid
     */
    public Equipment deleteEquipment(String input) throws EquipmentDeletionUnsuccessful {
        Equipment equipment = getEquipmentForDeletion(input);
        boolean removed = inventory.remove(equipment);
        assert removed : "Equipment selected for deletion must be in inventory.";
        LOGGER.log(Level.INFO, "Deleted {0} with item ID {1}.",
                new Object[] {equipment.getType(), equipment.getItemId()});
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

        if (itemString.isEmpty() || itemString.length() > 100) {
            throw new EquipmentAdditionUnsuccessful("Item name must contain 1 to 100 characters.");
        }

        if (!id.matches("[0-9]{3}") || id.equals("000")) {
            throw new EquipmentAdditionUnsuccessful("Item ID must be from 001 to 999.");
        }

        if (type.isEmpty()) {
            throw new EquipmentAdditionUnsuccessful("Type cannot be empty.");
        }

        String normalizedType = type.toLowerCase(Locale.ROOT);

        if (!conditionString.matches("10|[1-9]")) {
            throw new EquipmentAdditionUnsuccessful("Condition must be a whole number from 1 to 10.");
        }

        int condition = Integer.parseInt(conditionString);
        assert condition >= 1 && condition <= 10 : "Parsed condition must be between 1 and 10.";

        Equipment equipment = createEquipment(normalizedType, itemString, id, condition);
        assert equipment.getType().equals(normalizedType)
                : "Created equipment type must match the requested type.";
        LOGGER.log(Level.INFO, "Parsed {0} equipment with item ID {1}.",
                new Object[] {equipment.getType(), equipment.getItemId()});
        return equipment;
    }

    /**
     * Creates the concrete equipment subclass selected by a command's type.
     *
     * @param type normalized equipment type
     * @param itemName equipment name
     * @param id equipment ID
     * @param condition equipment condition
     * @return the concrete equipment instance
     * @throws EquipmentAdditionUnsuccessful when the type is unsupported
     */
    private static Equipment createEquipment(String type, String itemName, String id, int condition)
            throws EquipmentAdditionUnsuccessful {
        Equipment equipment = switch (type) {
        case "camera" -> new Camera(itemName, id, condition);
        case "tripod" -> new Tripod(itemName, id, condition);
        case "lens" -> new Lens(itemName, id, condition);
        case "battery" -> new Battery(itemName, id, condition);
        default -> {
            LOGGER.log(Level.WARNING, "Unsupported equipment type: {0}.", type);
            throw new EquipmentAdditionUnsuccessful(
                    "Unsupported type. Choose camera, tripod, lens, or battery.");
        }
        };
        assert equipment.getType().equals(type) : "Factory returned the wrong equipment subtype.";
        return equipment;
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

        for (int i = 0; i < equipmentList.size(); i++) {
            Equipment item = equipmentList.get(i);

            System.out.printf(
                    "%-4s %-24s | %-6s | %-15s | %-9d%n",
                    (i + 1) + ")", item.getItemName(), item.getItemId(), item.getType(), item.getCondition()
            );
        }
    }
}
