package seedu.snap.loan;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Objects;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanBorrowerUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;
import seedu.snap.exceptions.LoanReturnUnsuccessful;
import seedu.snap.exceptions.LoanHistoryUnsuccessful;
import seedu.snap.ui.LoanView;
import seedu.snap.exceptions.LoanViewingUnsuccessful;
import seedu.snap.exceptions.LoanUpdateUnsuccessful;

/** Parses loan commands and stores successfully created loans. */
public class LoanHandler {
    private static final int EXPECTED_ADD_ARGUMENT_COUNT = 5;
    private static final int FIRST_LOAN_NUMBER = 1;
    private static final int MAX_LOAN_NUMBER = 999;
    private static final Logger logger = Logger.getLogger(LoanHandler.class.getName());
    private static final String ID_FORMAT = "[0-9]{3}";
    private static final String LOAN_COMMAND_FORMAT = "Invalid command format. Use: loan id/[ITEM ID] "
            + "b/[BORROWER ID] [issued/DD-MM-YY] due/[DD-MM-YY]. Omitted issued/ uses today's date.";
    private static final Pattern HISTORY_COMMAND = Pattern.compile("(?U)\\Ahistory\\s+id/([0-9]{3})\\z");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Inventory whose item availability is maintained by this handler. */
    private final EquipmentHandler equipment;
    /** Loans managed by this handler instance. */
    private final ArrayList<Loan> loans = new ArrayList<>();
    /** Cached loan count kept in sync with {@link #loans}. */
    private int numLoans = 0;
    /** File used for persistence, or null when the handler is intentionally in-memory only. */
    private final Path storageFile;
    /** File storing the next identifier independently of deleted loan records. */
    private final Path sequenceFile;
    /** Supplies the local date once when an interactive loan command omits its issue date. */
    private final Clock clock;
    private int nextLoanNumber = FIRST_LOAN_NUMBER;

    /** Creates an in-memory loan handler, mainly useful for isolated unit tests. */
    public LoanHandler(EquipmentHandler equipment) {
        this(equipment, null, null);
    }

    /** Creates a loan handler backed by the supplied text file. */
    public LoanHandler(EquipmentHandler equipment, Path storageFile) {
        this(equipment, storageFile, defaultSequenceFile(storageFile));
    }

    /** Creates a handler with explicit loan and sequence files and a required equipment inventory. */
    public LoanHandler(EquipmentHandler equipment, Path storageFile, Path sequenceFile) {
        this(equipment, storageFile, sequenceFile, Clock.systemDefaultZone());
    }

    /** Creates a handler with an explicit clock so omitted issue dates can be tested without the system date. */
    public LoanHandler(EquipmentHandler equipment, Path storageFile, Path sequenceFile, Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock must not be null.");
        }
        assert equipment != null : "Loan handler requires an inventory";
        this.equipment = equipment;
        this.storageFile = storageFile;
        this.sequenceFile = sequenceFile;
        this.clock = clock;
    }

    /** Returns the next loan ID, or 1000 when all supported identifiers have been used. */
    public String getNextLoanId() {
        return formatLoanId(nextLoanNumber);
    }

    /** Returns the number of currently loaded loans. */
    public int getLoanCount() {
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        return numLoans;
    }

    /** Returns a read-only view of this handler's loans for inspection and display. */
    public List<Loan> getLoanList() {
        return Collections.unmodifiableList(loans);
    }

    /** Persists the current loans when this handler has a backing file. */
    public void saveToStorage() throws IOException {
        save(loans, nextLoanNumber);
    }

    /**
     * Replaces the current loan list with records loaded from the backing file.
     * A missing file represents an empty loan list.
     *
     * @throws IOException if saved data cannot be read or contains invalid records
     */
    public void loadFromStorage() throws IOException {
        if (storageFile == null) {
            return;
        }
        LoanStorage.LoadedLoans result = LoanStorage.load(storageFile, sequenceFile);
        List<Loan> loaded = result.loans();
        Set<String> activeItemIds = new HashSet<>();
        List<Equipment> activeEquipment = new ArrayList<>();
        boolean identitiesAdded = false;
        for (Loan loan : loaded) {
            if (loan.isReturned()) {
                continue;
            }
            if (!activeItemIds.add(loan.getItemId())) {
                throw new IOException("Multiple active loans found for item ID " + loan.getItemId() + ".");
            }
            try {
                Equipment item = findEquipment(loan.getItemId());
                if (loan.getEquipmentRecordId() == null) {
                    loan.bindEquipmentRecordId(item);
                    identitiesAdded = true;
                } else if (!loan.getEquipmentRecordId().equals(item.getRecordId())) {
                    throw new IOException("Active loan " + loan.getLoanId()
                            + " belongs to a different equipment record for item ID " + loan.getItemId() + ".");
                }
                activeEquipment.add(item);
            } catch (LoanAdditionUnsuccessful exception) {
                throw new IOException("Invalid active loan for item ID " + loan.getItemId() + ": "
                        + exception.getMessage(), exception);
            }
        }

        // Persist migration before changing the current list, counter, equipment flags or links.
        if (result.migrated() || identitiesAdded) {
            LoanStorage.save(storageFile, loaded, sequenceFile, result.nextLoanNumber());
            logger.log(Level.INFO, "Migrated legacy loans to persistent loan IDs and known equipment identities.");
        }
        for (Loan loan : loans) {
            loan.detachEquipment();
        }
        for (Equipment item : equipment.getEquipmentList()) {
            item.setLoaned(false);
        }
        int activeIndex = 0;
        for (Loan loan : loaded) {
            if (!loan.isReturned()) {
                loan.attachEquipment(activeEquipment.get(activeIndex++));
            }
        }
        loans.clear();
        loans.addAll(loaded);
        numLoans = loans.size();
        nextLoanNumber = result.nextLoanNumber();
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        assert nextLoanNumber >= FIRST_LOAN_NUMBER && nextLoanNumber <= MAX_LOAN_NUMBER + 1
                : "Next loan number must remain within its supported range";
        logger.log(Level.INFO, "Loaded {0} loan(s) from {1}", new Object[]{numLoans, storageFile});
    }


    /**
     * Adds a loan when the command contains valid IDs and dates.
     *
     * @param input complete loan command entered by the user
     * @return the created loan
     * @throws LoanAdditionUnsuccessful when the command cannot be added
     */
    public Loan addLoan(String input) throws LoanAdditionUnsuccessful {
        Loan parsed = parseLoanCommand(input);
        String itemId = parsed.getItemId();

        if (hasActiveLoanForItem(itemId)) {
            logger.log(Level.WARNING, "Loan rejected: item ID {0} already has an active loan.", itemId);
            throw new LoanAdditionUnsuccessful("An active loan already exists for item ID " + itemId + ".");
        }

        Equipment item = findAvailableEquipment(itemId);
        if (nextLoanNumber > MAX_LOAN_NUMBER) {
            throw new LoanAdditionUnsuccessful("No loan IDs remain; the maximum is 999.");
        }
        String loanId = formatLoanId(nextLoanNumber);
        Loan loan = new Loan(loanId, item, parsed.getBorrowerId(), parsed.getIssuedDate(), parsed.getDueDate());
        assert loan != null : "Created loan must not be null.";
        ArrayList<Loan> updatedLoans = new ArrayList<>(loans);
        updatedLoans.add(loan);
        try {
            save(updatedLoans, nextLoanNumber + 1);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Loan addition could not be saved", e);
            throw new LoanAdditionUnsuccessful("Could not save loan: " + e.getMessage());
        }

        loans.add(loan);
        numLoans++;
        nextLoanNumber++;
        loan.attachEquipment(item);
        assert item.isLoaned() : "A successfully added loan must mark equipment as loaned";
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Added loan {0} for item ID {1}.", new Object[]{loanId, itemId});
        return loan;
    }

    /** Updates one active loan after validating every supplied field and saving the proposal first. */
    public Loan updateLoan(String input) throws LoanUpdateUnsuccessful {
        LoanUpdate update;
        try {
            update = LoanUpdate.parse(input);
        } catch (LoanUpdateUnsuccessful exception) {
            logger.log(Level.WARNING, "Loan update rejected: {0}", exception.getMessage());
            throw exception;
        }
        Loan loan = findLoanForUpdate(update.loanId());
        if (loan.isReturned()) {
            throw rejectLoanUpdate("Loan " + update.loanId()
                    + " has already been returned and cannot be updated.");
        }

        Equipment current;
        Equipment replacement;
        try {
            current = findEquipment(loan.getItemId());
            if (!loan.ownsEquipment(current)) {
                throw rejectLoanUpdate("Loan " + loan.getLoanId()
                        + " no longer owns its equipment. No update was recorded.");
            }
            replacement = update.changesItem() ? findEquipment(update.itemId()) : current;
            if (replacement != current) {
                if (hasActiveLoanForItem(replacement.getItemId()) || replacement.isLoaned()) {
                    throw rejectLoanUpdate("Item ID " + replacement.getItemId()
                            + " is already loaned out.");
                }
                if (!replacement.isServiceable()) {
                    throw rejectLoanUpdate("Item ID " + replacement.getItemId()
                            + " is unserviceable and cannot be assigned to a loan.");
                }
            }
        } catch (LoanAdditionUnsuccessful exception) {
            logger.log(Level.WARNING, "Loan update equipment validation failed", exception);
            throw new LoanUpdateUnsuccessful(exception.getMessage());
        }

        String borrowerId = update.changesBorrower() ? update.borrowerId() : loan.getBorrowerId();
        LocalDateTime issuedDate = update.changesIssuedDate() ? update.issuedDate() : loan.getIssuedDate();
        LocalDateTime dueDate = update.changesDueDate() ? update.dueDate() : loan.getDueDate();
        if (issuedDate.isAfter(dueDate)) {
            throw rejectLoanUpdate("Issued date must not be later than the due date.");
        }

        Loan proposal = loan.updatedCopy(replacement.getItemId(), replacement.getRecordId(),
                borrowerId, issuedDate, dueDate, replacement);
        ArrayList<Loan> proposedLoans = new ArrayList<>(loans);
        proposedLoans.set(proposedLoans.indexOf(loan), proposal);
        try {
            save(proposedLoans, nextLoanNumber);
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Loan update could not be saved", exception);
            throw new LoanUpdateUnsuccessful("Could not save loan update: " + exception.getMessage());
        }

        loan.applyUpdate(proposal, replacement);
        assert loan.getLoanId().equals(update.loanId()) : "A loan update must preserve the loan ID";
        assert numLoans == loans.size() : "Updating a loan must not change the loan count";
        logger.log(Level.INFO, "Updated loan {0}; item ID {1}, borrower ID {2}.",
                new Object[]{loan.getLoanId(), loan.getItemId(), loan.getBorrowerId()});
        return loan;
    }

    /** Finds exactly one loan before an update can change its details. */
    private Loan findLoanForUpdate(String loanId) throws LoanUpdateUnsuccessful {
        Loan match = null;
        for (Loan candidate : loans) {
            if (loanId.equals(candidate.getLoanId())) {
                if (match != null) {
                    throw rejectLoanUpdate("Loan ID " + loanId + " is not unique.");
                }
                match = candidate;
            }
        }
        if (match == null) {
            throw rejectLoanUpdate("No loan found for loan ID " + loanId + ".");
        }
        return match;
    }

    /** Logs and creates a consistent rejection for a loan update that failed business validation. */
    private LoanUpdateUnsuccessful rejectLoanUpdate(String message) {
        logger.log(Level.WARNING, "Loan update rejected: {0}", message);
        return new LoanUpdateUnsuccessful(message);
    }

    /** Finds one matching item without changing its availability, including during reload validation. */
    private Equipment findEquipment(String itemId) throws LoanAdditionUnsuccessful {
        Equipment match = null;
        for (Equipment item : equipment.getEquipmentList()) {
            if (item.getItemId().equals(itemId)) {
                if (match != null) {
                    logger.log(Level.WARNING, "Loan rejected: item ID {0} is ambiguous.", itemId);
                    throw new LoanAdditionUnsuccessful("Item ID " + itemId + " is not unique.");
                }
                match = item;
            }
        }
        if (match == null) {
            logger.log(Level.WARNING, "Loan rejected: item ID {0} does not exist.", itemId);
            throw new LoanAdditionUnsuccessful("Item ID " + itemId + " does not exist.");
        }
        return match;
    }

    /** Finds one available item, rejecting missing or ambiguous IDs before a loan changes any state. */
    private Equipment findAvailableEquipment(String itemId) throws LoanAdditionUnsuccessful {
        Equipment match = findEquipment(itemId);
        if (match.isLoaned()) {
            logger.log(Level.WARNING, "Loan rejected: item ID {0} is already loaned out.", itemId);
            throw new LoanAdditionUnsuccessful("Item ID " + itemId + " is already loaned out.");
        }
        if (!match.isServiceable()) {
            throw new LoanAdditionUnsuccessful("Item ID " + itemId + " is unserviceable and cannot be loaned.");
        }
        return match;
    }

    /**
     * Records the mandatory return assessment without removing the loan or changing its identifier.
     * The proposed equipment and returned record are saved before the original objects change.
     *
     * @param input complete return command
     * @return the retained loan, now permanently returned
     * @throws LoanReturnUnsuccessful if the command, loan association or assessment is invalid, or saving fails
     */
    public Loan returnLoan(String input) throws LoanReturnUnsuccessful {
        LoanReturn assessment = LoanReturn.parse(input);
        Loan loan = findLoanForReturn(assessment.loanId());
        Equipment item;
        Equipment assessed;
        try {
            item = findEquipment(loan.getItemId());
            if (!loan.ownsEquipment(item)) {
                throw new LoanReturnUnsuccessful("Loan " + loan.getLoanId()
                        + " no longer owns its equipment. No return was recorded.");
            }
            for (Loan other : loans) {
                if (other != loan && !other.isReturned() && other.getItemId().equals(loan.getItemId())) {
                    throw new LoanReturnUnsuccessful("Multiple active loans found for item ID "
                            + loan.getItemId() + ". No return was recorded.");
                }
            }
            assessed = equipment.prepareReturnAssessment(item, assessment.serviceable(),
                    assessment.damage(), assessment.remarks());
            if (assessment.condition() != null) {
                assessed.updateDetails(assessed.getItemName(), assessment.condition());
            }
        } catch (LoanAdditionUnsuccessful | IllegalArgumentException exception) {
            throw new LoanReturnUnsuccessful(exception.getMessage());
        }

        // This detached proposal cannot release equipment or alter the live loan before saving.
        LoanReturnAssessment snapshot = LoanReturnAssessment.fromEquipment(assessed);
        Loan returned = loan.returnedCopy(snapshot);
        ArrayList<Loan> proposedLoans = new ArrayList<>(loans);
        proposedLoans.set(proposedLoans.indexOf(loan), returned);
        try {
            LoanReturnStorage.save(equipment, item, assessed, storageFile,
                    proposedLoans, sequenceFile, nextLoanNumber);
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Equipment return could not be saved", exception);
            String rollbackWarning = exception.getSuppressed().length == 0 ? ""
                    : " Restoring previous files also failed; check the saved files before retrying.";
            throw new LoanReturnUnsuccessful("Could not save return: " + exception.getMessage() + rollbackWarning);
        }

        item.applyReturnAssessment(assessed);
        loan.completeReturn(snapshot);
        assert loan.getReturnAssessment() == snapshot : "Saved return assessment must belong to this loan";
        assert !item.isLoaned() && item.getLoanId() == null : "Returned equipment must have no active loan";
        assert numLoans == loans.size() : "Returning equipment must retain its loan record";
        logger.log(Level.INFO, "Returned loan {0} for item ID {1}; serviceable: {2}.",
                new Object[]{loan.getLoanId(), item.getItemId(), item.isServiceable()});
        return loan;
    }

    /** Rejects missing, duplicated or already returned loan IDs before an assessment is prepared. */
    private Loan findLoanForReturn(String loanId) throws LoanReturnUnsuccessful {
        Loan match = null;
        for (Loan candidate : loans) {
            if (loanId.equals(candidate.getLoanId())) {
                if (match != null) {
                    throw new LoanReturnUnsuccessful("Loan ID " + loanId + " is not unique.");
                }
                match = candidate;
            }
        }
        if (match == null) {
            throw new LoanReturnUnsuccessful("No loan found for loan ID " + loanId + ".");
        }
        if (match.isReturned()) {
            throw new LoanReturnUnsuccessful("Loan " + loanId + " has already been returned.");
        }
        return match;
    }

    /**
     * Deletes the loan matching its unique loan ID.
     * Its equipment is released only after the deletion has been saved successfully.
     *
     * @param input complete delete loan command entered by the user
     * @return the deleted loan
     * @throws LoanDeletionUnsuccessful when the command is invalid
     *                                  or no matching loan is found
     */
    public Loan deleteLoan(String input) throws LoanDeletionUnsuccessful {
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        if (arguments.length != 2 || !arguments[0].equals("delete") || !arguments[1].startsWith("loan/")) {
            throw new LoanDeletionUnsuccessful("Invalid command format. Expected: delete loan/[LOAN ID].");
        }
        String loanId = parseDeleteLoanId(arguments[1]);
        Loan loan = null;
        for (Loan candidate : loans) {
            if (loanId.equals(candidate.getLoanId())) {
                if (loan != null) {
                    throw new LoanDeletionUnsuccessful("Loan ID " + loanId + " is not unique.");
                }
                loan = candidate;
            }
        }
        if (loan == null) {
            logger.log(Level.WARNING, "No loan found for loan ID {0}.", loanId);
            throw new LoanDeletionUnsuccessful("No loan found for loan ID " + loanId + ".");
        }

        ArrayList<Loan> updatedLoans = new ArrayList<>(loans);
        updatedLoans.remove(loan);
        try {
            save(updatedLoans, nextLoanNumber);
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Loan deletion could not be saved", exception);
            throw new LoanDeletionUnsuccessful("Could not save loan deletion: " + exception.getMessage());
        }
        loans.remove(loan);
        loan.detachEquipment();
        numLoans--;
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Deleted loan {0} for item ID {1}.", new Object[]{loanId, loan.getItemId()});
        return loan;
    }

    /** Parses interactive loan syntax, supplying an issue date only when the complete field is absent. */
    private Loan parseLoanCommand(String input) throws LoanAdditionUnsuccessful {
        if (input == null || input.matches("(?s).*\\R.*")) {
            throw new LoanAdditionUnsuccessful(LOAN_COMMAND_FORMAT);
        }
        String[] arguments = input.replaceAll("(?U)^\\s+|\\s+$", "").split("(?U)\\s+");
        if (arguments.length != EXPECTED_ADD_ARGUMENT_COUNT
                && arguments.length != EXPECTED_ADD_ARGUMENT_COUNT - 1) {
            throw new LoanAdditionUnsuccessful(LOAN_COMMAND_FORMAT);
        }
        return parseLoanArguments(arguments, clock);
    }

    /** Requires explicit dates in stored records; loading never supplies a current-date default. */
    static Loan parseLoanRecord(String input) throws LoanAdditionUnsuccessful {
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        if (arguments.length != EXPECTED_ADD_ARGUMENT_COUNT) {
            throw new LoanAdditionUnsuccessful("Incorrect number of arguments.");
        }
        return parseLoanArguments(arguments, null);
    }

    /** Shares ID, supplied-date and date-order validation between commands and complete stored records. */
    private static Loan parseLoanArguments(String[] arguments, Clock clock) throws LoanAdditionUnsuccessful {
        if (!arguments[0].equals("loan")) {
            throw new LoanAdditionUnsuccessful(LOAN_COMMAND_FORMAT);
        }

        String itemId = parseArgument(arguments[1], "id/", "/id", "item ID");
        if (itemId.equals("000")) {
            throw new LoanAdditionUnsuccessful("Item ID must be from 001 to 999.");
        }
        String borrowerId = parseArgument(arguments[2], "b/", "/b", "borrower ID");
        LocalDateTime issuedDate;
        LocalDateTime dueDate;
        if (arguments.length == EXPECTED_ADD_ARGUMENT_COUNT) {
            issuedDate = parseDateArgument(arguments[3], "issued/", "/issued");
            dueDate = parseDateArgument(arguments[4], "due/", "/due");
        } else {
            // Validate the required due date before reading the clock. Never default an invalid issued/ field.
            dueDate = parseDateArgument(arguments[3], "due/", "/due");
            issuedDate = LocalDate.now(clock).atStartOfDay();
        }

        if (issuedDate.isAfter(dueDate)) {
            throw new LoanAdditionUnsuccessful("Issued date must not be later than the due date.");
        }

        return new Loan(itemId, borrowerId, issuedDate, dueDate);
    }

    /** Saves the supplied loans when this handler has a backing file. */
    private void save(List<Loan> loansToSave, int nextNumber) throws IOException {
        if (storageFile != null) {
            LoanStorage.save(storageFile, loansToSave, sequenceFile, nextNumber);
            logger.log(Level.FINE, "Saved {0} loan(s) to {1}", new Object[]{loansToSave.size(), storageFile});
        }
    }

    /**
     * Extracts and validates an ID argument.
     *
     * @param argument        complete argument, including its prefix
     * @param prefix          required prefix for the documented argument format
     * @param alternatePrefix required prefix for the alternate argument format
     * @param name            human-readable argument name for the error message
     * @return the validated three-digit ID
     */
    private static String parseArgument(String argument, String prefix, String alternatePrefix, String name)
            throws LoanAdditionUnsuccessful {
        String value;
        if (argument.startsWith(prefix)) {
            value = argument.substring(prefix.length());
        } else if (argument.startsWith(alternatePrefix)) {
            value = argument.substring(alternatePrefix.length());
        } else {
            throw idFormatError(prefix, alternatePrefix, name);
        }

        if (!value.matches(ID_FORMAT)) {
            throw idFormatError(prefix, alternatePrefix, name);
        }
        return value;
    }

    /**
     * Extracts and validates the loan ID from a delete loan command.
     *
     * @param argument loan ID argument, including its prefix
     * @return the validated three-digit loan ID
     * @throws LoanDeletionUnsuccessful when the format is invalid
     */
    private String parseDeleteLoanId(String argument) throws LoanDeletionUnsuccessful {
        String loanId = argument.substring("loan/".length());
        if (!loanId.matches(ID_FORMAT) || loanId.equals("000")) {
            throw new LoanDeletionUnsuccessful("Invalid loan ID \"" + loanId
                    + "\". Loan ID must be exactly three digits from 001 to 999.");
        }
        return loanId;
    }

    /**
     * Creates an error for an invalid ID argument.
     */
    private static LoanAdditionUnsuccessful idFormatError(String prefix, String alternatePrefix, String name) {
        return new LoanAdditionUnsuccessful("Invalid " + name + " format. Expected "
                + prefix + "[three digits] or " + alternatePrefix + "[three digits].");
    }

    /**
     * Extracts and parses a date argument as a {@link LocalDateTime} at midnight.
     *
     * @param argument        complete date argument, including its prefix
     * @param prefix          required prefix for the documented date format
     * @param alternatePrefix required prefix for the alternate date format
     * @return the parsed date at midnight
     */
    private static LocalDateTime parseDateArgument(String argument, String prefix, String alternatePrefix)
            throws LoanAdditionUnsuccessful {
        String value;
        if (argument.startsWith(prefix)) {
            value = argument.substring(prefix.length());
        } else if (argument.startsWith(alternatePrefix)) {
            value = argument.substring(alternatePrefix.length());
        } else {
            throw dateFormatError(prefix, alternatePrefix);
        }

        try {
            LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
            return date.atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw dateFormatError(prefix, alternatePrefix);
        }
    }

    /**
     * Creates an error for an invalid date argument.
     */
    private static LoanAdditionUnsuccessful dateFormatError(String prefix, String alternatePrefix) {
        return new LoanAdditionUnsuccessful("Invalid date format. Expected " + prefix + "DD-MM-YY or "
                + alternatePrefix + "DD-MM-YY.");
    }

    /**
     * Checks whether an item currently has an unreturned loan.
     *
     * @param itemId equipment ID to check
     * @return whether deletion must be blocked to preserve an active loan's equipment association
     */
    public boolean hasActiveLoanForItem(String itemId) {
        assert itemId != null : "Active-loan checks require an item ID";
        for (Loan loan : loans) {
            if (loan.getItemId().equals(itemId) && !loan.isReturned()) {
                return true;
            }
        }
        return false;
    }

    // @@author Rohita-29

    /**
     * Returns active loans whose due dates are before the reference date.
     * The returned list preserves the order of the stored loans.
     *
     * @param referenceDate date against which loan due dates are checked
     * @return an unmodifiable list containing the overdue loans
     * @throws NullPointerException if the reference date is null
     */
    public List<Loan> getOverdueLoans(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "Reference date must not be null");
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";

        ArrayList<Loan> overdueLoans = new ArrayList<>();
        for (Loan loan : loans) {
            assert loan != null : "Loan list must not contain null items";
            if (loan.isOverdue(referenceDate)) {
                overdueLoans.add(loan);
            }
        }
        return Collections.unmodifiableList(overdueLoans);
    }

    /**
     * Displays active loans whose due dates are before the reference date.
     *
     * @param referenceDate date against which loan due dates are checked
     * @throws NullPointerException if the reference date is null
     */
    public void viewOverdueLoans(LocalDate referenceDate) {
        List<Loan> overdueLoans = getOverdueLoans(referenceDate);
        logger.log(Level.INFO, "Viewing {0} overdue loan(s)", overdueLoans.size());
        LoanView.print(overdueLoans, referenceDate, "Overdue loans", "No overdue loans found.");
    }

    // @@author

    // @@author rktheg

    /**
     * Compares immutable issue and return assessments for one loan without reading current equipment.
     * Active loans and missing legacy assessments are labelled explicitly.
     *
     * @param input complete view-loan command
     * @throws LoanViewingUnsuccessful if the command or loan ID is invalid
     */
    public void viewLoanAssessments(String input) throws LoanViewingUnsuccessful {
        String loanId = parseViewLoanId(input);
        Loan loan = findLoanForViewing(loanId);
        EquipmentConditionSnapshot issued = loan.getIssueConditionSnapshot();
        LoanReturnAssessment returned = loan.getReturnAssessment();
        EquipmentConditionSnapshot assessed = returned == null ? null : returned.condition();
        String returnStatus = loan.isReturned() ? "Not recorded" : "Not returned";

        System.out.println("Loan " + loanId + " condition assessments (Item ID: " + loan.getItemId() + ")");
        System.out.println("Status: " + (loan.isReturned() ? "Returned" : "Active"));
        printAssessmentRow("Field", "Issued", "Returned");
        printAssessmentRow("Type", issued == null ? "Not recorded" : issued.getEquipmentType(),
                assessed == null ? returnStatus : assessed.getEquipmentType());
        printAssessmentRow("Condition", issued == null ? "Not recorded" : String.valueOf(issued.getCondition()),
                assessed == null ? returnStatus : String.valueOf(assessed.getCondition()));
        printAssessmentRow("Damage", issued == null ? "Not recorded" : issued.getDamage(),
                assessed == null ? returnStatus : assessed.getDamage());
        printAssessmentRow("Serviceable", serviceabilityText(loan.getIssueServiceability()),
                returned == null ? returnStatus : serviceabilityText(returned.serviceable()));
        // Keep remarks outside the columns so two 200-character notes do not make a very wide table.
        System.out.println("Issue remarks: " + assessmentRemarks(issued, "Not recorded"));
        System.out.println("Return remarks: " + assessmentRemarks(assessed, returnStatus));
        logger.log(Level.FINE, "Viewed condition assessments for loan {0}; issue assessment recorded: {1}; "
                + "return assessment recorded: {2}.", new Object[]{loanId, issued != null, returned != null});
    }

    /** Keeps missing legacy information distinct from an explicitly saved yes/no assessment. */
    private static String serviceabilityText(Boolean serviceable) {
        return serviceable == null ? "Not recorded" : serviceable ? "Yes" : "No";
    }

    /** Aligns the short assessment values while leaving longer notes on separate labelled lines. */
    private static void printAssessmentRow(String field, String issued, String returned) {
        System.out.printf("%-12s | %-24s | %s%n", field, issued, returned);
    }

    /** Distinguishes an empty recorded note from a missing historical assessment. */
    private static String assessmentRemarks(EquipmentConditionSnapshot snapshot, String missing) {
        return snapshot == null ? missing : snapshot.getRemarks().isEmpty() ? "None" : snapshot.getRemarks();
    }

    // @@author Rohita-29

    /** Extracts and validates a loan ID from a viewing command. */
    private static String parseViewLoanId(String input)
            throws LoanViewingUnsuccessful {
        String normalizedInput =
                input == null
                        ? ""
                        : input.replaceAll("(?U)^\\s+|\\s+$", "");
        String[] arguments =
                normalizedInput.isEmpty()
                        ? new String[0]
                        : normalizedInput.split("(?U)\\s+");

        if (arguments.length != 2
                || !arguments[0].equals("view")
                || !arguments[1].startsWith("loan/")) {
            throw new LoanViewingUnsuccessful(
                    "Invalid command format. Expected: "
                            + "view loan/<LOAN ID>.");
        }

        String loanId =
                arguments[1].substring("loan/".length());
        if (!loanId.matches(ID_FORMAT)
                || loanId.equals("000")) {
            throw new LoanViewingUnsuccessful(
                    "Invalid loan ID \""
                            + loanId
                            + "\". Loan ID must be exactly "
                            + "three digits from 001 to 999.");
        }

        return loanId;
    }

    /** Finds the unique loan matching an ID without modifying it. */
    private Loan findLoanForViewing(String loanId)
            throws LoanViewingUnsuccessful {
        assert numLoans == loans.size()
                : "numLoans should match the size of the loans list";
        Loan matchingLoan = null;

        for (Loan loan : loans) {
            assert loan != null
                    : "Loan list must not contain null items";
            if (!loanId.equals(loan.getLoanId())) {
                continue;
            }
            if (matchingLoan != null) {
                throw new LoanViewingUnsuccessful(
                        "Loan ID "
                                + loanId
                                + " is not unique.");
            }
            matchingLoan = loan;
        }

        if (matchingLoan == null) {
            throw new LoanViewingUnsuccessful(
                    "No loan found for loan ID "
                            + loanId
                            + ".");
        }

        return matchingLoan;
    }

    // @@author

    /**
     * Prints out all equipment in inventory
     */
    public void viewAllLoans() {
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Viewing {0} loan(s)", numLoans);
        printLoans(loans);
    }

    /**
     * Selects active and returned loans for the currently registered physical item.
     * The query sorts a copy and never guesses associations from a reused Item ID.
     *
     * @param input command in the form {@code history id/001}
     * @return an unmodifiable list in issue-date order, then loan-ID order for equal dates
     * @throws LoanHistoryUnsuccessful if syntax is invalid or the item is missing or ambiguous
     */
    public List<Loan> getLoanHistory(String input) throws LoanHistoryUnsuccessful {
        String itemId = parseHistoryItemId(input);
        Equipment current;
        try {
            current = findEquipment(itemId);
        } catch (LoanAdditionUnsuccessful exception) {
            throw new LoanHistoryUnsuccessful(exception.getMessage());
        }
        List<Loan> history = getLoanHistoryForEquipment(current);
        logger.log(Level.FINE, "Selected {0} history record(s) for item ID {1}.",
                new Object[]{history.size(), itemId});
        return history;
    }

    /**
     * Selects all active and returned loans belonging to one physical equipment record.
     *
     * @param item equipment whose permanent record identity is queried
     * @return an unmodifiable list in issue-date order, then loan-ID order
     */
    public List<Loan> getLoanHistoryForEquipment(Equipment item) {
        if (item == null) {
            throw new IllegalArgumentException("Equipment must not be null.");
        }
        ArrayList<Loan> history = new ArrayList<>();
        for (Loan loan : loans) {
            if (item.getItemId().equals(loan.getItemId())
                    && item.getRecordId().equals(loan.getEquipmentRecordId())) {
                history.add(loan);
            }
        }
        history.sort(Comparator.comparing(Loan::getIssuedDate)
                .thenComparingInt(loan -> Integer.parseInt(loan.getLoanId())));
        assert history.stream().allMatch(loan -> loan.getEquipmentRecordId() != null)
                : "Equipment history must contain attributed loan records";
        return List.copyOf(history);
    }

    /** Displays a physical item's history and explains any older returned records that cannot be attributed. */
    public void viewLoanHistory(String input) throws LoanHistoryUnsuccessful {
        List<Loan> history = getLoanHistory(input);
        String itemId = parseHistoryItemId(input);
        printLoans(history, "Loan history for item ID " + itemId,
                "No loan history for item ID " + itemId + ".");
        boolean hasUnattributedHistory = loans.stream().anyMatch(loan -> itemId.equals(loan.getItemId())
                && loan.isReturned() && loan.getEquipmentRecordId() == null);
        if (hasUnattributedHistory) {
            System.out.println("Earlier returned loans for this Item ID cannot be attributed to the current item. "
                    + "Use list loans to view them.");
        }
    }

    /**
     * Returns active loans held by the borrower in a borrower query.
     * A borrower is known when at least one current or returned loan uses the ID.
     *
     * @param input command in the form {@code list borrower/002}
     * @return an unmodifiable snapshot containing only the borrower's active loans
     * @throws LoanBorrowerUnsuccessful if syntax is invalid or the borrower is unknown
     */
    public List<Loan> getActiveLoansForBorrower(String input) throws LoanBorrowerUnsuccessful {
        LoanBorrowerQuery query = parseBorrowerQuery(input);
        return findActiveLoansForBorrower(query);
    }

    /** Displays the active equipment held by a borrower without changing application state. */
    public void viewActiveLoansForBorrower(String input) throws LoanBorrowerUnsuccessful {
        LoanBorrowerQuery query = parseBorrowerQuery(input);
        List<Loan> activeLoans = findActiveLoansForBorrower(query);
        printLoans(activeLoans, "Equipment currently held by borrower ID " + query.borrowerId() + ":",
                "No active loans found for borrower ID " + query.borrowerId() + ".");
    }

    /** Parses and logs malformed borrower queries before they reach the loan collection. */
    private LoanBorrowerQuery parseBorrowerQuery(String input) throws LoanBorrowerUnsuccessful {
        try {
            return LoanBorrowerQuery.parse(input);
        } catch (LoanBorrowerUnsuccessful exception) {
            logger.log(Level.WARNING, "Borrower loan query rejected: {0}", exception.getMessage());
            throw exception;
        }
    }

    /** Selects a borrower's active loans while preserving the stored loan order and all state. */
    private List<Loan> findActiveLoansForBorrower(LoanBorrowerQuery query)
            throws LoanBorrowerUnsuccessful {
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        ArrayList<Loan> activeLoans = new ArrayList<>();
        boolean borrowerKnown = false;
        for (Loan loan : loans) {
            assert loan != null : "Loan list must not contain null items";
            if (query.borrowerId().equals(loan.getBorrowerId())) {
                borrowerKnown = true;
                if (!loan.isReturned()) {
                    activeLoans.add(loan);
                }
            }
        }
        if (!borrowerKnown) {
            logger.log(Level.WARNING, "Borrower loan query found no borrower with ID {0}.", query.borrowerId());
            throw new LoanBorrowerUnsuccessful("No member found for borrower ID " + query.borrowerId() + ".");
        }
        assert activeLoans.stream().allMatch(loan -> query.borrowerId().equals(loan.getBorrowerId())
                && !loan.isReturned()) : "Borrower query must return only active matching loans";
        logger.log(Level.INFO, "Viewed {0} active loan(s) for borrower ID {1}.",
                new Object[]{activeLoans.size(), query.borrowerId()});
        return List.copyOf(activeLoans);
    }

    /** Checks history syntax without reading or writing any record. */
    private static String parseHistoryItemId(String input) throws LoanHistoryUnsuccessful {
        if (input != null && !input.matches("(?s).*\\R.*")) {
            Matcher matcher = HISTORY_COMMAND.matcher(input.replaceAll("(?U)^\\s+|\\s+$", ""));
            if (matcher.matches() && !matcher.group(1).equals("000")) {
                return matcher.group(1);
            }
        }
        throw new LoanHistoryUnsuccessful("Invalid format. Use: history id/[ITEM ID], from 001 to 999.");
    }

    /** Prints loan records using the same table format as the all-loans command. */
    public void printLoans(List<Loan> records) {
        printLoans(records, "Loans", "No loans recorded. Type help loan for the command format.");
    }

    /** Shares the same rows and current-date state with contextual headings and empty messages. */
    public void printLoans(List<Loan> records, String heading, String emptyMessage) {
        if (records == null) {
            throw new IllegalArgumentException("Loan records must not be null.");
        }
        for (Loan record : records) {
            assert record != null : "Loan records must not contain null items";
        }
        LoanView.print(records, LocalDate.now(clock), heading, emptyMessage);
    }

    /** Formats identifiers independently of the computer's default locale. */
    private static String formatLoanId(int loanNumber) {
        return String.format(Locale.ROOT, "%03d", loanNumber);
    }

    /** Places the sequence beside the loan file by default. */
    private static Path defaultSequenceFile(Path file) {
        return file == null ? null : file.toAbsolutePath().resolveSibling("loan-sequence.txt");
    }
}
