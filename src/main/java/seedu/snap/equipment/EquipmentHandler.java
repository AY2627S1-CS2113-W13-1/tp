package seedu.snap.equipment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;

/** Handles adding, updating, deleting, and displaying equipment, including filtered and persistent records. */
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
    private static final String DAMAGE_MARKER = "damage/";
    private static final String[] DETAIL_MARKERS = {"sensor/", "zoom/", "capacity/", "thread/", DAMAGE_MARKER};
    private static final String REMARKS_MARKER = "remarks/";

    /** Optional command fields after the common equipment fields. */
    private record EquipmentDetails(String attribute, String damage) {
    }

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
            ensureUniqueItemId(equipment);
            saveAddition(equipment);
        } catch (EquipmentAdditionUnsuccessful e) {
            assert inventory.size() == previousSize : "Rejected additions must not change inventory size";
            logger.log(Level.FINE, "Equipment addition rejected: {0}", e.getMessage());
            throw e;
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
     * Finds equipment by its item ID.
     *
     * @param itemId three-digit equipment ID
     * @return the matching equipment, or null when no match exists
     */
    public Equipment getEquipmentById(String itemId) {
        for (Equipment equipment : inventory) {
            if (equipment.getItemId().equals(itemId)) {
                logger.log(Level.INFO, "Found {0} with item ID {1}.",
                        new Object[] {equipment.getType(), itemId});
                return equipment;
            }
        }
        logger.log(Level.WARNING, "No equipment found for item ID {0}.", itemId);
        return null;
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
        if (equipment.isLoaned()) {
            throw new EquipmentDeletionUnsuccessful("Cannot delete item ID " + equipment.getItemId()
                    + " while it has an active loan.");
        }
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
        if (userInput == null || !userInput.matches("(?sU)" + ADD_COMMAND + "(?:\\s.*)?")) {
            throw new EquipmentAdditionUnsuccessful(ADD_FORMAT_ERROR);
        }
        String addDetails = EquipmentFields.trimWhitespace(userInput.substring(ADD_COMMAND.length()));
        int itemIndex = findFieldMarker(addDetails, ITEM_MARKER, 0);
        int typeIndex = findFieldMarker(addDetails, TYPE_MARKER, 0);
        int conditionIndex = findFieldMarker(addDetails, CONDITION_MARKER, 0);
        int idIndex = findIdFieldMarker(addDetails, typeIndex);
        validateFieldMarkers(addDetails, itemIndex, idIndex, typeIndex, conditionIndex);

        String itemName = EquipmentFields.trimWhitespace(
                addDetails.substring(itemIndex + ITEM_MARKER.length(), idIndex));
        String id = EquipmentFields.trimWhitespace(addDetails.substring(idIndex + ID_MARKER.length(), typeIndex));
        String type = EquipmentFields.trimWhitespace(
                addDetails.substring(typeIndex + TYPE_MARKER.length(), conditionIndex));
        int conditionValueStart = conditionIndex + CONDITION_MARKER.length();
        int remarksIndex = findFieldMarker(addDetails, REMARKS_MARKER, conditionValueStart);
        int detailsIndex = findDetailsStart(addDetails, conditionValueStart, remarksIndex);
        if (remarksIndex != -1 && detailsIndex != -1 && remarksIndex < detailsIndex) {
            throw new EquipmentAdditionUnsuccessful(
                    "Remarks must appear after optional equipment details.");
        }
        int conditionEnd = firstFieldIndex(detailsIndex, remarksIndex, addDetails.length());
        String condition = EquipmentFields.trimWhitespace(
                addDetails.substring(conditionIndex + CONDITION_MARKER.length(), conditionEnd));
        int detailsEnd = remarksIndex == -1 ? addDetails.length() : remarksIndex;
        String details = detailsIndex == -1
                ? ""
                : EquipmentFields.trimWhitespace(addDetails.substring(detailsIndex, detailsEnd));
        String remarks = remarksIndex == -1
                ? ""
                : EquipmentFields.trimWhitespace(addDetails.substring(remarksIndex + REMARKS_MARKER.length()));

        return createValidatedEquipment(itemName, id, type, condition, details, remarks);
    }

    /** Finds optional fields after the condition, leaving existing names containing these prefixes valid. */
    private static int findDetailsStart(String details, int startIndex, int endIndex) {
        int firstIndex = -1;
        for (String marker : DETAIL_MARKERS) {
            int index = findFieldMarker(details, marker, startIndex);
            if (index != -1 && endIndex != -1 && index >= endIndex) {
                index = -1;
            }
            if (index != -1 && (firstIndex == -1 || index < firstIndex)) {
                firstIndex = index;
            }
        }
        return firstIndex;
    }

    /** Returns the earliest optional-field index, or the supplied end when neither field exists. */
    private static int firstFieldIndex(int firstIndex, int secondIndex, int endIndex) {
        if (firstIndex == -1) {
            return secondIndex == -1 ? endIndex : secondIndex;
        }
        if (secondIndex == -1) {
            return firstIndex;
        }
        return Math.min(firstIndex, secondIndex);
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
                firstIndex = skipNameIdFragments(details, firstIndex, idIndex);
            }
            if (findFieldMarker(details, marker, firstIndex + marker.length()) != -1) {
                throw new EquipmentAdditionUnsuccessful(ADD_FORMAT_ERROR);
            }
        }
    }

    /** Validates extracted values before construction, preserving field-specific error messages. */
    private static Equipment createValidatedEquipment(String itemName, String id, String type,
            String conditionString, String detailsString, String remarks) throws EquipmentAdditionUnsuccessful {
        validateEquipmentDetails(itemName, id, type, conditionString);
        validateRemarks(remarks);
        int condition = Integer.parseInt(conditionString);

        String normalisedType = type.toLowerCase(Locale.ROOT);
        String attributeMarker = switch (normalisedType) {
        case "camera" -> "sensor/";
        case "lens" -> "zoom/";
        case "battery" -> "capacity/";
        case "tripod" -> "thread/";
        default -> throw new EquipmentAdditionUnsuccessful(
                "Unsupported equipment type. Use: camera, lens, battery or tripod.");
        };
        EquipmentDetails details = parseDetails(detailsString, attributeMarker);
        return switch (normalisedType) {
        case "camera" -> new Camera(itemName, id, condition, details.attribute(),
                parseDamage(details.damage(), Camera.CameraDamage.class, normalisedType), remarks);
        case "lens" -> new Lens(itemName, id, condition, details.attribute(),
                parseDamage(details.damage(), Lens.LensDamage.class, normalisedType), remarks);
        case "battery" -> new Battery(itemName, id, condition, details.attribute(),
                parseDamage(details.damage(), Battery.BatteryDamage.class, normalisedType), remarks);
        case "tripod" -> new Tripod(itemName, id, condition, details.attribute(),
                parseDamage(details.damage(), Tripod.TripodDamage.class, normalisedType), remarks);
        default -> throw new AssertionError("Validated equipment type must have a subclass");
        };
    }

    /** Shares value rules between addition and updates before either changes the inventory. */
    private static void validateEquipmentDetails(String itemName, String id, String type, String conditionString)
            throws EquipmentAdditionUnsuccessful {
        if (!Equipment.hasText(itemName) || itemName.length() > Equipment.MAX_NAME_LENGTH) {
            throw new EquipmentAdditionUnsuccessful("Item name must contain 1 to 100 characters.");
        }
        if (!Equipment.isValidItemId(id)) {
            throw new EquipmentAdditionUnsuccessful("Item ID must be from 001 to 999.");
        }
        if (!Equipment.hasText(type)) {
            throw new EquipmentAdditionUnsuccessful("Type cannot be empty.");
        }
        if (!conditionString.matches("10|[1-9]")) {
            throw new EquipmentAdditionUnsuccessful("Condition must be a whole number from 1 to 10.");
        }
    }

    /** Validates remarks before they are stored in an equipment object or written to disk. */
    private static void validateRemarks(String remarks) throws EquipmentAdditionUnsuccessful {
        if (remarks == null || remarks.length() > Equipment.MAX_REMARKS_LENGTH
                || remarks.contains("\n") || remarks.contains("\r")) {
            throw new EquipmentAdditionUnsuccessful(
                    "Remarks must contain at most 200 characters and no line breaks.");
        }
    }

    /** Rejects repeated, reordered or inapplicable optional fields before extracting their values. */
    private static EquipmentDetails parseDetails(String details, String attributeMarker)
            throws EquipmentAdditionUnsuccessful {
        if (details.isEmpty()) {
            return new EquipmentDetails("unspecified", "NONE");
        }
        int attributeIndex = findFieldMarker(details, attributeMarker, 0);
        int damageIndex = findFieldMarker(details, DAMAGE_MARKER, 0);
        boolean attributeFirst = attributeIndex == 0 && (damageIndex == -1 || damageIndex > attributeIndex);
        boolean damageOnly = attributeIndex == -1 && damageIndex == 0;
        if (!attributeFirst && !damageOnly) {
            throw new EquipmentAdditionUnsuccessful("Optional fields must use " + attributeMarker
                    + "[DETAIL] followed by damage/[DAMAGE], with either field optional.");
        }
        for (String marker : DETAIL_MARKERS) {
            int index = findFieldMarker(details, marker, 0);
            if (index != -1 && (!marker.equals(attributeMarker) && !marker.equals(DAMAGE_MARKER)
                    || findFieldMarker(details, marker, index + marker.length()) != -1)) {
                throw new EquipmentAdditionUnsuccessful("Repeated or inapplicable equipment detail field: " + marker);
            }
        }
        String attribute = "unspecified";
        if (attributeFirst) {
            int attributeEnd = damageIndex == -1 ? details.length() : damageIndex;
            attribute = details.substring(attributeMarker.length(), attributeEnd).trim();
            if (attribute.isBlank() || attribute.length() > 100 || attribute.matches("(?s).*\\R.*")) {
                throw new EquipmentAdditionUnsuccessful(
                        "Equipment detail must contain 1 to 100 characters on one line.");
            }
        }
        String damage = damageIndex == -1 ? "NONE" : details.substring(damageIndex + DAMAGE_MARKER.length()).trim();
        return new EquipmentDetails(attribute, damage);
    }

    /** Converts the command value to the damage enum belonging to the selected equipment type. */
    private static <T extends Enum<T>> T parseDamage(String value, Class<T> damageClass, String type)
            throws EquipmentAdditionUnsuccessful {
        try {
            return Enum.valueOf(damageClass, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful("Invalid damage for " + type + ". Use: "
                    + Arrays.toString(damageClass.getEnumConstants()) + ".");
        }
    }

    /**
     * Finds a field prefix at the start or after whitespace, ignoring prefix-like text inside words.
     * The starting index also allows callers to detect repeated prefixes.
     */
    private static int findFieldMarker(String details, String marker, int startIndex) {
        int index = details.indexOf(marker, startIndex);
        while (index != -1) {
            if (index == 0 || EquipmentFields.isWhitespace(details.charAt(index - 1))) {
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
        return !EquipmentFields.isWhitespace(firstCharacter) && !Character.isDigit(firstCharacter)
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
        viewEquipment(getEquipmentList());
    }

    /** Prints matching items in their existing order using the normal equipment table. */
    public void viewEquipment(List<Equipment> equipmentList) {
        System.out.printf("%-4s %-24s | %-6s | %-15s | %-9s | %-10s | %-30s | %-30s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION", "STATUS", "DETAILS", "REMARKS");
        logger.log(Level.INFO, "Viewing {0} equipment item(s)", equipmentList.size());
        for (int index = 0; index < equipmentList.size(); index++) {
            Equipment item = equipmentList.get(index);
            assert item != null : "Equipment list must not contain null items";
            System.out.printf("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                    (index + 1) + ")", item.getItemName(), item.getItemId(), item.getType(), item.getCondition(),
                    item.isLoaned() ? "Loaned" : "Available", item.getDetails(), item.getRemarks());
        }
    }

    /** Rejects duplicate IDs before saving or changing the current inventory. */
    private void ensureUniqueItemId(Equipment item) throws EquipmentAdditionUnsuccessful {
        for (Equipment existing : inventory) {
            if (existing.getItemId().equals(item.getItemId())) {
                throw new EquipmentAdditionUnsuccessful("Item ID " + item.getItemId() + " already exists.");
            }
        }
    }

    /** Saves a candidate list first, so a failed write cannot add an item only in memory. */
    private void saveAddition(Equipment item) throws EquipmentAdditionUnsuccessful {
        // @@author MatthewLeon888
        ArrayList<Equipment> updatedInventory = new ArrayList<>(inventory);
        updatedInventory.add(item);
        try {
            save(updatedInventory);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Equipment addition could not be saved", e);
            throw new EquipmentAdditionUnsuccessful("Could not save equipment: " + e.getMessage());
        }
        // @@author
    }

    /** Checks that prefixes before the actual ID belong to textual name fragments. */
    private static int skipNameIdFragments(String details, int firstIndex, int idIndex)
            throws EquipmentAdditionUnsuccessful {
        int currentIndex = firstIndex;
        while (currentIndex < idIndex) {
            if (!isNameIdText(details, currentIndex)) {
                throw new EquipmentAdditionUnsuccessful(ADD_FORMAT_ERROR);
            }
            currentIndex = findFieldMarker(details, ID_MARKER, currentIndex + ID_MARKER.length());
        }
        return currentIndex;
    }

    /** Updates supplied details together while preserving identity and inventory order. */
    public Equipment updateEquipment(String input) throws EquipmentUpdateUnsuccessful {
        try {
            Map<String, String> fields = EquipmentFields.parse(input, "update", EquipmentFields.ID_FIELD,
                    EquipmentFields.NAME_FIELD, EquipmentFields.TYPE_FIELD,
                    EquipmentFields.CONDITION_FIELD);
            String id = fields.get(EquipmentFields.ID_FIELD);
            if (!Equipment.isValidItemId(id)) {
                throw new EquipmentUpdateUnsuccessful("Item ID must be from 001 to 999.");
            }
            if (fields.size() == 1) {
                throw new EquipmentUpdateUnsuccessful("Supply a name, type, or condition to update.");
            }
            Equipment target = findEquipmentForUpdate(id);
            return applyValidatedUpdate(target, fields);
        } catch (IllegalArgumentException | EquipmentAdditionUnsuccessful | EquipmentUpdateUnsuccessful e) {
            logger.log(Level.FINE, "Equipment update rejected: {0}", e.getMessage());
            throw new EquipmentUpdateUnsuccessful(e.getMessage());
        }
    }

    /** Finds exactly one item before applying an update. */
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

    /** Saves a copy of the same subclass and its details before changing the existing object. */
    private void saveUpdate(Equipment target, String name, int condition)
            throws EquipmentAdditionUnsuccessful, EquipmentUpdateUnsuccessful {
        Equipment proposed = createValidatedEquipment(name, target.getItemId(), target.getType(),
                Integer.toString(condition), target.getDetails(), target.getRemarks());
        ArrayList<Equipment> updatedInventory = new ArrayList<>(inventory);
        updatedInventory.set(inventory.indexOf(target), proposed);
        try {
            save(updatedInventory);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Equipment update could not be saved", e);
            throw new EquipmentUpdateUnsuccessful("Could not save equipment update: " + e.getMessage());
        }
    }

    /** Validates all values, rejects a different type, and saves before changing the existing object. */
    private Equipment applyValidatedUpdate(Equipment target, Map<String, String> fields)
            throws EquipmentAdditionUnsuccessful, EquipmentUpdateUnsuccessful {
        String name = fields.getOrDefault(EquipmentFields.NAME_FIELD, target.getItemName());
        String type = fields.getOrDefault(EquipmentFields.TYPE_FIELD, target.getType());
        String condition = fields.getOrDefault(EquipmentFields.CONDITION_FIELD,
                Integer.toString(target.getCondition()));
        validateEquipmentDetails(name, target.getItemId(), type, condition);
        if (!type.equalsIgnoreCase(target.getType())) {
            throw new EquipmentUpdateUnsuccessful("Equipment type cannot be changed. Item ID "
                    + target.getItemId() + " must remain " + target.getType() + ".");
        }
        int previousSize = inventory.size();
        int parsedCondition = Integer.parseInt(condition);
        saveUpdate(target, name, parsedCondition);
        target.updateDetails(name, parsedCondition);
        assert inventory.size() == previousSize : "Updates must not add or remove equipment";
        assert target.getItemId().equals(fields.get(EquipmentFields.ID_FIELD))
                : "Updates must preserve Item ID";
        logger.log(Level.FINE, "Updated equipment with item ID {0}", target.getItemId());
        return target;
    }
}
