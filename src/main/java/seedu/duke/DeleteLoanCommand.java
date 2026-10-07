package seedu.duke;

import java.util.List;

/**
 * Represents a command that deletes a loan record.
 */
public final class DeleteLoanCommand {
    private static final int LOAN_ID_INDEX = 0;
    private static final String LOAN_ID_PATTERN = "[0-9]{3}";

    private DeleteLoanCommand() {
    }

    /**
     * Deletes the loan record with the specified loan ID.
     *
     * @param loanId Loan ID to delete.
     * @param loans Loan records currently stored.
     * @return True if a loan record was deleted.
     */
    public static boolean execute(String loanId, List<String[]> loans) {
        String normalizedLoanId = loanId == null ? "" : loanId.trim();

        if (!isValidLoanId(normalizedLoanId)) {
            System.out.println("ERROR: Invalid loan ID.");
            return false;
        }

        for (int index = 0; index < loans.size(); index++) {
            String[] loan = loans.get(index);

            if (hasMatchingLoanId(loan, normalizedLoanId)) {
                loans.remove(index);
                System.out.println("Loan " + normalizedLoanId + " deleted successfully.");
                return true;
            }
        }

        System.out.println("ERROR: Loan ID not found.");
        return false;
    }

    /**
     * Returns whether the loan has the specified loan ID.
     *
     * @param loan Loan record to examine.
     * @param loanId Loan ID to locate.
     * @return True if the loan has the specified ID.
     */
    private static boolean hasMatchingLoanId(String[] loan, String loanId) {
        return loan != null
                && loan.length > LOAN_ID_INDEX
                && loan[LOAN_ID_INDEX].equals(loanId);
    }

    /**
     * Returns whether the specified loan ID is valid.
     *
     * @param loanId Loan ID to validate.
     * @return True if the ID is between 001 and 999.
     */
    private static boolean isValidLoanId(String loanId) {
        return loanId.matches(LOAN_ID_PATTERN)
                && !loanId.equals("000");
    }
}
