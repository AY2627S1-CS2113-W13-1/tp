package seedu.snap.loan;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;

/** Represents an item loaned to a borrower. */
public class Loan {
    private static final Logger logger = Logger.getLogger(Loan.class.getName());
    private final String itemId;
    private Equipment equipment;
    private String borrowerId;
    private LocalDateTime issuedDate;
    private LocalDateTime dueDate;
    private boolean isReturned;

    /** Creates a detached record for parsing or loading loan history, without changing equipment availability. */
    public Loan(String itemId, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        assert itemId != null && itemId.matches("\\d{3}") : "A loan must have a three-digit item ID";
        assert borrowerId != null && !borrowerId.isBlank() : "A loan must have a borrower ID";
        assert issuedDate != null && dueDate != null : "A loan must have issue and due dates";
        assert !issuedDate.isAfter(dueDate) : "A loan issue date must not be after its due date";
        this.itemId = itemId;
        this.borrowerId = borrowerId;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.isReturned = false;
    }

    /** Creates an active loan candidate; its equipment is reserved only after the handler saves it. */
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(getAvailableItemId(equipment), borrowerId, issuedDate, dueDate);
        logger.log(Level.FINE, "Created loan candidate for item ID {0}.", itemId);
    }

    /** Validates the candidate's equipment without retaining a reference or reserving it. */
    private static String getAvailableItemId(Equipment equipment) {
        if (equipment == null || equipment.isLoaned()) {
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        assert !equipment.isLoaned() : "A new loan must target available equipment";
        return equipment.getItemId();
    }

    public String getItemId() {
        return itemId;
    }

    public String getBorrowerId() {
        return borrowerId;
    }

    public void setBorrowerId(String borrowerId) {
        this.borrowerId = borrowerId;
    }

    /** Returns the date and time when the item was issued. */
    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    /** Updates the date and time when the item was issued. */
    public void setIssuedDate(LocalDateTime issuedDate) {
        this.issuedDate = issuedDate;
    }

    /** Returns the date and time when the item is due. */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    /** Updates the date and time when the item is due. */
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isReturned() {
        return isReturned;
    }

    /** Associates a validated active record with its item after saving or validating all loaded records. */
    void attachEquipment(Equipment equipment) {
        assert !isReturned : "Returned loan history must stay detached";
        assert equipment != null && equipment.getItemId().equals(itemId) : "Attached equipment must match the loan";
        assert !equipment.isLoaned() : "Attached equipment must be available";
        assert this.equipment == null || this.equipment == equipment : "A loan must not change equipment references";
        this.equipment = equipment;
        equipment.setLoaned(true);
    }

    /** Releases an active loan's item and prevents removed records from changing its future availability. */
    void detachEquipment() {
        if (equipment != null && !isReturned) {
            equipment.setLoaned(false);
            logger.log(Level.INFO, "Released equipment with item ID {0} from its loan.", itemId);
        }
        equipment = null;
        assert equipment == null : "Detached loans must not retain equipment references";
    }

    /** Releases returned equipment permanently; historical loans cannot be reactivated. */
    public void setReturned(boolean isReturned) {
        if (this.isReturned == isReturned) {
            return;
        }
        if (!isReturned) {
            logger.log(Level.WARNING, "Rejected reactivation of returned loan for item ID {0}.", itemId);
            throw new IllegalStateException("A returned loan cannot be reactivated.");
        }
        detachEquipment();
        this.isReturned = isReturned;
        logger.log(Level.INFO, "Loan for item ID {0} marked as {1}.",
                new Object[]{itemId, isReturned ? "returned" : "active"});
    }
}
