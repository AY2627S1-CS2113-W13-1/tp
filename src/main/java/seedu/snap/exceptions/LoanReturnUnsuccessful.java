package seedu.snap.exceptions;

/** Reports an invalid return assessment or a return that could not be saved. */
public class LoanReturnUnsuccessful extends Exception {
    /** Creates a return error suitable for displaying to the user. */
    public LoanReturnUnsuccessful(String message) {
        super(message);
    }
}
