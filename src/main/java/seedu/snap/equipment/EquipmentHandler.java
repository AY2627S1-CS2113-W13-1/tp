package seedu.snap.equipment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;

/** Parses add and delete commands and stores equipment for the current session. */
public class EquipmentHandler {
    /** Diagnostic logger; the application sends outcomes and detailed traces to local files. */
    private static final Logger logger = Logger.getLogger(EquipmentHandler.class.getName());
    private static final String ADD_COMMAND = "add";
    private static final String ADD_FORMAT_ERROR = "Invalid format. Use: add i/[ITEM] id/[ITEM ID] "
            + "type/[TYPE] cond/[CONDITION]";
    private static final Pattern DELETE_PATTERN = Pattern.compile("delete\\s+item/([0-9]{3})");
    private static final String ITEM_MARKER = "i/";
    private static final String ID_MARKER = "id/";
    private static final String TYPE_MARKER = "type/";
    private static final String CONDITION_MARKER = "cond/";

    /** Equipment stored by this handler during the current application session. */
    private final ArrayList<Equipment> inventory = new ArrayList<>();
    /** File used for persistence, or null when the handler is intentionally in-memory only. */
    private final Path storageFile;

    /** Creates an in-memory equipment handler, mainly useful for isolated unit tests. */
    public EquipmentHandler() {
        this(null);
    }

    /** Creates an equipment handler backed by the supplied text file. */
    public EquipmentHandler(Path storageFile) {
        this.storageFile = storageFile;
    }

    /**
     * Replaces the current inventory with records loaded from the backing file.
     * A missing file is treated as an empty inventory.
     *
     * @throws IOException if saved data cannot be read or is invalid
     */
    public void loadFromStorage() throws IOException {
        if (storageFile == null) {
            return;
        }
        List<Equipment> loaded = EquipmentStorage.load(storageFile);
        inventory.clear();
        inventory.addAll(loaded);
        assert inventory.size() == loaded.size() : "Loaded inventory size must be preserved";
        logger.log(Level.INFO, "Loaded {0} equipment item(s) from {1}",
                new Object[]{inventory.size(), storageFile});
    }

    /**
     * Adds equipment when the command contains valid details.
     *
     * @param input complete add command
     * @return the added equipment
     * @throws EquipmentAdditionUnsuccessful when the command is invalid or the item ID already exists
     */
    public Equipment addEquipment(String input) throws EquipmentAdditionUnsuccessful {
        int previousSize = inventory.size();
        Equipment equipment;
        try {
            equipment = parseEquipment(input);
            assert equipment != null : "Successful parsing must return equipment";
            for (Equipment existing : inventory) {
                if (existing.getItemId().equals(equipment.getItemId())) {
                    throw new EquipmentAdditionUnsuccessful("Item ID " + equipment.getItemId()
                            + " already exists.");
                }
            }
        } catch (EquipmentAdditionUnsuccessful e) {
            assert inventory.size() == previousSize : "Rejected additions must not change inventory size";
            logger.log(Level.FINE, "Equipment addition rejected: {0}", e.getMessage());
            throw e;
        }
        // Persist the candidate state before mutating memory so a failed write cannot
        // leave the in-memory inventory ahead of the saved inventory.
        ArrayList<Equipment> updatedInventory = new ArrayList<>(inventory);
        updatedInventory.add(equipment);
        try {
            save(updatedInventory);
        } catch (IOException e) {
            EquipmentAdditionUnsuccessful exception = new EquipmentAdditionUnsuccessful(
                    "Could not save equipment: " + e.getMessage());
            logger.log(Level.WARNING, "Equipment addition could not be saved", e);
            throw exception;
        }
        inventory.add(equipment);
        assert inventory.size() == previousSize + 1 : "Adding equipment must increase inventory size by one";
        assert inventory.getLast() == equipment : "The newly added equipment must be last in the inventory";
        logger.log(Level.FINE, "Added equipment with item ID {0}", equipment.getItemId());
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

    /** Persists the current inventory when this handler has a backing file. */
    public void saveToStorage() throws IOException {
        save(inventory);
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

        ArrayList<Equipment> updatedInventory = new ArrayList<>(inventory);
        updatedInventory.remove(equipment);
        try {
            save(updatedInventory);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Equipment deletion could not be saved", e);
            throw new EquipmentDeletionUnsuccessful("Could not save equipment deletion: " + e.getMessage());
        }

        // Removal must still run when assertions are disabled.
        boolean wasRemoved = inventory.remove(equipment);
        assert wasRemoved : "Equipment found for deletion must still be in the inventory";
        assert inventory.size() == previousSize - 1 : "Deletion must remove exactly one equipment record";
        logger.log(Level.INFO, "Deleted equipment with ID {0}.", equipment.getItemId());
        return equipment;
    }

    /** Saves the supplied inventory when this handler has a backing file. */
    private void save(List<Equipment> equipment) throws IOException {
        if (storageFile != null) {
            EquipmentStorage.save(storageFile, equipment);
            logger.log(Level.FINE, "Saved {0} equipment item(s) to {1}",
                    new Object[]{equipment.size(), storageFile});
        }
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
            throw new EquipmentAdditionUnsuccessful(ADD_FORMAT_ERROR);
        }
        String addDetails = userInput.substring(ADD_COMMAND.length()).trim();
        int itemIndex = findFieldMarker(addDetails, ITEM_MARKER, 0);
        int typeIndex = findFieldMarker(addDetails, TYPE_MARKER, 0);
        int conditionIndex = findFieldMarker(addDetails, CONDITION_MARKER, 0);
        int idIndex = findIdFieldMarker(addDetails, typeIndex);
        validateFieldMarkers(addDetails, itemIndex, idIndex, typeIndex, conditionIndex);

        String itemName = addDetails.substring(itemIndex + ITEM_MARKER.length(), idIndex).trim();
        String id = addDetails.substring(idIndex + ID_MARKER.length(), typeIndex).trim();
        String type = addDetails.substring(typeIndex + TYPE_MARKER.length(), conditionIndex).trim();
        String condition = addDetails.substring(conditionIndex + CONDITION_MARKER.length()).trim();

        return createValidatedEquipment(itemName, id, type, condition);
    }

    /** Finds the last ID prefix before the type field, allowing earlier id/card-like name text. */
    private static int findIdFieldMarker(String details, int typeIndex) {
        int idIndex = findFieldMarker(details, ID_MARKER, 0);
        int nextIdIndex = findFieldMarker(details, ID_MARKER, idIndex + ID_MARKER.length());
        while (nextIdIndex != -1 && nextIdIndex < typeIndex) {
            idIndex = nextIdIndex;
            nextIdIndex = findFieldMarker(details, ID_MARKER, idIndex + ID_MARKER.length());
        }
        return idIndex;
    }

    /** Rejects missing, reordered, or repeated delimiters, except permitted ID text within the name. */
    private static void validateFieldMarkers(String details, int itemIndex, int idIndex, int typeIndex,
            int conditionIndex) throws EquipmentAdditionUnsuccessful {
        boolean markersAreInOrder = itemIndex == 0
                && idIndex > itemIndex
                && typeIndex > idIndex
                && conditionIndex > typeIndex;
        if (!markersAreInOrder) {
            throw new EquipmentAdditionUnsuccessful(ADD_FORMAT_ERROR);
        }
        for (String marker : new String[]{ITEM_MARKER, ID_MARKER, TYPE_MARKER, CONDITION_MARKER}) {
            int firstIndex = findFieldMarker(details, marker, 0);
            if (marker.equals(ID_MARKER)) {
                while (firstIndex < idIndex) {
                    if (!isNameIdText(details, firstIndex)) {
                        throw new EquipmentAdditionUnsuccessful(ADD_FORMAT_ERROR);
                    }
                    firstIndex = findFieldMarker(details, marker, firstIndex + marker.length());
                }
            }
            if (findFieldMarker(details, marker, firstIndex + marker.length()) != -1) {
                throw new EquipmentAdditionUnsuccessful(ADD_FORMAT_ERROR);
            }
        }
    }

    /** Validates extracted values before construction, preserving field-specific error messages. */
    private static Equipment createValidatedEquipment(String itemName, String id, String type,
            String conditionString) throws EquipmentAdditionUnsuccessful {
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

        int condition = Integer.parseInt(conditionString);

        return new Equipment(itemName, id, type, condition);
    }

    /**
     * Finds a field prefix at the start or after whitespace, ignoring prefix-like text inside words.
     * The starting index also allows callers to detect repeated prefixes.
     */
    private static int findFieldMarker(String details, String marker, int startIndex) {
        int index = details.indexOf(marker, startIndex);
        while (index != -1) {
            if (index == 0 || Character.isWhitespace(details.charAt(index - 1))) {
                return index;
            }
            index = details.indexOf(marker, index + marker.length());
        }
        return -1;
    }

    /** Allows id/card-like name text, reserving empty, numeric, and signed ID values for command syntax. */
    private static boolean isNameIdText(String details, int markerIndex) {
        int valueIndex = markerIndex + ID_MARKER.length();
        if (valueIndex >= details.length()) {
            return false;
        }
        char firstCharacter = details.charAt(valueIndex);
        return !Character.isWhitespace(firstCharacter) && !Character.isDigit(firstCharacter)
                && firstCharacter != '+' && firstCharacter != '-';
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
