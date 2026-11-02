package seedu.snap.loan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.exceptions.LoanReturnUnsuccessful;

/** Holds the mandatory assessment and optional remarks from a return command, before any records change. */
record LoanReturn(String loanId, boolean serviceable, String damage, Integer condition, String remarks) {
    private static final String FORMAT = "Invalid format. Use: return loan/<LOAN ID> "
            + "serviceable/<yes|no> damage/<DAMAGE|none> [cond/<1-10>] [remarks/<REMARKS>].";
    private static final Pattern COMMAND = Pattern.compile("(?U)\\A\\s*return\\s+loan/([0-9]{3})"
            + "\\s+serviceable/((?i:yes|no))\\s+damage/([A-Za-z_]+)"
            + "(?:\\s+cond/([^\\s]+))?(?:\\s+remarks/(.*))?\\s*\\z");

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
        Integer condition = null;
        if (match.group(4) != null) {
            if (!match.group(4).matches("[1-9]|10")) {
                throw new LoanReturnUnsuccessful("Return condition must be a whole number from 1 to 10.");
            }
            condition = Integer.valueOf(match.group(4));
        }
        return new LoanReturn(match.group(1), match.group(2).equalsIgnoreCase("yes"),
                match.group(3), condition, match.group(5) == null ? "" : match.group(5));
    }
}
