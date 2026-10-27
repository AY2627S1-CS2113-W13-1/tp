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

/** Exercises backup restoration through the public application command interface. */
class RestoreIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        manager = new EqmManager(temporaryDirectory.resolve("data"));
        manager.executeInput("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void restoreCommand_validBackup_restoresAvailabilityHistoryAndSequence() throws Exception {
        Path backupDirectory = temporaryDirectory.resolve("backup");
        manager.executeInput("backup path/" + backupDirectory);
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        manager.executeInput("add i/Lens id/002 type/lens cond/5 zoom/24-70mm");
        output.reset();

        manager.executeInput("restore path/" + backupDirectory);

        assertTrue(printed().contains("Backup restored successfully"));
        output.reset();
        manager.executeInput("list loans");
        String loans = printed();
        assertTrue(loans.contains("001"));
        assertTrue(loans.contains("111"));
        assertTrue(loans.contains("false"));
        assertFalse(loans.contains("002"));
        output.reset();
        manager.executeInput("list");
        assertTrue(printed().contains("Loaned"));
    }

    @Test
    void restoreCommand_restoredState_survivesRestart() throws Exception {
        Path backupDirectory = temporaryDirectory.resolve("backup");
        manager.executeInput("backup path/" + backupDirectory);
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        output.reset();

        manager.executeInput("restore path/" + backupDirectory);
        EqmManager restarted = new EqmManager(temporaryDirectory.resolve("data"));
        restarted.executeInput("list loans");

        assertTrue(printed().contains("001"));
        assertTrue(printed().contains("111"));
        assertTrue(printed().contains("false"));
        assertEquals("nextLoanId/002" + System.lineSeparator(),
                Files.readString(temporaryDirectory.resolve("data/loan-sequence.txt")));
    }

    @Test
    void restoreCommand_success_clearsSessionOnlyBundlesThatReferenceOldEquipment() throws Exception {
        Path backupDirectory = temporaryDirectory.resolve("backup");
        manager.executeInput("backup path/" + backupDirectory);
        manager.executeInput("add-bundle name/Old session bundle");
        output.reset();

        manager.executeInput("restore path/" + backupDirectory);
        output.reset();
        manager.executeInput("list bundles");

        assertFalse(printed().contains("Old session bundle"));
    }

    @Test
    void restoreCommand_success_persistsClearedBundlesBeforeRestart() throws Exception {
        Path backupDirectory = temporaryDirectory.resolve("backup");
        manager.executeInput("backup path/" + backupDirectory);
        manager.executeInput("add i/Extra Lens id/002 type/lens cond/5 zoom/24-70mm");
        manager.executeInput("add-bundle name/Persisted old bundle");
        manager.executeInput("add-to-bundle id/001 002");
        manager.executeInput("bye");
        assertTrue(Files.readString(temporaryDirectory.resolve("data/bundles.txt")).contains("002"));
        manager = new EqmManager(temporaryDirectory.resolve("data"));
        output.reset();

        manager.executeInput("restore path/" + backupDirectory);

        assertTrue(printed().contains("Backup restored successfully"));
        assertEquals("", Files.readString(temporaryDirectory.resolve("data/bundles.txt")));
        EqmManager restarted = new EqmManager(temporaryDirectory.resolve("data"));
        output.reset();
        restarted.executeInput("list bundles");
        assertFalse(printed().contains("Could not load saved data"));
        assertFalse(printed().contains("Persisted old bundle"));
    }

    @Test
    void restoreCommand_startupLoadFailureCanRecoverFromValidBackup() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("broken-data");
        EqmManager creator = new EqmManager(dataDirectory);
        creator.executeInput("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        Path backupDirectory = temporaryDirectory.resolve("recovery-backup");
        creator.executeInput("backup path/" + backupDirectory);
        Files.writeString(dataDirectory.resolve("equipment.txt"), "not an equipment record\n");
        EqmManager broken = new EqmManager(dataDirectory);
        output.reset();

        broken.executeInput("restore path/" + backupDirectory);

        assertTrue(printed().contains("Backup restored successfully"));
        output.reset();
        broken.executeInput("list");
        assertTrue(printed().contains("Camera"));
        assertFalse(printed().contains("Could not load saved data"));
    }

    @Test
    void restoreCommand_invalidBackup_reportsFailureAndPreservesCurrentState() throws Exception {
        Path backupDirectory = temporaryDirectory.resolve("backup");
        manager.executeInput("backup path/" + backupDirectory);
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        String equipmentBefore = Files.readString(temporaryDirectory.resolve("data/equipment.txt"));
        String loansBefore = Files.readString(temporaryDirectory.resolve("data/loans.txt"));
        Files.delete(backupDirectory.resolve("loan-sequence.txt"));
        output.reset();

        manager.executeInput("restore path/" + backupDirectory);

        assertTrue(printed().contains("Could not restore backup"));
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("data/equipment.txt")));
        assertEquals(loansBefore, Files.readString(temporaryDirectory.resolve("data/loans.txt")));
        output.reset();
        manager.executeInput("list loans");
        assertTrue(printed().contains("true"));
    }

    /** Returns captured command output without relying on table spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
