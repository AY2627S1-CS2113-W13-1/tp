package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanHistoryUnsuccessful;
import seedu.snap.exceptions.LoanReturnUnsuccessful;

/** Verifies equipment-specific history, permanent equipment identities and safe legacy loading. */
class EquipmentLoanHistoryTest {
    private EquipmentHandler equipment;
    private Equipment camera;
    private LoanHandler loans;
    private Path equipmentFile;
    private Path loanFile;
    private Path sequenceFile;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() throws Exception {
        equipmentFile = temporaryDirectory.resolve("equipment.txt");
        loanFile = temporaryDirectory.resolve("loans.txt");
        sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        equipment = new EquipmentHandler(equipmentFile);
        camera = equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        equipment.addEquipment("add i/Lens id/002 type/lens cond/5 zoom/24-70mm");
        loans = new LoanHandler(equipment, loanFile, sequenceFile);
    }

    @Test
    void history_activeAndReturnedLoans_areSortedByIssueDateThenLoanId() throws Exception {
        Loan later = addLoan("001", "111", "20-10-26", "21-10-26");
        returnLoan(later);
        Loan earlier = addLoan("001", "222", "01-10-26", "02-10-26");
        returnLoan(earlier);
        Loan sameDate = addLoan("001", "333", "01-10-26", "03-10-26");
        Loan otherItem = addLoan("002", "444", "01-10-26", "04-10-26");

        List<Loan> history = loans.getLoanHistory("history id/001");

        assertEquals(List.of(earlier, sameDate, later), history);
        assertSame(earlier, history.getFirst());
        assertTrue(earlier.isReturned());
        assertFalse(sameDate.isReturned());
        assertFalse(history.contains(otherItem));
        assertEquals(List.of(later, earlier, sameDate, otherItem), loans.getLoanList());
        assertEquals(sameDate.getLoanId(), camera.getLoanId());
        assertTrue(camera.isLoaned());
    }

    @Test
    void history_returnedCopyAndReload_preserveTheOriginalEquipmentIdentity() throws Exception {
        Loan original = addLoan("001", "111", "01-10-26", "02-10-26");
        String recordId = camera.getRecordId();
        assertEquals(UUID.fromString(recordId).toString(), recordId);
        assertEquals(recordId, original.getEquipmentRecordId());

        Loan proposedReturn = original.returnedCopy();

        assertEquals(recordId, proposedReturn.getEquipmentRecordId());
        assertTrue(proposedReturn.isReturned());
        assertFalse(original.isReturned());
        assertEquals(original.getLoanId(), camera.getLoanId());
        returnLoan(original);
        equipment.updateEquipment("update id/001 i/Updated Camera cond/5");
        assertEquals(recordId, original.getEquipmentRecordId());
        assertEquals(recordId, camera.getRecordId());

        EquipmentHandler restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile);
        restartedLoans.loadFromStorage();
        Loan restored = restartedLoans.getLoanHistory("history id/001").getFirst();

        assertEquals(recordId, restartedEquipment.getEquipmentList().getFirst().getRecordId());
        assertEquals(recordId, restored.getEquipmentRecordId());
        assertEquals(original.getLoanId(), restored.getLoanId());
        assertTrue(restored.isReturned());
        assertFalse(restartedEquipment.getEquipmentList().getFirst().isLoaned());
        assertEquals("Updated Camera", restartedEquipment.getEquipmentList().getFirst().getItemName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"camera", "battery"})
    void history_reusedItemId_excludesPreviousPhysicalItemEvenWithSameName(String replacementType) throws Exception {
        Loan history = addLoan("001", "111", "01-10-26", "02-10-26");
        returnLoan(history);
        String oldRecordId = camera.getRecordId();
        assertSame(camera, equipment.deleteEquipment("delete item/001"));
        Equipment replacement = equipment.addEquipment(
                "add i/Camera id/001 type/" + replacementType + " cond/4");

        assertNotEquals(oldRecordId, replacement.getRecordId());
        assertTrue(loans.getLoanHistory("history id/001").isEmpty());
        Loan current = addLoan("001", "222", "03-10-26", "04-10-26");

        assertEquals(List.of(current), loans.getLoanHistory("history id/001"));
        assertEquals(List.of(history, current), loans.getLoanList());
        assertEquals(oldRecordId, history.getEquipmentRecordId());
        assertEquals(replacement.getRecordId(), current.getEquipmentRecordId());
        assertEquals("002", current.getLoanId());
        assertThrows(LoanReturnUnsuccessful.class,
                () -> loans.returnLoan("return loan/001 serviceable/no damage/NONE remarks/Old loan"));
        assertSame(history, loans.deleteLoan("delete loan/001"));
        history.setReturned(true);
        assertThrows(IllegalStateException.class, () -> history.setReturned(false));
        assertTrue(replacement.isLoaned());
        assertEquals(current.getLoanId(), replacement.getLoanId());
        assertEquals(List.of(current), loans.getLoanHistory("history id/001"));
    }

    @Test
    void history_restartBetweenDeletionAndReplacement_doesNotReconnectOldHistory() throws Exception {
        Loan oldLoan = addLoan("001", "111", "01-10-26", "02-10-26");
        returnLoan(oldLoan);
        String oldRecordId = camera.getRecordId();
        equipment.deleteEquipment("delete item/001");
        EquipmentHandler restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile);
        restartedLoans.loadFromStorage();
        assertThrows(LoanHistoryUnsuccessful.class, () -> restartedLoans.getLoanHistory("history id/001"));

        Equipment replacement = restartedEquipment.addEquipment("add i/Camera id/001 type/camera cond/4");
        assertNotEquals(oldRecordId, replacement.getRecordId());
        assertTrue(restartedLoans.getLoanHistory("history id/001").isEmpty());
        Loan current = restartedLoans.addLoan("loan id/001 b/222 issued/03-10-26 due/04-10-26");
        restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler reloadedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile);
        reloadedLoans.loadFromStorage();

        assertEquals(List.of(current.getLoanId()), reloadedLoans.getLoanHistory("history id/001")
                .stream().map(Loan::getLoanId).toList());
        assertEquals(List.of(oldRecordId, replacement.getRecordId()), reloadedLoans.getLoanList()
                .stream().map(Loan::getEquipmentRecordId).toList());
        assertEquals(2, reloadedLoans.getLoanCount());
        assertEquals("002", restartedEquipment.getEquipmentById("001").getLoanId());
    }

    @Test
    void history_existingItemWithoutLoans_returnsAnEmptyReadOnlyList() throws Exception {
        List<Loan> history = loans.getLoanHistory("history id/001");

        assertTrue(history.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> history.add(null));
        assertEquals(0, loans.getLoanCount());
        assertEquals("001", loans.getNextLoanId());
        assertFalse(camera.isLoaned());
        assertFalse(Files.exists(loanFile));
        assertFalse(Files.exists(sequenceFile));
    }

    @Test
    void history_queryAndDisplay_doNotChangeMemoryFilesOrModificationTimes() throws Exception {
        Loan active = addLoan("001", "111", "01-10-26", "02-10-26");
        String equipmentBefore = Files.readString(equipmentFile);
        String loansBefore = Files.readString(loanFile);
        String sequenceBefore = Files.readString(sequenceFile);
        FileTime marker = FileTime.fromMillis(1000);
        Files.setLastModifiedTime(equipmentFile, marker);
        Files.setLastModifiedTime(loanFile, marker);
        Files.setLastModifiedTime(sequenceFile, marker);

        List<Loan> history = loans.getLoanHistory("history id/001");
        captureHistory("history id/001");
        captureHistory("history id/002");

        assertEquals(List.of(active), history);
        assertThrows(UnsupportedOperationException.class, history::clear);
        assertThrows(UnsupportedOperationException.class, () -> history.remove(active));
        assertThrows(UnsupportedOperationException.class, () -> history.set(0, active));
        assertEquals(List.of(active), loans.getLoanList());
        assertEquals(1, loans.getLoanCount());
        assertEquals("002", loans.getNextLoanId());
        assertFalse(active.isReturned());
        assertEquals(active.getLoanId(), camera.getLoanId());
        assertEquals(equipmentBefore, Files.readString(equipmentFile));
        assertEquals(loansBefore, Files.readString(loanFile));
        assertEquals(sequenceBefore, Files.readString(sequenceFile));
        assertEquals(marker, Files.getLastModifiedTime(equipmentFile));
        assertEquals(marker, Files.getLastModifiedTime(loanFile));
        assertEquals(marker, Files.getLastModifiedTime(sequenceFile));
    }

    @Test
    void history_display_includesBorrowerDatesAndReturnedStatusForEachLoan() throws Exception {
        Loan returned = addLoan("001", "111", "01-10-26", "02-10-26");
        returnLoan(returned);
        addLoan("001", "222", "03-10-26", "04-10-26");

        String printed = captureHistory("history id/001");

        assertTrue(printed.contains("BORROWER ID"));
        assertTrue(printed.contains("ISSUED"));
        assertTrue(printed.contains("DUE"));
        assertTrue(printed.contains("STATE"));
        assertTrue(printed.lines().anyMatch(line -> line.contains("111") && line.contains("Returned")
                && line.contains("01-10-26") && line.contains("02-10-26")));
        assertTrue(printed.lines().anyMatch(line -> line.contains("222") && line.contains("Overdue")
                && line.contains("03-10-26") && line.contains("04-10-26")));
    }

    /** Supplies malformed history commands and unsupported item ID formats. */
    private static Stream<String> invalidHistoryCommands() {
        return Stream.of(null, "", "history", "history id", "history id/", "history id/000", "history id/01",
                "history id/1000", "history id/abc", "history id/-01", "history item/001", "history loan/001",
                "history /id001", "history id/001 extra", "history id/001 id/002", "history id/001 b/002",
                "History id/001", "history ID/001");
    }

    @ParameterizedTest
    @MethodSource("invalidHistoryCommands")
    void history_invalidCommand_preservesRecordsAndAllowsNextQuery(String command) throws Exception {
        Loan active = addLoan("001", "111", "01-10-26", "02-10-26");
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanHistoryUnsuccessful.class, () -> loans.getLoanHistory(command));

        assertEquals(List.of(active), loans.getLoanHistory("history id/001"));
        assertFalse(active.isReturned());
        assertTrue(camera.isLoaned());
        assertEquals(active.getLoanId(), camera.getLoanId());
        assertEquals("002", loans.getNextLoanId());
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
    }

    @Test
    void history_unknownEquipment_differsFromExistingEquipmentWithNoLoans() throws Exception {
        LoanHistoryUnsuccessful error = assertThrows(LoanHistoryUnsuccessful.class,
                () -> loans.getLoanHistory("history id/999"));

        assertTrue(error.getMessage().contains("999"));
        assertTrue(loans.getLoanHistory("history id/001").isEmpty());
        assertFalse(Files.exists(loanFile));
        assertEquals("001", loans.getNextLoanId());
    }

    @Test
    void history_legacyActiveLoan_bindsToCurrentEquipmentButReturnedLegacyStaysUnlinked() throws Exception {
        Files.writeString(loanFile, "loan loanId/001 id/001 b/111 issued/01-10-26 due/02-10-26 returned/true\n"
                + "loan loanId/002 id/001 b/222 issued/03-10-26 due/04-10-26 returned/false\n");
        Files.writeString(sequenceFile, "nextLoanId/003\n");

        loans.loadFromStorage();

        Loan oldReturned = loans.getLoanList().getFirst();
        Loan active = loans.getLoanList().getLast();
        assertNull(oldReturned.getEquipmentRecordId());
        assertEquals(camera.getRecordId(), active.getEquipmentRecordId());
        assertEquals(List.of(active), loans.getLoanHistory("history id/001"));
        assertEquals("002", camera.getLoanId());
        assertEquals(2, loans.getLoanCount());
        assertTrue(Files.readString(loanFile).contains("equipment/" + camera.getRecordId()));

        returnLoan(active);
        loans.loadFromStorage();
        assertNull(loans.getLoanList().getFirst().getEquipmentRecordId());
        assertEquals(List.of("002"), loans.getLoanHistory("history id/001")
                .stream().map(Loan::getLoanId).toList());
        assertTrue(loans.getLoanList().getLast().isReturned());
    }

    @Test
    void history_legacyReturnedLoanOnly_remainsInGlobalListButIsExcludedFromItemHistory() throws Exception {
        Files.writeString(loanFile, "loan id/001 b/111 issued/01-10-26 due/02-10-26 returned/true\n");

        loans.loadFromStorage();

        assertEquals(1, loans.getLoanCount());
        assertTrue(loans.getLoanList().getFirst().isReturned());
        assertNull(loans.getLoanList().getFirst().getEquipmentRecordId());
        assertTrue(loans.getLoanHistory("history id/001").isEmpty());
        assertFalse(camera.isLoaned());
        assertEquals("002", loans.getNextLoanId());
    }

    @Test
    void load_explicitWrongEquipmentIdentity_preservesExistingAssociationsAndAllowsRecovery() throws Exception {
        Loan original = addLoan("001", "111", "01-10-26", "02-10-26");
        String savedLoans = Files.readString(loanFile);
        String mismatched = savedLoans.replace("equipment/" + camera.getRecordId(),
                "equipment/" + UUID.randomUUID());
        assertNotEquals(savedLoans, mismatched);
        Files.writeString(loanFile, mismatched);
        String savedEquipment = Files.readString(equipmentFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(IOException.class, loans::loadFromStorage);

        assertEquals(List.of(original), loans.getLoanList());
        assertEquals(List.of(original), loans.getLoanHistory("history id/001"));
        assertSame(original, loans.getLoanList().getFirst());
        assertFalse(original.isReturned());
        assertEquals(original.getLoanId(), camera.getLoanId());
        assertTrue(camera.isLoaned());
        assertEquals("002", loans.getNextLoanId());
        assertEquals(mismatched, Files.readString(loanFile));
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));

        Files.writeString(loanFile, savedLoans);
        loans.loadFromStorage();
        assertEquals(camera.getRecordId(), loans.getLoanHistory("history id/001").getFirst()
                .getEquipmentRecordId());
    }

    @Test
    void load_unreadableLegacySequence_preservesLiveStateAndAllowsRetry() throws Exception {
        Loan original = addLoan("001", "111", "01-10-26", "02-10-26");
        String legacy = "loan loanId/007 id/002 b/222 issued/03-10-26 due/04-10-26 returned/false\n";
        Files.writeString(loanFile, legacy);
        String savedEquipment = Files.readString(equipmentFile);
        String savedSequence = Files.readString(sequenceFile);
        Files.delete(sequenceFile);
        Files.createDirectory(sequenceFile);
        Path sentinel = sequenceFile.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        assertThrows(IOException.class, loans::loadFromStorage);

        assertEquals(List.of(original), loans.getLoanList());
        assertSame(original, loans.getLoanList().getFirst());
        assertFalse(original.isReturned());
        assertEquals(original.getLoanId(), camera.getLoanId());
        assertTrue(camera.isLoaned());
        assertFalse(equipment.getEquipmentById("002").isLoaned());
        assertEquals("002", loans.getNextLoanId());
        assertEquals(legacy, Files.readString(loanFile));
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals("keep", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(sequenceFile);
        Files.writeString(sequenceFile, savedSequence);

        loans.loadFromStorage();

        assertFalse(camera.isLoaned());
        assertEquals("007", equipment.getEquipmentById("002").getLoanId());
        assertEquals("008", loans.getNextLoanId());
        assertEquals(equipment.getEquipmentById("002").getRecordId(),
                loans.getLoanHistory("history id/002").getFirst().getEquipmentRecordId());
        assertTrue(Files.readString(loanFile).contains("equipment/"
                + equipment.getEquipmentById("002").getRecordId()));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void load_failedLegacyIdentitySave_restoresFilesAndAllowsRetry() throws Exception {
        Loan original = addLoan("001", "111", "01-10-26", "02-10-26");
        String legacy = "loan loanId/007 id/002 b/222 issued/03-10-26 due/04-10-26 returned/false\n";
        Files.writeString(loanFile, legacy);
        String savedEquipment = Files.readString(equipmentFile);
        String savedSequence = Files.readString(sequenceFile);
        Files.setAttribute(sequenceFile, "dos:readonly", true);
        try {
            // A readable sequence reaches migration saving before its read-only destination rejects replacement.
            assertEquals(savedSequence, Files.readString(sequenceFile));
            assertThrows(IOException.class, loans::loadFromStorage);

            assertEquals(List.of(original), loans.getLoanList());
            assertSame(original, loans.getLoanList().getFirst());
            assertFalse(original.isReturned());
            assertEquals(original.getLoanId(), camera.getLoanId());
            assertTrue(camera.isLoaned());
            assertFalse(equipment.getEquipmentById("002").isLoaned());
            assertEquals("002", loans.getNextLoanId());
            assertEquals(legacy, Files.readString(loanFile));
            assertEquals(savedEquipment, Files.readString(equipmentFile));
            assertEquals(savedSequence, Files.readString(sequenceFile));
        } finally {
            Files.setAttribute(sequenceFile, "dos:readonly", false);
        }

        loans.loadFromStorage();

        assertFalse(camera.isLoaned());
        assertEquals("007", equipment.getEquipmentById("002").getLoanId());
        assertEquals("008", loans.getNextLoanId());
        assertEquals(equipment.getEquipmentById("002").getRecordId(),
                loans.getLoanHistory("history id/002").getFirst().getEquipmentRecordId());
        assertTrue(Files.readString(loanFile).contains("equipment/"
                + equipment.getEquipmentById("002").getRecordId()));
        assertEquals("nextLoanId/008", Files.readString(sequenceFile).trim());
    }

    /** Creates loans with distinct borrower IDs so history selection is independent of table layout. */
    private Loan addLoan(String itemId, String borrowerId, String issued, String due) throws Exception {
        return loans.addLoan("loan id/" + itemId + " b/" + borrowerId + " issued/" + issued + " due/" + due);
    }

    /** Completes a loan without removing the record needed by history queries. */
    private void returnLoan(Loan loan) throws Exception {
        loans.returnLoan("return loan/" + loan.getLoanId() + " serviceable/yes damage/NONE remarks/");
    }

    /** Captures history output and always restores the shared output stream. */
    private String captureHistory(String command) throws Exception {
        PrintStream previous = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            loans.viewLoanHistory(command);
        } finally {
            System.setOut(previous);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
