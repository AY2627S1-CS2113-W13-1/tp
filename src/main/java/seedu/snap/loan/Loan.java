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

    /** Creates a loan with an initially unreturned status. */
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        if (equipment == null || equipment.isLoaned()) {
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        assert !equipment.isLoaned() : "A new loan must target available equipment";
        assert borrowerId != null && !borrowerId.isBlank() : "A loan must have a borrower ID";
        assert issuedDate != null && dueDate != null : "A loan must have issue and due dates";
        assert !issuedDate.isAfter(dueDate) : "A loan issue date must not be after its due date";
        this.equipment = equipment;
        this.itemId = equipment.getItemId();
        this.borrowerId = borrowerId;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.isReturned = false;
        logger.log(Level.FINE, "Created loan candidate for item ID {0}.", itemId);
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

    /** Releases an active loan's item and prevents deleted records from changing its future availability. */
    void detachEquipment() {
        if (equipment != null && !isReturned) {
            equipment.setLoaned(false);
            logger.log(Level.INFO, "Released equipment with item ID {0} after loan deletion.", itemId);
        }
        equipment = null;
        assert equipment == null : "Deleted loans must not retain equipment references";
    }

    /** Updates return status and availability together; an existing active loan cannot be overwritten. */
    public void setReturned(boolean isReturned) {
        if (this.isReturned == isReturned) {
            return;
        }
        if (!isReturned && equipment != null && equipment.isLoaned()) {
            logger.log(Level.WARNING, "Rejected reactivation of loan for already-loaned item ID {0}.", itemId);
            throw new IllegalStateException("Equipment already has an active loan.");
        }
        this.isReturned = isReturned;
        if (equipment != null) {
            equipment.setLoaned(!isReturned);
        }
        logger.log(Level.INFO, "Loan for item ID {0} marked as {1}.",
                new Object[]{itemId, isReturned ? "returned" : "active"});
    }
}
