package seedu.snap.exceptions;

/** Signals that an add command could not create equipment. */
public class EquipmentAdditionUnsuccessful extends Exception {
    /** Creates an error with the reason equipment addition failed. */
    public EquipmentAdditionUnsuccessful(String message) {
        super(message);
    }
}
