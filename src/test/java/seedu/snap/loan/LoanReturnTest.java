package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.bundle.Bundle;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Camera.CameraDamage;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentFilter;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanReturnUnsuccessful;

/** Verifies return assessments, retained loan records and recovery from failed saves. */
class LoanReturnTest {
    // @@author yarnieee
    private static final String LOAN_COMMAND = "loan-add e/E001 u/U002 issued/01-10-26 due/02-10-26";
    // @@author
    private static final String RETURN_COMMAND =
            // @@author yarnieee
            "loan-return l/L001 serviceable/yes damage/NONE remarks/Checked at counter";
    // @@author
    private EquipmentHandler equipment;
    private Camera camera;
    private LoanHandler loans;
    private Loan original;
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
        camera = (Camera) equipment.addEquipment("eqm-add name/Camera e/E001 type/camera cond/4"
                // @@author
                + " sensor/APS-C damage/SENSOR_DAMAGE remarks/Original assessment");
        loans = new LoanHandler(equipment, loanFile, sequenceFile);
        original = loans.addLoan(LOAN_COMMAND);
    }

    @Test
    void returnLoan_serviceableItem_keepsIdentityHistoryAndSubtypeDetails() throws Exception {
        // @@author yarnieee
        Bundle bundle = new Bundle("Camera kit", "B001");
        // @@author
        bundle.addEquipment(camera);
        String savedSequence = Files.readString(sequenceFile);

        assertSame(original, loans.returnLoan(RETURN_COMMAND));

        assertSame(camera, equipment.getEquipmentList().getFirst());
        assertSame(camera, bundle.getEquipmentList().getFirst());
        assertTrue(camera.isServiceable());
        assertFalse(camera.isLoaned());
        assertNull(camera.getLoanId());
        assertEquals("Camera", camera.getItemName());
        assertEquals(4, camera.getCondition());
        assertEquals("APS-C", camera.getSensorFormat());
        assertEquals(CameraDamage.NONE, camera.getDamage());
        assertEquals("Checked at counter", camera.getRemarks());
        assertEquals(List.of(original), loans.getLoanList());
        assertEquals(1, loans.getLoanCount());
        assertTrue(original.isReturned());
        // @@author yarnieee
        assertEquals("U002", original.getBorrowerId());
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals(savedSequence, Files.readString(sequenceFile));
        assertTrue(Files.readString(loanFile).contains("returned/true"));
        // @@author yarnieee
        assertSame(camera, EquipmentFilter.parse("eqm-filter e/E001 status/available")
                // @@author
                .apply(equipment.getEquipmentList(), loans).getFirst());
    }

    // @@author yarnieee
    @Test
    void returnLoan_lowercaseLoanId_isAcceptedAndDisplayedCanonically() throws Exception {
        assertSame(original, loans.returnLoan(
                "loan-return l/l001 serviceable/yes damage/NONE remarks/Checked at counter"));

        // @@author
        assertTrue(original.isReturned());
        // @@author yarnieee
        assertEquals("L001", original.getLoanId());
    }

    // @@author
    @Test
    void returnLoan_changedConditionAssessmentAndReloan_snapshotsSurviveRestart()
            throws Exception {
        EquipmentConditionSnapshot firstSnapshot =
                original.getIssueConditionSnapshot();

        assertNotNull(firstSnapshot);
        assertEquals(4, firstSnapshot.getCondition());
        assertEquals("SENSOR_DAMAGE", firstSnapshot.getDamage());
        assertEquals("Original assessment", firstSnapshot.getRemarks());

        loans.returnLoan(
                // @@author yarnieee
                "loan-return l/L001 serviceable/yes "
                        // @@author
                        + "damage/SHUTTER_FAULT "
                        + "remarks/Assessed after return");
        // @@author yarnieee
        equipment.updateEquipment("eqm-update e/E001 cond/8");
        // @@author
        Loan second = loans.addLoan(
                // @@author yarnieee
                "loan-add e/E001 u/U003 issued/03-10-26 due/04-10-26");
        // @@author

        EquipmentConditionSnapshot secondSnapshot =
                second.getIssueConditionSnapshot();
        assertNotNull(secondSnapshot);
        assertEquals(4, firstSnapshot.getCondition());
        assertEquals("SENSOR_DAMAGE", firstSnapshot.getDamage());
        assertEquals("Original assessment", firstSnapshot.getRemarks());
        assertEquals(8, secondSnapshot.getCondition());
        assertEquals("SHUTTER_FAULT", secondSnapshot.getDamage());
        assertEquals("Assessed after return", secondSnapshot.getRemarks());

        EquipmentHandler reloadedEquipment =
                new EquipmentHandler(equipmentFile);
        reloadedEquipment.loadFromStorage();
        LoanHandler reloadedLoans =
                new LoanHandler(
                        reloadedEquipment,
                        loanFile,
                        sequenceFile);
        reloadedLoans.loadFromStorage();

        List<Loan> restoredLoans = reloadedLoans.getLoanList();
        assertEquals(2, restoredLoans.size());
        Loan restoredFirst = restoredLoans.get(0);
        Loan restoredSecond = restoredLoans.get(1);
        EquipmentConditionSnapshot restoredFirstSnapshot =
                restoredFirst.getIssueConditionSnapshot();
        EquipmentConditionSnapshot restoredSecondSnapshot =
                restoredSecond.getIssueConditionSnapshot();

        assertNotNull(restoredFirstSnapshot);
        assertNotNull(restoredSecondSnapshot);
        assertTrue(restoredFirst.isReturned());
        assertFalse(restoredSecond.isReturned());
        // @@author yarnieee
        assertEquals("L001", restoredFirst.getLoanId());
        assertEquals("L002", restoredSecond.getLoanId());
        // @@author
        assertEquals(4, restoredFirstSnapshot.getCondition());
        assertEquals(
                "SENSOR_DAMAGE",
                restoredFirstSnapshot.getDamage());
        assertEquals(
                "Original assessment",
                restoredFirstSnapshot.getRemarks());
        assertEquals(8, restoredSecondSnapshot.getCondition());
        assertEquals(
                "SHUTTER_FAULT",
                restoredSecondSnapshot.getDamage());
        assertEquals(
                "Assessed after return",
                restoredSecondSnapshot.getRemarks());
    }

    @Test
    void returnLoan_unserviceableItem_survivesReloadAndCannotBeLoanedAgain() throws Exception {
        // @@author yarnieee
        loans.returnLoan("loan-return l/L001 serviceable/no damage/SHUTTER_FAULT remarks/Needs repair");
        // @@author

        assertFalse(camera.isLoaned());
        assertFalse(camera.isServiceable());
        // @@author yarnieee
        assertTrue(EquipmentFilter.parse("eqm-filter e/E001 status/available")
                // @@author
                .apply(equipment.getEquipmentList(), loans).isEmpty());
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(LOAN_COMMAND));
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author

        EquipmentHandler reloadedEquipment = new EquipmentHandler(equipmentFile);
        reloadedEquipment.loadFromStorage();
        LoanHandler reloadedLoans = new LoanHandler(reloadedEquipment, loanFile, sequenceFile);
        reloadedLoans.loadFromStorage();
        Camera restored = (Camera) reloadedEquipment.getEquipmentList().getFirst();

        assertFalse(restored.isLoaned());
        assertFalse(restored.isServiceable());
        assertNull(restored.getLoanId());
        assertEquals(CameraDamage.SHUTTER_FAULT, restored.getDamage());
        assertEquals("Needs repair", restored.getRemarks());
        assertEquals("APS-C", restored.getSensorFormat());
        assertEquals(4, restored.getCondition());
        assertTrue(reloadedLoans.getLoanList().getFirst().isReturned());
        assertThrows(LoanAdditionUnsuccessful.class, () -> reloadedLoans.addLoan(LOAN_COMMAND));
        // @@author yarnieee
        assertEquals("L002", reloadedLoans.getNextLoanId());
        // @@author
    }

    @Test
    void returnLoan_assessmentValuesIgnoreCaseAndEmptyRemarksClearOldRemarks() throws Exception {
        // @@author yarnieee
        loans.returnLoan("loan-return l/L001 serviceable/YeS damage/sHuTtEr_FaUlT remarks/");
        // @@author

        assertTrue(camera.isServiceable());
        assertEquals(CameraDamage.SHUTTER_FAULT, camera.getDamage());
        assertEquals("", camera.getRemarks());
        assertTrue(original.isReturned());
    }

    @Test
    void returnLoan_maximumLengthRemarks_areSavedAndReloaded() throws Exception {
        String remarks = "x".repeat(200);

        // @@author yarnieee
        loans.returnLoan("loan-return l/L001 serviceable/yes damage/NONE remarks/" + remarks);
        // @@author

        EquipmentHandler reloaded = new EquipmentHandler(equipmentFile);
        reloaded.loadFromStorage();
        assertEquals(remarks, camera.getRemarks());
        assertEquals(remarks, reloaded.getEquipmentList().getFirst().getRemarks());
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "no"})
    void returnLoan_otherDamageWithRemarks_survivesRestart(String serviceability) throws Exception {
        String remarks = "Damage outside listed categories";
        // @@author yarnieee
        String command = "loan-return l/L001 serviceable/" + serviceability
                // @@author
                + " damage/other remarks/" + remarks;
        boolean expectedServiceable = "yes".equals(serviceability);

        assertSame(original, loans.returnLoan(command));
        assertEquals(CameraDamage.OTHER, camera.getDamage());
        assertEquals(remarks, camera.getRemarks());
        assertEquals(expectedServiceable, camera.isServiceable());
        assertFalse(camera.isLoaned());
        assertTrue(original.isReturned());

        EquipmentHandler restoredEquipment = new EquipmentHandler(equipmentFile);
        restoredEquipment.loadFromStorage();
        LoanHandler restoredLoans = new LoanHandler(restoredEquipment, loanFile, sequenceFile);
        restoredLoans.loadFromStorage();
        Camera restored = (Camera) restoredEquipment.getEquipmentList().getFirst();

        assertEquals(CameraDamage.OTHER, restored.getDamage());
        assertEquals(remarks, restored.getRemarks());
        assertEquals(expectedServiceable, restored.isServiceable());
        assertFalse(restored.isLoaned());
        assertTrue(restoredLoans.getLoanList().getFirst().isReturned());
    }

    @Test
    void returnLoan_fieldPrefixesInsideFinalRemarks_areSavedAsLiteralText() throws Exception {
        String remarks = "Checked serviceable/no damage/SENSOR_DAMAGE label on case";

        // @@author yarnieee
        loans.returnLoan("loan-return l/L001 serviceable/yes damage/NONE remarks/" + remarks);
        // @@author

        EquipmentHandler reloaded = new EquipmentHandler(equipmentFile);
        reloaded.loadFromStorage();
        assertTrue(camera.isServiceable());
        assertEquals(CameraDamage.NONE, camera.getDamage());
        assertEquals(remarks, camera.getRemarks());
        assertEquals(remarks, reloaded.getEquipmentList().getFirst().getRemarks());
    }

    @Test
    void returnLoan_secondReturnCannotChangeAssessmentOrReleaseANewerLoan() throws Exception {
        loans.returnLoan(RETURN_COMMAND);
        Loan next = loans.addLoan(LOAN_COMMAND);
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanReturnUnsuccessful.class,
                // @@author yarnieee
                () -> loans.returnLoan("loan-return l/L001 serviceable/no damage/SHUTTER_FAULT remarks/Rejected"));
        // @@author
        assertThrows(IllegalStateException.class, () -> original.setReturned(false));
        original.setReturned(true);

        assertEquals(List.of(original, next), loans.getLoanList());
        // @@author yarnieee
        assertEquals("L002", next.getLoanId());
        assertEquals("L003", loans.getNextLoanId());
        // @@author
        assertTrue(camera.isLoaned());
        assertEquals(next.getLoanId(), camera.getLoanId());
        assertEquals(CameraDamage.NONE, camera.getDamage());
        assertEquals("Checked at counter", camera.getRemarks());
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
    }

    @Test
    void returnLoan_reloadedReturnedRecord_remainsPermanentAndDetached() throws Exception {
        loans.returnLoan(RETURN_COMMAND);
        EquipmentHandler reloadedEquipment = new EquipmentHandler(equipmentFile);
        reloadedEquipment.loadFromStorage();
        LoanHandler reloadedLoans = new LoanHandler(reloadedEquipment, loanFile, sequenceFile);
        reloadedLoans.loadFromStorage();
        Loan history = reloadedLoans.getLoanList().getFirst();
        Loan current = reloadedLoans.addLoan(LOAN_COMMAND);
        Equipment restored = reloadedEquipment.getEquipmentList().getFirst();

        assertThrows(LoanReturnUnsuccessful.class, () -> reloadedLoans.returnLoan(RETURN_COMMAND));
        assertThrows(IllegalStateException.class, () -> history.setReturned(false));
        history.setReturned(true);

        assertTrue(history.isReturned());
        assertFalse(current.isReturned());
        // @@author yarnieee
        assertEquals("L002", restored.getLoanId());
        // @@author
        assertTrue(restored.isLoaned());
        assertEquals(2, reloadedLoans.getLoanCount());
    }

    /** Covers missing, malformed, misplaced and unsupported return assessment fields. */
    private static Stream<String> invalidReturnCommands() {
        // @@author yarnieee
        return Stream.of(null, "", "loan-return", "loan-return l/L001", "loan-return l/L000 serviceable/yes "
                + "damage/NONE remarks/",
                "loan-return l/01 serviceable/yes damage/NONE remarks/",
                "loan-return l/L1000 serviceable/yes damage/NONE remarks/",
                "loan-return l/L999 serviceable/yes damage/NONE remarks/",
                "loan-return l/L001 damage/NONE remarks/",
                "loan-return l/L001 serviceable/yes remarks/",
                "loan-return l/L001 serviceable/true damage/NONE remarks/",
                "loan-return l/L001 serviceable/ damage/NONE remarks/",
                "loan-return l/L001 serviceable/yes damage/ remarks/",
                "loan-return l/L001 serviceable/yes damage/SCRATCHED_GLASS remarks/",
                "loan-return l/L001 serviceable/yes damage/UNKNOWN remarks/",
                "loan-return l/L001 serviceable/yes damage/OTHER",
                "loan-return l/L001 serviceable/yes damage/OTHER remarks/",
                "loan-return l/L001 serviceable/no damage/other remarks/   ",
                "loan-return l/L001 damage/NONE serviceable/yes remarks/",
                // @@author
                "return serviceable/yes loan/001 damage/NONE remarks/",
                // @@author yarnieee
                "loan-return l/L001 loan/L001 serviceable/yes damage/NONE remarks/",
                "loan-return l/L001 serviceable/yes serviceable/no damage/NONE remarks/",
                "loan-return l/L001 serviceable/yes damage/NONE cond/11 remarks/",
                // @@author
                "Return loan/001 serviceable/yes damage/NONE remarks/",
                "return Loan/001 serviceable/yes damage/NONE remarks/",
                // @@author yarnieee
                "loan-return l/L001 Serviceable/yes damage/NONE remarks/",
                "loan-return l/L001 serviceable/yes Damage/NONE remarks/",
                "loan-return l/L001 serviceable/yes damage/NONE Remarks/",
                "loan-return l/L001 serviceable/yes damage/NONE remarks/" + "x".repeat(201),
                "loan-return l/L001 serviceable/yes damage/NONE remarks/first\nsecond");
        // @@author
    }

    @ParameterizedTest
    @MethodSource("invalidReturnCommands")
    void returnLoan_invalidCommand_preservesAllRecordsAndAllowsRetry(String command) throws Exception {
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanReturnUnsuccessful.class, () -> loans.returnLoan(command));

        assertActiveOriginal();
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
        assertSame(original, loans.returnLoan(RETURN_COMMAND));
        assertTrue(original.isReturned());
    }

    @Test
    void returnLoan_staleEquipmentLoanId_rejectsWithoutChangingOtherState() throws Exception {
        // @@author yarnieee
        camera.setLoanId("L099");
        // @@author
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);

        assertThrows(LoanReturnUnsuccessful.class, () -> loans.returnLoan(RETURN_COMMAND));

        assertFalse(original.isReturned());
        // @@author yarnieee
        assertEquals("L099", camera.getLoanId());
        // @@author
        assertTrue(camera.isLoaned());
        assertEquals(CameraDamage.SENSOR_DAMAGE, camera.getDamage());
        assertEquals("Original assessment", camera.getRemarks());
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        // @@author yarnieee
        camera.setLoanId("L001");
        // @@author
        assertSame(original, loans.returnLoan(RETURN_COMMAND));
    }

    @Test
    void returnLoan_replacedInventoryObject_rejectsUntilLoanAssociationsAreReloaded() throws Exception {
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        equipment.loadFromStorage();
        Equipment replacement = equipment.getEquipmentList().getFirst();

        assertThrows(LoanReturnUnsuccessful.class, () -> loans.returnLoan(RETURN_COMMAND));

        assertFalse(original.isReturned());
        assertTrue(camera.isLoaned());
        // @@author yarnieee
        assertEquals("L001", camera.getLoanId());
        // @@author
        assertFalse(replacement.isLoaned());
        assertNull(replacement.getLoanId());
        assertEquals("Original assessment", replacement.getRemarks());
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));

        loans.loadFromStorage();
        assertFalse(camera.isLoaned());
        assertTrue(replacement.isLoaned());
        Loan current = loans.getLoanList().getFirst();
        assertSame(current, loans.returnLoan(RETURN_COMMAND));
        assertTrue(current.isReturned());
        assertFalse(replacement.isLoaned());
    }

    @ParameterizedTest
    @ValueSource(strings = {"equipment.txt", "loans.txt", "loan-sequence.txt"})
    void returnLoan_failedSave_preservesEveryRecordAndRecoversAfterRetry(String blockedName) throws Exception {
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);
        Path blocked = temporaryDirectory.resolve(blockedName);
        String blockedContents = Files.readString(blocked);
        Files.delete(blocked);
        Files.createDirectory(blocked);
        Path sentinel = blocked.resolve("keep.txt");
        Files.writeString(sentinel, "retain blocked path");

        LoanReturnUnsuccessful error = assertThrows(LoanReturnUnsuccessful.class,
                () -> loans.returnLoan(RETURN_COMMAND));

        assertTrue(error.getMessage().contains("Could not save"));
        assertActiveOriginal();
        assertEquals("retain blocked path", Files.readString(sentinel));
        if (!blocked.equals(equipmentFile)) {
            assertEquals(savedEquipment, Files.readString(equipmentFile));
        }
        if (!blocked.equals(loanFile)) {
            assertEquals(savedLoans, Files.readString(loanFile));
        }
        if (!blocked.equals(sequenceFile)) {
            assertEquals(savedSequence, Files.readString(sequenceFile));
        }
        try (Stream<Path> entries = Files.list(temporaryDirectory)) {
            assertTrue(entries.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }

        Files.delete(sentinel);
        Files.delete(blocked);
        Files.writeString(blocked, blockedContents);
        assertSame(original, loans.returnLoan(RETURN_COMMAND));
        assertTrue(original.isReturned());
        assertFalse(camera.isLoaned());
        assertTrue(camera.isServiceable());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals(savedSequence, Files.readString(sequenceFile));

        EquipmentHandler restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile);
        restartedLoans.loadFromStorage();
        assertTrue(restartedLoans.getLoanList().getFirst().isReturned());
        assertEquals("Checked at counter", restartedEquipment.getEquipmentList().getFirst().getRemarks());
    }

    /** Checks that a rejected return retained the same active record and its original assessment. */
    private void assertActiveOriginal() {
        assertSame(camera, equipment.getEquipmentList().getFirst());
        assertSame(original, loans.getLoanList().getFirst());
        assertEquals(List.of(original), loans.getLoanList());
        assertEquals(1, loans.getLoanCount());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertFalse(original.isReturned());
        assertTrue(camera.isLoaned());
        assertTrue(camera.isServiceable());
        // @@author yarnieee
        assertEquals("L001", camera.getLoanId());
        // @@author
        assertEquals(CameraDamage.SENSOR_DAMAGE, camera.getDamage());
        assertEquals("Original assessment", camera.getRemarks());
        assertEquals("APS-C", camera.getSensorFormat());
        assertEquals(4, camera.getCondition());
    }
}
