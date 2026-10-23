package seedu.snap.exceptions;

/** Signals invalid delete syntax, an unknown ID, or an ID shared by multiple equipment records. */
public class EquipmentDeletionUnsuccessful extends Exception {
    /** Creates an error with the reason equipment deletion failed. */
    public EquipmentDeletionUnsuccessful(String message) {
        super(message);
    }
}
