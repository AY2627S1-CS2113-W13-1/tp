package seedu.snap.exceptions;

/** Signals that a loan could not be deleted. */
public class LoanDeletionUnsuccessful extends Exception {
    /** Creates an error with the reason that loan deletion failed. */
    public LoanDeletionUnsuccessful(String message) {
        super(message);
    }
}
