package seedu.snap.exceptions;

/** Signals that a maintenance-history query could not be completed. */
public class MaintenanceHistoryUnsuccessful extends Exception {
    /** Creates an error with the reason the maintenance-history query failed. */
    public MaintenanceHistoryUnsuccessful(String message) {
        super(message);
    }
}
