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

    /** Creates a loan with an initially unreturned status. */
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(availableItemId(equipment), borrowerId, issuedDate, dueDate);
        attachEquipment(equipment);
    }

    /** Restores a record without reserving equipment; the handler reconnects active records after validation. */
    public Loan(String itemId, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this.itemId = itemId;
        this.borrowerId = borrowerId;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.isReturned = false;
    }

    /** Checks availability before constructing a new borrowing transaction. */
    private static String availableItemId(Equipment equipment) {
        if (equipment == null || equipment.isLoaned()) {
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        return equipment.getItemId();
    }

    /** Connects a validated active record to its matching equipment after a successful load or save. */
    void attachEquipment(Equipment equipment) {
        if (isReturned || !itemId.equals(availableItemId(equipment))) {
            throw new IllegalArgumentException("An active loan requires matching available equipment.");
        }
        this.equipment = equipment;
        equipment.setLoaned(true);
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
        }
        equipment = null;
    }

    /** Updates return status and availability together; returned records cannot be reactivated. */
    public void setReturned(boolean isReturned) {
        if (this.isReturned == isReturned) {
            return;
        }
        if (!isReturned) {
            throw new IllegalStateException("Returned loans cannot be reactivated.");
        }
        this.isReturned = isReturned;
        if (equipment != null) {
            equipment.setLoaned(!isReturned);
        }
    }
}
