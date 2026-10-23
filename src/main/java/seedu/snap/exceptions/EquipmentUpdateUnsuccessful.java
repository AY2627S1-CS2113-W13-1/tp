package seedu.snap.exceptions;

/** Signals invalid equipment updates while preserving the original record. */
public class EquipmentUpdateUnsuccessful extends Exception {
    public EquipmentUpdateUnsuccessful(String message) {
        super(message);
    }
}
