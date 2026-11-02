package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Verifies saves, startup protection and termination when the console has no more input. */
@Timeout(5)
class EndOfInputIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private InputStream originalInput;
    private PrintStream originalOutput;

    @TempDir
    private Path dataDirectory;

    @BeforeEach
    void captureConsole() {
        originalInput = System.in;
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreConsole() {
        System.setIn(originalInput);
        System.setOut(originalOutput);
    }

    @Test
    void run_emptyInput_savesAnEmptyDataSetAndExits() {
        EqmManager manager = managerWithInput("");

        manager.run();

        assertTrue(Files.isRegularFile(dataDirectory.resolve("equipment.txt")));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("loans.txt")));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("loan-sequence.txt")));
        assertTrue(Files.isRegularFile(dataDirectory.resolve("maintenance.txt")));
        assertEquals(1, countOutputLines("snAp > "));
        assertEquals(1, countOutputLines("[OK] Goodbye."));
        assertFalse(printed().contains("[ERROR]"));
    }

    @Test
    void run_inputEndsAfterCommandWithoutNewline_savesBundleForRestart() {
        EqmManager manager = managerWithInput("add-bundle name/Retained kit");

        manager.run();

        assertTrue(Files.isRegularFile(dataDirectory.resolve("bundles.txt")));
        assertEquals(1, countOutputLines("[OK] Goodbye."));
        output.reset();
        EqmManager restarted = managerWithInput("list bundles\nbye\n");
        restarted.run();
        assertTrue(printed().contains("Retained kit"));
        assertFalse(printed().contains("[ERROR]"));
    }

    @Test
    void run_inputEndsAfterStartupFailure_preservesDamagedFileWithoutSaving() throws Exception {
        Path equipmentFile = dataDirectory.resolve("equipment.txt");
        String damagedRecords = "not an equipment record\n";
        Files.writeString(equipmentFile, damagedRecords);
        EqmManager manager = managerWithInput("");

        manager.run();

        assertEquals(damagedRecords, Files.readString(equipmentFile));
        assertFalse(Files.exists(dataDirectory.resolve("loans.txt")));
        assertFalse(Files.exists(dataDirectory.resolve("loan-sequence.txt")));
        assertFalse(Files.exists(dataDirectory.resolve("maintenance.txt")));
        assertTrue(printed().contains("Could not load saved data: equipment.txt:"));
        assertEquals(1, countOutputLines("snAp > "));
    }

    @Test
    void run_finalSaveFailsAfterInputEnds_reportsFailureOnceAndDoesNotClaimSuccess() throws Exception {
        EqmManager manager = managerWithInput("add-bundle name/Unsaved kit\n");
        Path bundleDirectory = blockBundleStorage();

        manager.run();

        assertEquals(1, countOutputLines("[ERROR] Could not save data: bundles.txt:"));
        assertTrue(printed().contains("Input has ended. Exiting with unsaved changes."));
        assertFalse(printed().contains("[OK] Goodbye."));
        assertEquals(2, countOutputLines("snAp > "));
        assertEquals("Keep this file.", Files.readString(bundleDirectory.resolve("keep.txt")));
    }

    @Test
    void run_failedByeStillAcceptsCommands_thenStopsAtEndOfInput() throws Exception {
        EqmManager manager = managerWithInput("bye\nadd-bundle name/After failed exit\n");
        blockBundleStorage();

        manager.run();

        String text = printed();
        int failure = text.indexOf("[ERROR] Could not save data: bundles.txt:");
        int addition = text.indexOf("[OK] Bundle 001 \"After failed exit\" created.");
        assertTrue(failure >= 0 && addition > failure);
        assertEquals(3, countOutputLines("snAp > "));
        assertFalse(text.contains("[OK] Goodbye."));
    }

    @Test
    void run_afterFailedByeAndStorageRepair_allowsSuccessfulRetry() throws Exception {
        EqmManager manager = managerWithInput("bye\n");
        manager.executeInput("add-bundle name/Recovered kit");
        Path bundleDirectory = blockBundleStorage();
        manager.executeInput("bye");
        assertTrue(printed().contains("[ERROR] Could not save data: bundles.txt:"));
        Files.delete(bundleDirectory.resolve("keep.txt"));
        Files.delete(bundleDirectory);
        output.reset();

        manager.run();

        assertTrue(Files.isRegularFile(dataDirectory.resolve("bundles.txt")));
        assertEquals(1, countOutputLines("snAp > "));
        assertEquals(1, countOutputLines("[OK] Goodbye."));
        assertFalse(printed().contains("[ERROR]"));
    }

    /** Supplies each test's console stream before the manager constructs its parser. */
    private EqmManager managerWithInput(String commands) {
        System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
        return new EqmManager(dataDirectory);
    }

    /** Makes bundle writes fail after startup without relying on platform-specific file permissions. */
    private Path blockBundleStorage() throws Exception {
        Path directory = Files.createDirectory(dataDirectory.resolve("bundles.txt"));
        Files.writeString(directory.resolve("keep.txt"), "Keep this file.");
        return directory;
    }

    private long countOutputLines(String prefix) {
        return printed().lines().filter(line -> line.startsWith(prefix)).count();
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
