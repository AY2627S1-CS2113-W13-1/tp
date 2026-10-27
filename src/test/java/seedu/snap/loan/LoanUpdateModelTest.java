package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;

/** Tests the model transition used after a loan update has been validated and saved. */
class LoanUpdateModelTest {
    @Test
    void applyUpdate_rebindsActiveLoanAndPreservesLoanId() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment originalEquipment = inventory.addEquipment("add i/Camera id/001 type/camera cond/4");
        Equipment replacementEquipment = inventory.addEquipment("add i/Tripod id/002 type/tripod cond/5");
        Loan original = new Loan("004", originalEquipment, "002",
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0));
        original.attachEquipment(originalEquipment);

        Loan updated = original.updatedCopy("002", replacementEquipment.getRecordId(), "007",
                LocalDateTime.of(2026, 10, 3, 0, 0), LocalDateTime.of(2026, 10, 9, 0, 0),
                replacementEquipment);
        original.applyUpdate(updated, replacementEquipment);

        assertEquals("004", original.getLoanId());
        assertEquals("002", original.getItemId());
        assertEquals("007", original.getBorrowerId());
        assertEquals(updated.getIssuedDate(), original.getIssuedDate());
        assertEquals(updated.getDueDate(), original.getDueDate());
        assertEquals("tripod", updated.getIssueConditionSnapshot().getEquipmentType());
        assertEquals(5, updated.getIssueConditionSnapshot().getCondition());
        assertFalse(originalEquipment.isLoaned());
        assertTrue(replacementEquipment.isLoaned());
        assertEquals("004", replacementEquipment.getLoanId());
    }

    @Test
    void applyUpdate_proposalWithDifferentLoanId_failsAssertion() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment originalEquipment = inventory.addEquipment("add i/Camera id/001 type/camera cond/4");
        Equipment replacementEquipment = inventory.addEquipment("add i/Tripod id/002 type/tripod cond/5");
        Loan original = new Loan("004", originalEquipment, "002",
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0));
        original.attachEquipment(originalEquipment);
        Loan mismatched = new Loan("005", "002", "007",
                LocalDateTime.of(2026, 10, 3, 0, 0), LocalDateTime.of(2026, 10, 9, 0, 0),
                replacementEquipment.getRecordId());

        assertThrows(AssertionError.class, () -> original.applyUpdate(mismatched, replacementEquipment));
        assertEquals("001", original.getItemId());
        assertEquals("004", original.getLoanId());
        assertFalse(replacementEquipment.isLoaned());
    }

    @Test
    void applyUpdate_returnedLoan_failsAssertion() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment originalEquipment = inventory.addEquipment("add i/Camera id/001 type/camera cond/4");
        Loan original = new Loan("004", originalEquipment, "002",
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0));
        original.attachEquipment(originalEquipment);
        original.setReturned(true);
        Loan proposal = new Loan("004", "001", "007",
                LocalDateTime.of(2026, 10, 3, 0, 0), LocalDateTime.of(2026, 10, 9, 0, 0),
                originalEquipment.getRecordId());

        assertThrows(AssertionError.class, () -> original.applyUpdate(proposal, originalEquipment));
        assertTrue(original.isReturned());
        assertFalse(originalEquipment.isLoaned());
    }
}
