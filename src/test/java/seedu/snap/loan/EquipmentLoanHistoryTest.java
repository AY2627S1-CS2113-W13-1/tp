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
        // @@author yarnieee
        camera = equipment.addEquipment("eqm-add name/Camera e/E001 type/camera cond/4 sensor/APS-C");
        equipment.addEquipment("eqm-add name/Lens e/E002 type/lens cond/5 zoom/24-70mm");
        // @@author
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

        // @@author yarnieee
        List<Loan> history = loans.getLoanHistory("eqm-loan-history e/E001");
        // @@author

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
        // @@author yarnieee
        equipment.updateEquipment("eqm-update e/E001 name/Updated Camera cond/5");
        // @@author
        assertEquals(recordId, original.getEquipmentRecordId());
        assertEquals(recordId, camera.getRecordId());

        EquipmentHandler restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile);
        restartedLoans.loadFromStorage();
        // @@author yarnieee
        Loan restored = restartedLoans.getLoanHistory("eqm-loan-history e/E001").getFirst();
        // @@author

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
        // @@author yarnieee
        assertSame(camera, equipment.deleteEquipment("eqm-delete e/E001"));
        // @@author
        Equipment replacement = equipment.addEquipment(
                // @@author yarnieee
                "eqm-add name/Camera e/E001 type/" + replacementType + " cond/4");
        // @@author

        assertNotEquals(oldRecordId, replacement.getRecordId());
        // @@author yarnieee
        assertTrue(loans.getLoanHistory("eqm-loan-history e/E001").isEmpty());
        // @@author
        Loan current = addLoan("001", "222", "03-10-26", "04-10-26");

        // @@author yarnieee
        assertEquals(List.of(current), loans.getLoanHistory("eqm-loan-history e/E001"));
        // @@author
        assertEquals(List.of(history, current), loans.getLoanList());
        assertEquals(oldRecordId, history.getEquipmentRecordId());
        assertEquals(replacement.getRecordId(), current.getEquipmentRecordId());
        // @@author yarnieee
        assertEquals("L002", current.getLoanId());
        // @@author
        assertThrows(LoanReturnUnsuccessful.class,
                // @@author yarnieee
                () -> loans.returnLoan("loan-return l/L001 serviceable/no damage/NONE remarks/Old loan"));
        assertSame(history, loans.deleteLoan("loan-delete l/L001"));
        // @@author
        history.setReturned(true);
        assertThrows(IllegalStateException.class, () -> history.setReturned(false));
        assertTrue(replacement.isLoaned());
        assertEquals(current.getLoanId(), replacement.getLoanId());
        // @@author yarnieee
        assertEquals(List.of(current), loans.getLoanHistory("eqm-loan-history e/E001"));
        // @@author
    }

    @Test
    void history_restartBetweenDeletionAndReplacement_doesNotReconnectOldHistory() throws Exception {
        Loan oldLoan = addLoan("001", "111", "01-10-26", "02-10-26");
        returnLoan(oldLoan);
        String oldRecordId = camera.getRecordId();
        // @@author yarnieee
        equipment.deleteEquipment("eqm-delete e/E001");
        // @@author
        EquipmentHandler restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile);
        restartedLoans.loadFromStorage();
        // @@author yarnieee
        assertThrows(LoanHistoryUnsuccessful.class, () -> restartedLoans.getLoanHistory("eqm-loan-history e/E001"));

        Equipment replacement = restartedEquipment.addEquipment("eqm-add name/Camera e/E001 type/camera cond/4");
        // @@author
        assertNotEquals(oldRecordId, replacement.getRecordId());
        // @@author yarnieee
        assertTrue(restartedLoans.getLoanHistory("eqm-loan-history e/E001").isEmpty());
        Loan current = restartedLoans.addLoan("loan-add e/E001 u/U222 issued/03-10-26 due/04-10-26");
        // @@author
        restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler reloadedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile);
        reloadedLoans.loadFromStorage();

        // @@author yarnieee
        assertEquals(List.of(current.getLoanId()), reloadedLoans.getLoanHistory("eqm-loan-history e/E001")
                // @@author
                .stream().map(Loan::getLoanId).toList());
        assertEquals(List.of(oldRecordId, replacement.getRecordId()), reloadedLoans.getLoanList()
                .stream().map(Loan::getEquipmentRecordId).toList());
        assertEquals(2, reloadedLoans.getLoanCount());
        // @@author yarnieee
        assertEquals("L002", restartedEquipment.getEquipmentById("E001").getLoanId());
        // @@author
    }

    @Test
    void history_existingItemWithoutLoans_returnsAnEmptyReadOnlyList() throws Exception {
        // @@author yarnieee
        List<Loan> history = loans.getLoanHistory("eqm-loan-history e/E001");
        // @@author

        assertTrue(history.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> history.add(null));
        assertEquals(0, loans.getLoanCount());
        // @@author yarnieee
        assertEquals("L001", loans.getNextLoanId());
        // @@author
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

        // @@author yarnieee
        List<Loan> history = loans.getLoanHistory("eqm-loan-history e/E001");
        captureHistory("eqm-loan-history e/E001");
        captureHistory("eqm-loan-history e/E002");
        // @@author

        assertEquals(List.of(active), history);
        assertThrows(UnsupportedOperationException.class, history::clear);
        assertThrows(UnsupportedOperationException.class, () -> history.remove(active));
        assertThrows(UnsupportedOperationException.class, () -> history.set(0, active));
        assertEquals(List.of(active), loans.getLoanList());
        assertEquals(1, loans.getLoanCount());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
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

        // @@author yarnieee
        String printed = captureHistory("eqm-loan-history e/E001");
        // @@author

        assertTrue(printed.contains("BORROWER ID"));
        assertTrue(printed.contains("ISSUED"));
        assertTrue(printed.contains("DUE"));
        assertTrue(printed.contains("STATE"));
        // @@author yarnieee
        assertTrue(printed.lines().anyMatch(line -> line.contains("U111") && line.contains("Returned")
                // @@author
                && line.contains("01-10-26") && line.contains("02-10-26")));
        // @@author yarnieee
        assertTrue(printed.lines().anyMatch(line -> line.contains("U222") && line.contains("Overdue")
                // @@author
                && line.contains("03-10-26") && line.contains("04-10-26")));
    }

    /** Supplies malformed history commands and unsupported item ID formats. */
    private static Stream<String> invalidHistoryCommands() {
        // @@author yarnieee
        return Stream.of(null, "", "eqm-loan-history", "history id", "eqm-loan-history e/",
        "eqm-loan-history e/E000", "eqm-loan-history e/01",
                "eqm-loan-history e/E1000", "eqm-loan-history e/abc", "eqm-loan-history e/-01", "history "
                        + "item/001", "history loan/001",
                "history /id001", "eqm-loan-history e/E001 extra", "eqm-loan-history e/E001 e/E002",
                "eqm-loan-history e/E001 b/U002",
                "History id/E001", "history ID/001");
        // @@author
    }

    @ParameterizedTest
    @MethodSource("invalidHistoryCommands")
    void history_invalidCommand_preservesRecordsAndAllowsNextQuery(String command) throws Exception {
        Loan active = addLoan("001", "111", "01-10-26", "02-10-26");
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanHistoryUnsuccessful.class, () -> loans.getLoanHistory(command));

        // @@author yarnieee
        assertEquals(List.of(active), loans.getLoanHistory("eqm-loan-history e/E001"));
        // @@author
        assertFalse(active.isReturned());
        assertTrue(camera.isLoaned());
        assertEquals(active.getLoanId(), camera.getLoanId());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
    }

    @Test
    void history_unknownEquipment_differsFromExistingEquipmentWithNoLoans() throws Exception {
        LoanHistoryUnsuccessful error = assertThrows(LoanHistoryUnsuccessful.class,
                // @@author yarnieee
                () -> loans.getLoanHistory("eqm-loan-history e/E999"));

        assertTrue(error.getMessage().contains("E999"));
        assertTrue(loans.getLoanHistory("eqm-loan-history e/E001").isEmpty());
        // @@author
        assertFalse(Files.exists(loanFile));
        // @@author yarnieee
        assertEquals("L001", loans.getNextLoanId());
        // @@author
    }

    @Test
    void history_legacyActiveLoan_bindsToCurrentEquipmentButReturnedLegacyStaysUnlinked() throws Exception {
        // @@author yarnieee
        Files.writeString(loanFile, "loan loanId/L001 id/E001 b/U111 issued/01-10-26 due/02-10-26 returned/true\n"
                + "loan loanId/L002 id/E001 b/U222 issued/03-10-26 due/04-10-26 returned/false\n");
        Files.writeString(sequenceFile, "nextLoanId/L003\n");
        // @@author

        loans.loadFromStorage();

        Loan oldReturned = loans.getLoanList().getFirst();
        Loan active = loans.getLoanList().getLast();
        assertNull(oldReturned.getEquipmentRecordId());
        assertEquals(camera.getRecordId(), active.getEquipmentRecordId());
        // @@author yarnieee
        assertEquals(List.of(active), loans.getLoanHistory("eqm-loan-history e/E001"));
        assertEquals("L002", camera.getLoanId());
        // @@author
        assertEquals(2, loans.getLoanCount());
        assertTrue(Files.readString(loanFile).contains("equipment/" + camera.getRecordId()));

        returnLoan(active);
        loans.loadFromStorage();
        assertNull(loans.getLoanList().getFirst().getEquipmentRecordId());
        // @@author yarnieee
        assertEquals(List.of("L002"), loans.getLoanHistory("eqm-loan-history e/E001")
                // @@author
                .stream().map(Loan::getLoanId).toList());
        assertTrue(loans.getLoanList().getLast().isReturned());
    }

    @Test
    void history_legacyReturnedLoanOnly_remainsInGlobalListButIsExcludedFromItemHistory() throws Exception {
        // @@author yarnieee
        Files.writeString(loanFile, "loan id/E001 b/U111 issued/01-10-26 due/02-10-26 returned/true\n");
        // @@author

        loans.loadFromStorage();

        assertEquals(1, loans.getLoanCount());
        assertTrue(loans.getLoanList().getFirst().isReturned());
        assertNull(loans.getLoanList().getFirst().getEquipmentRecordId());
        // @@author yarnieee
        assertTrue(loans.getLoanHistory("eqm-loan-history e/E001").isEmpty());
        // @@author
        assertFalse(camera.isLoaned());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
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
        // @@author yarnieee
        assertEquals(List.of(original), loans.getLoanHistory("eqm-loan-history e/E001"));
        // @@author
        assertSame(original, loans.getLoanList().getFirst());
        assertFalse(original.isReturned());
        assertEquals(original.getLoanId(), camera.getLoanId());
        assertTrue(camera.isLoaned());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals(mismatched, Files.readString(loanFile));
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));

        Files.writeString(loanFile, savedLoans);
        loans.loadFromStorage();
        // @@author yarnieee
        assertEquals(camera.getRecordId(), loans.getLoanHistory("eqm-loan-history e/E001").getFirst()
                // @@author
                .getEquipmentRecordId());
    }

    @Test
    void load_unreadableLegacySequence_preservesLiveStateAndAllowsRetry() throws Exception {
        Loan original = addLoan("001", "111", "01-10-26", "02-10-26");
        // @@author yarnieee
        String legacy = "loan loanId/L007 id/E002 b/U222 issued/03-10-26 due/04-10-26 returned/false\n";
        // @@author
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
        // @@author yarnieee
        assertFalse(equipment.getEquipmentById("E002").isLoaned());
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals(legacy, Files.readString(loanFile));
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals("keep", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(sequenceFile);
        Files.writeString(sequenceFile, savedSequence);

        loans.loadFromStorage();

        assertFalse(camera.isLoaned());
        // @@author yarnieee
        assertEquals("L007", equipment.getEquipmentById("E002").getLoanId());
        assertEquals("L008", loans.getNextLoanId());
        assertEquals(equipment.getEquipmentById("E002").getRecordId(),
                loans.getLoanHistory("eqm-loan-history e/E002").getFirst().getEquipmentRecordId());
        // @@author
        assertTrue(Files.readString(loanFile).contains("equipment/"
                // @@author yarnieee
                + equipment.getEquipmentById("E002").getRecordId()));
        // @@author
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void load_failedLegacyIdentitySave_restoresFilesAndAllowsRetry() throws Exception {
        Loan original = addLoan("001", "111", "01-10-26", "02-10-26");
        // @@author yarnieee
        String legacy = "loan loanId/L007 id/E002 b/U222 issued/03-10-26 due/04-10-26 returned/false\n";
        // @@author
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
            // @@author yarnieee
            assertFalse(equipment.getEquipmentById("E002").isLoaned());
            assertEquals("L002", loans.getNextLoanId());
            // @@author
            assertEquals(legacy, Files.readString(loanFile));
            assertEquals(savedEquipment, Files.readString(equipmentFile));
            assertEquals(savedSequence, Files.readString(sequenceFile));
        } finally {
            Files.setAttribute(sequenceFile, "dos:readonly", false);
        }

        loans.loadFromStorage();

        assertFalse(camera.isLoaned());
        // @@author yarnieee
        assertEquals("L007", equipment.getEquipmentById("E002").getLoanId());
        assertEquals("L008", loans.getNextLoanId());
        assertEquals(equipment.getEquipmentById("E002").getRecordId(),
                loans.getLoanHistory("eqm-loan-history e/E002").getFirst().getEquipmentRecordId());
        // @@author
        assertTrue(Files.readString(loanFile).contains("equipment/"
                // @@author yarnieee
                + equipment.getEquipmentById("E002").getRecordId()));
        assertEquals("nextLoanId/L008", Files.readString(sequenceFile).trim());
        // @@author
    }

    /** Creates loans with distinct borrower IDs so history selection is independent of table layout. */
    private Loan addLoan(String itemId, String borrowerId, String issued, String due) throws Exception {
        // @@author yarnieee
        return loans.addLoan("loan-add e/E" + itemId + " u/U" + borrowerId
                + " issued/" + issued + " due/" + due);
        // @@author
    }

    /** Completes a loan without removing the record needed by history queries. */
    private void returnLoan(Loan loan) throws Exception {
        // @@author yarnieee
        loans.returnLoan("loan-return l/" + loan.getLoanId() + " serviceable/yes damage/NONE remarks/");
        // @@author
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
