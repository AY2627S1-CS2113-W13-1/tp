package seedu.snap.loan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.Set;

import seedu.snap.exceptions.LoanUpdateUnsuccessful;

/** Holds validated optional changes from an update-loan command before any records change. */
record LoanUpdate(String loanId, String itemId, String borrowerId,
        LocalDateTime issuedDate, LocalDateTime dueDate) {
    private static final String ID_FORMAT = "[0-9]{3}";
    private static final String FORMAT = "Invalid format. Use: update loan/[LOAN ID] "
            + "[id/[ITEM ID]] [b/[BORROWER ID]] [issued/DD-MM-YY] [due/DD-MM-YY].";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Parses a complete update command and rejects duplicate or unsupported fields. */
    static LoanUpdate parse(String input) throws LoanUpdateUnsuccessful {
        if (input == null || input.matches("(?s).*\\R.*")) {
            throw new LoanUpdateUnsuccessful(FORMAT);
        }
        String trimmed = input.trim();
        String[] arguments = trimmed.isEmpty() ? new String[0] : trimmed.split("(?U)\\s+");
        if (arguments.length < 3 || !arguments[0].equals("update")
                || !arguments[1].startsWith("loan/")) {
            throw new LoanUpdateUnsuccessful(FORMAT);
        }

        String loanId = parseIdentifier(arguments[1].substring("loan/".length()), "loan ID");
        String itemId = null;
        String borrowerId = null;
        LocalDateTime issuedDate = null;
        LocalDateTime dueDate = null;
        Set<String> seenFields = new HashSet<>();
        for (int i = 2; i < arguments.length; i++) {
            String argument = arguments[i];
            String field;
            String value;
            if (argument.startsWith("id/")) {
                field = "id";
                value = argument.substring("id/".length());
                itemId = parseIdentifier(value, "item ID");
            } else if (argument.startsWith("b/")) {
                field = "b";
                value = argument.substring("b/".length());
                borrowerId = parseBorrowerId(value);
            } else if (argument.startsWith("issued/")) {
                field = "issued";
                value = argument.substring("issued/".length());
                issuedDate = parseDate(value, "issued");
            } else if (argument.startsWith("due/")) {
                field = "due";
                value = argument.substring("due/".length());
                dueDate = parseDate(value, "due");
            } else {
                throw new LoanUpdateUnsuccessful(FORMAT);
            }
            if (!seenFields.add(field)) {
                throw new LoanUpdateUnsuccessful("Each loan update field may be supplied only once.");
            }
        }

        if (issuedDate != null && dueDate != null && issuedDate.isAfter(dueDate)) {
            throw new LoanUpdateUnsuccessful("Issued date must not be later than the due date.");
        }
        return new LoanUpdate(loanId, itemId, borrowerId, issuedDate, dueDate);
    }

    /** Returns whether this command supplies an item ID replacement. */
    boolean changesItem() {
        return itemId != null;
    }

    /** Returns whether this command supplies a borrower replacement. */
    boolean changesBorrower() {
        return borrowerId != null;
    }

    /** Returns whether this command supplies an issue-date replacement. */
    boolean changesIssuedDate() {
        return issuedDate != null;
    }

    /** Returns whether this command supplies a due-date replacement. */
    boolean changesDueDate() {
        return dueDate != null;
    }

    /** Parses a three-digit identifier while excluding the reserved all-zero value. */
    private static String parseIdentifier(String value, String name) throws LoanUpdateUnsuccessful {
        if (!value.matches(ID_FORMAT) || value.equals("000")) {
            throw new LoanUpdateUnsuccessful(name + " must be exactly three digits from 001 to 999.");
        }
        return value;
    }

    /** Parses the borrower identifier range, which includes the valid boundary value 000. */
    private static String parseBorrowerId(String value) throws LoanUpdateUnsuccessful {
        if (!value.matches(ID_FORMAT)) {
            throw new LoanUpdateUnsuccessful("Borrower ID must be exactly three digits from 000 to 999.");
        }
        return value;
    }

    /** Parses a strict date in the format documented for loan commands. */
    private static LocalDateTime parseDate(String value, String name) throws LoanUpdateUnsuccessful {
        try {
            return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new LoanUpdateUnsuccessful("Invalid " + name + " date. Expected DD-MM-YY.");
        }
    }
}
