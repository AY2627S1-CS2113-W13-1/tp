package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanUpdateUnsuccessful;

/** Tests validated loan updates, equipment ownership and save-before-mutate behaviour. */
class LoanUpdateHandlerTest {
    private static final String ORIGINAL_LOAN =
            "loan id/001 b/002 issued/01-10-26 due/10-10-26";
    private EquipmentHandler equipment;
    private LoanHandler loans;
    private Equipment camera;
    private Equipment tripod;
    private Path loanFile;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() throws Exception {
        equipment = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        camera = equipment.addEquipment("add i/Camera id/001 type/camera cond/4");
        tripod = equipment.addEquipment("add i/Tripod id/002 type/tripod cond/5");
        loanFile = temporaryDirectory.resolve("loans.txt");
        loans = new LoanHandler(equipment, loanFile);
        loans.addLoan(ORIGINAL_LOAN);
    }

    @Test
    void updateLoan_borrowerAndDueDate_updatesOneLoanAndKeepsEquipmentOwnership() throws Exception {
        Loan original = loans.getLoanList().getFirst();

        Loan updated = loans.updateLoan("update loan/001 b/007 due/15-10-26");

        assertSame(original, updated);
        assertEquals("001", updated.getLoanId());
        assertEquals("007", updated.getBorrowerId());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), updated.getIssuedDate());
        assertEquals(LocalDateTime.of(2026, 10, 15, 0, 0), updated.getDueDate());
        assertTrue(camera.isLoaned());
        assertEquals("001", camera.getLoanId());
        assertFalse(tripod.isLoaned());
        assertTrue(Files.readString(loanFile).contains("b/007 issued/01-10-26 due/15-10-26"));
    }

    @Test
    void updateLoan_borrowerZero_updatesWithinDocumentedBoundary() throws Exception {
        Loan updated = loans.updateLoan("update loan/001 b/000");

        assertEquals("000", updated.getBorrowerId());
        assertTrue(Files.readString(loanFile).contains("b/000"));
    }

    @Test
    void updateLoan_issueDateAndDueDateEqual_areAccepted() throws Exception {
        Loan updated = loans.updateLoan("update loan/001 issued/10-10-26 due/10-10-26");

        assertEquals(LocalDateTime.of(2026, 10, 10, 0, 0), updated.getIssuedDate());
        assertEquals(LocalDateTime.of(2026, 10, 10, 0, 0), updated.getDueDate());
    }

    @Test
    void updateLoan_itemId_rebindsEquipmentWithoutChangingLoanId() throws Exception {
        Loan original = loans.getLoanList().getFirst();

        Loan updated = loans.updateLoan("update loan/001 id/002 b/005 due/20-10-26");

        assertSame(original, updated);
        assertEquals("001", updated.getLoanId());
        assertEquals("002", updated.getItemId());
        assertEquals("005", updated.getBorrowerId());
        assertFalse(camera.isLoaned());
        assertTrue(tripod.isLoaned());
        assertEquals("001", tripod.getLoanId());
        assertTrue(Files.readString(loanFile).contains("id/002 b/005"));
    }

    @Test
    void updateLoan_unknownLoanId_rejectsAndPreservesOriginal() {
        Loan original = loans.getLoanList().getFirst();

        assertThrows(LoanUpdateUnsuccessful.class,
                () -> loans.updateLoan("update loan/999 b/007"));

        assertSame(original, loans.getLoanList().getFirst());
        assertEquals("002", original.getBorrowerId());
        assertTrue(camera.isLoaned());
    }

    @Test
    void updateLoan_invalidEffectiveDateRange_rejectsAndPreservesSavedRecord() throws Exception {
        Loan original = loans.getLoanList().getFirst();
        String saved = Files.readString(loanFile);

        assertThrows(LoanUpdateUnsuccessful.class,
                () -> loans.updateLoan("update loan/001 due/30-09-26"));

        assertEquals("002", original.getBorrowerId());
        assertEquals(LocalDateTime.of(2026, 10, 10, 0, 0), original.getDueDate());
        assertEquals(saved, Files.readString(loanFile));
    }

    @Test
    void updateLoan_returnedLoan_rejectsWithoutChangingHistory() {
        Loan original = loans.getLoanList().getFirst();
        original.setReturned(true);

        assertThrows(LoanUpdateUnsuccessful.class,
                () -> loans.updateLoan("update loan/001 b/007 due/15-10-26"));

        assertTrue(original.isReturned());
        assertEquals("002", original.getBorrowerId());
        assertFalse(camera.isLoaned());
    }

    @Test
    void updateLoan_loanedReplacement_rejectsWithoutChangingEitherAssociation() throws Exception {
        loans.addLoan("loan id/002 b/003 issued/01-10-26 due/10-10-26");
        Loan original = loans.getLoanList().getFirst();

        assertThrows(LoanUpdateUnsuccessful.class,
                () -> loans.updateLoan("update loan/001 id/002"));

        assertEquals("001", original.getItemId());
        assertEquals("001", camera.getLoanId());
        assertEquals("002", tripod.getLoanId());
    }

    @Test
    void updateLoan_unserviceableReplacement_rejectsWithoutChangingHistory() throws Exception {
        loans.addLoan("loan id/002 b/003 issued/01-10-26 due/10-10-26");
        loans.returnLoan("return loan/002 serviceable/no damage/NONE");
        Loan original = loans.getLoanList().getFirst();
        String saved = Files.readString(loanFile);

        assertThrows(LoanUpdateUnsuccessful.class,
                () -> loans.updateLoan("update loan/001 id/002"));

        assertEquals("001", original.getItemId());
        assertTrue(camera.isLoaned());
        assertFalse(tripod.isLoaned());
        assertFalse(tripod.isServiceable());
        assertEquals(saved, Files.readString(loanFile));
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateCommands")
    void updateLoan_invalidCommand_preservesLoanAndSavedFile(String command) throws Exception {
        Loan original = loans.getLoanList().getFirst();
        String saved = Files.readString(loanFile);

        assertThrows(LoanUpdateUnsuccessful.class, () -> loans.updateLoan(command), command);

        assertSame(original, loans.getLoanList().getFirst());
        assertEquals("001", original.getItemId());
        assertEquals("002", original.getBorrowerId());
        assertTrue(camera.isLoaned());
        assertFalse(tripod.isLoaned());
        assertEquals(saved, Files.readString(loanFile));
    }

    private static Stream<String> invalidUpdateCommands() {
        return Stream.of(null, "", "update", "update loan", "update loan/001",
                "update loan/001 b/007 b/008", "update loan/001 issued/11-10-26",
                "update loan/001 due/31-02-26", "update loan/001 id/999", "update loan/001 id/000",
                "update loan/001 id/003", "update loan/999 b/007", "update loan/001 unknown/007",
                "update loan/001 b/007\n due/15-10-26");
    }

    @Test
    void updateLoan_saveFailure_preservesLiveLoanAndSavedFile() throws Exception {
        Loan original = loans.getLoanList().getFirst();
        String saved = Files.readString(loanFile);
        Files.delete(loanFile);
        Files.createDirectory(loanFile);
        Path sentinel = loanFile.resolve("keep.txt");
        Files.writeString(sentinel, "retain blocked path");

        assertThrows(LoanUpdateUnsuccessful.class,
                () -> loans.updateLoan("update loan/001 b/007 due/15-10-26"));

        assertEquals("002", original.getBorrowerId());
        assertTrue(camera.isLoaned());
        assertTrue(Files.isDirectory(loanFile));
        assertEquals("retain blocked path", Files.readString(sentinel));

        Files.delete(sentinel);
        Files.delete(loanFile);
        Files.writeString(loanFile, saved);
    }

    @Test
    void updateLoan_sequenceSaveFailure_rollsBackLoanFileAndLiveState() throws Exception {
        Loan original = loans.getLoanList().getFirst();
        String savedLoans = Files.readString(loanFile);
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        String savedSequence = Files.readString(sequenceFile);
        Files.delete(sequenceFile);
        Files.createDirectory(sequenceFile);
        Path sentinel = sequenceFile.resolve("keep.txt");
        Files.writeString(sentinel, "retain blocked sequence path");

        assertThrows(LoanUpdateUnsuccessful.class,
                () -> loans.updateLoan("update loan/001 b/007 due/15-10-26"));

        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals("002", original.getBorrowerId());
        assertTrue(camera.isLoaned());
        assertEquals("retain blocked sequence path", Files.readString(sentinel));

        Files.delete(sentinel);
        Files.delete(sequenceFile);
        Files.writeString(sequenceFile, savedSequence);
    }

    @Test
    void updateLoan_reloadedHandler_preservesEditedDetailsAndEquipmentAssociation() throws Exception {
        loans.updateLoan("update loan/001 id/002 b/007 issued/03-10-26 due/15-10-26");
        equipment.saveToStorage();

        EquipmentHandler reloadedEquipment = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        reloadedEquipment.loadFromStorage();
        LoanHandler reloadedLoans = new LoanHandler(reloadedEquipment, loanFile);
        reloadedLoans.loadFromStorage();
        Loan reloaded = reloadedLoans.getLoanList().getFirst();

        assertEquals("001", reloaded.getLoanId());
        assertEquals("002", reloaded.getItemId());
        assertEquals("007", reloaded.getBorrowerId());
        assertEquals(LocalDateTime.of(2026, 10, 3, 0, 0), reloaded.getIssuedDate());
        assertEquals(LocalDateTime.of(2026, 10, 15, 0, 0), reloaded.getDueDate());
        assertFalse(reloadedEquipment.getEquipmentById("001").isLoaned());
        assertTrue(reloadedEquipment.getEquipmentById("002").isLoaned());
        assertEquals("001", reloadedEquipment.getEquipmentById("002").getLoanId());
    }
}
