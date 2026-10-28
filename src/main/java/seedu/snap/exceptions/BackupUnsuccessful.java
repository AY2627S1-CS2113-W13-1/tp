package seedu.snap.exceptions;

/** Reports a backup or restore operation that could not be completed safely. */
public class BackupUnsuccessful extends Exception {
    /** Creates an operation error suitable for displaying to the user. */
    public BackupUnsuccessful(String message) {
        super(message);
    }

    /** Creates an operation error while retaining the underlying I/O or validation cause. */
    public BackupUnsuccessful(String message, Throwable cause) {
        super(message, cause);
    }
}
