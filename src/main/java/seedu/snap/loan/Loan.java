package seedu.snap.loan;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;

/** Represents an identified loan to a borrower, with an optional active equipment association. */
public class Loan {
    private static final Logger logger = Logger.getLogger(Loan.class.getName());
    private final String loanId;
    private final String itemId;
    private Equipment equipment;
    private String borrowerId;
    private LocalDateTime issuedDate;
    private LocalDateTime dueDate;
    private boolean isReturned;

    /** Creates a detached legacy record for parsing or migration, without assigning a loan ID. */
    public Loan(String itemId, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(null, itemId, borrowerId, issuedDate, dueDate);
    }

    /** Creates an identified detached record without changing equipment availability. */
    public Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        assert loanId == null || isValidIdentifier(loanId) : "Loan ID must be from 001 to 999";
        assert itemId != null && isValidIdentifier(itemId) : "Item ID must be from 001 to 999";
        assert borrowerId != null && borrowerId.matches("[0-9]{3}") : "Borrower ID must be three digits";
        assert issuedDate != null && dueDate != null : "A loan must have issue and due dates";
        assert !issuedDate.isAfter(dueDate) : "A loan issue date must not be after its due date";
        this.loanId = loanId;
        this.itemId = itemId;
        this.borrowerId = borrowerId;
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
    }

    /** Creates a legacy candidate without retaining or reserving its equipment before saving. */
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(null, equipment, borrowerId, issuedDate, dueDate);
    }

    /** Creates an identified candidate; the handler attaches its equipment only after saving. */
    public Loan(String loanId, Equipment equipment, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        this(loanId, getAvailableItemId(equipment), borrowerId, issuedDate, dueDate);
        logger.log(Level.FINE, "Created loan candidate {0} for item ID {1}.", new Object[]{loanId, itemId});
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
        assert borrowerId != null && borrowerId.matches("[0-9]{3}") : "Borrower ID must be three digits";
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

    /** Associates a saved or fully validated active record with its equipment. */
    void attachEquipment(Equipment equipment) {
        assert !isReturned : "Returned loan history must stay detached";
        assert loanId != null : "Active equipment associations require an assigned loan ID";
        assert equipment != null && equipment.getItemId().equals(itemId) : "Attached equipment must match the loan";
        assert !equipment.isLoaned() : "Attached equipment must be available";
        assert this.equipment == null || this.equipment == equipment : "A loan must not change equipment references";
        this.equipment = equipment;
        equipment.setLoanId(loanId);
    }

    /** Releases only the equipment still owned by this loan, then discards its reference. */
    void detachEquipment() {
        if (equipment != null && loanId != null && loanId.equals(equipment.getLoanId())) {
            equipment.setLoanId(null);
            logger.log(Level.INFO, "Released equipment with item ID {0} from loan {1}.",
                    new Object[]{itemId, loanId});
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
            logger.log(Level.WARNING, "Rejected reactivation of returned loan {0}.", loanId);
            throw new IllegalStateException("A returned loan cannot be reactivated.");
        }
        detachEquipment();
        this.isReturned = true;
        logger.log(Level.INFO, "Loan {0} for item ID {1} marked as returned.", new Object[]{loanId, itemId});
    }

    /** Validates candidate equipment without retaining a reference or changing its availability. */
    private static String getAvailableItemId(Equipment equipment) {
        if (equipment == null || equipment.isLoaned()) {
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        return equipment.getItemId();
    }

    /** Returns whether an item or loan identifier is in the supported range. */
    private static boolean isValidIdentifier(String id) {
        return id.matches("[0-9]{3}") && !id.equals("000");
    }
}
