package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import seedu.snap.equipment.Equipment;
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

    private EquipmentHandler equipmentHandler;

    @BeforeEach
    public void resetLoans() throws EquipmentAdditionUnsuccessful {
        LoanHandler.loans.clear();
        LoanHandler.numLoans = 0;
        equipmentHandler = new EquipmentHandler();
        equipmentHandler.addEquipment("add i/Camera id/001 type/camera cond/1");
        equipmentHandler.addEquipment("add i/Camera id/003 type/camera cond/1");
    }

    @Test
    public void deleteLoan_existingItemId_deletesCorrectLoan()
            throws LoanAdditionUnsuccessful,
            LoanDeletionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
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
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
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
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
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
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
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
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
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
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.loans.get(0).setReturned(true);
        equipmentHandler.getEquipmentById("001").markReturned();
        loanHandler.addLoan("loan id/001 b/005 issued/05-10-26 due/06-10-26");

        assertThrows(LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan("delete-loan id/001"));
        assertEquals(2, loanHandler.numLoans);

        Loan deletedLoan = loanHandler.deleteLoan("delete-loan id/001 issued/05-10-26");
        assertEquals("005", deletedLoan.getBorrowerId());
        assertEquals(1, loanHandler.numLoans);
    }

    @Test
    public void addLoan_existingEquipment_marksEquipmentLoaned() throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);

        loanHandler.addLoan(FIRST_LOAN);

        assertTrue(equipmentHandler.getEquipmentById("001").isLoaned());
    }

    @Test
    public void addLoan_unknownEquipment_rejectsLoan() {
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);

        LoanAdditionUnsuccessful exception = assertThrows(LoanAdditionUnsuccessful.class,
                () -> loanHandler.addLoan("loan id/999 b/002 issued/01-10-26 due/02-10-26"));

        assertEquals("Equipment ID 999 does not exist.", exception.getMessage());
        assertEquals(0, loanHandler.numLoans);
    }

    @Test
    public void addLoan_duplicateLoan_keepsEquipmentLoaned() throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
        loanHandler.addLoan(FIRST_LOAN);

        assertThrows(LoanAdditionUnsuccessful.class, () -> loanHandler.addLoan(
                "loan id/001 b/005 issued/05-10-26 due/06-10-26"));

        assertTrue(equipmentHandler.getEquipmentById("001").isLoaned());
        assertEquals(1, loanHandler.numLoans);
    }

    @Test
    public void deleteLoan_existingLoan_marksEquipmentReturned()
            throws LoanAdditionUnsuccessful, LoanDeletionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipmentHandler);
        loanHandler.addLoan(FIRST_LOAN);

        loanHandler.deleteLoan("delete-loan id/001");

        Equipment equipment = equipmentHandler.getEquipmentById("001");
        assertFalse(equipment.isLoaned());
    }
}
