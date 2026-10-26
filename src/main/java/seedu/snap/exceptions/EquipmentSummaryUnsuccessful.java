package seedu.snap.exceptions;

/** Signals invalid equipment-summary commands or unavailable summary data. */
public class EquipmentSummaryUnsuccessful extends Exception {
    /** Creates a summary error suitable for displaying to the user. */
    public EquipmentSummaryUnsuccessful(String message) {
        super(message);
    }
}
