package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanHistoryUnsuccessful;

/** Verifies selecting the active equipment currently held by one borrower. */
class BorrowerLoanQueryTest {
    private EquipmentHandler equipment;
    private LoanHandler loans;

    @BeforeEach
    void setUp() throws Exception {
        equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        equipment.addEquipment("add i/Lens id/002 type/lens cond/5 zoom/24-70mm");
        equipment.addEquipment("add i/Tripod id/003 type/tripod cond/3");
        loans = new LoanHandler(equipment);
    }

    @Test
    void activeLoansForBorrower_oneLoan_returnsThatLoan() throws Exception {
        Loan camera = addLoan("001", "111");

        List<Loan> result = loans.getActiveLoansForBorrower("list loans b/111");

        assertEquals(List.of(camera), result);
        assertFalse(result.getFirst().isReturned());
    }

    @Test
    void activeLoansForBorrower_multipleLoans_returnsAllMatchingActiveLoans() throws Exception {
        Loan camera = addLoan("001", "111");
        Loan lens = addLoan("002", "111");
        Loan tripod = addLoan("003", "222");

        List<Loan> result = loans.getActiveLoansForBorrower("list loans member/111");

        assertEquals(List.of(camera, lens), result);
        assertFalse(result.contains(tripod));
    }

    @Test
    void activeLoansForBorrower_excludesReturnedLoansAndOtherBorrowers() throws Exception {
        Loan returned = addLoan("001", "111");
        loans.returnLoan("return loan/" + returned.getLoanId()
                + " serviceable/yes damage/NONE remarks/");
        Loan active = addLoan("002", "111");
        Loan otherBorrower = addLoan("003", "222");

        List<Loan> result = loans.getActiveLoansForBorrower("list loans b/111");

        assertEquals(List.of(active), result);
        assertTrue(returned.isReturned());
        assertFalse(result.contains(otherBorrower));
        assertEquals(3, loans.getLoanCount());
    }

    @Test
    void activeLoansForBorrower_unknownOrEmptyBorrower_returnsReadOnlyEmptyList() throws Exception {
        addLoan("001", "111");

        List<Loan> unknownBorrower = loans.getActiveLoansForBorrower("list loans b/999");
        List<Loan> noActiveLoans = loans.getActiveLoansForBorrower("list loans b/222");

        assertTrue(unknownBorrower.isEmpty());
        assertTrue(noActiveLoans.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> unknownBorrower.add(null));
        assertEquals(1, loans.getLoanCount());
        assertTrue(equipment.getEquipmentById("001").isLoaned());
    }

    @ParameterizedTest
    @MethodSource("invalidBorrowerLoanListCommands")
    void activeLoansForBorrower_invalidCommand_rejectsWithoutChangingLoans(String command) {
        assertThrows(LoanHistoryUnsuccessful.class, () -> loans.getActiveLoansForBorrower(command));
        assertEquals(0, loans.getLoanCount());
    }

    private static Stream<String> invalidBorrowerLoanListCommands() {
        return Stream.of(null, "", "list loans", "list loans b/", "list loans b/12", "list loans b/1000",
                "list loans b/abc", "list loan b/111", "list loans id/111", "list loans b/-11",
                "list loans b/111 extra", "List loans b/111", "list loans b/111\n");
    }

    private Loan addLoan(String itemId, String borrowerId) throws Exception {
        return loans.addLoan("loan id/" + itemId + " b/" + borrowerId
                + " issued/01-10-26 due/02-10-26");
    }
}
