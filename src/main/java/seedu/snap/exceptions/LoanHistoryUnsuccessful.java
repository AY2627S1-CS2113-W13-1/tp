package seedu.snap.exceptions;

/** Reports invalid loan-history or borrower-specific loan-view syntax and selection requests. */
public class LoanHistoryUnsuccessful extends Exception {
    /** Creates a history error suitable for displaying to the user. */
    public LoanHistoryUnsuccessful(String message) {
        super(message);
    }
}
