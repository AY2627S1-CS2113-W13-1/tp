package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

// @@author Rohita-29

/** Tests overdue-loan behaviour in the loan model. */
public class LoanTest {
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 10, 24);

    @Test
    public void isOverdue_activeLoanDueBeforeReferenceDate_returnsTrue() {
        Loan loan = createLoan(REFERENCE_DATE.minusDays(1));

        assertTrue(loan.isOverdue(REFERENCE_DATE));
    }

    @Test
    public void isOverdue_activeLoanDueOnReferenceDate_returnsFalse() {
        Loan loan = createLoan(REFERENCE_DATE);

        assertFalse(loan.isOverdue(REFERENCE_DATE));
    }

    @Test
    public void isOverdue_activeLoanDueAfterReferenceDate_returnsFalse() {
        Loan loan = createLoan(REFERENCE_DATE.plusDays(1));

        assertFalse(loan.isOverdue(REFERENCE_DATE));
    }

    @Test
    public void isOverdue_returnedLoanDueBeforeReferenceDate_returnsFalse() {
        Loan loan = createLoan(REFERENCE_DATE.minusDays(1));
        loan.setReturned(true);

        assertFalse(loan.isOverdue(REFERENCE_DATE));
    }

    @Test
    public void isOverdue_nullReferenceDate_throwsNullPointerException() {
        Loan loan = createLoan(REFERENCE_DATE.minusDays(1));

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> loan.isOverdue(null));

        assertEquals("Reference date must not be null", exception.getMessage());
    }

    /**
     * Creates an active loan whose issue date is five days before its due date.
     *
     * @param dueDate required due date
     * @return active loan using the supplied due date
     */
    private Loan createLoan(LocalDate dueDate) {
        LocalDateTime dueDateTime = dueDate.atStartOfDay();
        LocalDateTime issuedDateTime = dueDate.minusDays(5).atStartOfDay();
        return new Loan("001", "001", "002", issuedDateTime, dueDateTime);
    }
}

// @@author
