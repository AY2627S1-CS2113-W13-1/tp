package seedu.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DeleteLoanCommandTest {

    @Test
    public void execute_existingLoan_deletesCorrectLoan() {
        List<String[]> loans = createLoanList();

        boolean isDeleted = DeleteLoanCommand.execute("002", loans);

        assertTrue(isDeleted);
        assertEquals(1, loans.size());
        assertEquals("001", loans.get(0)[0]);
    }

    @Test
    public void execute_unknownLoan_keepsLoanListUnchanged() {
        List<String[]> loans = createLoanList();

        boolean isDeleted = DeleteLoanCommand.execute("999", loans);

        assertFalse(isDeleted);
        assertEquals(2, loans.size());
    }

    @Test
    public void execute_invalidLoanId_keepsLoanListUnchanged() {
        List<String[]> loans = createLoanList();

        boolean isDeleted = DeleteLoanCommand.execute("abc", loans);

        assertFalse(isDeleted);
        assertEquals(2, loans.size());
    }

    @Test
    public void execute_emptyLoanId_keepsLoanListUnchanged() {
        List<String[]> loans = createLoanList();

        boolean isDeleted = DeleteLoanCommand.execute("", loans);

        assertFalse(isDeleted);
        assertEquals(2, loans.size());
    }

    /**
     * Returns sample loan records for testing.
     *
     * @return Sample loan records.
     */
    private List<String[]> createLoanList() {
        List<String[]> loans = new ArrayList<>();
        loans.add(new String[]{"001", "017", "012", "07-10-26", "14-10-26"});
        loans.add(new String[]{"002", "021", "014", "08-10-26", "15-10-26"});
        return loans;
    }
}
