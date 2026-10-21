package seedu.snap.loan;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;

/** Represents an item loaned to a borrower. */
public class Loan {
    private static final Logger logger = Logger.getLogger(Loan.class.getName());
    private final String loanId;
    private final String itemId;
    private Equipment equipment;
    private String borrowerId;
    private LocalDateTime issuedDate;
    private LocalDateTime dueDate;
    private boolean isReturned;

    /**
     * Creates a legacy in-memory loan without an identifier.
     * New loans should be created by {@link LoanHandler}, which assigns an identifier first.
     */
    public Loan(String itemId, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(null, itemId, borrowerId, issuedDate, dueDate);
    }

    /** Creates a persisted or newly assigned loan record. */
    public Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        assert loanId == null || isValidLoanId(loanId) : "Loan ID must be three digits";
        assert itemId != null && isValidLoanId(itemId) : "Item ID must be three digits";
        assert borrowerId != null && isValidLoanId(borrowerId) : "Borrower ID must be three digits";
        assert issuedDate != null && dueDate != null : "A loan must have issue and due dates";
        assert !issuedDate.isAfter(dueDate) : "A loan issue date must not be after its due date";
        this.loanId = loanId;
        this.itemId = itemId;
        this.borrowerId = borrowerId;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        this.isReturned = false;
    }

    /** Creates an identified loan associated with an available equipment item. */
    public Loan(String loanId, Equipment equipment, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        this(loanId, requireAvailableEquipment(equipment).getItemId(), borrowerId, issuedDate, dueDate);
        this.equipment = equipment;
        logger.log(Level.FINE, "Created loan {0} for item ID {1}.", new Object[]{loanId, itemId});
    }

    /** Creates a legacy in-memory loan associated with an available equipment item. */
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(null, equipment, borrowerId, issuedDate, dueDate);
    }

    public String getLoanId() {
        return loanId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getBorrowerId() {
        return borrowerId;
    }

    public void setBorrowerId(String borrowerId) {
        assert borrowerId != null && isValidLoanId(borrowerId) : "Borrower ID must be three digits";
        this.borrowerId = borrowerId;
    }

    /** Returns the date and time when the item was issued. */
    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    /** Updates the date and time when the item was issued. */
    public void setIssuedDate(LocalDateTime issuedDate) {
        assert issuedDate != null : "Issue date must not be null";
        assert dueDate == null || !issuedDate.isAfter(dueDate) : "Issue date must not be after due date";
        this.issuedDate = issuedDate;
    }

    /** Returns the date and time when the item is due. */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    /** Updates the date and time when the item is due. */
    public void setDueDate(LocalDateTime dueDate) {
        assert dueDate != null : "Due date must not be null";
        assert issuedDate == null || !issuedDate.isAfter(dueDate) : "Issue date must not be after due date";
        this.dueDate = dueDate;
    }

    public boolean isReturned() {
        return isReturned;
    }

    /** Returns the associated equipment for handler rollback and lifecycle coordination. */
    Equipment getEquipment() {
        return equipment;
    }

    /** Associates a loaded loan with its equipment and restores active availability state. */
    void attachEquipment(Equipment equipment) {
        if (equipment == null || !itemId.equals(equipment.getItemId())) {
            throw new IllegalArgumentException("Loan and equipment IDs must match.");
        }
        String currentLoanId = equipment.getLoanId();
        if (!isReturned && currentLoanId != null && !Objects.equals(currentLoanId, loanId)) {
            logger.log(Level.WARNING, "Loan {0} could not claim item ID {1}; it belongs to loan {2}.",
                    new Object[]{loanId, itemId, currentLoanId});
            throw new IllegalStateException("Equipment already has a different active loan.");
        }
        this.equipment = equipment;
        if (!isReturned && loanId != null) {
            equipment.setLoanId(loanId);
        }
        logger.log(Level.FINE, "Attached loan {0} to item ID {1}.", new Object[]{loanId, itemId});
    }

    /** Releases an active loan's item and prevents deleted records from changing its future availability. */
    void detachEquipment() {
        if (equipment != null) {
            if (Objects.equals(equipment.getLoanId(), loanId)) {
                equipment.setLoanId(null);
                logger.log(Level.INFO, "Released equipment with item ID {0} after loan {1} deletion.",
                        new Object[]{itemId, loanId});
            } else {
                logger.log(Level.WARNING, "Loan {0} did not clear item ID {1}; a different loan owns it.",
                        new Object[]{loanId, itemId});
            }
        }
        equipment = null;
        assert equipment == null : "Deleted loans must not retain equipment references";
    }

    /** Updates return status and availability together; an existing active loan cannot be overwritten. */
    public void setReturned(boolean isReturned) {
        if (this.isReturned == isReturned) {
            return;
        }
        if (!isReturned && equipment != null) {
            String currentLoanId = equipment.getLoanId();
            if (currentLoanId != null && !Objects.equals(currentLoanId, loanId)) {
                logger.log(Level.WARNING, "Rejected reactivation of loan {0} for already-loaned item ID {1}.",
                        new Object[]{loanId, itemId});
                throw new IllegalStateException("Equipment already has an active loan.");
            }
        }
        this.isReturned = isReturned;
        if (equipment != null && loanId != null) {
            if (isReturned) {
                if (Objects.equals(equipment.getLoanId(), loanId)) {
                    equipment.setLoanId(null);
                }
            } else {
                equipment.setLoanId(loanId);
            }
        }
        logger.log(Level.INFO, "Loan {0} for item ID {1} marked as {2}.",
                new Object[]{loanId, itemId, isReturned ? "returned" : "active"});
    }

    private static Equipment requireAvailableEquipment(Equipment equipment) {
        if (equipment == null || equipment.isLoaned()) {
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        assert !equipment.isLoaned() : "A new loan must target available equipment";
        return equipment;
    }

    /** Returns whether a persisted identifier has the canonical three-digit format. */
    private static boolean isValidLoanId(String id) {
        return id.matches("[0-9]{3}") && !id.equals("000");
    }
}
