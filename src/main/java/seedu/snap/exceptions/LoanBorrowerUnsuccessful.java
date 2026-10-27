package seedu.snap.exceptions;

/** Reports invalid borrower queries or a borrower ID that cannot be found. */
public class LoanBorrowerUnsuccessful extends Exception {
    /** Creates a borrower query error suitable for displaying to the user. */
    public LoanBorrowerUnsuccessful(String message) {
        super(message);
    }
}
