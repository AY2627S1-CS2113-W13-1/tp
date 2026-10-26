package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** Exercises the summary command through normal application input and restarts. */
class EquipmentSummaryIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void summaryCommand_completeItem_includesCurrentAndHistoricalInformation() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5 sensor/Full frame remarks/Studio camera");
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/Returned safely");
        manager.executeInput("loan id/001 b/222 issued/03-10-26 due/04-10-26");
        output.reset();

        manager.executeInput("summary id/001");

        String summary = printed();
        assertTrue(summary.contains("Equipment summary for item ID 001:"));
        assertTrue(summary.contains("Name: Camera"));
        assertTrue(summary.contains("Type: camera"));
        assertTrue(summary.contains("Condition: 5"));
        assertTrue(summary.contains("Status: Loaned"));
        assertTrue(summary.contains("Remarks: Returned safely"));
        assertTrue(summary.contains("001 | 111 | 2026-10-01 | 2026-10-02 | true"));
        assertTrue(summary.contains("002 | 222 | 2026-10-03 | 2026-10-04 | false"));
        assertTrue(summary.contains("No status history recorded."));
        assertTrue(summary.contains("No maintenance history recorded."));
    }

    @Test
    void summaryCommand_noHistoryAndRestart_preservesSummaryAndFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Lens id/001 type/lens cond/4 zoom/24 to 70 mm");
        manager.executeInput("bye");
        String equipmentBefore = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        String loansBefore = Files.readString(temporaryDirectory.resolve("loans.txt"));
        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();

        restarted.executeInput("summary id/001");

        String summary = printed();
        assertTrue(summary.contains("Status: Available"));
        assertTrue(summary.contains("Subtype details: zoom/24 to 70 mm damage/NONE"));
        assertTrue(summary.contains("No loan history recorded."));
        assertTrue(summary.contains("No maintenance history recorded."));
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertEquals(loansBefore, Files.readString(temporaryDirectory.resolve("loans.txt")));
    }

    @Test
    void summaryCommand_invalidAndUnknownIds_areRejectedWithoutChangingFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        String equipmentBefore = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        output.reset();

        manager.executeInput("summary id/999");
        String unknownMessage = printed();
        output.reset();
        manager.executeInput("summary id/01");
        String invalidMessage = printed();

        assertTrue(unknownMessage.contains("Item ID 999 does not exist."));
        assertTrue(invalidMessage.contains("Item ID must be from 001 to 999."));
        assertFalse(unknownMessage.contains("Equipment summary for"));
        assertFalse(invalidMessage.contains("Equipment summary for"));
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("equipment.txt")));
    }

    /** Returns captured application output without depending on spacing in other sections. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
