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
                "loan-update l/007 due/15-10-26 e/003 u/004 issued/10-10-26");

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
        LoanUpdate update = LoanUpdate.parse("loan-update l/007 u/004");

        assertEquals("007", update.loanId());
        assertNull(update.itemId());
        assertEquals("004", update.borrowerId());
        assertNull(update.issuedDate());
        assertNull(update.dueDate());
    }

    @Test
    void parse_borrowerBoundaryValues_acceptsDocumentedRange() throws LoanUpdateUnsuccessful {
        LoanUpdate zero = LoanUpdate.parse("loan-update l/001 u/000");
        LoanUpdate maximum = LoanUpdate.parse("loan-update l/001 u/999");

        assertEquals("000", zero.borrowerId());
        assertEquals("999", maximum.borrowerId());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void parse_invalidCommand_rejectsWithoutCreatingAnUpdate(String command) {
        assertThrows(LoanUpdateUnsuccessful.class, () -> LoanUpdate.parse(command));
    }

    private static Stream<String> invalidCommands() {
        return Stream.of(null, "", "eqm-update", "loan-update l/001", "eqm-update item/001 u/002",
                "loan-update l/000 u/002", "loan-update l/01 u/002", "loan-update l/001 e/000",
                "loan-update l/001 u/00", "loan-update l/001 e/002 e/003",
                "loan-update l/001 unknown/002",
                "loan-update l/001 due/31-02-26", "loan-update l/001 issued/20-10-26 due/19-10-26",
                "loan-update l/001 u/002\n due/20-10-26");
    }
}
