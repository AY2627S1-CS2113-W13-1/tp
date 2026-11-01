package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.exceptions.LoanBorrowerUnsuccessful;

/** Tests strict parsing of commands that query equipment held by a borrower. */
class LoanBorrowerQueryTest {
    @Test
    void parse_borrowerCommand_returnsBorrowerId() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("list-loan-borrower U-id/002");

        assertEquals("002", query.borrowerId());
    }

    @Test
    void parse_borrowerCommand_acceptsTrimmedInput() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("  list-loan-borrower U-id/007  ");

        assertEquals("007", query.borrowerId());
    }

    @Test
    void parse_boundaryBorrowerIds_acceptsZeroAndMaximum() throws Exception {
        assertEquals("000", LoanBorrowerQuery.parse("list-loan-borrower U-id/000").borrowerId());
        assertEquals("999", LoanBorrowerQuery.parse("list-loan-borrower U-id/999").borrowerId());
    }

    @Test
    void parse_unicodeWhitespaceAroundCommand_acceptsTrimmedInput() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("\tlist-loan-borrower\u2003U-id/007\u00a0");

        assertEquals("007", query.borrowerId());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void parse_invalidCommand_rejectsWithoutCreatingAQuery(String command) {
        LoanBorrowerUnsuccessful error = assertThrows(LoanBorrowerUnsuccessful.class,
                () -> LoanBorrowerQuery.parse(command));

        assertEquals("Invalid format. Use: list-loan-borrower U-id/[BORROWER ID], from 000 to 999.",
                error.getMessage());
    }

    private static Stream<String> invalidCommands() {
        return Stream.of(null, "", "list-eqm", "list borrower", "list-loan-borrower U-id/",
                "list-loan-borrower U-id/00", "list-loan-borrower U-id/1000", "list-loan-borrower U-id/abc",
                "list member/001 extra", "List borrower/001",
                "list-loan-borrower U-id/001\n");
    }
}
