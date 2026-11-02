// @@author yarnieee
package seedu.snap.exceptions;
// @@author

/** Signals invalid delete syntax, an unknown ID, or an ID shared by multiple equipment records. */
// @@author yarnieee
public class EquipmentDeletionUnsuccessful extends Exception {
    /** Creates an error with the reason equipment deletion failed. */
    public EquipmentDeletionUnsuccessful(String message) {
        super(message);
    }
}
// @@author
