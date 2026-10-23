package seedu.snap.bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;

/** Parses bundle commands and stores bundles for the current session. */
public class BundleHandler {
    private static final String ADD_BUNDLE_COMMAND = "add-bundle";
    private static final String ADD_TO_BUNDLE_COMMAND = "add-to-bundle";
    private static final String LIST_BUNDLES_COMMAND = "list bundles";
    private static final String NAME_MARKER = "name/";
    private static final String ID_MARKER = "id/";
    private static final Logger LOGGER = Logger.getLogger(BundleHandler.class.getName());

    private final ArrayList<Bundle> bundles = new ArrayList<>();
    private final EquipmentHandler equipmentHandler;
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
        if (equipmentHandler == null) {
            throw new IllegalArgumentException("Equipment handler cannot be null.");
        }
        this.equipmentHandler = equipmentHandler;
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

    /**
     * Checks all session bundles before an inventory item is deleted.
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
        if (bundle.containsEquipmentId(itemId)) {
            return failureMessage(itemId, "equipment is already in this bundle");
        }
        if (item.getType().equals("camera") && bundle.countType("camera") >= 1) {
            return failureMessage(itemId, "a bundle can contain only one camera");
        }
        if (item.getType().equals("tripod") && bundle.countType("tripod") >= 1) {
            return failureMessage(itemId, "a bundle can contain at most one tripod");
        }

        bundle.addEquipment(item);
        LOGGER.log(Level.INFO, "Added equipment {0} to bundle {1}.",
                new Object[] {itemId, bundle.getBundleId()});
        return "Equipment ID " + itemId + " added to bundle " + bundle.getBundleId() + " successfully.";
    }

    private Bundle findBundle(String bundleId) {
        for (Bundle bundle : bundles) {
            if (bundle.getBundleId().equals(bundleId)) {
                return bundle;
            }
        }
        return null;
    }

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
