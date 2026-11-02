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

import seedu.snap.Identifier;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;
import seedu.snap.exceptions.EquipmentViewingUnsuccessful;
import seedu.snap.ui.EquipmentView;

/** Handles adding, updating, deleting, and displaying equipment, including filtered and persistent records. */
public class EquipmentHandler {
    /** Diagnostic logger; the application sends outcomes and detailed traces to local files. */
    private static final Logger logger = Logger.getLogger(EquipmentHandler.class.getName());
    private static final String ADD_COMMAND = "eqm-add";
    private static final String ADD_FORMAT_ERROR = "Invalid format. Use: eqm-add name/[ITEM] e/[ITEM ID] "
            + "type/[TYPE] cond/[CONDITION]";
    private static final String LEGACY_ADD_COMMAND = "add";
    private static final String LEGACY_ITEM_MARKER = "i/";
    private static final String LEGACY_ID_MARKER = "id/";
    private static final String LEGACY_ADD_FORMAT_ERROR = "Invalid format. Use: add i/[ITEM] id/[ITEM ID] "
            + "type/[TYPE] cond/[CONDITION]";
    private static final Pattern DELETE_PATTERN = Pattern.compile("eqm-delete\\s+e/([Ee][0-9]{3})");
    private static final Pattern SHOW_PATTERN = Pattern.compile("(?U)eqm-show\\s+e/([Ee][0-9]{3})");
    private static final String ITEM_MARKER = "name/";
    private static final String ID_MARKER = "e/";
    private static final String TYPE_MARKER = "type/";
    private static final String CONDITION_MARKER = "cond/";
    private static final String[] DETAIL_MARKERS =
            Arrays.stream(EquipmentDetailField.values())
                    .map(EquipmentDetailField::getMarker)
                    .toArray(String[]::new);
    private static final String REMARKS_MARKER = "remarks/";

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

    // @@author rktheg
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
    // @@author

    /**
     * Returns the inventory for listing or inspecting stored equipment.
     *
     * @return an unmodifiable view of the inventory
     */
    public List<Equipment> getEquipmentList() {
        return Collections.unmodifiableList(inventory);
    }

    /** Returns the backing file, or null for an inventory held only in memory. */
    public Path getStorageFile() {
        return storageFile;
    }

    /**
     * Validates a return assessment and creates a detached proposal of the same equipment subtype.
     * Existing equipment and its active loan references remain unchanged until the return is saved.
     *
     * @param current the equipment being returned.
     * @param serviceable whether the equipment can be loaned again.
     * @param damage a damage value supported by the equipment subtype.
     * @param remarks single-line assessment remarks, up to 200 characters; required for OTHER damage.
     * @return the validated detached equipment proposal.
     * @throws IllegalArgumentException if any assessment field is invalid.
     */
    public Equipment prepareReturnAssessment(Equipment current, boolean serviceable, String damage, String remarks) {
        if (current == null || damage == null) {
            throw new IllegalArgumentException("Equipment and return damage must be specified.");
        }
        try {
            if (remarks == null || remarks.matches("(?s).*\\R.*")) {
                throw new EquipmentAdditionUnsuccessful(
                        "Remarks must contain at most 200 characters and no line breaks.");
            }
            String normalisedRemarks = EquipmentFields.trimWhitespace(remarks);
            validateRemarks(normalisedRemarks);
            String value = EquipmentFields.trimWhitespace(damage);
            String name = current.getItemName();
            String id = current.getItemId();
            int condition = current.getCondition();
            Equipment proposed = switch (current) {
            case Camera camera -> new Camera(
                    name,
                    id,
                    condition,
                    camera.getSensorFormat(),
                    EquipmentDetailParser.parseEnumValue(
                            value,
                            EquipmentDetailField.DAMAGE,
                            Camera.CameraDamage.class,
                            current.getType()),
                    normalisedRemarks,
                    camera.getLensMount(),
                    camera.getTripodThread(),
                    camera.getBatteryType());
            case Lens lens -> new Lens(
                    name,
                    id,
                    condition,
                    lens.getZoomRange(),
                    EquipmentDetailParser.parseEnumValue(
                            value,
                            EquipmentDetailField.DAMAGE,
                            Lens.LensDamage.class,
                            current.getType()),
                    normalisedRemarks,
                    lens.getLensMount(),
                    lens.getSensorFormat());
            case Battery battery -> new Battery(
                    name,
                    id,
                    condition,
                    battery.getCapacity(),
                    EquipmentDetailParser.parseEnumValue(
                            value,
                            EquipmentDetailField.DAMAGE,
                            Battery.BatteryDamage.class,
                            current.getType()),
                    normalisedRemarks,
                    battery.getBatteryType());
            case Tripod tripod -> new Tripod(
                    name,
                    id,
                    condition,
                    tripod.getThread(),
                    EquipmentDetailParser.parseEnumValue(
                            value,
                            EquipmentDetailField.DAMAGE,
                            Tripod.TripodDamage.class,
                            current.getType()),
                    normalisedRemarks);
            default -> throw new IllegalArgumentException("Unsupported equipment type for return assessment.");
            };
            Enum<?> assessedDamage = switch (proposed) {
            case Camera camera -> camera.getDamage();
            case Lens lens -> lens.getDamage();
            case Battery battery -> battery.getDamage();
            case Tripod tripod -> tripod.getDamage();
            default -> throw new IllegalArgumentException("Unsupported equipment type for return assessment.");
            };
            EquipmentDetailParser.validateDamageRemarks(
                    assessedDamage, normalisedRemarks, current.getType());
            proposed.restoreRecordId(current.getRecordId());
            proposed.setServiceable(serviceable);
            return proposed;
        } catch (EquipmentAdditionUnsuccessful exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    /**
     * Saves a proposed assessment without changing the inventory or the equipment object.
     * The caller commits the proposal only after every file involved in the return has been saved.
     *
     * @param current original equipment present in this inventory
     * @param assessed validated detached proposal
     * @throws IOException if the proposed equipment records cannot be saved
     * @throws IllegalArgumentException if the equipment is missing or the proposal belongs to another item
     */
    public void saveReturnAssessment(Equipment current, Equipment assessed) throws IOException {
        int index = inventory.indexOf(current);
        if (index < 0 || assessed == null || current.getClass() != assessed.getClass()
                || !current.getItemId().equals(assessed.getItemId())
                || !current.getRecordId().equals(assessed.getRecordId())) {
            throw new IllegalArgumentException("The return assessment must belong to equipment in the inventory.");
        }
        ArrayList<Equipment> proposedInventory = new ArrayList<>(inventory);
        proposedInventory.set(index, assessed);
        save(proposedInventory);
    }

    /** Persists the current inventory when this handler has a backing file. */
    public void saveToStorage() throws IOException {
        save(inventory);
    }

    /**
     * Finds equipment by its item ID.
     *
     * @param itemId typed equipment ID
     * @return the matching equipment, or null when no match exists
     */
    public Equipment getEquipmentById(String itemId) {
        String canonicalItemId;
        try {
            canonicalItemId = normalizeItemId(itemId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        for (Equipment equipment : inventory) {
            if (equipment.getItemId().equals(canonicalItemId)) {
                logger.log(Level.INFO, "Found {0} with item ID {1}.",
                        new Object[] {equipment.getType(), canonicalItemId});
                return equipment;
            }
        }
        logger.log(Level.WARNING, "No equipment found for item ID {0}.", canonicalItemId);
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
        return parseEquipmentRecord(userInput, ADD_COMMAND, ITEM_MARKER, ID_MARKER, ADD_FORMAT_ERROR);
    }

    /** Parses the legacy command-style representation used only by existing saved equipment files. */
    static Equipment parseStoredEquipment(String userInput) throws EquipmentAdditionUnsuccessful {
        return parseEquipmentRecord(userInput, LEGACY_ADD_COMMAND, LEGACY_ITEM_MARKER, LEGACY_ID_MARKER,
                LEGACY_ADD_FORMAT_ERROR);
    }

    /** Parses an equipment record using either the interactive or legacy storage syntax. */
    private static Equipment parseEquipmentRecord(String userInput, String command, String itemMarker,
            String idMarker, String formatError) throws EquipmentAdditionUnsuccessful {
        if (userInput == null || !userInput.matches("(?sU)" + command + "(?:\\s.*)?")) {
            throw new EquipmentAdditionUnsuccessful(formatError);
        }
        String addDetails = EquipmentFields.trimWhitespace(userInput.substring(command.length()));
        int itemIndex = findFieldMarker(addDetails, itemMarker, 0);
        int typeIndex = findFieldMarker(addDetails, TYPE_MARKER, 0);
        int conditionIndex = findFieldMarker(addDetails, CONDITION_MARKER, 0);
        int idIndex = findIdFieldMarker(addDetails, typeIndex, idMarker);
        int conditionValueStart = conditionIndex + CONDITION_MARKER.length();
        int remarksIndex = conditionIndex == -1 ? -1 : findFieldMarker(addDetails, REMARKS_MARKER, conditionValueStart);
        // The final remarks field is free text, including literal strings such as id/E001 and type/camera.
        String structuralDetails = remarksIndex == -1 ? addDetails : addDetails.substring(0, remarksIndex);
        validateFieldMarkers(structuralDetails, itemIndex, idIndex, typeIndex, conditionIndex,
                itemMarker, idMarker, formatError);

        String itemName = EquipmentFields.trimWhitespace(
                addDetails.substring(itemIndex + itemMarker.length(), idIndex));
        String id = EquipmentFields.trimWhitespace(addDetails.substring(idIndex + idMarker.length(), typeIndex));
        try {
            id = normalizeItemId(id);
        } catch (IllegalArgumentException exception) {
            throw new EquipmentAdditionUnsuccessful("Item ID must be from E001 to E999.");
        }
        String type = EquipmentFields.trimWhitespace(
                addDetails.substring(typeIndex + TYPE_MARKER.length(), conditionIndex));
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
    private static int findIdFieldMarker(String details, int typeIndex, String idMarker) {
        int idIndex = findFieldMarker(details, idMarker, 0);
        int nextIdIndex = findFieldMarker(details, idMarker, idIndex + idMarker.length());
        while (nextIdIndex != -1 && nextIdIndex < typeIndex) {
            idIndex = nextIdIndex;
            nextIdIndex = findFieldMarker(details, idMarker, idIndex + idMarker.length());
        }
        return idIndex;
    }

    /** Rejects missing, reordered, or repeated delimiters, except permitted ID text within the name. */
    private static void validateFieldMarkers(String details, int itemIndex, int idIndex, int typeIndex,
            int conditionIndex, String itemMarker, String idMarker, String formatError)
            throws EquipmentAdditionUnsuccessful {
        boolean markersAreInOrder = itemIndex == 0
                && idIndex > itemIndex
                && typeIndex > idIndex
                && conditionIndex > typeIndex;
        if (!markersAreInOrder) {
            throw new EquipmentAdditionUnsuccessful(formatError);
        }
        for (String marker : new String[]{itemMarker, idMarker, TYPE_MARKER, CONDITION_MARKER}) {
            int firstIndex = findFieldMarker(details, marker, 0);
            if (marker.equals(idMarker)) {
                firstIndex = skipNameIdFragments(details, firstIndex, idIndex, idMarker, formatError);
            }
            if (findFieldMarker(details, marker, firstIndex + marker.length()) != -1) {
                throw new EquipmentAdditionUnsuccessful(formatError);
            }
        }
    }

    /** Validates extracted values and creates the correct equipment subclass. */
    private static Equipment createValidatedEquipment(
            String itemName,
            String id,
            String type,
            String conditionString,
            String detailsString,
            String remarks)
            throws EquipmentAdditionUnsuccessful {
        validateEquipmentDetails(
                itemName,
                id,
                type,
                conditionString);
        validateRemarks(remarks);

        int condition = Integer.parseInt(conditionString);
        String normalisedType = type.toLowerCase(Locale.ROOT);

        if (normalisedType.equals("camera")) {
            return CameraDetailsParser.createCamera(
                    itemName,
                    id,
                    condition,
                    detailsString,
                    remarks);
        }

        if (normalisedType.equals("lens")) {
            return LensDetailsParser.createLens(
                    itemName,
                    id,
                    condition,
                    detailsString,
                    remarks);
        }

        if (normalisedType.equals("battery")) {
            return BatteryDetailsParser.createBattery(
                    itemName,
                    id,
                    condition,
                    detailsString,
                    remarks);
        }

        if (normalisedType.equals("tripod")) {
            return TripodDetailsParser.createTripod(
                    itemName,
                    id,
                    condition,
                    detailsString,
                    remarks);
        }

        throw new EquipmentAdditionUnsuccessful(
                "Unsupported equipment type. Use: "
                        + "camera, lens, battery or tripod.");
    }

    /** Shares value rules between addition and updates before either changes the inventory. */
    private static void validateEquipmentDetails(String itemName, String id, String type, String conditionString)
            throws EquipmentAdditionUnsuccessful {
        if (!Equipment.hasText(itemName) || itemName.length() > Equipment.MAX_NAME_LENGTH) {
            throw new EquipmentAdditionUnsuccessful("Item name must contain 1 to 100 characters.");
        }
        if (!Equipment.isValidItemId(id)) {
            throw new EquipmentAdditionUnsuccessful("Item ID must be from E001 to E999.");
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
                || remarks.matches("(?s).*\\R.*")) {
            throw new EquipmentAdditionUnsuccessful(
                    "Remarks must contain at most 200 characters and no line breaks.");
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
    private static boolean isNameIdText(String details, int markerIndex, String idMarker) {
        int valueIndex = markerIndex + idMarker.length();
        if (valueIndex >= details.length()) {
            return false;
        }
        char firstCharacter = details.charAt(valueIndex);
        return !EquipmentFields.isWhitespace(firstCharacter) && !Character.isDigit(firstCharacter)
                && firstCharacter != '+' && firstCharacter != '-'
                && !EquipmentFields.isTypedItemId(details, valueIndex);
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
        if (matcher == null || !matcher.matches()) {
            logger.fine("Equipment deletion rejected: invalid command syntax.");
            throw new EquipmentDeletionUnsuccessful(
                    "Invalid delete syntax. ITEM ID must be from E001 to E999.");
        }
        try {
            return normalizeItemId(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            logger.fine("Equipment deletion rejected: invalid item ID.");
            throw new EquipmentDeletionUnsuccessful(
                    "Invalid delete syntax. ITEM ID must be from E001 to E999.");
        }
    }

    /**
     * Prints out all equipment in inventory
     */
    public void viewAllEquipment() {
        EquipmentView.printInventory(getEquipmentList());
        logger.log(Level.INFO, "Viewing {0} equipment item(s)", inventory.size());
    }

    /** Prints matching items in their existing order using the normal equipment table. */
    public void viewEquipment(List<Equipment> equipmentList) {
        EquipmentView.printMatches(equipmentList);
        logger.log(Level.INFO, "Viewing {0} equipment item(s)", equipmentList.size());
    }

    /** Validates an exact item-detail command and prints full values without saving or changing records. */
    public void showEquipment(String input) throws EquipmentViewingUnsuccessful {
        Matcher matcher = input == null ? null : SHOW_PATTERN.matcher(EquipmentFields.trimWhitespace(input));
        if (matcher == null || !matcher.matches()) {
            throw new EquipmentViewingUnsuccessful("Invalid format. Use: eqm-show e/[ITEM ID], from E001 to E999.");
        }
        String itemId;
        try {
            itemId = normalizeItemId(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            throw new EquipmentViewingUnsuccessful("Invalid format. Use: eqm-show e/[ITEM ID], from E001 to E999.");
        }
        Equipment item = getEquipmentById(itemId);
        if (item == null) {
            throw new EquipmentViewingUnsuccessful("Item ID " + itemId + " does not exist.");
        }
        EquipmentView.printDetails(item);
    }

    // @@author rktheg
    /** Rejects duplicate IDs before saving or changing the current inventory. */
    private void ensureUniqueItemId(Equipment item) throws EquipmentAdditionUnsuccessful {
        for (Equipment existing : inventory) {
            if (existing.getItemId().equals(item.getItemId())) {
                throw new EquipmentAdditionUnsuccessful("Item ID " + item.getItemId() + " already exists.");
            }
        }
    }
    // @@author

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
    private static int skipNameIdFragments(String details, int firstIndex, int idIndex, String idMarker,
            String formatError)
            throws EquipmentAdditionUnsuccessful {
        int currentIndex = firstIndex;
        while (currentIndex < idIndex) {
            if (!isNameIdText(details, currentIndex, idMarker)) {
                throw new EquipmentAdditionUnsuccessful(formatError);
            }
            currentIndex = findFieldMarker(details, idMarker, currentIndex + idMarker.length());
        }
        return currentIndex;
    }

    /** Updates supplied details together while preserving identity and inventory order. */
    public Equipment updateEquipment(String input) throws EquipmentUpdateUnsuccessful {
        try {
            Map<String, String> fields = EquipmentFields.parse(input, "eqm-update", EquipmentFields.ID_FIELD,
                    EquipmentFields.NAME_FIELD, EquipmentFields.TYPE_FIELD,
                    EquipmentFields.CONDITION_FIELD);
            String id = fields.get(EquipmentFields.ID_FIELD);
            fields.put(EquipmentFields.ID_FIELD, normalizeItemId(id));
            id = fields.get(EquipmentFields.ID_FIELD);
            if (!Equipment.isValidItemId(id)) {
                throw new EquipmentUpdateUnsuccessful("Item ID must be from E001 to E999.");
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
        proposed.restoreRecordId(target.getRecordId());
        proposed.setServiceable(target.isServiceable());
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

    /** Converts a user-supplied equipment ID to the uppercase value used by the model. */
    private static String normalizeItemId(String itemId) {
        try {
            return Identifier.normalize(itemId, Identifier.Type.EQUIPMENT);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Item ID must be from E001 to E999.", exception);
        }
    }
}
