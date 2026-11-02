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
    void constructor_wrongIdentifierType_rejectsExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> new LoanBorrowerQuery("E001"));
    }

    @Test
    void parse_borrowerCommand_returnsBorrowerId() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("loan-list-borrower u/U002");

        assertEquals("U002", query.borrowerId());
    }

    @Test
    void parse_lowercaseBorrowerId_returnsCanonicalValue() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("loan-list-borrower u/u002");

        assertEquals("U002", query.borrowerId());
    }

    @Test
    void parse_memberAlias_returnsBorrowerId() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("  loan-list-borrower u/U007  ");

        assertEquals("U007", query.borrowerId());
    }

    @Test
    void parse_boundaryBorrowerIds_acceptsZeroAndMaximum() throws Exception {
        assertEquals("U000", LoanBorrowerQuery.parse("loan-list-borrower u/U000").borrowerId());
        assertEquals("U999", LoanBorrowerQuery.parse("loan-list-borrower u/U999").borrowerId());
    }

    @Test
    void parse_unicodeWhitespaceAroundCommand_acceptsTrimmedInput() throws Exception {
        LoanBorrowerQuery query = LoanBorrowerQuery.parse("\tloan-list-borrower\u2003u/U007\u00a0");

        assertEquals("U007", query.borrowerId());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void parse_invalidCommand_rejectsWithoutCreatingAQuery(String command) {
        LoanBorrowerUnsuccessful error = assertThrows(LoanBorrowerUnsuccessful.class,
                () -> LoanBorrowerQuery.parse(command));

        assertEquals("Invalid format. Use: loan-list-borrower u/[BORROWER ID], from U000 to U999.", error.getMessage());
    }

    private static Stream<String> invalidCommands() {
        return Stream.of(null, "", "eqm-list", "loan-list-borrower", "loan-list-borrower u/", "loan-list-borrower u/00",
                "loan-list-borrower u/U1000", "loan-list-borrower u/abc", "loan-list-borrower u/U001 "
                        + "extra", "List borrower/001",
                "loan-list-borrower u/U001\n");
    }
}
