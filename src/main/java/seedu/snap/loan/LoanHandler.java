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
    private static final String ID_FORMAT = "\\d{3}";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Loans managed by this handler instance. */
    private final ArrayList<Loan> loans = new ArrayList<>();
    /** Cached loan count kept in sync with {@link #loans}. */
    private int numLoans = 0;
    /** File used for persistence, or null when the handler is intentionally in-memory only. */
    private final Path storageFile;
    /** Equipment inventory used to validate and update availability. */
    private final EquipmentHandler equipment;
    /** File containing the next monotonic loan identifier, or null for in-memory handlers. */
    private final Path sequenceFile;
    /** Next unused numeric loan identifier. */
    private int nextLoanNumber = FIRST_LOAN_NUMBER;

    /** Creates an in-memory loan handler, mainly useful for isolated unit tests. */
    public LoanHandler() {
        this(null, null, null);
    }

    /** Creates a loan handler backed by the supplied text file. */
    public LoanHandler(Path storageFile) {
        this(null, storageFile, defaultSequenceFile(storageFile));
    }

    /** Creates an in-memory loan handler connected to an equipment inventory. */
    public LoanHandler(EquipmentHandler equipment) {
        this(equipment, null, null);
    }

    /** Creates a loan handler connected to an inventory and optional storage file. */
    public LoanHandler(EquipmentHandler equipment, Path storageFile) {
        this(equipment, storageFile, defaultSequenceFile(storageFile));
    }

    /** Creates a loan handler with explicit loan and sequence persistence files. */
    public LoanHandler(EquipmentHandler equipment, Path storageFile, Path sequenceFile) {
        this.equipment = equipment;
        this.storageFile = storageFile;
        this.sequenceFile = sequenceFile;
    }

    /** Returns the number of currently loaded loans. */
    public int getLoanCount() {
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        return numLoans;
    }

    /** Returns the next identifier that will be assigned, or 1000 after the limit is exhausted. */
    public String getNextLoanId() {
        return String.format("%03d", nextLoanNumber);
    }

    /** Returns a read-only view of this handler's loans for inspection and display. */
    public List<Loan> getLoanList() {
        return Collections.unmodifiableList(loans);
    }

    /** Persists the current loans when this handler has a backing file. */
    public void saveToStorage() throws IOException {
        save(loans);
        if (equipment != null) {
            equipment.saveToStorage();
        }
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
        LoanStorage.LoadedLoans loadedResult = LoanStorage.load(storageFile, sequenceFile);
        List<Loan> loaded = loadedResult.loans();
        Set<String> activeItemIds = new HashSet<>();
        for (Loan loan : loaded) {
            if (!loan.isReturned() && !activeItemIds.add(loan.getItemId())) {
                throw new IOException("Multiple active loans found for item ID " + loan.getItemId() + ".");
            }
        }

        boolean equipmentStateChanged = false;
        if (equipment != null) {
            // Active loan records are authoritative. Clear stale persisted IDs before rebuilding
            // associations so legacy loaned/true records cannot reserve an item indefinitely.
            for (Equipment item : equipment.getEquipmentList()) {
                if (item.getLoanId() != null) {
                    logger.log(Level.WARNING, "Clearing stale persisted loan ID {0} from item ID {1}.",
                            new Object[]{item.getLoanId(), item.getItemId()});
                    item.setLoanId(null);
                    equipmentStateChanged = true;
                }
            }
            for (Loan loan : loaded) {
                Equipment item = findStoredEquipment(loan.getItemId());
                if (item != null) {
                    String previousLoanId = item.getLoanId();
                    loan.attachEquipment(item);
                    equipmentStateChanged |= !java.util.Objects.equals(previousLoanId, item.getLoanId());
                } else if (!loan.isReturned()) {
                    logger.log(Level.WARNING, "Saved active loan {0} for item ID {1} has no equipment yet.",
                            new Object[]{loan.getLoanId(), loan.getItemId()});
                }
            }
        }
        loans.clear();
        loans.addAll(loaded);
        numLoans = loans.size();
        nextLoanNumber = loadedResult.nextLoanNumber();
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        assert nextLoanNumber >= FIRST_LOAN_NUMBER && nextLoanNumber <= MAX_LOAN_NUMBER + 1
                : "Next loan number must remain within its supported range";
        if (loadedResult.migrated()) {
            // Persist migrated identifiers immediately so the next process observes the same IDs.
            LoanStorage.save(storageFile, loans, sequenceFile, nextLoanNumber);
            logger.log(Level.INFO, "Migrated legacy loan records to persistent loan IDs.");
        }
        if (equipment != null && (loadedResult.migrated() || equipmentStateChanged)) {
            equipment.saveToStorage();
        }
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
        Loan parsedLoan = parseLoanRecord(input);
        String itemId = parsedLoan.getItemId();
        Equipment item = equipment == null ? null : findAvailableEquipment(itemId);
        if (nextLoanNumber > MAX_LOAN_NUMBER) {
            logger.log(Level.WARNING, "Loan rejected: the 999 loan ID limit has been reached.");
            throw new LoanAdditionUnsuccessful("No loan IDs remain; the maximum is 999.");
        }
        String loanId = formatLoanId(nextLoanNumber);
        Loan loan = item == null
                ? new Loan(loanId, itemId, parsedLoan.getBorrowerId(), parsedLoan.getIssuedDate(),
                        parsedLoan.getDueDate())
                : new Loan(loanId, item, parsedLoan.getBorrowerId(), parsedLoan.getIssuedDate(),
                        parsedLoan.getDueDate());
        assert loan != null : "Created loan must not be null.";
        ArrayList<Loan> updatedLoans = new ArrayList<>(loans);
        updatedLoans.add(loan);
        String previousLoanId = item == null ? null : item.getLoanId();
        if (item != null) {
            item.setLoanId(loanId);
        }
        try {
            save(updatedLoans);
            if (equipment != null) {
                equipment.saveToStorage();
            }
        } catch (IOException e) {
            if (item != null) {
                item.setLoanId(previousLoanId);
            }
            try {
                save(loans);
            } catch (IOException rollbackException) {
                e.addSuppressed(rollbackException);
            }
            logger.log(Level.WARNING, "Loan addition could not be saved", e);
            throw new LoanAdditionUnsuccessful("Could not save loan: " + e.getMessage());
        }

        loans.add(loan);
        numLoans = loans.size();
        nextLoanNumber++;
        assert item == null || loanId.equals(item.getLoanId()) : "Loan and equipment IDs must agree";
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Added loan {0} for item ID {1}.", new Object[]{loanId, itemId});
        return loan;
    }

    /** Finds one available item, rejecting missing or ambiguous IDs before a loan changes any state. */
    private Equipment findAvailableEquipment(String itemId) throws LoanAdditionUnsuccessful {
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
        if (match.getLoanId() != null) {
            logger.log(Level.WARNING, "Loan rejected: item ID {0} is already loaned out.", itemId);
            throw new LoanAdditionUnsuccessful("Item ID " + itemId + " is already loaned out.");
        }
        return match;
    }

    /**
     * Deletes the loan matching its unique loan ID.
     *
     * @param input complete delete-loan command entered by the user
     * @return the deleted loan
     * @throws LoanDeletionUnsuccessful when the command is invalid
     *                                  or no matching loan is found
     */
    public Loan deleteLoan(String input) throws LoanDeletionUnsuccessful {
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        if (arguments.length != 2 || !arguments[0].equals("delete-loan")) {
            throw new LoanDeletionUnsuccessful("Invalid command format. Expected: "
                    + "delete-loan id/[LOAN ID].");
        }

        String loanId = parseDeleteLoanId(arguments[1]);
        Loan loan = null;
        for (Loan candidate : loans) {
            if (loanId.equals(candidate.getLoanId())) {
                if (loan != null) {
                    logger.log(Level.WARNING, "Loan deletion rejected: duplicate loan ID {0}.", loanId);
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
        Equipment item = loan.getEquipment();
        try {
            save(updatedLoans);
            loan.detachEquipment();
            if (equipment != null) {
                equipment.saveToStorage();
            }
        } catch (IOException e) {
            if (item != null && loan.getEquipment() == null) {
                try {
                    loan.attachEquipment(item);
                } catch (RuntimeException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
            }
            try {
                save(loans);
            } catch (IOException rollbackException) {
                e.addSuppressed(rollbackException);
            }
            logger.log(Level.WARNING, "Loan deletion could not be saved", e);
            throw new LoanDeletionUnsuccessful("Could not save loan deletion: " + e.getMessage());
        }

        loans.remove(loan);
        numLoans = loans.size();
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Deleted loan {0} for item ID {1}.",
                new Object[]{loanId, loan.getItemId()});
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
        String borrowerId = parseArgument(arguments[2], "b/", "/b", "borrower ID");
        LocalDateTime issuedDate = parseDateArgument(arguments[3], "issued/", "/issued");
        LocalDateTime dueDate = parseDateArgument(arguments[4], "due/", "/due");

        if (issuedDate.isAfter(dueDate)) {
            throw new LoanAdditionUnsuccessful("Issued date must not be later than the due date.");
        }

        return new Loan(itemId, borrowerId, issuedDate, dueDate);
    }

    /** Saves the supplied loans when this handler has a backing file. */
    private void save(List<Loan> loansToSave) throws IOException {
        if (storageFile != null) {
            LoanStorage.save(storageFile, loansToSave, sequenceFile, nextLoanNumber
                    + (loansToSave.size() > loans.size() ? 1 : 0));
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
     * Extracts and validates the loan ID from a delete-loan command.
     *
     * @param argument loan ID argument, including its prefix
     * @return the validated three-digit loan ID
     * @throws LoanDeletionUnsuccessful when the format is invalid
     */
    private String parseDeleteLoanId(String argument) throws LoanDeletionUnsuccessful {
        String loanId;
        if (argument.startsWith("id/")) {
            loanId = argument.substring("id/".length());
        } else {
            throw new LoanDeletionUnsuccessful(
                    "Missing loan ID prefix. Expected id/[three digits].");
        }

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
     * @return whether an active loan record exists for the item
     * @deprecated use {@link Equipment#getLoanId()} for equipment availability decisions
     */
    @Deprecated
    public boolean hasActiveLoanForItem(String itemId) {
        assert itemId != null : "Active-loan checks require an item ID";
        for (Loan loan : loans) {
            if (loan.getItemId().equals(itemId) && !loan.isReturned()) {
                return true;
            }
        }
        return false;
    }

    /** Associates a newly loaded equipment item with any matching active loan. */
    public void synchronizeEquipment(Equipment item) {
        assert item != null : "Equipment synchronization requires an item";
        Loan activeLoan = null;
        for (Loan loan : loans) {
            if (loan.getItemId().equals(item.getItemId()) && !loan.isReturned()) {
                if (activeLoan != null) {
                    logger.log(Level.WARNING, "Cannot synchronize ambiguous active loans for item ID {0}.",
                            item.getItemId());
                    throw new IllegalStateException("Multiple active loans exist for item ID "
                            + item.getItemId() + ".");
                }
                activeLoan = loan;
            }
        }
        if (activeLoan != null) {
            item.setLoanId(activeLoan.getLoanId());
            activeLoan.attachEquipment(item);
            logger.log(Level.INFO, "Reconnected active loan {0} for item ID {1}.",
                    new Object[]{activeLoan.getLoanId(), item.getItemId()});
        } else if (item.getLoanId() != null) {
            logger.log(Level.WARNING, "Clearing loan ID {0} from returned item ID {1}.",
                    new Object[]{item.getLoanId(), item.getItemId()});
            item.setLoanId(null);
        }
    }

    /** Finds a uniquely identified equipment item while restoring saved loan associations. */
    private Equipment findStoredEquipment(String itemId) throws IOException {
        Equipment match = null;
        for (Equipment item : equipment.getEquipmentList()) {
            if (item.getItemId().equals(itemId)) {
                if (match != null) {
                    throw new IOException("Item ID " + itemId + " is not unique.");
                }
                match = item;
            }
        }
        return match;
    }

    /**
     * Prints out all equipment in inventory
     */
    public void viewAllLoans() {
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Viewing {0} loan(s)", numLoans);
        System.out.printf(
                "%-4s %-10s | %-12s | %-12s | %-16s | %-16s%n",
                "No.", "LOAN ID", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE"
        );

        for (int i = 0; i < numLoans; i++) {
            Loan item = loans.get(i);
            assert item != null : "Loan list must not contain null items";

            System.out.printf(
                    "%-4s %-10s | %-12s | %-12s | %-16s | %-16s%n",
                    (i + 1) + ")", item.getLoanId(), item.getItemId(), item.getBorrowerId(),
                    item.getIssuedDate(), item.getDueDate()
            );
        }
    }

    /** Returns the next loan identifier in canonical three-digit form. */
    private static String formatLoanId(int loanNumber) {
        return String.format("%03d", loanNumber);
    }

    /** Returns the default sequence file beside a loan storage file. */
    private static Path defaultSequenceFile(Path storageFile) {
        return storageFile == null ? null : storageFile.toAbsolutePath().resolveSibling("loan-sequence.txt");
    }
}
