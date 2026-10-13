package seedu.snap.loan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Parses loan commands and stores successfully created loans. */
public class LoanHandler {
    static ArrayList<Loan> loans = new ArrayList<>();
    static int numLoans = 0;

    private static final int EXPECTED_ADD_ARGUMENT_COUNT = 5;
    private static final Logger logger = Logger.getLogger(LoanHandler.class.getName());
    private static final String ID_FORMAT = "\\d{3}";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu")
            .withResolverStyle(ResolverStyle.STRICT);


    /**
     * Adds a loan when the command contains valid IDs and dates.
     *
     * @param input complete loan command entered by the user
     * @return the created loan
     * @throws LoanAdditionUnsuccessful when the command cannot be added
     */
    public Loan addLoan(String input) throws LoanAdditionUnsuccessful {
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

        if (hasActiveLoanForItem(itemId)) {
            throw new LoanAdditionUnsuccessful("An active loan already exists for item ID " + itemId + ".");
        }

        if (issuedDate.isAfter(dueDate)) {
            throw new LoanAdditionUnsuccessful("Issued date must not be later than the due date.");
        }

        Loan loan = new Loan(itemId, borrowerId, issuedDate, dueDate);
        assert loan != null : "Created loan must not be null.";
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
        loans.remove(loan);
        numLoans--;
        assert numLoans == loans.size() : "numLoans should match the size of the loans list";
        logger.log(Level.INFO, "Deleted loan for item ID " + itemId);
        return loan;
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
    private String parseArgument(String argument, String prefix, String alternatePrefix, String name)
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
    private LoanAdditionUnsuccessful idFormatError(String prefix, String alternatePrefix, String name) {
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
    private LocalDateTime parseDateArgument(String argument, String prefix, String alternatePrefix)
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
    private LoanAdditionUnsuccessful dateFormatError(String prefix, String alternatePrefix) {
        return new LoanAdditionUnsuccessful("Invalid date format. Expected " + prefix + "DD-MM-YY or "
                + alternatePrefix + "DD-MM-YY.");
    }

    /**
     * Checks whether an item currently has an unreturned loan.
     */
    private boolean hasActiveLoanForItem(String itemId) {
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
        System.out.printf(
                "%-4s %-12s | %-12s | %-16s | %-16s%n",
                "No.", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE"
        );

        for (int i = 0; i < numLoans; i++) {
            Loan item = loans.get(i);

            System.out.printf(
                    "%-4s %-12s | %-12s | %-16s | %-16s%n",
                    (i + 1) + ")", item.getItemId(), item.getBorrowerId(), item.getIssuedDate(), item.getDueDate()
            );
        }
    }
}
