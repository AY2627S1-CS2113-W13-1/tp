package seedu.snap.exceptions;

/** Signals that a bundle command could not be completed. */
public class BundleAdditionUnsuccessful extends Exception {
    /** Creates an error with the reason that bundle creation or modification failed. */
    public BundleAdditionUnsuccessful(String message) {
        super(message);
    }
}
