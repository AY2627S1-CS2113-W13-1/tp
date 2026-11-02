package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Tests assignment, persistence, and lifecycle safety of unique loan identifiers. */
class LoanIdPersistenceTest {
    private static final String FIRST_LOAN =
            "loan id/E001 b/U002 issued/01-10-26 due/02-10-26";
    private EquipmentHandler equipment;
    private Equipment camera;
    private ByteArrayOutputStream output;
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() throws Exception {
        equipment = new EquipmentHandler();
        camera = equipment.addEquipment("add i/Camera id/E001 type/camera cond/1");
        equipment.addEquipment("add i/Tripod id/E003 type/tripod cond/2");
        equipment.addEquipment("add i/Lens id/E005 type/lens cond/3");
        output = new ByteArrayOutputStream();
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void addLoan_assignsSequentialIdentifiersAndReservesEquipment() throws Exception {
        LoanHandler handler = new LoanHandler(equipment);

        Loan first = handler.addLoan(FIRST_LOAN);
        Loan second = handler.addLoan("loan id/E003 b/U004 issued/03-10-26 due/04-10-26");

        assertEquals("L001", first.getLoanId());
        assertEquals("L002", second.getLoanId());
        assertEquals("L001", camera.getLoanId());
        assertTrue(camera.isLoaned());
        assertEquals("L003", handler.getNextLoanId());
    }

    @Test
    void deletingLoan_createsGapAndNeverReusesIdentifier() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, loanFile);
        handler.addLoan(FIRST_LOAN);
        handler.addLoan("loan id/E003 b/U004 issued/03-10-26 due/04-10-26");

        handler.deleteLoan("delete loan/L001");
        Loan replacement = handler.addLoan("loan id/E005 b/U006 issued/05-10-26 due/06-10-26");

        assertEquals("L003", replacement.getLoanId());
        assertEquals("L004", Files.readString(temporaryDirectory.resolve("loan-sequence.txt")).trim()
                .replace("nextLoanId/", ""));
    }

    @Test
    void loadHighestIdentifier_assignsNextIdentifier() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        Files.writeString(loanFile,
                "loan loanId/L021 id/E001 b/U002 issued/01-10-26 due/02-10-26 returned/false\n");
        Files.writeString(sequenceFile, "nextLoanId/L022\n");

        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile);
        handler.loadFromStorage();
        Loan next = handler.addLoan("loan id/E003 b/U004 issued/03-10-26 due/04-10-26");

        assertEquals("L022", next.getLoanId());
    }

    @Test
    void deletingHighestIdentifier_preservesNextSequence() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        Files.writeString(loanFile,
                "loan loanId/L021 id/E001 b/U002 issued/01-10-26 due/02-10-26 returned/false\n");
        Files.writeString(sequenceFile, "nextLoanId/L022\n");

        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile);
        handler.loadFromStorage();
        handler.deleteLoan("delete loan/L021");
        handler = new LoanHandler(equipment, loanFile, sequenceFile);
        handler.loadFromStorage();
        Loan next = handler.addLoan("loan id/E003 b/U004 issued/03-10-26 due/04-10-26");

        assertEquals("L022", next.getLoanId());
        assertEquals("nextLoanId/L023", Files.readString(sequenceFile).trim());
    }

    @Test
    void loanIdLimit_rejectsTheOneThousandthLoan() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        Files.writeString(sequenceFile, "nextLoanId/L999\n");
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile);
        handler.loadFromStorage();

        assertEquals("L999", handler.addLoan(FIRST_LOAN).getLoanId());
        LoanAdditionUnsuccessful exception = assertThrows(LoanAdditionUnsuccessful.class,
                () -> handler.addLoan("loan id/E003 b/U004 issued/03-10-26 due/04-10-26"));

        assertTrue(exception.getMessage().contains("maximum is 999"));
        assertEquals("L1000", handler.getNextLoanId());
        assertEquals("nextLoanId/L1000", Files.readString(sequenceFile).trim());
    }

    @Test
    void legacyLoanRecords_areAssignedAndPersistedInFileOrder() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Files.writeString(loanFile, "loan id/E001 b/U002 issued/01-10-26 due/02-10-26\n"
                + "loan id/E003 b/U004 issued/03-10-26 due/04-10-26 returned/true\n");

        LoanHandler handler = new LoanHandler(equipment, loanFile);
        handler.loadFromStorage();

        assertEquals("L001", handler.getLoanList().get(0).getLoanId());
        assertEquals("L002", handler.getLoanList().get(1).getLoanId());
        assertTrue(Files.readString(loanFile).contains("loan loanId/L001 id/E001"));
        assertEquals("nextLoanId/L003", Files.readString(temporaryDirectory.resolve("loan-sequence.txt")).trim());
    }

    @Test
    void equipmentLoanId_isRestoredFromActiveLoansWithoutChangingEquipmentStorage() throws Exception {
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        EquipmentHandler firstEquipment = new EquipmentHandler(equipmentFile);
        Equipment firstCamera = firstEquipment.addEquipment(
                "add i/Camera id/E001 type/camera cond/1 sensor/APS-C remarks/Loan ID label");
        String savedEquipment = Files.readString(equipmentFile);
        LoanHandler firstLoans = new LoanHandler(firstEquipment, loanFile);
        firstLoans.addLoan(FIRST_LOAN);

        assertEquals("L001", firstCamera.getLoanId());
        assertEquals(savedEquipment, Files.readString(equipmentFile));

        EquipmentHandler reloadedEquipment = new EquipmentHandler(equipmentFile);
        reloadedEquipment.loadFromStorage();
        assertNull(reloadedEquipment.getEquipmentList().getFirst().getLoanId());
        LoanHandler reloadedLoans = new LoanHandler(reloadedEquipment, loanFile);
        reloadedLoans.loadFromStorage();

        assertEquals("L001", reloadedEquipment.getEquipmentList().getFirst().getLoanId());
        assertTrue(reloadedEquipment.getEquipmentList().getFirst().isLoaned());
        assertEquals(savedEquipment, Files.readString(equipmentFile));
    }

    @Test
    void borrowerZero_isAcceptedInNewAndMigratedSavedRecords() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, loanFile);
        handler.addLoan(FIRST_LOAN.replace("b/U002", "b/U000"));
        handler.loadFromStorage();
        assertEquals("U000", handler.getLoanList().getFirst().getBorrowerId());

        Files.writeString(loanFile, FIRST_LOAN.replace("b/U002", "b/U000") + "\n");
        handler.loadFromStorage();

        assertEquals("U000", handler.getLoanList().getFirst().getBorrowerId());
        assertEquals("L002", handler.getLoanList().getFirst().getLoanId());
        assertEquals("L002", camera.getLoanId());
        assertTrue(Files.readString(loanFile).contains("b/U000"));
    }

    @Test
    void staleRuntimeLoanId_withoutActiveLoan_isClearedAfterLoad() throws Exception {
        camera.setLoanId("L021");
        LoanHandler handler = new LoanHandler(equipment, temporaryDirectory.resolve("loans.txt"));

        handler.loadFromStorage();

        assertNull(camera.getLoanId());
        assertFalse(camera.isLoaned());
        assertTrue(handler.getLoanList().isEmpty());
    }

    @Test
    void deletingHistoricalLoan_doesNotReleaseNewerActiveLoan() throws Exception {
        LoanHandler handler = new LoanHandler(equipment);
        Loan first = handler.addLoan(FIRST_LOAN);
        first.setReturned(true);
        Loan current = handler.addLoan(FIRST_LOAN.replace("01-10-26", "03-10-26")
                .replace("02-10-26", "04-10-26"));

        handler.deleteLoan("delete loan/L001");

        assertEquals("L002", current.getLoanId());
        assertEquals("L002", camera.getLoanId());
        assertThrows(IllegalStateException.class, () -> first.setReturned(false));
        assertEquals("L002", camera.getLoanId());
    }

    @Test
    void deleteLoan_acceptsOnlyExactLoanIdSyntax() throws Exception {
        LoanHandler handler = new LoanHandler(equipment);
        handler.addLoan(FIRST_LOAN);

        for (String command : new String[]{"delete-loan id/E001", "delete item/E001", "delete /id001",
            "delete loan/L001 issued/01-10-26", "delete loan/01", "delete loan/L000"}) {
            assertThrows(LoanDeletionUnsuccessful.class, () -> handler.deleteLoan(command));
        }
        assertEquals(1, handler.getLoanCount());
        assertEquals("L001", camera.getLoanId());
    }

    @Test
    void failedLoanSave_doesNotConsumeIdOrReserveEquipment() throws Exception {
        Path parentFile = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(parentFile, "not a directory");
        Path loanFile = parentFile.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, loanFile);

        assertThrows(LoanAdditionUnsuccessful.class, () -> handler.addLoan(FIRST_LOAN));

        assertEquals(0, handler.getLoanCount());
        assertEquals("L001", handler.getNextLoanId());
        assertFalse(camera.isLoaned());
        assertNull(camera.getLoanId());
    }

    @Test
    void failedSequenceSave_rollsBackLoanFileStateAndCounter() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile);
        Loan original = handler.addLoan(FIRST_LOAN);
        String savedLoans = Files.readString(loanFile);
        Files.delete(sequenceFile);
        Files.createDirectory(sequenceFile);
        Files.writeString(sequenceFile.resolve("keep.txt"), "keep sequence data");

        assertThrows(LoanAdditionUnsuccessful.class,
                () -> handler.addLoan(FIRST_LOAN.replace("id/E001", "id/E003")));

        assertEquals(List.of(original), handler.getLoanList());
        assertEquals("L002", handler.getNextLoanId());
        assertEquals("L001", camera.getLoanId());
        assertNull(equipment.getEquipmentList().get(1).getLoanId());
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals("keep sequence data", Files.readString(sequenceFile.resolve("keep.txt")));
    }

    @Test
    void viewAllLoans_displaysLoanId() throws Exception {
        LoanHandler handler = new LoanHandler(equipment);
        handler.addLoan(FIRST_LOAN);
        handler.viewAllLoans();

        String listing = output.toString(StandardCharsets.UTF_8);
        assertTrue(listing.contains("LOAN ID"));
        assertTrue(listing.contains("L001"));
    }

    @Test
    void mixedLegacyRecords_existingSequenceDoesNotReuseDeletedIdentifiers() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        Files.writeString(loanFile, FIRST_LOAN + "\n"
                + "loan loanId/L006 id/E003 b/U000 issued/01-10-26 due/02-10-26 returned/true\n"
                + "loan id/E005 b/U004 issued/01-10-26 due/02-10-26 returned/true\n");
        Files.writeString(sequenceFile, "nextLoanId/L005\n");
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile);

        handler.loadFromStorage();

        assertEquals(List.of("L005", "L006", "L007"),
                handler.getLoanList().stream().map(Loan::getLoanId).toList());
        assertEquals("L008", handler.getNextLoanId());
        assertEquals("L005", camera.getLoanId());
        String migrated = Files.readString(loanFile);
        handler.loadFromStorage();
        assertEquals(migrated, Files.readString(loanFile));
        assertEquals("L008", handler.getNextLoanId());
    }

    @Test
    void failedMigration_preservesCurrentLinksCounterAndLegacyFile() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile);
        Loan original = handler.addLoan(FIRST_LOAN);
        String legacy = FIRST_LOAN.replace("id/E001", "id/E003") + "\n";
        Files.writeString(loanFile, legacy);
        Files.delete(sequenceFile);
        Files.createDirectory(sequenceFile);
        Files.writeString(sequenceFile.resolve("keep.txt"), "keep sequence data");

        assertThrows(IOException.class, handler::loadFromStorage);

        assertEquals(List.of(original), handler.getLoanList());
        assertEquals("L002", handler.getNextLoanId());
        assertEquals("L001", camera.getLoanId());
        assertNull(equipment.getEquipmentList().get(1).getLoanId());
        assertEquals(legacy, Files.readString(loanFile));
        original.setReturned(true);
        assertFalse(camera.isLoaned());
    }

    @Test
    void invalidItemAndLoanIds_areRejectedBeforeConstruction() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, loanFile);
        assertThrows(LoanAdditionUnsuccessful.class,
                () -> handler.addLoan(FIRST_LOAN.replace("id/E001", "id/E000")));
        String[] invalidRecords = {
            FIRST_LOAN.replace("id/E001", "id/E000"),
            "loan loanId/L000 " + FIRST_LOAN.substring("loan ".length()),
            "loan loanId/L001 " + FIRST_LOAN.substring("loan ".length()).replace("id/E001", "id/E000"),
            "loan loanId/L1000 " + FIRST_LOAN.substring("loan ".length())
        };
        for (String record : invalidRecords) {
            Files.writeString(loanFile, record + "\n");
            assertThrows(IOException.class, handler::loadFromStorage);
            assertTrue(handler.getLoanList().isEmpty());
            assertEquals("L001", handler.getNextLoanId());
            assertNull(camera.getLoanId());
        }
    }

    @Test
    void deletingByLoanId_targetsTheRecordEvenWhenItsItemIdDiffers() throws Exception {
        LoanHandler handler = new LoanHandler(equipment);
        Loan first = handler.addLoan(FIRST_LOAN.replace("id/E001", "id/E003"));
        Loan second = handler.addLoan(FIRST_LOAN);

        assertSame(first, handler.deleteLoan("delete loan/L001"));

        assertEquals(List.of(second), handler.getLoanList());
        assertEquals("L002", camera.getLoanId());
        assertNull(equipment.getEquipmentList().get(1).getLoanId());
    }

}
