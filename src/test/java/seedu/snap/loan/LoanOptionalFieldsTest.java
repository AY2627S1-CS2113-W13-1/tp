package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Camera.CameraDamage;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanReturnUnsuccessful;

/** Verifies optional loan dates and return remarks without weakening validation or save recovery. */
class LoanOptionalFieldsTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
    private EquipmentHandler equipment;
    private Camera camera;
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
        camera = (Camera) equipment.addEquipment("eqm-add name/Camera e/E001 type/camera cond/4"
                // @@author
                + " sensor/APS-C damage/SENSOR_DAMAGE remarks/Original assessment");
        // @@author yarnieee
        equipment.addEquipment("eqm-add name/Lens e/E002 type/lens cond/5 zoom/24-70mm");
        // @@author
        loans = new LoanHandler(equipment, loanFile, sequenceFile, FIXED_CLOCK);
    }

    @Test
    void addLoan_missingIssuedDate_savesLocalTodayAtMidnightAndReloadsWithoutRecalculating() throws Exception {
        // @@author yarnieee
        Loan added = loans.addLoan("loan-add e/E001 u/U111 due/25-10-26");
        // @@author

        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), added.getIssuedDate());
        assertEquals(LocalDate.of(2026, 10, 25).atStartOfDay(), added.getDueDate());
        // @@author yarnieee
        assertEquals("L001", added.getLoanId());
        assertEquals("L001", camera.getLoanId());
        // @@author
        assertTrue(Files.readString(loanFile).contains("issued/24-10-26 due/25-10-26"));
        Clock laterClock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
        EquipmentHandler restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile, laterClock);

        restartedLoans.loadFromStorage();

        assertEquals(added.getIssuedDate(), restartedLoans.getLoanList().getFirst().getIssuedDate());
        assertEquals(added.getDueDate(), restartedLoans.getLoanList().getFirst().getDueDate());
        // @@author yarnieee
        assertEquals("L001", restartedEquipment.getEquipmentById("E001").getLoanId());
        assertEquals("L002", restartedLoans.getNextLoanId());
        // @@author
    }

    /** Supplies instants on both sides of local midnight, including a zone behind UTC. */
    private static Stream<Arguments> localDates() {
        return Stream.of(
                Arguments.of("2026-10-24T15:59:59Z", "Asia/Singapore", "2026-10-24"),
                Arguments.of("2026-10-24T16:00:00Z", "Asia/Singapore", "2026-10-25"),
                Arguments.of("2026-10-24T16:00:00Z", "UTC", "2026-10-24"),
                Arguments.of("2026-10-24T02:00:00Z", "America/New_York", "2026-10-23"));
    }

    @ParameterizedTest
    @MethodSource("localDates")
    void addLoan_defaultDate_usesTheClockZoneAndLocalMidnight(String instant, String zone, String expected)
            throws Exception {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of(zone));
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile, clock);

        // @@author yarnieee
        Loan added = handler.addLoan("loan-add e/E001 u/U111 due/26-10-26");
        // @@author

        assertEquals(LocalDate.parse(expected).atStartOfDay(), added.getIssuedDate());
        assertTrue(camera.isLoaned());
    }

    @Test
    void addLoan_defaultDate_readsClockOnceWhenTheDateChangesBetweenReads() throws Exception {
        CountingClock clock = new CountingClock();
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile, clock);

        // @@author yarnieee
        Loan added = handler.addLoan("loan-add e/E001 u/U111 due/25-10-26");
        // @@author

        assertEquals(1, clock.reads);
        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), added.getIssuedDate());
    }

    @Test
    void addLoan_explicitIssuedDate_overridesDefaultWithoutReadingClock() throws Exception {
        CountingClock clock = new CountingClock();
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile, clock);

        // @@author yarnieee
        Loan added = handler.addLoan("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        // @@author

        assertEquals(0, clock.reads);
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(), added.getIssuedDate());
        assertTrue(Files.readString(loanFile).contains("issued/01-10-26 due/02-10-26"));
    }

    @Test
    void addLoan_dueDateEqualsDefaultIssuedDate_isAccepted() throws Exception {
        // @@author yarnieee
        Loan added = loans.addLoan("loan-add e/E001 u/U111 due/24-10-26");
        // @@author

        assertEquals(added.getIssuedDate(), added.getDueDate());
        assertTrue(camera.isLoaned());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
    }

    /** Covers present invalid dates, omitted required fields and unsupported duplicate or reordered fields. */
    private static Stream<String> invalidLoanCommands() {
        // @@author yarnieee
        return Stream.of("loan-add e/E002 u/U222 due/23-10-26", "loan-add e/E002 u/U222 issued/ due/25-10-26",
                "loan-add e/E002 u/U222 issued/invalid due/25-10-26",
                "loan-add e/E002 u/U222 issued/31-02-26 due/25-10-26",
                "loan-add e/E002 u/U222 issued/25-10-26 due/24-10-26",
                "loan-add e/E002 u/U222 issued/2026-10-24 due/25-10-26",
                "loan-add e/E002 u/U222 issued/24-10-26", "loan-add e/E002 u/U222", "loan-add e/E002 due/25-10-26",
                "loan u/U222 due/25-10-26", "loan-add e/E002 u/U222 due/", "loan-add e/E002 u/U222 due/invalid",
                "loan-add e/E002 u/U222 due/31-02-26", "loan-add e/E002 u/U222 due/25-10-26 issued/24-10-26",
                "loan-add e/E002 issued/24-10-26 u/U222 due/25-10-26",
                "loan-add e/E002 u/U222 issued/24-10-26 issued/24-10-26 due/25-10-26",
                "loan-add e/E002 u/U222 due/25-10-26 due/26-10-26",
                "loan-add e/E002 u/U222 e/E002 due/25-10-26", "loan-add e/E002 u/U222 due/25-10-26 extra");
        // @@author
    }

    @ParameterizedTest
    @MethodSource("invalidLoanCommands")
    void addLoan_invalidOptionalOrRequiredField_preservesRecordsAndAllowsRetry(String command) throws Exception {
        // @@author yarnieee
        Loan original = loans.addLoan("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        // @@author
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(command));

        assertEquals(List.of(original), loans.getLoanList());
        assertSame(original, loans.getLoanList().getFirst());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals(original.getLoanId(), camera.getLoanId());
        // @@author yarnieee
        assertFalse(equipment.getEquipmentById("E002").isLoaned());
        assertNull(equipment.getEquipmentById("E002").getLoanId());
        // @@author
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
        // @@author yarnieee
        Loan next = loans.addLoan("loan-add e/E002 u/U222 due/25-10-26");
        assertEquals("L002", next.getLoanId());
        // @@author
        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), next.getIssuedDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"loans.txt", "loan-sequence.txt"})
    void addLoan_failedDefaultedSave_preservesCounterAndRetryUsesTheDefault(String blockedName) throws Exception {
        // @@author yarnieee
        Loan original = loans.addLoan("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        // @@author
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);
        Path blocked = temporaryDirectory.resolve(blockedName);
        String blockedContents = Files.readString(blocked);
        Files.delete(blocked);
        Files.createDirectory(blocked);
        Path sentinel = blocked.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        // @@author yarnieee
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan("loan-add e/E002 u/U222 due/25-10-26"));
        // @@author

        assertEquals(List.of(original), loans.getLoanList());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        assertEquals("L001", camera.getLoanId());
        assertFalse(equipment.getEquipmentById("E002").isLoaned());
        // @@author
        assertEquals("keep", Files.readString(sentinel));
        if (!blocked.equals(loanFile)) {
            assertEquals(savedLoans, Files.readString(loanFile));
        }
        if (!blocked.equals(sequenceFile)) {
            assertEquals(savedSequence, Files.readString(sequenceFile));
        }
        Files.delete(sentinel);
        Files.delete(blocked);
        Files.writeString(blocked, blockedContents);

        // @@author yarnieee
        Loan retried = loans.addLoan("loan-add e/E002 u/U222 due/25-10-26");

        assertEquals("L002", retried.getLoanId());
        // @@author
        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), retried.getIssuedDate());
        // @@author yarnieee
        assertEquals("L003", loans.getNextLoanId());
        // @@author
        assertTrue(Files.readString(loanFile).contains("issued/24-10-26 due/25-10-26"));
    }

    @Test
    void load_savedLoanWithoutIssuedDate_isRejectedInsteadOfUsingCurrentDate() throws Exception {
        // @@author yarnieee
        String invalid = "loan loanId/L001 id/E001 b/U111 due/25-10-26 returned/false\n";
        // @@author
        Files.writeString(loanFile, invalid);

        assertThrows(IOException.class, loans::loadFromStorage);

        assertTrue(loans.getLoanList().isEmpty());
        // @@author yarnieee
        assertEquals("L001", loans.getNextLoanId());
        // @@author
        assertFalse(camera.isLoaned());
        assertEquals(invalid, Files.readString(loanFile));
        assertFalse(Files.exists(sequenceFile));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " remarks/"})
    void returnLoan_missingOrEmptyRemarks_clearsPreviousRemarksAndPersists(String optionalRemarks) throws Exception {
        // @@author yarnieee
        Loan original = loans.addLoan("loan-add e/E001 u/U111 due/25-10-26");

        assertSame(original, loans.returnLoan("loan-return l/L001 serviceable/yes damage/NONE" + optionalRemarks));
        // @@author

        assertTrue(original.isReturned());
        assertTrue(camera.isServiceable());
        assertFalse(camera.isLoaned());
        assertEquals(CameraDamage.NONE, camera.getDamage());
        assertEquals("", camera.getRemarks());
        assertEquals(4, camera.getCondition());
        assertEquals("APS-C", camera.getSensorFormat());
        EquipmentHandler restarted = new EquipmentHandler(equipmentFile);
        restarted.loadFromStorage();
        // @@author yarnieee
        assertEquals("", restarted.getEquipmentById("E001").getRemarks());
        // @@author
        LoanHandler restartedLoans = new LoanHandler(restarted, loanFile, sequenceFile, FIXED_CLOCK);
        restartedLoans.loadFromStorage();
        assertTrue(restartedLoans.getLoanList().getFirst().isReturned());
        // @@author yarnieee
        assertEquals("L002", restartedLoans.getNextLoanId());
        // @@author
    }

    @Test
    void returnLoan_optionalRemarks_preservesExplicitAssessmentAndLiteralFinalText() throws Exception {
        // @@author yarnieee
        loans.addLoan("loan-add e/E001 u/U111 due/25-10-26");
        // @@author
        String remarks = "Keep damage/NONE serviceable/yes label for repair";

        // @@author yarnieee
        loans.returnLoan("loan-return l/L001 serviceable/no damage/SHUTTER_FAULT remarks/" + remarks);
        // @@author

        assertFalse(camera.isServiceable());
        assertFalse(camera.isLoaned());
        assertEquals(CameraDamage.SHUTTER_FAULT, camera.getDamage());
        assertEquals(remarks, camera.getRemarks());
        // @@author yarnieee
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan("loan-add e/E001 u/U222 due/25-10-26"));
        assertEquals("L002", loans.getNextLoanId());
        // @@author
    }

    /** The return assessment remains mandatory even though its final remarks field is optional. */
    private static Stream<String> invalidReturnCommands() {
        // @@author yarnieee
        return Stream.of("loan-return l/L001", "loan-return l/L001 damage/NONE", "loan-return l/L001 serviceable/yes",
                "return serviceable/yes damage/NONE", "loan-return l/L001 serviceable/ damage/NONE",
                "loan-return l/L001 serviceable/maybe damage/NONE", "loan-return l/L001 serviceable/yes damage/",
                "loan-return l/L001 serviceable/yes damage/UNKNOWN", "loan-return l/L001 damage/NONE serviceable/yes",
                "loan-return l/L001 serviceable/yes damage/NONE cond/11",
                "loan-return l/L001 serviceable/yes serviceable/no damage/NONE",
                "loan-return l/L001 serviceable/yes damage/NONE damage/NONE",
                "loan-return l/L001 serviceable/yes remarks/Before damage/NONE");
        // @@author
    }

    @ParameterizedTest
    @MethodSource("invalidReturnCommands")
    void returnLoan_missingOrInvalidAssessment_preservesAllRecordsAndAllowsRetry(String command) throws Exception {
        // @@author yarnieee
        Loan original = loans.addLoan("loan-add e/E001 u/U111 due/25-10-26");
        // @@author
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanReturnUnsuccessful.class, () -> loans.returnLoan(command));

        assertSame(original, loans.getLoanList().getFirst());
        assertFalse(original.isReturned());
        assertTrue(camera.isLoaned());
        assertTrue(camera.isServiceable());
        // @@author yarnieee
        assertEquals("L001", camera.getLoanId());
        // @@author
        assertEquals(CameraDamage.SENSOR_DAMAGE, camera.getDamage());
        assertEquals("Original assessment", camera.getRemarks());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
        // @@author yarnieee
        loans.returnLoan("loan-return l/L001 serviceable/no damage/SHUTTER_FAULT");
        // @@author
        assertTrue(original.isReturned());
        assertFalse(camera.isServiceable());
        assertEquals("", camera.getRemarks());
    }

    @ParameterizedTest
    @ValueSource(strings = {"equipment.txt", "loans.txt", "loan-sequence.txt"})
    void returnLoan_failedSaveWithOmittedRemarks_preservesOldRemarksUntilRetry(String blockedName) throws Exception {
        // @@author yarnieee
        Loan original = loans.addLoan("loan-add e/E001 u/U111 due/25-10-26");
        // @@author
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);
        Path blocked = temporaryDirectory.resolve(blockedName);
        String blockedContents = Files.readString(blocked);
        Files.delete(blocked);
        Files.createDirectory(blocked);
        Path sentinel = blocked.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        assertThrows(LoanReturnUnsuccessful.class,
                // @@author yarnieee
                () -> loans.returnLoan("loan-return l/L001 serviceable/no damage/SHUTTER_FAULT"));
        // @@author

        assertFalse(original.isReturned());
        assertTrue(camera.isLoaned());
        assertTrue(camera.isServiceable());
        assertEquals(CameraDamage.SENSOR_DAMAGE, camera.getDamage());
        assertEquals("Original assessment", camera.getRemarks());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
        assertEquals("keep", Files.readString(sentinel));
        if (!blocked.equals(equipmentFile)) {
            assertEquals(savedEquipment, Files.readString(equipmentFile));
        }
        if (!blocked.equals(loanFile)) {
            assertEquals(savedLoans, Files.readString(loanFile));
        }
        if (!blocked.equals(sequenceFile)) {
            assertEquals(savedSequence, Files.readString(sequenceFile));
        }
        Files.delete(sentinel);
        Files.delete(blocked);
        Files.writeString(blocked, blockedContents);

        // @@author yarnieee
        assertSame(original, loans.returnLoan("loan-return l/L001 serviceable/no damage/SHUTTER_FAULT"));
        // @@author

        assertTrue(original.isReturned());
        assertFalse(camera.isLoaned());
        assertFalse(camera.isServiceable());
        assertEquals("", camera.getRemarks());
        assertEquals(CameraDamage.SHUTTER_FAULT, camera.getDamage());
        assertEquals(savedSequence, Files.readString(sequenceFile));
    }

    /** Advances across midnight after its first read so duplicate default-date reads are visible. */
    private static final class CountingClock extends Clock {
        private int reads;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(Instant.parse("2026-10-24T23:59:59Z"), zone);
        }

        @Override
        public Instant instant() {
            return Instant.parse("2026-10-24T23:59:59Z").plusSeconds(reads++ * 2L);
        }
    }
}
