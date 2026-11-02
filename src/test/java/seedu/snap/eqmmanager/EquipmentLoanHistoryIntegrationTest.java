package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.loan.LoanHandler;

/** Exercises history commands, equipment replacement and legacy records through the application. */
class EquipmentLoanHistoryIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        manager = new EqmManager(temporaryDirectory);
        // @@author yarnieee
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/4 sensor/Full frame");
        // @@author
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void historyCommand_activeAndReturnedLoans_displaysBorrowersDatesAndStatuses() throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U111 issued/03-10-26 due/04-10-26");
        manager.executeInput("loan-return l/L001 serviceable/yes damage/NONE remarks/");
        manager.executeInput("loan-add e/E001 u/U222 issued/01-10-26 due/02-10-26");
        // @@author
        output.reset();

        // @@author yarnieee
        manager.executeInput("eqm-loan-history e/E001");
        // @@author

        String printed = printed();
        assertTrue(printed.contains("BORROWER ID"));
        assertTrue(printed.contains("STATE"));
        // @@author yarnieee
        assertTrue(printed.lines().anyMatch(line -> line.contains("U111") && line.contains("Returned")
                // @@author
                && line.contains("03-10-26") && line.contains("04-10-26")));
        // @@author yarnieee
        assertTrue(printed.lines().anyMatch(line -> line.contains("U222") && line.contains("Overdue")
                // @@author
                && line.contains("01-10-26") && line.contains("02-10-26")));
        // @@author yarnieee
        assertTrue(printed.indexOf("U222") < printed.indexOf("U111"));
        // @@author
        assertEquals(2, Files.readAllLines(temporaryDirectory.resolve("loans.txt")).size());
        // @@author yarnieee
        assertEquals("nextLoanId/L003", Files.readString(temporaryDirectory.resolve("loan-sequence.txt")).trim());
        // @@author
    }

    @Test
    void historyCommand_reusedItemIdAfterRestart_displaysOnlyReplacementHistory() throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        manager.executeInput("loan-return l/L001 serviceable/yes damage/NONE remarks/");
        // @@author
        EquipmentHandler before = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        before.loadFromStorage();
        // @@author yarnieee
        String oldRecordId = before.getEquipmentById("E001").getRecordId();
        manager.executeInput("eqm-delete e/E001");
        // @@author
        EqmManager restarted = new EqmManager(temporaryDirectory);
        // @@author yarnieee
        restarted.executeInput("eqm-add name/Camera e/E001 type/camera cond/4 sensor/Full frame");
        // @@author
        output.reset();
        // @@author yarnieee
        restarted.executeInput("eqm-loan-history e/E001");
        assertFalse(printed().contains("U111"));
        restarted.executeInput("loan-add e/E001 u/U222 issued/03-10-26 due/04-10-26");
        // @@author
        EqmManager reloaded = new EqmManager(temporaryDirectory);
        output.reset();

        // @@author yarnieee
        reloaded.executeInput("eqm-loan-history e/E001");

        assertTrue(printed().contains("U222"));
        assertFalse(printed().contains("U111"));
        // @@author
        EquipmentHandler after = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        after.loadFromStorage();
        // @@author yarnieee
        assertNotEquals(oldRecordId, after.getEquipmentById("E001").getRecordId());
        // @@author
        LoanHandler loans = new LoanHandler(after, temporaryDirectory.resolve("loans.txt"));
        loans.loadFromStorage();
        assertEquals(2, loans.getLoanCount());
        // @@author yarnieee
        assertEquals(1, loans.getLoanHistory("eqm-loan-history e/E001").size());
        assertEquals("U222", loans.getLoanHistory("eqm-loan-history e/E001").getFirst().getBorrowerId());
        // @@author
        assertEquals(oldRecordId, loans.getLoanList().getFirst().getEquipmentRecordId());
    }

    @Test
    void historyCommand_invalidUnknownAndNoHistory_areReportedWithoutChangingRecords() throws Exception {
        String equipmentBefore = Files.readString(temporaryDirectory.resolve("equipment.txt"));

        // @@author yarnieee
        manager.executeInput("eqm-loan-history e/E001");
        // @@author
        String emptyMessage = printed();
        assertFalse(emptyMessage.isBlank());
        assertFalse(emptyMessage.contains("Command not recognised"));
        output.reset();
        // @@author yarnieee
        manager.executeInput("eqm-loan-history e/E999");
        // @@author
        String unknownMessage = printed();
        assertTrue(unknownMessage.contains("999"));
        assertNotEquals(emptyMessage, unknownMessage);
        // @@author yarnieee
        for (String command : new String[]{"eqm-loan-history", "eqm-loan-history e/E000",
            "eqm-loan-history e/01", "history loan/001",
            "eqm-loan-history e/E001 extra"}) {
            // @@author
            output.reset();
            manager.executeInput(command);
            assertFalse(printed().isBlank());
        }
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertFalse(Files.exists(temporaryDirectory.resolve("loans.txt")));
        assertFalse(Files.exists(temporaryDirectory.resolve("loan-sequence.txt")));

        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U222 issued/01-10-26 due/02-10-26");
        // @@author
        output.reset();
        // @@author yarnieee
        manager.executeInput("eqm-loan-history e/E001");
        assertTrue(printed().contains("U222"));
        // @@author
    }

    @Test
    void historyCommand_legacyReturnedRecord_staysGlobalAndCannotBeAssignedToReplacement() throws Exception {
        Files.writeString(temporaryDirectory.resolve("loans.txt"),
                // @@author yarnieee
                "loan id/E001 b/U111 issued/01-10-26 due/02-10-26 returned/true\n");
        // @@author
        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();

        // @@author yarnieee
        restarted.executeInput("eqm-loan-history e/E001");

        assertFalse(printed().contains("U111"));
        // @@author
        output.reset();
        // @@author yarnieee
        restarted.executeInput("loan-list");
        assertTrue(printed().contains("U111"));
        restarted.executeInput("loan-add e/E001 u/U222 issued/03-10-26 due/04-10-26");
        // @@author
        output.reset();
        // @@author yarnieee
        restarted.executeInput("eqm-loan-history e/E001");
        assertTrue(printed().contains("U222"));
        assertFalse(printed().contains("U111"));
        // @@author
        assertEquals(2, Files.readAllLines(temporaryDirectory.resolve("loans.txt")).size());
    }

    /** Returns the application output without requiring exact table column spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
