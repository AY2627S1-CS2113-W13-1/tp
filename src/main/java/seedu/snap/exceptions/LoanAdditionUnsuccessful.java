package seedu.snap.exceptions;

/** Signals that a loan command could not be added. */
public class LoanAdditionUnsuccessful extends Exception {
    /** Creates an error with the reason that loan addition failed. */
    public LoanAdditionUnsuccessful(String message) {
        super(message);
    }
}
