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
        manager.executeInput("add i/Camera id/001 type/camera cond/4 sensor/Full frame");
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void historyCommand_activeAndReturnedLoans_displaysBorrowersDatesAndStatuses() throws Exception {
        manager.executeInput("loan id/001 b/111 issued/03-10-26 due/04-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        manager.executeInput("loan id/001 b/222 issued/01-10-26 due/02-10-26");
        output.reset();

        manager.executeInput("history id/001");

        String printed = printed();
        assertTrue(printed.contains("BORROWER ID"));
        assertTrue(printed.contains("STATE"));
        assertTrue(printed.lines().anyMatch(line -> line.contains("111") && line.contains("Returned")
                && line.contains("03-10-26") && line.contains("04-10-26")));
        assertTrue(printed.lines().anyMatch(line -> line.contains("222") && line.contains("Overdue")
                && line.contains("01-10-26") && line.contains("02-10-26")));
        assertTrue(printed.indexOf("222") < printed.indexOf("111"));
        assertEquals(2, Files.readAllLines(temporaryDirectory.resolve("loans.txt")).size());
        assertEquals("nextLoanId/003", Files.readString(temporaryDirectory.resolve("loan-sequence.txt")).trim());
    }

    @Test
    void historyCommand_reusedItemIdAfterRestart_displaysOnlyReplacementHistory() throws Exception {
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        EquipmentHandler before = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        before.loadFromStorage();
        String oldRecordId = before.getEquipmentById("001").getRecordId();
        manager.executeInput("delete item/001");
        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("add i/Camera id/001 type/camera cond/4 sensor/Full frame");
        output.reset();
        restarted.executeInput("history id/001");
        assertFalse(printed().contains("111"));
        restarted.executeInput("loan id/001 b/222 issued/03-10-26 due/04-10-26");
        EqmManager reloaded = new EqmManager(temporaryDirectory);
        output.reset();

        reloaded.executeInput("history id/001");

        assertTrue(printed().contains("222"));
        assertFalse(printed().contains("111"));
        EquipmentHandler after = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        after.loadFromStorage();
        assertNotEquals(oldRecordId, after.getEquipmentById("001").getRecordId());
        LoanHandler loans = new LoanHandler(after, temporaryDirectory.resolve("loans.txt"));
        loans.loadFromStorage();
        assertEquals(2, loans.getLoanCount());
        assertEquals(1, loans.getLoanHistory("history id/001").size());
        assertEquals("222", loans.getLoanHistory("history id/001").getFirst().getBorrowerId());
        assertEquals(oldRecordId, loans.getLoanList().getFirst().getEquipmentRecordId());
    }

    @Test
    void historyCommand_invalidUnknownAndNoHistory_areReportedWithoutChangingRecords() throws Exception {
        String equipmentBefore = Files.readString(temporaryDirectory.resolve("equipment.txt"));

        manager.executeInput("history id/001");
        String emptyMessage = printed();
        assertFalse(emptyMessage.isBlank());
        assertFalse(emptyMessage.contains("Command not recognised"));
        output.reset();
        manager.executeInput("history id/999");
        String unknownMessage = printed();
        assertTrue(unknownMessage.contains("999"));
        assertNotEquals(emptyMessage, unknownMessage);
        for (String command : new String[]{"history", "history id/000", "history id/01", "history loan/001",
            "history id/001 extra"}) {
            output.reset();
            manager.executeInput(command);
            assertFalse(printed().isBlank());
        }
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertFalse(Files.exists(temporaryDirectory.resolve("loans.txt")));
        assertFalse(Files.exists(temporaryDirectory.resolve("loan-sequence.txt")));

        manager.executeInput("loan id/001 b/222 issued/01-10-26 due/02-10-26");
        output.reset();
        manager.executeInput("history id/001");
        assertTrue(printed().contains("222"));
    }

    @Test
    void historyCommand_legacyReturnedRecord_staysGlobalAndCannotBeAssignedToReplacement() throws Exception {
        Files.writeString(temporaryDirectory.resolve("loans.txt"),
                "loan id/001 b/111 issued/01-10-26 due/02-10-26 returned/true\n");
        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();

        restarted.executeInput("history id/001");

        assertFalse(printed().contains("111"));
        output.reset();
        restarted.executeInput("list loans");
        assertTrue(printed().contains("111"));
        restarted.executeInput("loan id/001 b/222 issued/03-10-26 due/04-10-26");
        output.reset();
        restarted.executeInput("history id/001");
        assertTrue(printed().contains("222"));
        assertFalse(printed().contains("111"));
        assertEquals(2, Files.readAllLines(temporaryDirectory.resolve("loans.txt")).size());
    }

    /** Returns the application output without requiring exact table column spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
