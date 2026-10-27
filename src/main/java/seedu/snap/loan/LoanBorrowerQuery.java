package seedu.snap.loan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.LoanBorrowerUnsuccessful;

/** Represents a validated command for finding equipment held by one borrower. */
public record LoanBorrowerQuery(String borrowerId) {
    private static final Pattern COMMAND = Pattern.compile(
            "(?U)\\Alist\\s+(?:borrower|member)/([0-9]{3})\\z");

    /** Creates a query after validating the borrower's three-digit identifier. */
    public LoanBorrowerQuery {
        if (borrowerId == null || !borrowerId.matches("[0-9]{3}")) {
            throw new IllegalArgumentException("Borrower ID must be exactly three digits from 000 to 999.");
        }
    }

    /** Parses either the documented borrower form or its member alias. */
    public static LoanBorrowerQuery parse(String input) throws LoanBorrowerUnsuccessful {
        if (input == null || input.matches("(?s).*\\R.*")) {
            throw invalidFormat();
        }
        String normalized = input.replaceAll("(?U)^\\s+|\\s+$", "");
        Matcher matcher = COMMAND.matcher(normalized);
        if (!matcher.matches()) {
            throw invalidFormat();
        }
        return new LoanBorrowerQuery(matcher.group(1));
    }

    /** Creates the user-facing error shared by all malformed borrower query commands. */
    private static LoanBorrowerUnsuccessful invalidFormat() {
        return new LoanBorrowerUnsuccessful(
                "Invalid format. Use: list borrower/[BORROWER ID], from 000 to 999.");
    }
}
