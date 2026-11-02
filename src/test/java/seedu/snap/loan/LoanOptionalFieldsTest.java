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
        camera = (Camera) equipment.addEquipment("add i/Camera id/001 type/camera cond/4"
                + " sensor/APS-C damage/SENSOR_DAMAGE remarks/Original assessment");
        equipment.addEquipment("add i/Lens id/002 type/lens cond/5 zoom/24-70mm");
        loans = new LoanHandler(equipment, loanFile, sequenceFile, FIXED_CLOCK);
    }

    @Test
    void addLoan_missingIssuedDate_savesLocalTodayAtMidnightAndReloadsWithoutRecalculating() throws Exception {
        Loan added = loans.addLoan("loan id/001 b/111 due/25-10-26");

        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), added.getIssuedDate());
        assertEquals(LocalDate.of(2026, 10, 25).atStartOfDay(), added.getDueDate());
        assertEquals("001", added.getLoanId());
        assertEquals("001", camera.getLoanId());
        assertTrue(Files.readString(loanFile).contains("issued/24-10-26 due/25-10-26"));
        Clock laterClock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
        EquipmentHandler restartedEquipment = new EquipmentHandler(equipmentFile);
        restartedEquipment.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restartedEquipment, loanFile, sequenceFile, laterClock);

        restartedLoans.loadFromStorage();

        assertEquals(added.getIssuedDate(), restartedLoans.getLoanList().getFirst().getIssuedDate());
        assertEquals(added.getDueDate(), restartedLoans.getLoanList().getFirst().getDueDate());
        assertEquals("001", restartedEquipment.getEquipmentById("001").getLoanId());
        assertEquals("002", restartedLoans.getNextLoanId());
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

        Loan added = handler.addLoan("loan id/001 b/111 due/26-10-26");

        assertEquals(LocalDate.parse(expected).atStartOfDay(), added.getIssuedDate());
        assertTrue(camera.isLoaned());
    }

    @Test
    void addLoan_defaultDate_readsClockOnceWhenTheDateChangesBetweenReads() throws Exception {
        CountingClock clock = new CountingClock();
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile, clock);

        Loan added = handler.addLoan("loan id/001 b/111 due/25-10-26");

        assertEquals(1, clock.reads);
        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), added.getIssuedDate());
    }

    @Test
    void addLoan_explicitIssuedDate_overridesDefaultWithoutReadingClock() throws Exception {
        CountingClock clock = new CountingClock();
        LoanHandler handler = new LoanHandler(equipment, loanFile, sequenceFile, clock);

        Loan added = handler.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");

        assertEquals(0, clock.reads);
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(), added.getIssuedDate());
        assertTrue(Files.readString(loanFile).contains("issued/01-10-26 due/02-10-26"));
    }

    @Test
    void addLoan_dueDateEqualsDefaultIssuedDate_isAccepted() throws Exception {
        Loan added = loans.addLoan("loan id/001 b/111 due/24-10-26");

        assertEquals(added.getIssuedDate(), added.getDueDate());
        assertTrue(camera.isLoaned());
        assertEquals("002", loans.getNextLoanId());
    }

    /** Covers present invalid dates, omitted required fields and unsupported duplicate or reordered fields. */
    private static Stream<String> invalidLoanCommands() {
        return Stream.of("loan id/002 b/222 due/23-10-26", "loan id/002 b/222 issued/ due/25-10-26",
                "loan id/002 b/222 issued/invalid due/25-10-26",
                "loan id/002 b/222 issued/31-02-26 due/25-10-26",
                "loan id/002 b/222 issued/25-10-26 due/24-10-26",
                "loan id/002 b/222 issued/2026-10-24 due/25-10-26",
                "loan id/002 b/222 issued/24-10-26", "loan id/002 b/222", "loan id/002 due/25-10-26",
                "loan b/222 due/25-10-26", "loan id/002 b/222 due/", "loan id/002 b/222 due/invalid",
                "loan id/002 b/222 due/31-02-26", "loan id/002 b/222 due/25-10-26 issued/24-10-26",
                "loan id/002 issued/24-10-26 b/222 due/25-10-26",
                "loan id/002 b/222 issued/24-10-26 issued/24-10-26 due/25-10-26",
                "loan id/002 b/222 due/25-10-26 due/26-10-26",
                "loan id/002 b/222 id/002 due/25-10-26", "loan id/002 b/222 due/25-10-26 extra");
    }

    @ParameterizedTest
    @MethodSource("invalidLoanCommands")
    void addLoan_invalidOptionalOrRequiredField_preservesRecordsAndAllowsRetry(String command) throws Exception {
        Loan original = loans.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(command));

        assertEquals(List.of(original), loans.getLoanList());
        assertSame(original, loans.getLoanList().getFirst());
        assertEquals("002", loans.getNextLoanId());
        assertEquals(original.getLoanId(), camera.getLoanId());
        assertFalse(equipment.getEquipmentById("002").isLoaned());
        assertNull(equipment.getEquipmentById("002").getLoanId());
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
        Loan next = loans.addLoan("loan id/002 b/222 due/25-10-26");
        assertEquals("002", next.getLoanId());
        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), next.getIssuedDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"loans.txt", "loan-sequence.txt"})
    void addLoan_failedDefaultedSave_preservesCounterAndRetryUsesTheDefault(String blockedName) throws Exception {
        Loan original = loans.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);
        Path blocked = temporaryDirectory.resolve(blockedName);
        String blockedContents = Files.readString(blocked);
        Files.delete(blocked);
        Files.createDirectory(blocked);
        Path sentinel = blocked.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan("loan id/002 b/222 due/25-10-26"));

        assertEquals(List.of(original), loans.getLoanList());
        assertEquals("002", loans.getNextLoanId());
        assertEquals("001", camera.getLoanId());
        assertFalse(equipment.getEquipmentById("002").isLoaned());
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

        Loan retried = loans.addLoan("loan id/002 b/222 due/25-10-26");

        assertEquals("002", retried.getLoanId());
        assertEquals(LocalDate.of(2026, 10, 24).atStartOfDay(), retried.getIssuedDate());
        assertEquals("003", loans.getNextLoanId());
        assertTrue(Files.readString(loanFile).contains("issued/24-10-26 due/25-10-26"));
    }

    @Test
    void load_savedLoanWithoutIssuedDate_isRejectedInsteadOfUsingCurrentDate() throws Exception {
        String invalid = "loan loanId/001 id/001 b/111 due/25-10-26 returned/false\n";
        Files.writeString(loanFile, invalid);

        assertThrows(IOException.class, loans::loadFromStorage);

        assertTrue(loans.getLoanList().isEmpty());
        assertEquals("001", loans.getNextLoanId());
        assertFalse(camera.isLoaned());
        assertEquals(invalid, Files.readString(loanFile));
        assertFalse(Files.exists(sequenceFile));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " remarks/"})
    void returnLoan_missingOrEmptyRemarks_clearsPreviousRemarksAndPersists(String optionalRemarks) throws Exception {
        Loan original = loans.addLoan("loan id/001 b/111 due/25-10-26");

        assertSame(original, loans.returnLoan("return loan/001 serviceable/yes damage/NONE" + optionalRemarks));

        assertTrue(original.isReturned());
        assertTrue(camera.isServiceable());
        assertFalse(camera.isLoaned());
        assertEquals(CameraDamage.NONE, camera.getDamage());
        assertEquals("", camera.getRemarks());
        assertEquals(4, camera.getCondition());
        assertEquals("APS-C", camera.getSensorFormat());
        EquipmentHandler restarted = new EquipmentHandler(equipmentFile);
        restarted.loadFromStorage();
        assertEquals("", restarted.getEquipmentById("001").getRemarks());
        LoanHandler restartedLoans = new LoanHandler(restarted, loanFile, sequenceFile, FIXED_CLOCK);
        restartedLoans.loadFromStorage();
        assertTrue(restartedLoans.getLoanList().getFirst().isReturned());
        assertEquals("002", restartedLoans.getNextLoanId());
    }

    @Test
    void returnLoan_optionalRemarks_preservesExplicitAssessmentAndLiteralFinalText() throws Exception {
        loans.addLoan("loan id/001 b/111 due/25-10-26");
        String remarks = "Keep damage/NONE serviceable/yes label for repair";

        loans.returnLoan("return loan/001 serviceable/no damage/SHUTTER_FAULT remarks/" + remarks);

        assertFalse(camera.isServiceable());
        assertFalse(camera.isLoaned());
        assertEquals(CameraDamage.SHUTTER_FAULT, camera.getDamage());
        assertEquals(remarks, camera.getRemarks());
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan("loan id/001 b/222 due/25-10-26"));
        assertEquals("002", loans.getNextLoanId());
    }

    /** The return assessment remains mandatory even though its final remarks field is optional. */
    private static Stream<String> invalidReturnCommands() {
        return Stream.of("return loan/001", "return loan/001 damage/NONE", "return loan/001 serviceable/yes",
                "return serviceable/yes damage/NONE", "return loan/001 serviceable/ damage/NONE",
                "return loan/001 serviceable/maybe damage/NONE", "return loan/001 serviceable/yes damage/",
                "return loan/001 serviceable/yes damage/UNKNOWN", "return loan/001 damage/NONE serviceable/yes",
                "return loan/001 serviceable/yes damage/NONE cond/11",
                "return loan/001 serviceable/yes serviceable/no damage/NONE",
                "return loan/001 serviceable/yes damage/NONE damage/NONE",
                "return loan/001 serviceable/yes remarks/Before damage/NONE");
    }

    @ParameterizedTest
    @MethodSource("invalidReturnCommands")
    void returnLoan_missingOrInvalidAssessment_preservesAllRecordsAndAllowsRetry(String command) throws Exception {
        Loan original = loans.addLoan("loan id/001 b/111 due/25-10-26");
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);

        assertThrows(LoanReturnUnsuccessful.class, () -> loans.returnLoan(command));

        assertSame(original, loans.getLoanList().getFirst());
        assertFalse(original.isReturned());
        assertTrue(camera.isLoaned());
        assertTrue(camera.isServiceable());
        assertEquals("001", camera.getLoanId());
        assertEquals(CameraDamage.SENSOR_DAMAGE, camera.getDamage());
        assertEquals("Original assessment", camera.getRemarks());
        assertEquals("002", loans.getNextLoanId());
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
        loans.returnLoan("return loan/001 serviceable/no damage/SHUTTER_FAULT");
        assertTrue(original.isReturned());
        assertFalse(camera.isServiceable());
        assertEquals("", camera.getRemarks());
    }

    @ParameterizedTest
    @ValueSource(strings = {"equipment.txt", "loans.txt", "loan-sequence.txt"})
    void returnLoan_failedSaveWithOmittedRemarks_preservesOldRemarksUntilRetry(String blockedName) throws Exception {
        Loan original = loans.addLoan("loan id/001 b/111 due/25-10-26");
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
                () -> loans.returnLoan("return loan/001 serviceable/no damage/SHUTTER_FAULT"));

        assertFalse(original.isReturned());
        assertTrue(camera.isLoaned());
        assertTrue(camera.isServiceable());
        assertEquals(CameraDamage.SENSOR_DAMAGE, camera.getDamage());
        assertEquals("Original assessment", camera.getRemarks());
        assertEquals("002", loans.getNextLoanId());
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

        assertSame(original, loans.returnLoan("return loan/001 serviceable/no damage/SHUTTER_FAULT"));

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
