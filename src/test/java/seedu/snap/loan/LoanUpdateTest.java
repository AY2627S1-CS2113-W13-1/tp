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
                "loan-update l/L007 due/15-10-26 e/E003 u/U004 issued/10-10-26");

        assertEquals("L007", update.loanId());
        assertEquals("E003", update.itemId());
        assertEquals("U004", update.borrowerId());
        assertEquals(LocalDateTime.of(2026, 10, 10, 0, 0), update.issuedDate());
        assertEquals(LocalDateTime.of(2026, 10, 15, 0, 0), update.dueDate());
        assertTrue(update.changesItem());
        assertTrue(update.changesBorrower());
        assertTrue(update.changesIssuedDate());
        assertTrue(update.changesDueDate());
    }

    @Test
    void parse_lowercaseIds_normalisesValues() throws LoanUpdateUnsuccessful {
        LoanUpdate update = LoanUpdate.parse(
                "loan-update l/l007 due/15-10-26 e/e003 u/u004 issued/10-10-26");

        assertEquals("L007", update.loanId());
        assertEquals("E003", update.itemId());
        assertEquals("U004", update.borrowerId());
    }

    @Test
    void parse_partialFields_leavesOmittedValuesUnset() throws LoanUpdateUnsuccessful {
        LoanUpdate update = LoanUpdate.parse("loan-update l/L007 u/U004");

        assertEquals("L007", update.loanId());
        assertNull(update.itemId());
        assertEquals("U004", update.borrowerId());
        assertNull(update.issuedDate());
        assertNull(update.dueDate());
    }

    @Test
    void parse_borrowerBoundaryValues_acceptsDocumentedRange() throws LoanUpdateUnsuccessful {
        LoanUpdate zero = LoanUpdate.parse("loan-update l/L001 u/U000");
        LoanUpdate maximum = LoanUpdate.parse("loan-update l/L001 u/U999");

        assertEquals("U000", zero.borrowerId());
        assertEquals("U999", maximum.borrowerId());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void parse_invalidCommand_rejectsWithoutCreatingAnUpdate(String command) {
        assertThrows(LoanUpdateUnsuccessful.class, () -> LoanUpdate.parse(command));
    }

    private static Stream<String> invalidCommands() {
        return Stream.of(null, "", "eqm-update", "loan-update l/L001", "update item/E001 u/U002",
                "loan-update l/L000 u/U002", "loan-update l/01 u/U002", "loan-update l/L001 e/E000",
                "loan-update l/L001 u/00", "loan-update l/L001 u/L002", "loan-update l/L001 e/L002",
                "loan-update l/L001 e/E002 e/E003", "loan-update l/L001 unknown/002",
                "loan-update l/L001 due/31-02-26", "loan-update l/L001 issued/20-10-26 due/19-10-26",
                "loan-update l/L001 u/U002\n due/20-10-26");
    }
}
