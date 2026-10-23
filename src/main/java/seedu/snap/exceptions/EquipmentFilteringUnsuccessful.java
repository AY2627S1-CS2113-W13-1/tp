package seedu.snap.exceptions;

/** Signals an invalid equipment filter without changing the inventory. */
public class EquipmentFilteringUnsuccessful extends Exception {
    public EquipmentFilteringUnsuccessful(String message) {
        super(message);
    }
}
