package seedu.snap.loan;

import java.time.LocalDateTime;

import seedu.snap.equipment.Equipment;

/** Represents an item loaned to a borrower. */
public class Loan {
    private final String itemId;
    private Equipment equipment;
    private String borrowerId;
    private LocalDateTime issuedDate;
    private LocalDateTime dueDate;
    private boolean isReturned;

    /** Creates a loan record before it is associated with an in-memory equipment item. */
    public Loan(String itemId, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this.itemId = itemId;
        this.borrowerId = borrowerId;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.isReturned = false;
    }

    /** Creates a loan associated with an available equipment item. */
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        if (equipment == null || equipment.isLoaned()) {
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        this.itemId = equipment.getItemId();
        this.equipment = equipment;
        this.borrowerId = borrowerId;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.isReturned = false;
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

    /** Associates a loaded loan with its equipment and restores active availability state. */
    void attachEquipment(Equipment equipment) {
        if (equipment == null || !itemId.equals(equipment.getItemId())) {
            throw new IllegalArgumentException("Loan and equipment IDs must match.");
        }
        this.equipment = equipment;
        if (!isReturned) {
            equipment.setLoaned(true);
        }
    }

    /** Releases an active loan's item and prevents deleted records from changing its future availability. */
    void detachEquipment() {
        if (equipment != null && !isReturned) {
            equipment.setLoaned(false);
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
            throw new IllegalStateException("Equipment already has an active loan.");
        }
        this.isReturned = isReturned;
        if (equipment != null) {
            equipment.setLoaned(!isReturned);
        }
    }
}
