package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Tests the deletion of loans using item IDs. */
public class LoanHandlerTest {
    private static final String FIRST_LOAN =
            "loan id/001 b/002 issued/01-10-26 due/02-10-26";

    private static final String SECOND_LOAN =
            "loan id/003 b/004 issued/03-10-26 due/04-10-26";

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    public void redirectOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    public void addLoan_validLoan_keepsCountConsistent() throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();

        loanHandler.addLoan(FIRST_LOAN);

        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(1, loanHandler.getLoanList().size());
    }

    @Test
    public void persistence_addDeleteAndReload_preservesLoansAcrossHandlers() throws Exception {
        Path file = temporaryDirectory.resolve("data/loans.txt");
        LoanHandler firstHandler = new LoanHandler(file);
        firstHandler.addLoan(FIRST_LOAN);

        LoanHandler secondHandler = new LoanHandler(file);
        secondHandler.loadFromStorage();
        assertEquals(1, secondHandler.getLoanCount());
        assertEquals("001", secondHandler.getLoanList().getFirst().getItemId());
        assertEquals("002", secondHandler.getLoanList().getFirst().getBorrowerId());

        secondHandler.deleteLoan("delete-loan id/001");
        LoanHandler thirdHandler = new LoanHandler(file);
        thirdHandler.loadFromStorage();
        assertEquals(0, thirdHandler.getLoanCount());
    }

    @Test
    public void saveToStorage_persistsInMemoryLoanStatus() throws Exception {
        Path file = temporaryDirectory.resolve("data/loans.txt");
        LoanHandler loanHandler = new LoanHandler(file);
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.getLoanList().getFirst().setReturned(true);

        loanHandler.saveToStorage();

        LoanHandler reloadedHandler = new LoanHandler(file);
        reloadedHandler.loadFromStorage();
        assertTrue(reloadedHandler.getLoanList().getFirst().isReturned());
    }

    @Test
    public void storage_blankLines_areIgnoredDuringLoad() throws Exception {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, System.lineSeparator()
                + "  " + System.lineSeparator()
                + "loan id/001 b/002 issued/01-10-26 due/02-10-26" + System.lineSeparator());

        List<Loan> loaded = LoanStorage.load(file);

        assertEquals(1, loaded.size());
        assertEquals("001", loaded.getFirst().getItemId());
    }

    @Test
    public void deleteLoan_existingItemId_deletesCorrectLoan()
            throws LoanAdditionUnsuccessful,
            LoanDeletionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.addLoan(SECOND_LOAN);

        Loan deletedLoan =
                loanHandler.deleteLoan("delete-loan id/001");

        assertEquals("001", deletedLoan.getItemId());
        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(
                "003",
                loanHandler.getLoanList().get(0).getItemId());
    }

    @Test
    public void deleteLoan_unknownItemId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        "delete-loan id/999"));

        assertEquals(
                "No loan found for item ID 999.",
                exception.getMessage());
        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(
                "001",
                loanHandler.getLoanList().get(0).getItemId());
    }

    @Test
    public void deleteLoan_invalidItemId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        "delete-loan id/01"));

        assertEquals(
                "Invalid item ID \"01\". Item ID must be exactly three digits, e.g. 001.",
                exception.getMessage());
        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    public void deleteLoan_missingItemId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan("delete-loan"));

        assertEquals(
                "Invalid command format. Expected: delete-loan id/[ITEM ID] [issued/DD-MM-YY].",
                exception.getMessage());
        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    public void deleteLoan_invalidPrefix_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();
        loanHandler.addLoan(FIRST_LOAN);

        assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        "delete-loan item/001"));

        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    public void deleteLoan_duplicateItemId_requiresIssuedDate()
            throws LoanAdditionUnsuccessful, LoanDeletionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.getLoanList().get(0).setReturned(true);
        loanHandler.addLoan("loan id/001 b/005 issued/05-10-26 due/06-10-26");

        assertThrows(LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan("delete-loan id/001"));
        assertEquals(2, loanHandler.getLoanCount());

        Loan deletedLoan = loanHandler.deleteLoan("delete-loan id/001 issued/05-10-26");
        assertEquals("005", deletedLoan.getBorrowerId());
        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    void viewAllLoans_noLoans_printsHeaderOnly() {
        new LoanHandler().viewAllLoans();

        String expected = String.format(
                "%-4s %-12s | %-12s | %-16s | %-16s%n",
                "No.", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE");
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewAllLoans_existingLoans_printsHeaderAndRows()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler();
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.addLoan(SECOND_LOAN);

        loanHandler.viewAllLoans();

        String expected = String.format(
                "%-4s %-12s | %-12s | %-16s | %-16s%n",
                "No.", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE");
        expected += String.format(
                "%-4s %-12s | %-12s | %-16s | %-16s%n",
                "1)", "001", "002", "2026-10-01T00:00", "2026-10-02T00:00");
        expected += String.format(
                "%-4s %-12s | %-12s | %-16s | %-16s%n",
                "2)", "003", "004", "2026-10-03T00:00", "2026-10-04T00:00");

        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }
}
