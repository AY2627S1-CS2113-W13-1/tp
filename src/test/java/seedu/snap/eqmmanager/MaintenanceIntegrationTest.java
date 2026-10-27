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
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests maintenance commands through the application manager and persistent data directory. */
class MaintenanceIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void redirectOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void maintain_validCommand_reportsSuccessAndPersistsRecord() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        output.reset();

        manager.executeInput("maintain id/001 date/27-10-26 work/Replaced battery outcome/Completed");

        assertEquals("Maintenance recorded successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        Path file = temporaryDirectory.resolve("maintenance.txt");
        assertTrue(Files.isRegularFile(file));
        String saved = Files.readString(file);
        assertTrue(saved.contains("maintenance\tequipment/"));
        assertTrue(saved.contains("\tdate/27-10-26\twork/Replaced battery\toutcome/Completed"));
    }

    @Test
    void maintain_invalidCommand_reportsErrorAndAcceptsNextValidCommand() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        output.reset();

        manager.executeInput("maintain id/001 date/31-02-26 work/Repair outcome/Completed");
        manager.executeInput("maintain id/001 date/27-10-26 work/Repair outcome/Completed");

        assertEquals("Maintenance date must use DD-MM-YY." + System.lineSeparator()
                        + "Maintenance recorded successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertEquals(1, Files.readAllLines(temporaryDirectory.resolve("maintenance.txt")).size());
    }

    @Test
    void bye_withoutMaintenanceRecords_createsMaintenanceFile() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);

        manager.executeInput("bye");

        Path file = temporaryDirectory.resolve("maintenance.txt");
        assertTrue(Files.isRegularFile(file));
        assertEquals("", Files.readString(file));
    }

    @Test
    void invalidSavedMaintenanceData_abortsCommandExecutionWithoutOverwritingFile() throws Exception {
        Path file = temporaryDirectory.resolve("maintenance.txt");
        String original = "not a maintenance record" + System.lineSeparator();
        Files.writeString(file, original);
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);

        manager.executeInput("maintain id/001 date/27-10-26 work/Repair outcome/Completed");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Could not load saved data: maintenance.txt:"));
        assertFalse(printed.contains("Maintenance recorded successfully."));
        assertEquals(original, Files.readString(file));
    }
}
