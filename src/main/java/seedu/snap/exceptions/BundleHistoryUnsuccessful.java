package seedu.snap.exceptions;

/** Reports invalid bundle-history syntax or an unknown or ambiguous bundle. */
public class BundleHistoryUnsuccessful extends Exception {
    /** Creates a bundle-history error suitable for displaying to the user. */
    public BundleHistoryUnsuccessful(String message) {
        super(message);
    }
}
