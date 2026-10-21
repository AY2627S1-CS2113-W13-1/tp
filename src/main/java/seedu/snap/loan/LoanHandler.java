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

import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Parses loan commands and stores successfully created loans. */
public class LoanHandler {
    private static final int EXPECTED_ADD_ARGUMENT_COUNT = 5;
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

    /** Creates an in-memory loan handler, mainly useful for isolated unit tests. */
    public LoanHandler() {
        this(null);
    }

    /** Creates a loan handler backed by the supplied text file. */
    public LoanHandler(Path storageFile) {
        this.storageFile = storageFile;
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
        save(loans);
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
        List<Loan> loaded = LoanStorage.load(storageFile);
        Set<String> activeItemIds = new HashSet<>();
        for (Loan loan : loaded) {
            if (!loan.isReturned() && !activeItemIds.add(loan.getItemId())) {
                throw new IOException("Multiple active loans found for item ID " + loan.getItemId() + ".");
            }
        }
        loans.clear();
        loans.addAll(loaded);
        numLoans = loans.size();
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
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
        Loan loan = parseLoanRecord(input);
        String itemId = loan.getItemId();

        if (hasActiveLoanForItem(itemId)) {
            throw new LoanAdditionUnsuccessful("An active loan already exists for item ID " + itemId + ".");
        }

        assert loan != null : "Created loan must not be null.";
        ArrayList<Loan> updatedLoans = new ArrayList<>(loans);
        updatedLoans.add(loan);
        try {
            save(updatedLoans);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Loan addition could not be saved", e);
            throw new LoanAdditionUnsuccessful("Could not save loan: " + e.getMessage());
        }

        loans.add(loan);
        numLoans++;
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.FINE, "Added loan for item ID {0}.", itemId);
        return loan;
    }

    /**
     * * Deletes the loan matching the item ID, and the issued date if one is given.
     *
     * @param input complete delete-loan command entered by the user
     * @return the deleted loan
     * @throws LoanDeletionUnsuccessful when the command is invalid
     *                                  or no matching loan is found
     */
    public Loan deleteLoan(String input) throws LoanDeletionUnsuccessful {
        String[] arguments = input == null ? new String[0] : input.trim().split("\\s+");
        boolean hasValidCount = arguments.length == 2 || arguments.length == 3;
        if (!hasValidCount || !arguments[0].equals("delete-loan")) {
            throw new LoanDeletionUnsuccessful("Invalid command format. Expected: "
                    + "delete-loan id/[ITEM ID] [issued/DD-MM-YY].");
        }

        String itemId = parseDeleteItemId(arguments[1]);
        LocalDateTime issuedDate = arguments.length == 3 ? parseDeleteIssuedDate(arguments[2]) : null;

        ArrayList<Loan> matches = new ArrayList<>();
        for (Loan loan : loans) {
            boolean isSameItem = loan.getItemId().equals(itemId);
            boolean isSameDate = issuedDate == null || loan.getIssuedDate().equals(issuedDate);
            if (isSameItem && isSameDate) {
                matches.add(loan);
            }
        }

        if (matches.isEmpty()) {
            logger.log(Level.WARNING, "No loan found for item ID " + itemId);
            throw new LoanDeletionUnsuccessful("No loan found for item ID " + itemId + ".");
        }
        if (matches.size() > 1) {
            logger.log(Level.WARNING, "Multiple loans found for item ID " + itemId);
            throw new LoanDeletionUnsuccessful(matches.size() + " loans found for item ID " + itemId
                    + ". Specify which one: delete-loan id/" + itemId + " issued/DD-MM-YY.");
        }

        Loan loan = matches.get(0);
        ArrayList<Loan> updatedLoans = new ArrayList<>(loans);
        updatedLoans.remove(loan);
        try {
            save(updatedLoans);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Loan deletion could not be saved", e);
            throw new LoanDeletionUnsuccessful("Could not save loan deletion: " + e.getMessage());
        }

        loans.remove(loan);
        numLoans--;
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Deleted loan for item ID " + itemId);
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
            LoanStorage.save(storageFile, loansToSave);
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
     * Extracts and validates the item ID from a delete-loan command.
     *
     * @param argument item ID argument, including its prefix
     * @return the validated three-digit item ID
     * @throws LoanDeletionUnsuccessful when the format is invalid
     */
    private String parseDeleteItemId(String argument) throws LoanDeletionUnsuccessful {
        String itemId;
        if (argument.startsWith("id/")) {
            itemId = argument.substring("id/".length());
        } else if (argument.startsWith("/id")) {
            itemId = argument.substring("/id".length());
        } else {
            throw new LoanDeletionUnsuccessful(
                    "Missing item ID prefix. Expected id/[three digits] or /id[three digits].");
        }

        if (!itemId.matches(ID_FORMAT)) {
            throw new LoanDeletionUnsuccessful("Invalid item ID \"" + itemId
                    + "\". Item ID must be exactly three digits, e.g. 001.");
        }
        return itemId;
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
        for (Loan loan : loans) {
            if (loan.getItemId().equals(itemId) && !loan.isReturned()) {
                return true;
            }
        }
        return false;
    }

    private LocalDateTime parseDeleteIssuedDate(String argument) throws LoanDeletionUnsuccessful {
        try {
            return parseDateArgument(argument, "issued/", "/issued");
        } catch (LoanAdditionUnsuccessful e) {
            throw new LoanDeletionUnsuccessful(e.getMessage());
        }
    }

    /**
     * Prints out all equipment in inventory
     */
    public void viewAllLoans() {
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Viewing {0} loan(s)", numLoans);
        System.out.printf(
                "%-4s %-12s | %-12s | %-16s | %-16s%n",
                "No.", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE"
        );

        for (int i = 0; i < numLoans; i++) {
            Loan item = loans.get(i);
            assert item != null : "Loan list must not contain null items";

            System.out.printf(
                    "%-4s %-12s | %-12s | %-16s | %-16s%n",
                    (i + 1) + ")", item.getItemId(), item.getBorrowerId(), item.getIssuedDate(), item.getDueDate()
            );
        }
    }
}
