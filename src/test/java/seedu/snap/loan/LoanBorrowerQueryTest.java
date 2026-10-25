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
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("list borrower/002");

        assertEquals("002", query.borrowerId());
    }

    @Test
    void parse_memberAlias_returnsBorrowerId() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("  list member/007  ");

        assertEquals("007", query.borrowerId());
    }

    @Test
    void parse_boundaryBorrowerIds_acceptsZeroAndMaximum() throws Exception {
        assertEquals("000", LoanBorrowerQuery.parse("list borrower/000").borrowerId());
        assertEquals("999", LoanBorrowerQuery.parse("list borrower/999").borrowerId());
    }

    @Test
    void parse_unicodeWhitespaceAroundCommand_acceptsTrimmedInput() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("\tlist\u2003member/007\u00a0");

        assertEquals("007", query.borrowerId());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void parse_invalidCommand_rejectsWithoutCreatingAQuery(String command) {
        LoanBorrowerUnsuccessful error = assertThrows(LoanBorrowerUnsuccessful.class,
                () -> LoanBorrowerQuery.parse(command));

        assertEquals("Invalid format. Use: list borrower/[BORROWER ID], from 000 to 999.", error.getMessage());
    }

    private static Stream<String> invalidCommands() {
        return Stream.of(null, "", "list", "list borrower", "list borrower/", "list borrower/00",
                "list borrower/1000", "list borrower/abc", "list member/001 extra", "List borrower/001",
                "list borrower/001\n");
    }
}
