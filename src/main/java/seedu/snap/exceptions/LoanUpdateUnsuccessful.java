package seedu.snap.exceptions;

/** Signals that an existing loan could not be updated. */
public class LoanUpdateUnsuccessful extends Exception {
    /** Creates an error with the reason that the loan update failed. */
    public LoanUpdateUnsuccessful(String message) {
        super(message);
    }
}
