package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Exercises omitted issue dates and return remarks through the application and its saved files. */
class OptionalFieldsIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-24T16:00:00Z"),
            ZoneId.of("Asia/Singapore"));
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        // @@author yarnieee
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/4 sensor/Full frame remarks/Old remarks");
        // @@author
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void optionalCommands_defaultLocalDateAndEmptyRemarks_areSavedAndRestored() throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U111 due/25-10-26");
        assertTrue(printed().contains("[OK] Loan L001 issued."));
        // @@author
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                .contains("issued/25-10-26 due/25-10-26"));
        output.reset();

        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/yes damage/NONE");

        assertTrue(printed().contains("[OK] Loan L001 returned."));
        // @@author
        Clock later = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        EqmManager restarted = new EqmManager(temporaryDirectory, later);
        output.reset();
        // @@author yarnieee
        restarted.executeInput("loan-list");
        // @@author
        assertTrue(printed().contains("25-10-26"));
        assertTrue(printed().contains("Returned"));
        EquipmentHandler inventory = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        inventory.loadFromStorage();
        // @@author yarnieee
        Equipment camera = inventory.getEquipmentById("E001");
        // @@author
        assertEquals("", camera.getRemarks());
        assertTrue(camera.isServiceable());
        assertFalse(camera.isLoaned());
        LoanHandler loans = new LoanHandler(inventory, temporaryDirectory.resolve("loans.txt"),
                temporaryDirectory.resolve("loan-sequence.txt"), later);
        loans.loadFromStorage();
        Loan restored = loans.getLoanList().getFirst();
        assertEquals(LocalDate.of(2026, 10, 25).atStartOfDay(), restored.getIssuedDate());
        assertEquals(restored.getIssuedDate(), restored.getDueDate());
        assertTrue(restored.isReturned());
    }

    @Test
    void optionalCommands_invalidPresentDateAndMissingAssessment_recoverWithoutConsumingLoanIds() throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U111 issued/ due/25-10-26");
        manager.executeInput("loan-add e/E001 u/U111 due/24-10-26");
        manager.executeInput("loan-add e/E001 u/U111");
        // @@author
        assertFalse(printed().contains("[OK]"));
        assertFalse(Files.exists(temporaryDirectory.resolve("loans.txt")));
        output.reset();
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U111 due/25-10-26");
        assertTrue(printed().contains("[OK] Loan L001 issued."));
        // @@author
        String savedLoans = Files.readString(temporaryDirectory.resolve("loans.txt"));
        String savedEquipment = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        output.reset();

        // @@author yarnieee
        manager.executeInput("loan-return l/L001 damage/NONE");
        manager.executeInput("loan-return l/L001 serviceable/yes");
        manager.executeInput("loan-return l/L001 serviceable/maybe damage/NONE");

        assertFalse(printed().contains("[OK] Loan L001 returned."));
        // @@author
        assertEquals(savedLoans, Files.readString(temporaryDirectory.resolve("loans.txt")));
        assertEquals(savedEquipment, Files.readString(temporaryDirectory.resolve("equipment.txt")));
        output.reset();
        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/no damage/SENSOR_DAMAGE");
        assertTrue(printed().contains("[OK] Loan L001 returned."));
        // @@author
        output.reset();
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U222 due/25-10-26");
        // @@author
        assertFalse(printed().contains("[OK]"));
        assertTrue(printed().contains("unserviceable"));
        // @@author yarnieee
        assertEquals("nextLoanId/L002", Files.readString(temporaryDirectory.resolve("loan-sequence.txt")).trim());
        // @@author
    }

    @Test
    void optionalCommands_errorMessagesShowWhichFieldsMayBeOmitted() {
        // @@author yarnieee
        manager.executeInput("loan-add");
        // @@author

        String loanHelp = printed();
        assertTrue(loanHelp.contains("[issued/"));
        assertTrue(loanHelp.contains("due/"));
        output.reset();
        // @@author yarnieee
        manager.executeInput("loan-return");
        // @@author
        String returnHelp = printed();
        assertTrue(returnHelp.contains("[remarks/"));
        assertTrue(returnHelp.contains("serviceable/"));
        assertTrue(returnHelp.contains("damage/"));
    }

    @Test
    void optionalCommands_failedReturnSave_retainsPreviousRemarksAndAllowsShortFormRetry() throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U111 due/25-10-26");
        // @@author
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        String savedLoans = Files.readString(loanFile);
        String savedEquipment = Files.readString(equipmentFile);
        Files.delete(loanFile);
        Files.createDirectory(loanFile);
        Path sentinel = loanFile.resolve("keep.txt");
        Files.writeString(sentinel, "keep");
        output.reset();

        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/yes damage/NONE");
        // @@author

        assertTrue(printed().contains("Could not save"));
        // @@author yarnieee
        assertFalse(printed().contains("[OK] Loan L001 returned."));
        // @@author
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals("keep", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(loanFile);
        Files.writeString(loanFile, savedLoans);
        output.reset();
        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/yes damage/NONE");
        assertTrue(printed().contains("[OK] Loan L001 returned."));
        // @@author
        EquipmentHandler restarted = new EquipmentHandler(equipmentFile);
        restarted.loadFromStorage();
        // @@author yarnieee
        assertEquals("", restarted.getEquipmentById("E001").getRemarks());
        assertFalse(restarted.getEquipmentById("E001").isLoaned());
        // @@author
        assertTrue(Files.readString(loanFile).contains("returned/true"));
    }

    /** Reads output without depending on the width of displayed table columns. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
