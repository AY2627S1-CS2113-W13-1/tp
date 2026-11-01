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
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("list borrower/U002");

        assertEquals("U002", query.borrowerId());
    }

    @Test
    void parse_memberAlias_returnsBorrowerId() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("  list member/U007  ");

        assertEquals("U007", query.borrowerId());
    }

    @Test
    void parse_boundaryBorrowerIds_acceptsZeroAndMaximum() throws Exception {
        assertEquals("U000", LoanBorrowerQuery.parse("list borrower/U000").borrowerId());
        assertEquals("U999", LoanBorrowerQuery.parse("list borrower/U999").borrowerId());
    }

    @Test
    void parse_unicodeWhitespaceAroundCommand_acceptsTrimmedInput() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("\tlist\u2003member/U007\u00a0");

        assertEquals("U007", query.borrowerId());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void parse_invalidCommand_rejectsWithoutCreatingAQuery(String command) {
        LoanBorrowerUnsuccessful error = assertThrows(LoanBorrowerUnsuccessful.class,
                () -> LoanBorrowerQuery.parse(command));

        assertEquals("Invalid format. Use: list borrower/[BORROWER ID], from U000 to U999.", error.getMessage());
    }

    private static Stream<String> invalidCommands() {
        return Stream.of(null, "", "list", "list borrower", "list borrower/", "list borrower/00",
                "list borrower/U1000", "list borrower/abc", "list member/U001 extra", "List borrower/001",
                "list borrower/U001\n");
    }
}
