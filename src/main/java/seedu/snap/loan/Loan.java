package seedu.snap.loan;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDate;
import java.util.Objects;

import seedu.snap.equipment.Equipment;

/** Represents an identified loan to a borrower, with an optional active equipment association. */
public class Loan {
    private static final Logger logger = Logger.getLogger(Loan.class.getName());
    private final String loanId;
    private String itemId;
    /** Permanent physical-item identity; null only for legacy records that have not been attributed. */
    private String equipmentRecordId;
    private Equipment equipment;
    private String borrowerId;
    private LocalDateTime issuedDate;
    private LocalDateTime dueDate;
    private boolean isReturned;

    // @@author Rohita-29

    private final EquipmentConditionSnapshot issueConditionSnapshot;

    // @@author


    /** Creates a detached legacy record for parsing or migration, without assigning a loan ID. */
    public Loan(String itemId, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(null, itemId, borrowerId, issuedDate, dueDate);
    }

    /** Creates an identified detached record without changing equipment availability. */
    public Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        this(
                loanId,
                itemId,
                borrowerId,
                issuedDate,
                dueDate,
                null,
                null);
    }

    /** Creates a stored record with an optional permanent equipment identity for legacy compatibility. */
    public Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate, String equipmentRecordId) {
        this(
                loanId,
                itemId,
                borrowerId,
                issuedDate,
                dueDate,
                equipmentRecordId,
                null);
    }

    /** Creates a detached record with an optional saved issue-condition snapshot. */
    Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate, EquipmentConditionSnapshot issueConditionSnapshot) {
        this(
                loanId,
                itemId,
                borrowerId,
                issuedDate,
                dueDate,
                null,
                issueConditionSnapshot);
    }

    /** Creates a stored record with optional identity and issue-condition information. */
    Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate, String equipmentRecordId,
            EquipmentConditionSnapshot issueConditionSnapshot) {
        if (equipmentRecordId != null && !isValidEquipmentRecordId(equipmentRecordId)) {
            throw new IllegalArgumentException("Equipment record identity must be a canonical UUID.");
        }
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
        this.equipmentRecordId = equipmentRecordId;
        this.issueConditionSnapshot = issueConditionSnapshot;
    }

    /** Creates a legacy candidate without retaining or reserving its equipment before saving. */
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(null, equipment, borrowerId, issuedDate, dueDate);
    }

    /** Creates an identified candidate; the handler attaches its equipment only after saving. */
    public Loan(String loanId, Equipment equipment, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        this(
                loanId,
                getAvailableItemId(equipment),
                borrowerId,
                issuedDate,
                dueDate,
                equipment.getRecordId(),
                EquipmentConditionSnapshot.fromEquipment(equipment));
        logger.log(Level.FINE, "Created loan candidate {0} for item ID {1}.", new Object[]{loanId, itemId});
    }

    public String getLoanId() {
        return loanId;
    }

    public String getItemId() {
        return itemId;
    }

    /** Returns the permanent physical-item identity, or null for unattributed legacy history. */
    public String getEquipmentRecordId() {
        return equipmentRecordId;
    }

    /** Attributes an active legacy proposal once; returned legacy history is never guessed from its Item ID. */
    void bindEquipmentRecordId(Equipment item) {
        if (isReturned || item == null || !itemId.equals(item.getItemId())
                || equipmentRecordId != null && !equipmentRecordId.equals(item.getRecordId())) {
            throw new IllegalArgumentException("The active loan must belong to the same equipment record.");
        }
        equipmentRecordId = item.getRecordId();
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

    // @@author Rohita-29

    /**
     * Returns whether this loan is active and was due before the reference date.
     *
     * @param referenceDate date against which the due date is checked
     * @return true if the loan has not been returned and is past its due date
     * @throws NullPointerException if the reference date is null
     */
    public boolean isOverdue(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "Reference date must not be null");
        LocalDate dueDateOnly = dueDate.toLocalDate();
        return !isReturned && dueDateOnly.isBefore(referenceDate);
    }

    // @@author

    public boolean isReturned() {
        return isReturned;
    }

    // @@author Rohita-29

    /** Returns the equipment-condition snapshot captured when this loan was issued. */
    public EquipmentConditionSnapshot getIssueConditionSnapshot() {
        return issueConditionSnapshot;
    }

    // @@author


    /** Checks object identity as well as loan ownership before a return can assess live equipment. */
    boolean ownsEquipment(Equipment item) {
        return !isReturned && equipment == item && item.isLoaned()
                && loanId != null && loanId.equals(item.getLoanId())
                && item.getRecordId().equals(equipmentRecordId);
    }

    /** Creates a detached storage proposal without changing or logging a return on the live loan. */
    Loan returnedCopy() {
        Loan returned =
                new Loan(
                        loanId,
                        itemId,
                        borrowerId,
                        issuedDate,
                        dueDate,
                        equipmentRecordId,
                        issueConditionSnapshot);
        returned.isReturned = true;
        return returned;
    }

    /** Creates a detached active proposal for a validated loan update. */
    Loan updatedCopy(String itemId, String equipmentRecordId, String borrowerId,
            LocalDateTime issuedDate, LocalDateTime dueDate) {
        assert !isReturned : "Returned loan history must not be updated";
        return new Loan(loanId, itemId, borrowerId, issuedDate, dueDate, equipmentRecordId,
                issueConditionSnapshot);
    }

    /** Applies a saved update and keeps equipment ownership synchronized with the loan. */
    void applyUpdate(Loan updated, Equipment replacement) {
        assert updated != null && replacement != null : "Loan update requires a proposal and equipment";
        assert !isReturned && !updated.isReturned : "Returned loans must not be updated";
        assert updated.loanId.equals(loanId) : "A loan update must preserve the loan ID";
        assert updated.itemId.equals(replacement.getItemId()) : "Updated loan must match its equipment";
        assert updated.equipmentRecordId.equals(replacement.getRecordId())
                : "Updated loan must match the physical equipment identity";
        assert replacement == equipment || !replacement.isLoaned()
                : "Replacement equipment must be available before rebinding";

        Equipment previous = equipment;
        if (previous != replacement) {
            assert previous != null : "An active loan must own its current equipment";
            previous.setLoanId(null);
        }
        itemId = updated.itemId;
        equipmentRecordId = updated.equipmentRecordId;
        borrowerId = updated.borrowerId;
        issuedDate = updated.issuedDate;
        dueDate = updated.dueDate;
        equipment = replacement;
        replacement.setLoanId(loanId);
        assert equipment == replacement : "Updated loan must retain its replacement equipment";
        assert replacement.isLoaned() && loanId.equals(replacement.getLoanId())
                : "Updated loan must own loaned replacement equipment";
    }

    /** Associates a saved or fully validated active record with its equipment. */
    void attachEquipment(Equipment equipment) {
        assert !isReturned : "Returned loan history must stay detached";
        assert loanId != null : "Active equipment associations require an assigned loan ID";
        assert equipment != null && equipment.getItemId().equals(itemId) : "Attached equipment must match the loan";
        assert equipment.getRecordId().equals(equipmentRecordId) : "Attached equipment identity must match the loan";
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
        if (equipment == null || equipment.isLoaned() || !equipment.isServiceable()) {
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        return equipment.getItemId();
    }

    /** Returns whether an item or loan identifier is in the supported range. */
    private static boolean isValidIdentifier(String id) {
        return id.matches("[0-9]{3}") && !id.equals("000");
    }

    /** Rejects shortened, non-canonical and malformed UUIDs before constructing stored loan records. */
    static boolean isValidEquipmentRecordId(String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
