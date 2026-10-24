package seedu.snap.loan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.LoanReturnUnsuccessful;

/** Holds the mandatory assessment and optional remarks from a return command, before any records change. */
record LoanReturn(String loanId, boolean serviceable, String damage, String remarks) {
    private static final String FORMAT = "Invalid format. Use: return loan/[LOAN ID] "
            + "serviceable/[yes|no] damage/[DAMAGE|none] [remarks/[REMARKS]].";
    private static final Pattern COMMAND = Pattern.compile("(?U)\\A\\s*return\\s+loan/([0-9]{3})"
            + "\\s+serviceable/((?i:yes|no))\\s+damage/([A-Za-z_]+)(?:\\s+remarks/(.*))?\\s*\\z");

    /** Parses the required assessment and defaults omitted final remarks to empty text. */
    static LoanReturn parse(String input) throws LoanReturnUnsuccessful {
        if (input == null || input.matches("(?s).*\\R.*")) {
            throw new LoanReturnUnsuccessful(FORMAT);
        }
        Matcher match = COMMAND.matcher(input);
        if (!match.matches()) {
            throw new LoanReturnUnsuccessful(FORMAT);
        }
        if (match.group(1).equals("000")) {
            throw new LoanReturnUnsuccessful("Loan ID must be from 001 to 999.");
        }
        return new LoanReturn(match.group(1), match.group(2).equalsIgnoreCase("yes"),
                match.group(3), match.group(4) == null ? "" : match.group(4));
    }
}
