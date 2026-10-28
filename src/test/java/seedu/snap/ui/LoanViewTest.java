package seedu.snap.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.loan.Loan;

/** Checks readable dates and state boundaries independently of record selection and persistence. */
class LoanViewTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @BeforeEach
    void captureOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void print_mixedRecords_preservesOrderAndUsesDateOnlyStates() {
        Loan overdue = loan("001", TODAY.minusDays(1));
        Loan dueToday = loan("002", TODAY);
        Loan future = loan("003", TODAY.plusDays(1));
        Loan returned = loan("004", TODAY.minusDays(2));
        returned.setReturned(true);
        List<Loan> records = List.of(future, returned, dueToday, overdue);

        LoanView.print(records, TODAY, "Loans", "No loans recorded.");

        List<String> rows = printed().lines().filter(line -> line.matches("\\| 00[1-4] .*\\|")).toList();
        assertEquals(4, rows.size());
        assertTrue(rows.get(0).contains("003") && rows.get(0).contains("28-10-26")
                && rows.get(0).contains("Active"));
        assertTrue(rows.get(1).contains("004") && rows.get(1).contains("25-10-26")
                && rows.get(1).contains("Returned"));
        assertTrue(rows.get(2).contains("002") && rows.get(2).contains("27-10-26")
                && rows.get(2).contains("Active"));
        assertTrue(rows.get(3).contains("001") && rows.get(3).contains("26-10-26")
                && rows.get(3).contains("Overdue"));
        assertTrue(printed().contains("4 loans."));
        assertFalse(printed().contains("T00:00"));
        assertFalse(printed().contains("false"));
        assertFalse(printed().contains("true"));
        assertTrue(printed().lines().allMatch(line -> line.length() <= 100));
        assertEquals(List.of(future, returned, dueToday, overdue), records);
        assertFalse(overdue.isReturned());
        assertTrue(returned.isReturned());
    }

    @Test
    void state_timeOfDayDoesNotMakeTodaysLoanOverdue() {
        Loan loan = loan("001", TODAY);
        loan.setDueDate(TODAY.atTime(23, 59));

        assertEquals("Active", LoanView.state(loan, TODAY));
        assertEquals("Overdue", LoanView.state(loan, TODAY.plusDays(1)));
        loan.setReturned(true);
        assertEquals("Returned", LoanView.state(loan, TODAY.plusDays(1)));
    }

    @Test
    void print_emptyAndSingleResults_haveUsefulMessagesAndCounts() {
        LoanView.print(List.of(), TODAY, "Overdue loans", "No overdue loans found.");
        assertTrue(printed().contains("No overdue loans found."));
        assertTrue(printed().contains("0 loans."));
        output.reset();
        LoanView.print(List.of(loan("001", TODAY)), TODAY, "Loans", "No loans recorded.");
        assertTrue(printed().contains("1 loan."));
    }

    @Test
    void date_ignoresDefaultLocaleAndOmitsTime() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));
            assertEquals("03-01-26", LoanView.date(LocalDateTime.of(2026, 1, 3, 23, 59)));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void print_emptyHistory_escapesControlCharactersInItsSubject() {
        LoanView.print(List.of(), TODAY, "History for Kit\nOne", "No history for Kit\nOne.");

        assertTrue(printed().contains("History for Kit\\u000aOne"));
        assertTrue(printed().contains("No history for Kit\\u000aOne."));
        assertFalse(printed().lines().anyMatch(line -> line.equals("One.")));
    }

    /** Creates a valid retained record without introducing an equipment association. */
    private static Loan loan(String id, LocalDate due) {
        return new Loan(id, id, "222", TODAY.minusDays(4).atStartOfDay(), due.atStartOfDay());
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8).replaceAll("\u001b\\[[;\\d]*m", "");
    }
}
