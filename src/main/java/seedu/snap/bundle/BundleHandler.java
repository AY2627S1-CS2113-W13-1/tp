package seedu.snap.bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.CompatibilityResult;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentCompatibility;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;
import seedu.snap.exceptions.BundleHistoryUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Parses bundle commands and stores bundles for the current session and saved data. */
public class BundleHandler {
    private static final String ADD_BUNDLE_COMMAND = "add-bundle";
    private static final String ADD_TO_BUNDLE_COMMAND = "add-to-bundle";
    private static final String REMOVE_FROM_BUNDLE_COMMAND = "remove-from-bundle";
    private static final String LIST_BUNDLES_COMMAND = "list bundles";
    private static final String HISTORY_COMMAND_PREFIX = "history bundle/";
    private static final String NAME_MARKER = "name/";
    private static final String ID_MARKER = "id/";
    private static final Logger LOGGER = Logger.getLogger(BundleHandler.class.getName());

    private final ArrayList<Bundle> bundles = new ArrayList<>();
    private final EquipmentHandler equipmentHandler;
    private final Path storageFile;
    private int nextBundleNumber = 1;

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
        this(equipmentHandler, null);
    }

    /** Creates a bundle handler using the supplied inventory and optional storage file. */
    public BundleHandler(EquipmentHandler equipmentHandler, Path storageFile) {
        if (equipmentHandler == null) {
            throw new IllegalArgumentException("Equipment handler cannot be null.");
        }
        this.equipmentHandler = equipmentHandler;
        this.storageFile = storageFile;
    }

    /**
     * Creates a bundle from an add-bundle command.
     *
     * @param input command in the form {@code add-bundle name/[NAME]}
     * @return the created bundle
     * @throws BundleAdditionUnsuccessful when the command is invalid
     */
    public Bundle addBundle(String input) throws BundleAdditionUnsuccessful {
        String name = parseBundleName(input);
        if (bundles.stream().anyMatch(bundle -> bundle.getBundleName().equals(name))) {
            LOGGER.log(Level.WARNING, "Bundle name {0} is already in use.", name);
            throw new BundleAdditionUnsuccessful("Bundle name " + name + " already exists.");
        }
        if (nextBundleNumber > 999) {
            throw new BundleAdditionUnsuccessful("No more bundle IDs are available this session.");
        }
        String bundleId = String.format(Locale.ROOT, "%03d", nextBundleNumber);
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
        List<BundleStorage.StoredBundle> storedBundles = BundleStorage.load(storageFile);
        ArrayList<Bundle> restoredBundles = new ArrayList<>();
        int highestBundleNumber = 0;
        for (BundleStorage.StoredBundle stored : storedBundles) {
            Bundle bundle = new Bundle(stored.bundleName(), stored.bundleId());
            for (BundleStorage.StoredItem storedItem : stored.items()) {
                Equipment item = findEquipment(storedItem);
                if (item == null) {
                    throw new IOException("Bundle " + stored.bundleId() + " references missing equipment ID "
                            + storedItem.itemId() + ".");
                }
                if (item.getType().equals("camera") && bundle.countType("camera") >= 1) {
                    throw new IOException("Bundle " + stored.bundleId() + " contains multiple cameras.");
                }
                if (item.getType().equals("tripod") && bundle.countType("tripod") >= 1) {
                    throw new IOException("Bundle " + stored.bundleId() + " contains multiple tripods.");
                }
                bundle.addEquipment(item);
            }
            restoredBundles.add(bundle);
            highestBundleNumber = Math.max(highestBundleNumber, Integer.parseInt(stored.bundleId()));
        }

        bundles.clear();
        bundles.addAll(restoredBundles);
        nextBundleNumber = highestBundleNumber + 1;
        assert bundles.size() == storedBundles.size() : "All saved bundles must be restored";
        LOGGER.log(Level.INFO, "Loaded {0} bundle(s) from {1}",
                new Object[]{bundles.size(), storageFile});
    }

    /** Saves the current bundle definitions when this handler has a backing file. */
    public void saveToStorage() throws IOException {
        if (storageFile == null || bundles.isEmpty() && Files.notExists(storageFile)) {
            return;
        }
        BundleStorage.save(storageFile, bundles);
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
        for (Bundle bundle : bundles) {
            if (bundle.containsEquipmentId(itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds every valid equipment ID from a command and reports each result.
     *
     * @param input command in the form {@code add-to-bundle id/[BUNDLE ID] [EQUIPMENT ID]...}
     * @return one success or failure message for every requested equipment ID
     * @throws BundleAdditionUnsuccessful when the command syntax is invalid
     */
    public List<String> addItemsToBundle(String input) throws BundleAdditionUnsuccessful {
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        if (arguments.length < 3 || !arguments[0].equals(ADD_TO_BUNDLE_COMMAND)) {
            throw new BundleAdditionUnsuccessful(
                    "Invalid format. Use: add-to-bundle id/[BUNDLE ID] [EQUIPMENT ID]...");
        }

        String bundleId = parseBundleId(arguments[1]);
        Bundle bundle = findBundle(bundleId);
        if (bundle == null) {
            LOGGER.log(Level.WARNING, "No bundle found for ID {0}.", bundleId);
            throw new BundleAdditionUnsuccessful("Bundle ID " + bundleId + " does not exist.");
        }

        ArrayList<String> messages = new ArrayList<>();
        for (int i = 2; i < arguments.length; i++) {
            messages.add(addItemToBundle(bundle, arguments[i]));
        }
        assert messages.size() == arguments.length - 2 : "Every requested item must produce a result.";
        return Collections.unmodifiableList(messages);
    }

    /**
     * Removes every requested equipment ID from one bundle and reports each result.
     *
     * @param input command in the form {@code remove-from-bundle id/[BUNDLE ID] [EQUIPMENT ID]...}
     * @return one success or failure message for every requested equipment ID
     * @throws BundleAdditionUnsuccessful when the command syntax or bundle ID is invalid
     */
    public List<String> removeItemsFromBundle(String input) throws BundleAdditionUnsuccessful {
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        if (arguments.length < 3 || !arguments[0].equals(REMOVE_FROM_BUNDLE_COMMAND)) {
            throw new BundleAdditionUnsuccessful(
                    "Invalid format. Use: remove-from-bundle id/[BUNDLE ID] [EQUIPMENT ID]...");
        }

        String bundleId = parseBundleId(arguments[1]);
        Bundle bundle = findBundle(bundleId);
        if (bundle == null) {
            LOGGER.log(Level.WARNING, "No bundle found for ID {0}.", bundleId);
            throw new BundleAdditionUnsuccessful("Bundle ID " + bundleId + " does not exist.");
        }

        List<Equipment> previousEquipment = List.copyOf(bundle.getEquipmentList());
        ArrayList<String> messages = new ArrayList<>();
        boolean membershipChanged = false;
        for (int index = 2; index < arguments.length; index++) {
            String itemId = arguments[index];
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
        }
        assert messages.size() == arguments.length - 2 : "Every requested item must produce a result.";
        return Collections.unmodifiableList(messages);
    }

    /**
     * Prints all bundles and their contained equipment.
     *
     * @param input command that must be {@code list bundles}
     * @throws BundleAdditionUnsuccessful when the command is invalid
     */
    public void viewAllBundles(String input) throws BundleAdditionUnsuccessful {
        if (input == null || !input.trim().equals(LIST_BUNDLES_COMMAND)) {
            throw new BundleAdditionUnsuccessful("Invalid format. Use: list bundles");
        }

        System.out.printf("%-4s %-6s %-24s | %-10s%n", "No.", "ID", "NAME", "COMPLETE");
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = bundles.get(i);
            System.out.printf("%-4s %-6s %-24s | %-10s%n",
                    (i + 1) + ")", bundle.getBundleId(), bundle.getBundleName(), bundle.isComplete());
            for (Equipment item : bundle.getEquipmentList()) {
                System.out.printf("     - %-6s %-24s | %-15s%n",
                        item.getItemId(), item.getItemName(), item.getType());
            }
        }
    }

    /**
     * Selects all loans for every physical item in a bundle.
     *
     * @param input command in the form {@code history bundle/id/[ID]} or
     *              {@code history bundle/name/[NAME]}
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
                .thenComparingInt(loan -> Integer.parseInt(loan.getLoanId())));
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
        if (history.isEmpty()) {
            System.out.println("No loan history for bundle " + bundle.getBundleId()
                    + " (" + bundle.getBundleName() + ").");
            return;
        }
        System.out.println("Loan history for bundle " + bundle.getBundleId()
                + " (" + bundle.getBundleName() + "):");
        loans.printLoans(history);
    }

    /**
     * Validates and adds one equipment ID to a bundle.
     */
    private String addItemToBundle(Bundle bundle, String itemId) {
        Equipment item = equipmentHandler.getEquipmentById(itemId);
        if (item == null) {
            return failureMessage(itemId, "equipment does not exist");
        }
        if (item.isLoaned()) {
            return failureMessage(itemId, "equipment is currently loaned");
        }
        if (!item.isServiceable()) {
            return failureMessage(itemId, "equipment is unserviceable");
        }
        if (bundle.containsEquipmentId(itemId)) {
            return failureMessage(itemId, "equipment is already in this bundle");
        }
        if (item.getType().equals("camera") && bundle.countType("camera") >= 1) {
            return failureMessage(itemId, "a bundle can contain only one camera");
        }
        if (item.getType().equals("tripod") && bundle.countType("tripod") >= 1) {
            return failureMessage(itemId, "a bundle can contain at most one tripod");
        }
        Equipment incompatibleItem = findIncompatibleEquipment(bundle, item);
        if (incompatibleItem != null) {
            return failureMessage(itemId,
                    "equipment is incompatible with equipment ID " + incompatibleItem.getItemId());
        }

        bundle.addEquipment(item);
        LOGGER.log(Level.INFO, "Added equipment {0} to bundle {1}.",
                new Object[] {itemId, bundle.getBundleId()});
        return "Equipment ID " + itemId + " added to bundle " + bundle.getBundleId() + " successfully.";
    }

    /** Removes one equipment ID from a bundle without affecting the inventory record. */
    private String removeItemFromBundle(Bundle bundle, String itemId) {
        if (!bundle.removeEquipmentById(itemId)) {
            LOGGER.log(Level.WARNING, "Could not remove equipment {0} from bundle {1}: not a member.",
                    new Object[] {itemId, bundle.getBundleId()});
            return "Equipment ID " + itemId + " was not removed: it is not in bundle "
                    + bundle.getBundleId() + ".";
        }
        return "Equipment ID " + itemId + " removed from bundle " + bundle.getBundleId() + " successfully.";
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

    private Bundle findBundle(String bundleId) {
        for (Bundle bundle : bundles) {
            if (bundle.getBundleId().equals(bundleId)) {
                return bundle;
            }
        }
        return null;
    }

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
                    "Invalid format. Use: history bundle/id/[BUNDLE ID] or history bundle/name/[NAME].");
        }

        String selector = normalized.substring(HISTORY_COMMAND_PREFIX.length()).trim();
        if (selector.startsWith("id/")) {
            String id = selector.substring("id/".length()).trim();
            if (!id.matches("[0-9]{3}") || id.equals("000")) {
                throw new BundleHistoryUnsuccessful("Bundle ID must be from 001 to 999.");
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
                "Invalid format. Use: history bundle/id/[BUNDLE ID] or history bundle/name/[NAME].");
    }

    /** Represents one validated bundle history selector. */
    private record BundleSelector(String id, String name) { }

    private String parseBundleName(String input) throws BundleAdditionUnsuccessful {
        if (input == null || !input.trim().startsWith(ADD_BUNDLE_COMMAND + " " + NAME_MARKER)) {
            throw new BundleAdditionUnsuccessful("Invalid format. Use: add-bundle name/[NAME]");
        }
        String name = input.trim().substring((ADD_BUNDLE_COMMAND + " " + NAME_MARKER).length()).trim();
        if (name.isBlank() || name.length() > 100) {
            throw new BundleAdditionUnsuccessful("Bundle name must contain 1 to 100 characters.");
        }
        return name;
    }

    private String parseBundleId(String argument) throws BundleAdditionUnsuccessful {
        if (!argument.startsWith(ID_MARKER)) {
            throw new BundleAdditionUnsuccessful("Bundle ID must use the id/[three digits] format.");
        }
        String bundleId = argument.substring(ID_MARKER.length());
        if (!bundleId.matches("[0-9]{3}") || bundleId.equals("000")) {
            throw new BundleAdditionUnsuccessful("Bundle ID must be from 001 to 999.");
        }
        return bundleId;
    }

    private String failureMessage(String itemId, String reason) {
        LOGGER.log(Level.WARNING, "Could not add equipment {0}: {1}.",
                new Object[] {itemId, reason});
        return "Equipment ID " + itemId + " was not added: " + reason + ".";
    }
}
