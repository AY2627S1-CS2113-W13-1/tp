package seedu.snap.exceptions;

// @@author Rohita-29

/** Signals that a loan's issue-time condition could not be viewed. */
public class LoanViewingUnsuccessful extends Exception {
    /** Creates an error with the reason that loan viewing failed. */
    public LoanViewingUnsuccessful(String message) {
        super(message);
    }
}

// @@author
