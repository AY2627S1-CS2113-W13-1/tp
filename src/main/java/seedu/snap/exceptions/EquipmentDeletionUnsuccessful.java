package seedu.snap.exceptions;

/** Signals that a delete command could not remove equipment. */
public class EquipmentDeletionUnsuccessful extends Exception {
    /** Creates an error with the reason equipment deletion failed. */
    public EquipmentDeletionUnsuccessful(String message) {
        super(message);
    }
}
