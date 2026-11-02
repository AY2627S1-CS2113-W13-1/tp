// @@author yarnieee
package seedu.snap.loan;

import java.time.LocalDateTime;
// @@author
import java.util.UUID;
// @@author yarnieee
import java.util.logging.Level;
import java.util.logging.Logger;
// @@author
import java.time.LocalDate;
import java.util.Objects;
// @@author yarnieee

import seedu.snap.Identifier;
// @@author
import seedu.snap.equipment.Equipment;

/** Represents an identified loan to a borrower, with an optional active equipment association. */
// @@author yarnieee
public class Loan {
    private static final Logger logger = Logger.getLogger(Loan.class.getName());
    private final String loanId;
    // @@author
    private String itemId;
    /** Permanent physical-item identity; null only for legacy records that have not been attributed. */
    private String equipmentRecordId;
    private Equipment equipment;
    private String borrowerId;
    // @@author yarnieee
    private LocalDateTime issuedDate;
    private LocalDateTime dueDate;
    private boolean isReturned;
    // @@author
    /** Null for active loans and older returned loans without a recorded assessment. */
    private LoanReturnAssessment returnAssessment;
    /** Serviceability captured at issue; null means older records did not save this information. */
    private Boolean issueServiceability;

    // @@author Rohita-29

    private EquipmentConditionSnapshot issueConditionSnapshot;

    // @@author


    /** Creates a detached legacy record for parsing or migration, without assigning a loan ID. */
    public Loan(String itemId, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        // @@author yarnieee
        this(null, itemId, borrowerId, issuedDate, dueDate);
    }

    // @@author
    /** Creates an identified detached record without changing equipment availability. */
    // @@author yarnieee
    public Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        // @@author
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
    // @@author yarnieee
    public Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            // @@author
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
        // @@author yarnieee
        assert loanId == null || Identifier.isValid(loanId, Identifier.Type.LOAN)
                : "Loan ID must be from L001 to L999";
        assert itemId != null && Identifier.isValid(itemId, Identifier.Type.EQUIPMENT)
                : "Item ID must be from E001 to E999";
        assert borrowerId != null && Identifier.isValid(borrowerId, Identifier.Type.BORROWER)
                : "Borrower ID must be from U000 to U999";
        assert issuedDate != null && dueDate != null : "A loan must have issue and due dates";
        assert !issuedDate.isAfter(dueDate) : "A loan issue date must not be after its due date";
        this.loanId = loanId;
        this.itemId = itemId;
        // @@author
        this.borrowerId = borrowerId;
        // @@author yarnieee
        this.issuedDate = issuedDate;
        this.dueDate = dueDate;
        // @@author
        this.equipmentRecordId = equipmentRecordId;
        this.issueConditionSnapshot = issueConditionSnapshot;
        // @@author yarnieee
    }

    // @@author
    /** Restores a saved assessment as returned history without attaching current equipment. */
    Loan(String loanId, String itemId, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate, String equipmentRecordId, EquipmentConditionSnapshot issueConditionSnapshot,
            LoanReturnAssessment returnAssessment, Boolean issueServiceability) {
        this(loanId, itemId, borrowerId, issuedDate, dueDate, equipmentRecordId, issueConditionSnapshot);
        if (issueServiceability != null && issueConditionSnapshot == null) {
            throw new IllegalArgumentException("Issue serviceability requires an issue-condition snapshot.");
        }
        this.issueServiceability = issueServiceability;
        if (returnAssessment != null) {
            validateReturnAssessment(returnAssessment);
            this.returnAssessment = returnAssessment;
            isReturned = true;
        }
        // @@author yarnieee
    }
    // @@author

    /** Creates a legacy candidate without retaining or reserving its equipment before saving. */
    // @@author yarnieee
    public Loan(Equipment equipment, String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate) {
        this(null, equipment, borrowerId, issuedDate, dueDate);
    }

    // @@author
    /** Creates an identified candidate; the handler attaches its equipment only after saving. */
    // @@author yarnieee
    public Loan(String loanId, Equipment equipment, String borrowerId, LocalDateTime issuedDate,
            LocalDateTime dueDate) {
        // @@author
        this(
                loanId,
                getAvailableItemId(equipment),
                borrowerId,
                issuedDate,
                dueDate,
                equipment.getRecordId(),
                EquipmentConditionSnapshot.fromEquipment(equipment));
        issueServiceability = equipment.isServiceable();
        logger.log(Level.FINE, "Created loan candidate {0} for item ID {1}.", new Object[]{loanId, itemId});
    }

    /** Returns saved issue-time serviceability, or null when it was not recorded. */
    public Boolean getIssueServiceability() {
        return issueServiceability;
    }

    /** Returns the immutable return assessment, or null when none was recorded. */
    public LoanReturnAssessment getReturnAssessment() {
        return returnAssessment;
    }

    /** Records a successful return once; legacy returned records cannot be reassessed. */
    void completeReturn(LoanReturnAssessment assessment) {
        if (isReturned || assessment == null) {
            throw new IllegalStateException("Only an active loan can receive a return assessment.");
        }
        validateReturnAssessment(assessment);
        setReturned(true);
        returnAssessment = assessment;
    }

    /** Prevents an assessment for a different equipment subtype from being attached to this transaction. */
    private void validateReturnAssessment(LoanReturnAssessment assessment) {
        if (issueConditionSnapshot != null && !issueConditionSnapshot.getEquipmentType()
                .equals(assessment.condition().getEquipmentType())) {
            throw new IllegalArgumentException("Issue and return assessments must describe the same equipment type.");
        }
    }

    // @@author yarnieee
    public String getLoanId() {
        return loanId;
    }

    // @@author
    public String getItemId() {
        return itemId;
        // @@author yarnieee
    }

    // @@author
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
        // @@author yarnieee
    }

    // @@author
    public void setBorrowerId(String borrowerId) {
        // @@author yarnieee
        assert borrowerId != null && Identifier.isValid(borrowerId, Identifier.Type.BORROWER)
                : "Borrower ID must be from U000 to U999";
        // @@author
        this.borrowerId = borrowerId;
        // @@author yarnieee
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

    // @@author
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

    // @@author yarnieee
    public boolean isReturned() {
        return isReturned;
    }

    // @@author
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
        returned.issueServiceability = issueServiceability;
        return returned;
    }

    /** Creates the saved return proposal while preserving the original issue assessment. */
    Loan returnedCopy(LoanReturnAssessment assessment) {
        if (isReturned || assessment == null) {
            throw new IllegalStateException("Only an active loan can receive a return assessment.");
        }
        return new Loan(loanId, itemId, borrowerId, issuedDate, dueDate, equipmentRecordId,
                issueConditionSnapshot, assessment, issueServiceability);
    }

    // @@author MatthewLeon888
    /**
     * Creates a detached active proposal for a validated loan update.
     * Reassigning equipment captures the replacement's condition instead of reusing the old item's snapshot.
     */
    Loan updatedCopy(String itemId, String equipmentRecordId, String borrowerId,
            LocalDateTime issuedDate, LocalDateTime dueDate, Equipment replacement) {
        assert !isReturned : "Returned loan history must not be updated";
        assert replacement != null : "A loan update requires replacement equipment";
        EquipmentConditionSnapshot updatedSnapshot = replacement == equipment
                ? issueConditionSnapshot : EquipmentConditionSnapshot.fromEquipment(replacement);
        Loan updated = new Loan(loanId, itemId, borrowerId, issuedDate, dueDate, equipmentRecordId,
                updatedSnapshot);
        updated.issueServiceability = issueServiceability;
        if (replacement != equipment) {
            updated.issueServiceability = replacement.isServiceable();
        }
        return updated;
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
        issueConditionSnapshot = updated.issueConditionSnapshot;
        issueServiceability = updated.issueServiceability;
        equipment = replacement;
        replacement.setLoanId(loanId);
        assert equipment == replacement : "Updated loan must retain its replacement equipment";
        assert replacement.isLoaned() && loanId.equals(replacement.getLoanId())
                : "Updated loan must own loaned replacement equipment";
    }
    // @@author

    /** Associates a saved or fully validated active record with its equipment. */
    void attachEquipment(Equipment equipment) {
        assert !isReturned : "Returned loan history must stay detached";
        assert loanId != null : "Active equipment associations require an assigned loan ID";
        assert equipment != null && equipment.getItemId().equals(itemId) : "Attached equipment must match the loan";
        assert equipment.getRecordId().equals(equipmentRecordId) : "Attached equipment identity must match the loan";
        assert !equipment.isLoaned() : "Attached equipment must be available";
        assert this.equipment == null || this.equipment == equipment : "A loan must not change equipment references";
        this.equipment = equipment;
        // @@author yarnieee
        equipment.setLoanId(loanId);
        // @@author
    }

    /** Releases only the equipment still owned by this loan, then discards its reference. */
    void detachEquipment() {
        if (equipment != null && loanId != null && loanId.equals(equipment.getLoanId())) {
            // @@author yarnieee
            equipment.setLoanId(null);
            // @@author
            logger.log(Level.INFO, "Released equipment with item ID {0} from loan {1}.",
                    // @@author yarnieee
                    new Object[]{itemId, loanId});
            // @@author
        }
        equipment = null;
        assert equipment == null : "Detached loans must not retain equipment references";
    }

    /** Releases returned equipment permanently; historical loans cannot be reactivated. */
    // @@author yarnieee
    public void setReturned(boolean isReturned) {
        // @@author
        if (this.isReturned == isReturned) {
            return;
        }
        if (!isReturned) {
            logger.log(Level.WARNING, "Rejected reactivation of returned loan {0}.", loanId);
            throw new IllegalStateException("A returned loan cannot be reactivated.");
            // @@author yarnieee
        }
        // @@author
        detachEquipment();
        this.isReturned = true;
        logger.log(Level.INFO, "Loan {0} for item ID {1} marked as returned.", new Object[]{loanId, itemId});
        // @@author yarnieee
    }

    // @@author
    /** Validates candidate equipment without retaining a reference or changing its availability. */
    private static String getAvailableItemId(Equipment equipment) {
        if (equipment == null || equipment.isLoaned() || !equipment.isServiceable()) {
            // @@author yarnieee
            throw new IllegalArgumentException("A loan requires available equipment.");
        }
        // @@author
        return equipment.getItemId();
        // @@author yarnieee
    }

    // @@author
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
    // @@author yarnieee
}
// @@author
