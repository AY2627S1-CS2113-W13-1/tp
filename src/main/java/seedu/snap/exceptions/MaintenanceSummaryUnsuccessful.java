package seedu.snap.exceptions;

/** Indicates that a maintenance summary command could not be validated or displayed. */
public class MaintenanceSummaryUnsuccessful extends Exception {
    /** Creates an exception with a user-facing explanation. */
    public MaintenanceSummaryUnsuccessful(String message) {
        super(message);
    }
}
