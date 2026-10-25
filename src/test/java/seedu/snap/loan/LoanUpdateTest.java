package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.LoanUpdateUnsuccessful;

/** Tests parsing and validation of update-loan commands before state changes. */
class LoanUpdateTest {
    @Test
    void parse_allFieldsInAnyOrder_returnsValidatedValues() throws LoanUpdateUnsuccessful {
        LoanUpdate update = LoanUpdate.parse(
                "update loan/007 due/15-10-26 id/003 b/004 issued/10-10-26");

        assertEquals("007", update.loanId());
        assertEquals("003", update.itemId());
        assertEquals("004", update.borrowerId());
        assertEquals(LocalDateTime.of(2026, 10, 10, 0, 0), update.issuedDate());
        assertEquals(LocalDateTime.of(2026, 10, 15, 0, 0), update.dueDate());
        assertTrue(update.changesItem());
        assertTrue(update.changesBorrower());
        assertTrue(update.changesIssuedDate());
        assertTrue(update.changesDueDate());
    }

    @Test
    void parse_partialFields_leavesOmittedValuesUnset() throws LoanUpdateUnsuccessful {
        LoanUpdate update = LoanUpdate.parse("update loan/007 b/004");

        assertEquals("007", update.loanId());
        assertNull(update.itemId());
        assertEquals("004", update.borrowerId());
        assertNull(update.issuedDate());
        assertNull(update.dueDate());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void parse_invalidCommand_rejectsWithoutCreatingAnUpdate(String command) {
        assertThrows(LoanUpdateUnsuccessful.class, () -> LoanUpdate.parse(command));
    }

    private static Stream<String> invalidCommands() {
        return Stream.of(null, "", "update", "update loan/001", "update item/001 b/002",
                "update loan/000 b/002", "update loan/01 b/002", "update loan/001 id/000",
                "update loan/001 b/000", "update loan/001 id/002 id/003", "update loan/001 unknown/002",
                "update loan/001 due/31-02-26", "update loan/001 issued/20-10-26 due/19-10-26",
                "update loan/001 b/002\n due/20-10-26");
    }
}
