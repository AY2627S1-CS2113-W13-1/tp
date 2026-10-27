package seedu.snap.exceptions;

/** Reports an invalid equipment-detail request without changing the inventory. */
public class EquipmentViewingUnsuccessful extends Exception {
    public EquipmentViewingUnsuccessful(String message) {
        super(message);
    }
}
