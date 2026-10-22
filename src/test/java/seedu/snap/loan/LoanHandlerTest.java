package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
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
    private EquipmentHandler equipment;
    private LoanHandler loanHandler;

    @BeforeEach
    public void resetLoans() throws EquipmentAdditionUnsuccessful {
        equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/001 type/camera cond/1");
        equipment.addEquipment("add i/Tripod id/003 type/accessory cond/2");
        loanHandler = new LoanHandler(equipment);
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    public void addLoan_validLoan_keepsCountConsistent() throws LoanAdditionUnsuccessful {

        loanHandler.addLoan(FIRST_LOAN);

        assertEquals(1, loanHandler.numLoans);
        assertEquals(1, loanHandler.loans.size());
    }

    @Test
    public void deleteLoan_existingItemId_deletesCorrectLoan()
            throws LoanAdditionUnsuccessful,
            LoanDeletionUnsuccessful {
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.addLoan(SECOND_LOAN);

        Loan deletedLoan =
                loanHandler.deleteLoan("delete-loan id/001");

        assertEquals("001", deletedLoan.getItemId());
        assertEquals(1, loanHandler.numLoans);
        assertEquals(
                "003",
                loanHandler.loans.get(0).getItemId());
    }

    @Test
    public void deleteLoan_unknownItemId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        "delete-loan id/999"));

        assertEquals(
                "No loan found for item ID 999.",
                exception.getMessage());
        assertEquals(1, loanHandler.numLoans);
        assertEquals(
                "001",
                loanHandler.loans.get(0).getItemId());
    }

    @Test
    public void deleteLoan_invalidItemId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        "delete-loan id/01"));

        assertEquals(
                "Invalid item ID \"01\". Item ID must be exactly three digits, e.g. 001.",
                exception.getMessage());
        assertEquals(1, loanHandler.numLoans);
    }

    @Test
    public void deleteLoan_missingItemId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan("delete-loan"));

        assertEquals(
                "Invalid command format. Expected: delete-loan id/[ITEM ID] [issued/DD-MM-YY].",
                exception.getMessage());
        assertEquals(1, loanHandler.numLoans);
    }

    @Test
    public void deleteLoan_invalidPrefix_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        loanHandler.addLoan(FIRST_LOAN);

        assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        "delete-loan item/001"));

        assertEquals(1, loanHandler.numLoans);
    }

    @Test
    public void deleteLoan_duplicateItemId_requiresIssuedDate()
            throws LoanAdditionUnsuccessful, LoanDeletionUnsuccessful {
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.loans.get(0).setReturned(true);
        loanHandler.addLoan("loan id/001 b/005 issued/05-10-26 due/06-10-26");

        assertThrows(LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan("delete-loan id/001"));
        assertEquals(2, loanHandler.numLoans);

        Loan deletedLoan = loanHandler.deleteLoan("delete-loan id/001 issued/05-10-26");
        assertEquals("005", deletedLoan.getBorrowerId());
        assertEquals(1, loanHandler.numLoans);
    }

    @Test
    void viewAllLoans_noLoans_printsHeaderOnly() {
        loanHandler.viewAllLoans();

        String expected = String.format(
                "%-4s %-12s | %-12s | %-16s | %-16s%n",
                "No.", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE");
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewAllLoans_existingLoans_printsHeaderAndRows()
            throws LoanAdditionUnsuccessful {
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
