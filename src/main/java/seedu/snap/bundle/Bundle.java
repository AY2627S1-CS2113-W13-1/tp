package seedu.snap.bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;

/** Represents a named group of equipment with a required composition. */
public class Bundle {
    private static final String CAMERA_TYPE = "camera";
    private static final String BATTERY_TYPE = "battery";
    private static final String TRIPOD_TYPE = "tripod";
    private static final String LENS_TYPE = "lens";
    private static final Logger LOGGER = Logger.getLogger(Bundle.class.getName());

    private final String bundleName;
    private final String bundleId;
    /** References to inventory items so updates to their names and conditions remain visible in the bundle. */
    private final ArrayList<Equipment> equipment = new ArrayList<>();
    private boolean isComplete;

    /**
     * Creates an initially incomplete bundle.
     *
     * @param name bundle name
     * @param id three-digit bundle ID
     */
    public Bundle(String name, String id) {
        this.bundleName = name;
        this.bundleId = id;
        this.isComplete = false;
        assert hasValidIdentity() : "Bundle identity must contain valid values.";
        assert !isComplete : "A new bundle must be incomplete.";
        LOGGER.log(Level.INFO, "Created bundle {0} with ID {1}.",
                new Object[] {name, id});
    }

    /** @return the bundle name */
    public String getBundleName() {
        return bundleName;
    }

    /** @return the bundle ID */
    public String getBundleId() {
        return bundleId;
    }

    /** @return whether this bundle currently satisfies its composition requirements */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Returns the equipment currently contained in this bundle.
     *
     * @return an unmodifiable view of the bundle contents
     */
    public List<Equipment> getEquipmentList() {
        return Collections.unmodifiableList(equipment);
    }

    /**
     * Checks whether this bundle already contains an equipment item.
     *
     * @param itemId equipment ID to find
     * @return true when the equipment ID is already present
     */
    public boolean containsEquipmentId(String itemId) {
        for (Equipment item : equipment) {
            if (item.getItemId().equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the equipment of a particular type in this bundle.
     *
     * @param type canonical equipment type
     * @return number of matching items
     */
    public int countType(String type) {
        int count = 0;
        for (Equipment item : equipment) {
            if (item.getType().equals(type)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Adds an already-validated item and refreshes the completion state.
     *
     * @param item equipment to add
     */
    public void addEquipment(Equipment item) {
        if (item == null) {
            throw new IllegalArgumentException("Equipment cannot be null.");
        }
        assert !containsEquipmentId(item.getItemId()) : "Bundle contents must not contain duplicate IDs.";
        equipment.add(item);
        updateCompletionState();
        assert equipment.contains(item) : "Added equipment must be present in the bundle.";
        LOGGER.log(Level.INFO, "Added equipment {0} to bundle {1}.",
                new Object[] {item.getItemId(), bundleId});
    }

    /** Recomputes whether the bundle meets all required type counts. */
    private void updateCompletionState() {
        int cameraCount = countType(CAMERA_TYPE);
        int batteryCount = countType(BATTERY_TYPE);
        int tripodCount = countType(TRIPOD_TYPE);
        int lensCount = countType(LENS_TYPE);
        isComplete = cameraCount == 1 && batteryCount >= 1 && tripodCount <= 1 && lensCount >= 1;
    }

    /** @return whether the bundle identity is valid */
    private boolean hasValidIdentity() {
        return bundleName != null && !bundleName.isBlank() && bundleName.length() <= 100
                && bundleId != null && bundleId.matches("[0-9]{3}") && !bundleId.equals("000");
    }
}
