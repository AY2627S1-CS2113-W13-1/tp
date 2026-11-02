package seedu.snap.loan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.Identifier;
import seedu.snap.exceptions.LoanBorrowerUnsuccessful;

/** Represents a validated command for finding equipment held by one borrower. */
public record LoanBorrowerQuery(String borrowerId) {
    private static final Pattern COMMAND = Pattern.compile(
            "(?U)\\Alist\\s+(?:borrower|member)/([Uu][0-9]{3})\\z");

    /** Creates a query after validating the borrower's typed identifier. */
    public LoanBorrowerQuery {
        try {
            borrowerId = Identifier.normalize(borrowerId, Identifier.Type.BORROWER);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Borrower ID must be from U000 to U999.", exception);
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
                "Invalid format. Use: list borrower/[BORROWER ID], from U000 to U999.");
    }
}
