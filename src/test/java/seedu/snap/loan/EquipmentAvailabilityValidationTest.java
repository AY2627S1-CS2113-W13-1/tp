package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;

/** Checks that rejected loan arguments and stale records cannot reserve or release another loan's item. */
class EquipmentAvailabilityValidationTest {
    private static final String COMMAND = "loan id/001 b/002 issued/01-10-26 due/02-10-26";
    private Equipment camera;
    private LoanHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        camera = inventory.addEquipment("add i/Camera id/001 type/camera cond/1");
        handler = new LoanHandler(inventory);
    }

    @Test
    void addLoan_malformedArguments_preserveAvailabilityAndAllowRecovery() throws Exception {
        String[] invalid = {
            null, "", "borrow id/001 b/002 issued/01-10-26 due/02-10-26",
            COMMAND.replace("id/001", "item/001"), COMMAND.replace("id/001", "id/01"),
            COMMAND.replace("id/001", "/id+01"), COMMAND.replace("b/002", "borrower/002"),
            COMMAND.replace("b/002", "b/abc"), COMMAND.replace("b/002", "/b"),
            COMMAND.replace("issued/", "start/"), COMMAND.replace("due/", "end/"),
            COMMAND.replace("01-10-26", "31-02-26"), COMMAND.replace("02-10-26", "01-10-25")
        };
        for (String command : invalid) {
            assertThrows(LoanAdditionUnsuccessful.class, () -> handler.addLoan(command), command);
            assertFalse(camera.isLoaned(), command);
            assertTrue(handler.loans.isEmpty(), command);
            assertEquals(0, handler.numLoans, command);
        }
        assertEquals("001", handler.addLoan(COMMAND).getItemId());
        assertTrue(camera.isLoaned());
    }

    @Test
    void addLoan_itemAlreadyReservedWithoutRecord_doesNotCreateAnotherLoan() {
        camera.setLoaned(true);
        LoanAdditionUnsuccessful error = assertThrows(LoanAdditionUnsuccessful.class,
                () -> handler.addLoan(COMMAND));
        assertEquals("Item ID 001 is already loaned out.", error.getMessage());
        assertTrue(camera.isLoaned());
        assertTrue(handler.loans.isEmpty());
        assertEquals(0, handler.numLoans);
    }

    @Test
    void constructor_missingOrAlreadyLoanedEquipment_rejectsWithoutChangingCurrentLoan() throws Exception {
        LocalDateTime date = LocalDateTime.of(2026, 10, 1, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new Loan(null, "002", date, date));
        Loan original = handler.addLoan(COMMAND);
        assertThrows(IllegalArgumentException.class, () -> new Loan(camera, "003", date, date));
        assertEquals(List.of(original), handler.loans);
        assertEquals("002", original.getBorrowerId());
        assertTrue(camera.isLoaned());
    }

    @Test
    void detachEquipment_repeatedCleanup_cannotReleaseReplacementLoan() throws Exception {
        Loan old = handler.addLoan(COMMAND);
        old.setReturned(false);
        assertTrue(camera.isLoaned());
        handler.deleteLoan("delete-loan id/001");
        Loan current = handler.addLoan(COMMAND);
        old.detachEquipment();
        old.setReturned(true);
        assertTrue(camera.isLoaned());
        assertFalse(current.isReturned());
        assertEquals(List.of(current), handler.loans);
    }
}
