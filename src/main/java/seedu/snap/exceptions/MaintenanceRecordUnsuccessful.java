package seedu.snap.exceptions;

// @@author MatthewLeon888
/** Signals that a maintenance record command could not be completed. */
public class MaintenanceRecordUnsuccessful extends Exception {
    /** Creates an error with the reason the maintenance record operation failed. */
    public MaintenanceRecordUnsuccessful(String message) {
        super(message);
    }
}
// @@author
