package seedu.snap.exceptions;

/** Signals that a maintenance-due query or inspection could not be completed. */
public class MaintenanceInspectionUnsuccessful extends Exception {
    /** Creates an error with the reason the maintenance-inspection operation failed. */
    public MaintenanceInspectionUnsuccessful(String message) {
        super(message);
    }
}
