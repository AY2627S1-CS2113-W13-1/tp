// @@author yarnieee
package seedu.snap.bundle;

// @@author
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
// @@author yarnieee
import java.util.ArrayList;
import java.util.Collections;
// @@author
import java.util.Comparator;
// @@author yarnieee
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.Identifier;
// @@author
import seedu.snap.equipment.CompatibilityResult;
// @@author yarnieee
import seedu.snap.equipment.Equipment;
// @@author
import seedu.snap.equipment.EquipmentCompatibility;
// @@author yarnieee
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;
// @@author
import seedu.snap.exceptions.BundleHistoryUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;
import seedu.snap.ui.BundleView;

/** Parses bundle commands and stores bundles for the current session and saved data. */
// @@author yarnieee
public class BundleHandler {
    private static final String ADD_BUNDLE_COMMAND = "bundle-create";
    private static final String ADD_TO_BUNDLE_COMMAND = "bundle-add-eqm";
    private static final String REMOVE_FROM_BUNDLE_COMMAND = "bundle-remove-eqm";
    private static final String LIST_BUNDLES_COMMAND = "bundle-list";
    private static final String HISTORY_COMMAND_PREFIX = "bundle-history ";
    private static final String NAME_MARKER = "name/";
    private static final String ID_MARKER = "b/";
    private static final Logger LOGGER = Logger.getLogger(BundleHandler.class.getName());

    private final ArrayList<Bundle> bundles = new ArrayList<>();
    private final EquipmentHandler equipmentHandler;
    // @@author
    private final Path storageFile;
    // @@author yarnieee
    private int nextBundleNumber = 1;
    private boolean needsMigrationSave;

    /** Creates a bundle handler with its own equipment inventory. */
    public BundleHandler() {
        this(new EquipmentHandler());
    }

    /**
     * Creates a bundle handler using the supplied equipment inventory.
     *
     * @param equipmentHandler inventory containing equipment available for bundling
     */
    public BundleHandler(EquipmentHandler equipmentHandler) {
        // @@author
        this(equipmentHandler, null);
    }

    /** Creates a bundle handler using the supplied inventory and optional storage file. */
    public BundleHandler(EquipmentHandler equipmentHandler, Path storageFile) {
        // @@author yarnieee
        if (equipmentHandler == null) {
            throw new IllegalArgumentException("Equipment handler cannot be null.");
        }
        this.equipmentHandler = equipmentHandler;
        // @@author
        this.storageFile = storageFile;
        // @@author yarnieee
    }

    /**
     * Creates a bundle from a bundle-create command.
     *
     * @param input command in the form {@code bundle-create name/[NAME]}
     * @return the created bundle
     * @throws BundleAdditionUnsuccessful when the command is invalid
     */
    public Bundle addBundle(String input) throws BundleAdditionUnsuccessful {
        String name = parseBundleName(input);
        // @@author
        if (bundles.stream().anyMatch(bundle -> bundle.getBundleName().equals(name))) {
            LOGGER.log(Level.WARNING, "Bundle name {0} is already in use.", name);
            throw new BundleAdditionUnsuccessful("Bundle name " + name + " already exists.");
        }
        // @@author yarnieee
        if (nextBundleNumber > 999) {
            throw new BundleAdditionUnsuccessful("No more bundle IDs are available this session.");
        }
        String bundleId = Identifier.format(nextBundleNumber, Identifier.Type.BUNDLE);
        nextBundleNumber++;
        Bundle bundle = new Bundle(name, bundleId);
        bundles.add(bundle);
        assert bundles.contains(bundle) : "Created bundle must be stored.";
        LOGGER.log(Level.INFO, "Added bundle {0} with ID {1}.",
                new Object[] {name, bundleId});
        return bundle;
    }

    /** @return an unmodifiable view of all bundles */
    public List<Bundle> getBundleList() {
        return Collections.unmodifiableList(bundles);
    }

    // @@author
    /** Clears session-only bundles after a restore replaces the equipment objects they reference. */
    public void clearBundles() {
        int previousSize = bundles.size();
        bundles.clear();
        nextBundleNumber = 1;
        assert bundles.isEmpty() : "Clearing bundles must remove every session-only bundle";
        LOGGER.log(Level.INFO, "Cleared {0} session-only bundle(s) after data restore.", previousSize);
    }

    /**
     * Loads saved bundles after equipment and loan associations have been restored.
     *
     * @throws IOException if a saved bundle is malformed or references missing equipment
     */
    public void loadFromStorage() throws IOException {
        if (storageFile == null) {
            return;
        }
        // @@author yarnieee
        BundleStorage.LoadedBundles loadedBundles = BundleStorage.load(storageFile);
        List<BundleStorage.StoredBundle> storedBundles = loadedBundles.bundles();
        // @@author
        ArrayList<Bundle> restoredBundles = new ArrayList<>();
        int highestBundleNumber = 0;
        for (BundleStorage.StoredBundle stored : storedBundles) {
            Bundle bundle = new Bundle(stored.bundleName(), stored.bundleId());
            for (BundleStorage.StoredItem storedItem : stored.items()) {
                Equipment item = findEquipment(storedItem);
                if (item == null) {
                    throw new IOException("Bundle " + stored.bundleId() + " references missing equipment ID "
                            + storedItem.itemId() + ".");
                    // @@author yarnieee
                }
                if (item.getType().equals("camera") && bundle.countType("camera") >= 1) {
                    // @@author
                    throw new IOException("Bundle " + stored.bundleId() + " contains multiple cameras.");
                    // @@author yarnieee
                }
                if (item.getType().equals("tripod") && bundle.countType("tripod") >= 1) {
                    // @@author
                    throw new IOException("Bundle " + stored.bundleId() + " contains multiple tripods.");
                }
                // @@author yarnieee
                bundle.addEquipment(item);
                // @@author
            }
            restoredBundles.add(bundle);
            // @@author yarnieee
            highestBundleNumber = Math.max(highestBundleNumber,
                    Identifier.number(stored.bundleId(), Identifier.Type.BUNDLE));
            // @@author
        }

        bundles.clear();
        bundles.addAll(restoredBundles);
        nextBundleNumber = highestBundleNumber + 1;
        // @@author yarnieee
        needsMigrationSave = loadedBundles.migrated();
        // @@author
        assert bundles.size() == storedBundles.size() : "All saved bundles must be restored";
        LOGGER.log(Level.INFO, "Loaded {0} bundle(s) from {1}",
                new Object[]{bundles.size(), storageFile});
    }

    /** Saves the current bundle definitions when this handler has a backing file. */
    public void saveToStorage() throws IOException {
        // @@author yarnieee
        if (storageFile == null || bundles.isEmpty() && Files.notExists(storageFile) && !needsMigrationSave) {
            // @@author
            return;
        }
        BundleStorage.save(storageFile, bundles);
        // @@author yarnieee
        needsMigrationSave = false;
        // @@author
        LOGGER.log(Level.FINE, "Saved {0} bundle(s) to {1}",
                new Object[]{bundles.size(), storageFile});
    }

    /**
     * Checks all bundles before an inventory item is deleted.
     * Keeping bundled items in the inventory prevents bundle contents and completion from becoming stale.
     *
     * @param itemId equipment ID to find
     * @return true when any bundle contains the equipment ID
     */
    public boolean containsEquipmentId(String itemId) {
        // @@author yarnieee
        for (Bundle bundle : bundles) {
            if (bundle.containsEquipmentId(itemId)) {
                // @@author
                return true;
            }
        }
        return false;
    }

    // @@author yarnieee
    /**
     * Adds every valid equipment ID from a command and reports each result.
     *
     * @param input command in the form {@code bundle-add-eqm b/[BUNDLE ID] [EQUIPMENT ID]...}
     * @return one success or failure message for every requested equipment ID
     * @throws BundleAdditionUnsuccessful when the command syntax is invalid
     */
    public List<String> addItemsToBundle(String input) throws BundleAdditionUnsuccessful {
        // @@author
        return addItemsToBundleWithResults(input).stream().map(ItemResult::message).toList();
    }

    /** Retains the validated outcome and item identity for each requested membership addition. */
    public List<ItemResult> addItemsToBundleWithResults(String input) throws BundleAdditionUnsuccessful {
        // @@author yarnieee
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        if (arguments.length < 3 || !arguments[0].equals(ADD_TO_BUNDLE_COMMAND)) {
            throw new BundleAdditionUnsuccessful(
                    "Invalid format. Use: bundle-add-eqm b/[BUNDLE ID] [EQUIPMENT ID]...");
        }

        String bundleId = parseBundleId(arguments[1]);
        Bundle bundle = findBundle(bundleId);
        if (bundle == null) {
            LOGGER.log(Level.WARNING, "No bundle found for ID {0}.", bundleId);
            throw new BundleAdditionUnsuccessful("Bundle ID " + bundleId + " does not exist.");
        }

        // @@author
        ArrayList<ItemResult> messages = new ArrayList<>();
        // @@author yarnieee
        for (int i = 2; i < arguments.length; i++) {
            messages.add(addItemToBundle(bundle, parseEquipmentId(arguments[i])));
        }
        assert messages.size() == arguments.length - 2 : "Every requested item must produce a result.";
        return Collections.unmodifiableList(messages);
    }

    // @@author
    /**
     * Removes every requested equipment ID from one bundle and reports each result.
     *
     * @param input command in the form {@code bundle-remove-eqm b/[BUNDLE ID] [EQUIPMENT ID]...}
     * @return one success or failure message for every requested equipment ID
     * @throws BundleAdditionUnsuccessful when the command syntax or bundle ID is invalid
     */
    public List<String> removeItemsFromBundle(String input) throws BundleAdditionUnsuccessful {
        return removeItemsFromBundleWithResults(input).stream().map(ItemResult::message).toList();
    }

    /** Returns outcomes only after changed memberships are saved, preserving rollback on failure. */
    public List<ItemResult> removeItemsFromBundleWithResults(String input) throws BundleAdditionUnsuccessful {
        // @@author yarnieee
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        // @@author
        if (arguments.length < 3 || !arguments[0].equals(REMOVE_FROM_BUNDLE_COMMAND)) {
            // @@author yarnieee
            throw new BundleAdditionUnsuccessful(
                    "Invalid format. Use: bundle-remove-eqm b/[BUNDLE ID] [EQUIPMENT ID]...");
        }

        String bundleId = parseBundleId(arguments[1]);
        Bundle bundle = findBundle(bundleId);
        if (bundle == null) {
            LOGGER.log(Level.WARNING, "No bundle found for ID {0}.", bundleId);
            throw new BundleAdditionUnsuccessful("Bundle ID " + bundleId + " does not exist.");
        }

        // @@author
        List<Equipment> previousEquipment = List.copyOf(bundle.getEquipmentList());
        ArrayList<ItemResult> messages = new ArrayList<>();
        boolean membershipChanged = false;
        for (int index = 2; index < arguments.length; index++) {
            // @@author yarnieee
            String itemId = parseEquipmentId(arguments[index]);
            // @@author
            boolean wasMember = bundle.containsEquipmentId(itemId);
            messages.add(removeItemFromBundle(bundle, itemId));
            membershipChanged |= wasMember;
        }
        if (membershipChanged) {
            try {
                saveToStorage();
            } catch (IOException exception) {
                bundle.restoreEquipment(previousEquipment);
                LOGGER.log(Level.WARNING, "Bundle membership removal could not be saved", exception);
                throw new BundleAdditionUnsuccessful("Could not save bundle removal: " + exception.getMessage());
            }
            // @@author yarnieee
        }
        assert messages.size() == arguments.length - 2 : "Every requested item must produce a result.";
        return Collections.unmodifiableList(messages);
    }

    /**
     * Prints all bundles and their contained equipment.
     *
     * @param input command that must be {@code bundle-list}
     * @throws BundleAdditionUnsuccessful when the command is invalid
     */
    public void viewAllBundles(String input) throws BundleAdditionUnsuccessful {
        if (input == null || !input.trim().equals(LIST_BUNDLES_COMMAND)) {
            throw new BundleAdditionUnsuccessful("Invalid format. Use: bundle-list");
        }

        // @@author
        BundleView.print(getBundleList());
        // @@author yarnieee
    }

    // @@author
    // @@author MatthewLeon888
    /**
     * Selects all loans for every physical item in a bundle.
     *
     * @param input command in the form {@code bundle-history b/[ID]} or
     *              {@code bundle-history name/[NAME]}
     * @param loans handler containing active and returned loan records
     * @return an unmodifiable list sorted by issue date and loan ID
     * @throws BundleHistoryUnsuccessful when the selector is invalid or cannot identify one bundle
     */
    public List<Loan> getBundleLoanHistory(String input, LoanHandler loans)
            throws BundleHistoryUnsuccessful {
        if (loans == null) {
            throw new IllegalArgumentException("Loan handler must not be null.");
        }
        Bundle bundle = findBundleForHistory(parseBundleSelector(input));
        if (bundle == null) {
            throw new BundleHistoryUnsuccessful("The requested bundle does not exist.");
        }

        ArrayList<Loan> history = new ArrayList<>();
        for (Equipment item : bundle.getEquipmentList()) {
            history.addAll(loans.getLoanHistoryForEquipment(item));
        }
        history.sort(Comparator.comparing(Loan::getIssuedDate)
                .thenComparingInt(loan -> Identifier.number(loan.getLoanId(), Identifier.Type.LOAN)));
        assert history.stream().allMatch(loan -> loan != null) : "Bundle history must not contain null loans";
        LOGGER.log(Level.FINE, "Selected {0} history record(s) for bundle {1}.",
                new Object[]{history.size(), bundle.getBundleId()});
        return List.copyOf(history);
    }

    /** Displays all active and returned loans associated with one bundle. */
    public void viewBundleLoanHistory(String input, LoanHandler loans)
            throws BundleHistoryUnsuccessful {
        Bundle bundle = findBundleForHistory(parseBundleSelector(input));
        List<Loan> history = getBundleLoanHistory(input, loans);
        String subject = "bundle " + bundle.getBundleId() + " (" + bundle.getBundleName() + ")";
        loans.printLoans(history, "Loan history for " + subject, "No loan history for " + subject + ".");
    }
    // @@author

    // @@author yarnieee
    /**
     * Validates and adds one equipment ID to a bundle.
     */
    // @@author
    private ItemResult addItemToBundle(Bundle bundle, String itemId) {
        // @@author yarnieee
        Equipment item = equipmentHandler.getEquipmentById(itemId);
        if (item == null) {
            // @@author
            return itemResult(bundle, itemId, false, failureMessage(itemId, "equipment does not exist"));
            // @@author yarnieee
        }
        if (item.isLoaned()) {
            // @@author
            return itemResult(bundle, itemId, false, failureMessage(itemId, "equipment is currently loaned"));
            // @@author yarnieee
        }
        // @@author
        if (!item.isServiceable()) {
            return itemResult(bundle, itemId, false, failureMessage(itemId, "equipment is unserviceable"));
        }
        // @@author yarnieee
        if (bundle.containsEquipmentId(itemId)) {
            // @@author
            return itemResult(bundle, itemId, false, failureMessage(itemId, "equipment is already in this bundle"));
            // @@author yarnieee
        }
        if (item.getType().equals("camera") && bundle.countType("camera") >= 1) {
            // @@author
            return itemResult(bundle, itemId, false, failureMessage(itemId, "a bundle can contain only one camera"));
            // @@author yarnieee
        }
        if (item.getType().equals("tripod") && bundle.countType("tripod") >= 1) {
            // @@author
            return itemResult(bundle, itemId, false,
                    failureMessage(itemId, "a bundle can contain at most one tripod"));
            // @@author yarnieee
        }
        // @@author
        Equipment incompatibleItem = findIncompatibleEquipment(bundle, item);
        if (incompatibleItem != null) {
            return itemResult(bundle, itemId, false, failureMessage(itemId,
                    "equipment is incompatible with equipment ID " + incompatibleItem.getItemId()));
        }
        // @@author yarnieee

        bundle.addEquipment(item);
        LOGGER.log(Level.INFO, "Added equipment {0} to bundle {1}.",
                new Object[] {itemId, bundle.getBundleId()});
        // @@author
        return itemResult(bundle, itemId, true,
                "Equipment ID " + itemId + " added to bundle " + bundle.getBundleId() + " successfully.");
        // @@author yarnieee
    }

    // @@author
    /** Removes one equipment ID from a bundle without affecting the inventory record. */
    private ItemResult removeItemFromBundle(Bundle bundle, String itemId) {
        if (!bundle.removeEquipmentById(itemId)) {
            LOGGER.log(Level.WARNING, "Could not remove equipment {0} from bundle {1}: not a member.",
                    // @@author yarnieee
                    new Object[] {itemId, bundle.getBundleId()});
            // @@author
            return itemResult(bundle, itemId, false,
                    "Equipment ID " + itemId + " was not removed: it is not in bundle " + bundle.getBundleId() + ".");
        }
        return itemResult(bundle, itemId, true,
                "Equipment ID " + itemId + " removed from bundle " + bundle.getBundleId() + " successfully.");
    }

    /** Adds display metadata without making the UI infer success by parsing a human-readable sentence. */
    private ItemResult itemResult(Bundle bundle, String itemId, boolean successful, String message) {
        // @@author yarnieee
        Equipment item = equipmentHandler.getEquipmentById(itemId);
        // @@author
        return new ItemResult(bundle.getBundleId(), itemId, item == null ? null : item.getItemName(),
                successful, message);
    }

    /** Carries a membership result while retaining the original message API for existing callers. */
    public record ItemResult(String bundleId, String itemId, String itemName, boolean successful, String message) {
    }

    // @@author Rohita-29

    /**
     * Finds the first existing bundle item that is incompatible with a candidate.
     *
     * @param bundle bundle whose current contents are checked
     * @param candidate equipment proposed for addition
     * @return the first incompatible item, or null when no known incompatibility exists
     */
    private Equipment findIncompatibleEquipment(Bundle bundle, Equipment candidate) {
        for (Equipment existingItem : bundle.getEquipmentList()) {
            if (EquipmentCompatibility.compare(candidate, existingItem) == CompatibilityResult.INCOMPATIBLE) {
                return existingItem;
            }
        }
        return null;
    }

    // @@author

    // @@author yarnieee
    private Bundle findBundle(String bundleId) {
        for (Bundle bundle : bundles) {
            if (bundle.getBundleId().equals(bundleId)) {
                return bundle;
            }
        }
        return null;
    }

    // @@author
    /** Finds the exact physical equipment record referenced by persisted bundle data. */
    private Equipment findEquipment(BundleStorage.StoredItem storedItem) {
        for (Equipment item : equipmentHandler.getEquipmentList()) {
            if (item.getItemId().equals(storedItem.itemId())
                    && item.getRecordId().equals(storedItem.recordId())) {
                return item;
            }
        }
        return null;
    }

    /** Finds a bundle by its explicit ID or exact name selector. */
    private Bundle findBundleForHistory(BundleSelector selector) throws BundleHistoryUnsuccessful {
        if (selector.id() != null) {
            Bundle bundle = findBundle(selector.id());
            if (bundle == null) {
                LOGGER.log(Level.WARNING, "No bundle found for ID {0}.", selector.id());
            }
            return bundle;
        }

        Bundle match = null;
        for (Bundle bundle : bundles) {
            if (bundle.getBundleName().equals(selector.name())) {
                if (match != null) {
                    throw new BundleHistoryUnsuccessful("Bundle name " + selector.name()
                            + " is not unique.");
                }
                match = bundle;
            }
        }
        return match;
    }

    /** Parses the explicit bundle ID or name selector used by bundle history. */
    private static BundleSelector parseBundleSelector(String input) throws BundleHistoryUnsuccessful {
        String normalized = input == null ? "" : input.trim();
        if (!normalized.startsWith(HISTORY_COMMAND_PREFIX)) {
            throw new BundleHistoryUnsuccessful(
                    // @@author yarnieee
                    "Invalid format. Use: bundle-history b/[BUNDLE ID] or bundle-history name/[NAME].");
            // @@author
        }

        String selector = normalized.substring(HISTORY_COMMAND_PREFIX.length()).trim();
        // @@author yarnieee
        if (selector.startsWith(ID_MARKER)) {
            String id = selector.substring(ID_MARKER.length()).trim();
            try {
                id = Identifier.normalize(id, Identifier.Type.BUNDLE);
            } catch (IllegalArgumentException exception) {
                throw new BundleHistoryUnsuccessful("Bundle ID must be from B001 to B999.");
                // @@author
            }
            return new BundleSelector(id, null);
        }
        if (selector.startsWith("name/")) {
            String name = selector.substring("name/".length()).trim();
            if (name.isBlank()) {
                throw new BundleHistoryUnsuccessful("Bundle name must not be blank.");
            }
            return new BundleSelector(null, name);
        }
        throw new BundleHistoryUnsuccessful(
                // @@author yarnieee
                "Invalid format. Use: bundle-history b/[BUNDLE ID] or bundle-history name/[NAME].");
        // @@author
    }

    /** Represents one validated bundle history selector. */
    private record BundleSelector(String id, String name) { }

    // @@author yarnieee
    private String parseBundleName(String input) throws BundleAdditionUnsuccessful {
        if (input == null || !input.trim().startsWith(ADD_BUNDLE_COMMAND + " " + NAME_MARKER)) {
            throw new BundleAdditionUnsuccessful("Invalid format. Use: bundle-create name/[NAME]");
        }
        String name = input.trim().substring((ADD_BUNDLE_COMMAND + " " + NAME_MARKER).length()).trim();
        // @@author
        if (name.isBlank() || name.length() > 100) {
            // @@author yarnieee
            throw new BundleAdditionUnsuccessful("Bundle name must contain 1 to 100 characters.");
        }
        return name;
    }

    private String parseBundleId(String argument) throws BundleAdditionUnsuccessful {
        if (!argument.startsWith(ID_MARKER)) {
            throw new BundleAdditionUnsuccessful("Bundle ID must use the b/B001 to b/B999 format.");
        }
        String bundleId = argument.substring(ID_MARKER.length());
        try {
            return Identifier.normalize(bundleId, Identifier.Type.BUNDLE);
        } catch (IllegalArgumentException exception) {
            throw new BundleAdditionUnsuccessful("Bundle ID must be from B001 to B999.");
        }
    }

    /** Validates an equipment ID before a bundle operation can look it up. */
    private static String parseEquipmentId(String value) throws BundleAdditionUnsuccessful {
        try {
            return Identifier.normalize(value, Identifier.Type.EQUIPMENT);
        } catch (IllegalArgumentException exception) {
            if (Identifier.isMismatched(value, Identifier.Type.EQUIPMENT)) {
                throw new BundleAdditionUnsuccessful("Expected an equipment ID beginning with E: " + value + ".");
            }
            throw new BundleAdditionUnsuccessful("Equipment ID must be from E001 to E999.");
        }
    }

    private String failureMessage(String itemId, String reason) {
        LOGGER.log(Level.WARNING, "Could not add equipment {0}: {1}.",
                new Object[] {itemId, reason});
        return "Equipment ID " + itemId + " was not added: " + reason + ".";
    }
}
            // @@author
