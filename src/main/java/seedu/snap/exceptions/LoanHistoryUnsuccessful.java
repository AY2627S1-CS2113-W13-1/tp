package seedu.snap.exceptions;

/** Reports invalid history syntax or an equipment item that cannot be selected uniquely. */
public class LoanHistoryUnsuccessful extends Exception {
    /** Creates a history error suitable for displaying to the user. */
    public LoanHistoryUnsuccessful(String message) {
        super(message);
    }
}
