package seedu.snap.loan;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Parses loan commands and stores successfully created loans. */
public class LoanHandler {
    private static final int EXPECTED_ADD_ARGUMENT_COUNT = 5;
    private static final int FIRST_LOAN_NUMBER = 1;
    private static final int MAX_LOAN_NUMBER = 999;
    private static final Logger logger = Logger.getLogger(LoanHandler.class.getName());
    private static final String ID_FORMAT = "[0-9]{3}";
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
        assert equipment != null : "Loan handler requires an inventory";
        this.equipment = equipment;
        this.storageFile = storageFile;
        this.sequenceFile = sequenceFile;
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
        for (Loan loan : loaded) {
            if (loan.isReturned()) {
                continue;
            }
            if (!activeItemIds.add(loan.getItemId())) {
                throw new IOException("Multiple active loans found for item ID " + loan.getItemId() + ".");
            }
            try {
                activeEquipment.add(findEquipment(loan.getItemId()));
            } catch (LoanAdditionUnsuccessful exception) {
                throw new IOException("Invalid active loan for item ID " + loan.getItemId() + ": "
                        + exception.getMessage(), exception);
            }
        }

        // Persist migration before changing the current list, counter, equipment flags or links.
        if (result.migrated()) {
            LoanStorage.save(storageFile, loaded, sequenceFile, result.nextLoanNumber());
            logger.log(Level.INFO, "Migrated legacy loan records to persistent loan IDs.");
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
        Loan parsed = parseLoanRecord(input);
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

    /**
     * Parses a loan record without changing the current list.
     * This is also used when validating persisted records.
     */
    static Loan parseLoanRecord(String input) throws LoanAdditionUnsuccessful {
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        if (arguments.length != EXPECTED_ADD_ARGUMENT_COUNT) {
            throw new LoanAdditionUnsuccessful("Incorrect number of arguments.");
        }

        if (!arguments[0].equals("loan")) {
            throw new LoanAdditionUnsuccessful("Invalid command format. Expected: "
                    + "loan id/[ITEM ID] b/[BORROWER ID] issued/[DD-MM-YY] due/[DD-MM-YY].");
        }

        String itemId = parseArgument(arguments[1], "id/", "/id", "item ID");
        if (itemId.equals("000")) {
            throw new LoanAdditionUnsuccessful("Item ID must be from 001 to 999.");
        }
        String borrowerId = parseArgument(arguments[2], "b/", "/b", "borrower ID");
        LocalDateTime issuedDate = parseDateArgument(arguments[3], "issued/", "/issued");
        LocalDateTime dueDate = parseDateArgument(arguments[4], "due/", "/due");

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



    /**
     * Prints out all equipment in inventory
     */
    public void viewAllLoans() {
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Viewing {0} loan(s)", numLoans);
        System.out.printf(
                "%-4s %-12s | %-12s | %-12s | %-16s | %-16s%n",
                "No.", "LOAN ID", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE"
        );
        for (int i = 0; i < numLoans; i++) {
            Loan item = loans.get(i);
            assert item != null : "Loan list must not contain null items";
            System.out.printf(
                    "%-4s %-12s | %-12s | %-12s | %-16s | %-16s%n",
                    (i + 1) + ")", item.getLoanId(), item.getItemId(), item.getBorrowerId(),
                    item.getIssuedDate(), item.getDueDate()
            );
        }
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
