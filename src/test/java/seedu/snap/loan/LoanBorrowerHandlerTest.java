package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanBorrowerUnsuccessful;

/** Tests borrower loan selection without changing loans, equipment or persistence. */
class LoanBorrowerHandlerTest {
    private static final Logger LOAN_LOGGER = Logger.getLogger(LoanHandler.class.getName());
    private EquipmentHandler equipment;
    private LoanHandler loans;
    private Equipment camera;
    private Equipment tripod;
    private Equipment lens;
    private Path loanFile;
    private RecordingHandler logHandler;
    private Level originalLoggerLevel;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() throws Exception {
        equipment = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        camera = equipment.addEquipment("add i/Camera id/001 type/camera cond/4");
        tripod = equipment.addEquipment("add i/Tripod id/002 type/tripod cond/5");
        lens = equipment.addEquipment("add i/Lens id/003 type/lens cond/5");
        loanFile = temporaryDirectory.resolve("loans.txt");
        loans = new LoanHandler(equipment, loanFile);
        logHandler = new RecordingHandler();
        logHandler.setLevel(Level.ALL);
        originalLoggerLevel = LOAN_LOGGER.getLevel();
        LOAN_LOGGER.setLevel(Level.ALL);
        LOAN_LOGGER.addHandler(logHandler);
    }

    @AfterEach
    void restoreLogger() {
        LOAN_LOGGER.removeHandler(logHandler);
        LOAN_LOGGER.setLevel(originalLoggerLevel);
    }

    @Test
    void getActiveLoansForBorrower_oneMatchingLoan_returnsThatLoanOnly() throws Exception {
        Loan matching = addLoan("001", "002", "01-10-26", "10-10-26");
        Loan otherBorrower = addLoan("002", "003", "01-10-26", "10-10-26");

        List<Loan> result = loans.getActiveLoansForBorrower("list borrower/002");

        assertEquals(List.of(matching), result);
        assertSame(matching, result.getFirst());
        assertFalse(result.contains(otherBorrower));
        assertTrue(camera.isLoaned());
        assertTrue(tripod.isLoaned());
        assertTrue(logHandler.contains(Level.INFO, "Viewed {0} active loan(s) for borrower ID {1}."));
    }

    @Test
    void getActiveLoansForBorrower_multipleMatchingLoans_preservesLoanOrder() throws Exception {
        Loan returned = addLoan("001", "002", "01-10-26", "02-10-26");
        loans.returnLoan("return loan/" + returned.getLoanId() + " serviceable/yes damage/NONE remarks/");
        Loan firstActive = addLoan("002", "002", "03-10-26", "04-10-26");
        Loan secondActive = addLoan("003", "002", "05-10-26", "06-10-26");

        List<Loan> result = loans.getActiveLoansForBorrower("list member/002");

        assertEquals(List.of(firstActive, secondActive), result);
        assertFalse(result.contains(returned));
        assertEquals(List.of(returned, firstActive, secondActive), loans.getLoanList());
    }

    @Test
    void getActiveLoansForBorrower_unknownBorrower_reportsClearError() {
        LoanBorrowerUnsuccessful error = assertThrows(LoanBorrowerUnsuccessful.class,
                () -> loans.getActiveLoansForBorrower("list borrower/999"));

        assertEquals("No member found for borrower ID 999.", error.getMessage());
        assertEquals(0, loans.getLoanCount());
        assertFalse(camera.isLoaned());
        assertTrue(logHandler.contains(Level.WARNING, "Borrower loan query found no borrower with ID {0}."));
    }

    @Test
    void getActiveLoansForBorrower_knownBorrowerWithOnlyReturnedLoans_returnsEmptyList() throws Exception {
        Loan returned = addLoan("001", "002", "01-10-26", "02-10-26");
        loans.returnLoan("return loan/" + returned.getLoanId() + " serviceable/yes damage/NONE remarks/");

        List<Loan> result = loans.getActiveLoansForBorrower("list borrower/002");

        assertTrue(result.isEmpty());
        assertTrue(returned.isReturned());
        assertFalse(camera.isLoaned());
        assertTrue(logHandler.contains(Level.INFO, "Viewed {0} active loan(s) for borrower ID {1}."));
    }

    @Test
    void getActiveLoansForBorrower_resultIsReadOnlyAndDoesNotPersistChanges() throws Exception {
        Loan matching = addLoan("001", "002", "01-10-26", "10-10-26");
        String savedLoans = Files.readString(loanFile);
        String savedEquipment = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        String savedSequence = Files.readString(temporaryDirectory.resolve("loan-sequence.txt"));

        List<Loan> result = loans.getActiveLoansForBorrower("list borrower/002");

        assertThrows(UnsupportedOperationException.class, () -> result.add(matching));
        assertEquals(List.of(matching), loans.getLoanList());
        assertEquals("002", loans.getNextLoanId());
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedEquipment, Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertEquals(savedSequence, Files.readString(temporaryDirectory.resolve("loan-sequence.txt")));
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), matching.getIssuedDate());
    }

    @Test
    void getActiveLoansForBorrower_invalidCommand_logsWarning() {
        assertThrows(LoanBorrowerUnsuccessful.class,
                () -> loans.getActiveLoansForBorrower("list borrower/abc"));

        assertTrue(logHandler.contains(Level.WARNING, "Borrower loan query rejected: {0}"));
    }

    /** Adds a persisted loan with fixed dates so each selection case is deterministic. */
    private Loan addLoan(String itemId, String borrowerId, String issued, String due) throws Exception {
        return loans.addLoan("loan id/" + itemId + " b/" + borrowerId
                + " issued/" + issued + " due/" + due);
    }

    /** Captures borrower-query records without changing the application's configured handlers. */
    private static class RecordingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
            // Records are kept in memory and need no flushing.
        }

        @Override
        public void close() {
            // Records are owned by the test and need no external resource cleanup.
        }

        boolean contains(Level level, String message) {
            return records.stream().anyMatch(record -> record.getLevel().equals(level)
                    && record.getMessage().equals(message));
        }
    }
}
