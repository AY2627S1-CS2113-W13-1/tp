package seedu.snap.loan;

import java.time.LocalDateTime;

/** Represents an item loaned to a borrower. */
public class Loan {
    private String itemID;
    private String borrowerID;
    private LocalDateTime issuedDate;
    private LocalDateTime dueDate;
    private boolean isReturned;

    /** Creates a loan with an initially unreturned status. */
    public Loan(String itemID, String borrowerID, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this.itemID = itemID;
        this.borrowerID = borrowerID;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.isReturned = false;
    }

    public String getItemID() {
        return itemID;
    }

    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    public String getBorrowerID() {
        return borrowerID;
    }

    public void setBorrowerID(String borrowerID) {
        this.borrowerID = borrowerID;
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

    public void setReturned(boolean isReturned) {
        this.isReturned = isReturned;
    }
}
