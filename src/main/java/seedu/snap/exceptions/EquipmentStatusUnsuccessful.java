package seedu.snap.exceptions;

/** Signals invalid equipment-status commands or status-history queries. */
public class EquipmentStatusUnsuccessful extends Exception {
    /** Creates a status-operation error suitable for displaying to the user. */
    public EquipmentStatusUnsuccessful(String message) {
        super(message);
    }
}
