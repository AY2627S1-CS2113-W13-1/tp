package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

        assertTrue(printed().contains("[OK] Backup restored successfully"));
        output.reset();
        manager.executeInput("list loans");
        String loans = printed();
        assertTrue(loans.contains("001"));
        assertTrue(loans.contains("111"));
        assertTrue(loans.contains("02-10-26"));
        assertTrue(loans.contains("1 loan."));
        assertFalse(loans.contains("Returned"));
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
        assertTrue(printed().contains("02-10-26"));
        assertFalse(printed().contains("Returned"));
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
    void run_startupLoadFailure_acceptsRestoreAndContinuesThroughThePrompt() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Path backupDirectory = temporaryDirectory.resolve("interactive-backup");
        manager.executeInput("backup path/" + backupDirectory);
        Files.writeString(dataDirectory.resolve("equipment.txt"), "not an equipment record\n");
        InputStream originalInput = System.in;
        try {
            String commands = "restore path/" + backupDirectory + "\nlist loans\nbye\n";
            System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
            EqmManager broken = new EqmManager(dataDirectory);
            output.reset();

            broken.run();

            String printed = printed();
            assertTrue(printed.contains("[ERROR] Could not load saved data:"));
            assertTrue(printed.contains("snAp > "));
            assertTrue(printed.contains("[OK] Backup restored successfully"));
            assertTrue(printed.contains("02-10-26"));
            assertTrue(printed.contains("1 loan."));
            assertTrue(printed.contains("[OK] Goodbye."));
            assertTrue(Files.readString(dataDirectory.resolve("equipment.txt")).contains("add i/Camera"));
        } finally {
            System.setIn(originalInput);
        }
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

        assertTrue(printed().contains("[ERROR] Could not restore backup"));
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("data/equipment.txt")));
        assertEquals(loansBefore, Files.readString(temporaryDirectory.resolve("data/loans.txt")));
        output.reset();
        manager.executeInput("list loans");
        assertTrue(printed().contains("Returned"));
    }

    @Test
    void restoreCommand_malformedMaintenance_preservesEveryLiveFileAndStartupGuard() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Path maintenanceFile = dataDirectory.resolve("maintenance.txt");
        manager.executeInput("maintain id/001 date/01-01-20 work/Checked camera outcome/Completed");
        Path backupDirectory = temporaryDirectory.resolve("maintenance-backup");
        manager.executeInput("backup path/" + backupDirectory);
        manager.executeInput("update id/001 cond/7");
        manager.executeInput("add-bundle name/Keep this bundle");
        manager.executeInput("bye");
        Files.writeString(maintenanceFile, Files.readString(maintenanceFile) + "broken record\n");
        byte[] maintenanceBefore = Files.readAllBytes(maintenanceFile);
        byte[] equipmentBefore = Files.readAllBytes(dataDirectory.resolve("equipment.txt"));
        byte[] loansBefore = Files.readAllBytes(dataDirectory.resolve("loans.txt"));
        byte[] sequenceBefore = Files.readAllBytes(dataDirectory.resolve("loan-sequence.txt"));
        byte[] bundlesBefore = Files.readAllBytes(dataDirectory.resolve("bundles.txt"));
        EqmManager broken = new EqmManager(dataDirectory);
        output.reset();

        broken.executeInput("restore path/" + backupDirectory);

        assertTrue(printed().contains("maintenance data could not be loaded"));
        assertFalse(printed().contains("[OK] Backup restored successfully"));
        output.reset();
        broken.executeInput("maintain id/001 date/01-01-20 work/Another check outcome/Completed");
        broken.executeInput("list");
        assertTrue(printed().contains("Could not load saved data: maintenance.txt:"));
        assertFalse(printed().contains("Maintenance recorded successfully"));
        broken.executeInput("bye");

        assertArrayEquals(maintenanceBefore, Files.readAllBytes(maintenanceFile));
        assertArrayEquals(equipmentBefore, Files.readAllBytes(dataDirectory.resolve("equipment.txt")));
        assertArrayEquals(loansBefore, Files.readAllBytes(dataDirectory.resolve("loans.txt")));
        assertArrayEquals(sequenceBefore, Files.readAllBytes(dataDirectory.resolve("loan-sequence.txt")));
        assertArrayEquals(bundlesBefore, Files.readAllBytes(dataDirectory.resolve("bundles.txt")));
    }

    @Test
    void restoreCommand_unreadableMaintenance_preservesDirectoryAndStartupGuard() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Path backupDirectory = temporaryDirectory.resolve("unreadable-maintenance-backup");
        manager.executeInput("backup path/" + backupDirectory);
        manager.executeInput("update id/001 cond/7");
        byte[] equipmentBefore = Files.readAllBytes(dataDirectory.resolve("equipment.txt"));
        Path maintenanceDirectory = Files.createDirectory(dataDirectory.resolve("maintenance.txt"));
        Path retainedFile = maintenanceDirectory.resolve("keep.txt");
        Files.writeString(retainedFile, "Keep this file.");
        EqmManager broken = new EqmManager(dataDirectory);
        output.reset();

        broken.executeInput("restore path/" + backupDirectory);

        assertTrue(printed().contains("maintenance data could not be loaded"));
        output.reset();
        broken.executeInput("list");
        assertTrue(printed().contains("Could not load saved data: maintenance.txt:"));
        broken.executeInput("bye");
        assertTrue(Files.isDirectory(maintenanceDirectory));
        assertEquals("Keep this file.", Files.readString(retainedFile));
        assertArrayEquals(equipmentBefore, Files.readAllBytes(dataDirectory.resolve("equipment.txt")));
    }

    @Test
    void restoreCommand_afterMaintenanceRepairAndRestart_retainsHistoryAndAllowsFurtherRecords() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Path maintenanceFile = dataDirectory.resolve("maintenance.txt");
        manager.executeInput("maintain id/001 date/01-01-20 work/Checked camera outcome/Completed");
        byte[] validMaintenance = Files.readAllBytes(maintenanceFile);
        Path backupDirectory = temporaryDirectory.resolve("repaired-maintenance-backup");
        manager.executeInput("backup path/" + backupDirectory);
        Files.writeString(maintenanceFile, "broken record\n");
        EqmManager broken = new EqmManager(dataDirectory);
        broken.executeInput("restore path/" + backupDirectory);
        broken.executeInput("bye");
        Files.write(maintenanceFile, validMaintenance);
        EqmManager repaired = new EqmManager(dataDirectory);
        output.reset();

        repaired.executeInput("restore path/" + backupDirectory);

        assertTrue(printed().contains("[OK] Backup restored successfully"));
        assertArrayEquals(validMaintenance, Files.readAllBytes(maintenanceFile));
        repaired.executeInput("maintain id/001 date/02-01-20 work/Cleaned camera outcome/Completed");
        repaired.executeInput("bye");
        EqmManager restarted = new EqmManager(dataDirectory);
        restarted.executeInput("bye");
        assertEquals(2, Files.readAllLines(maintenanceFile).size());
        assertTrue(Files.readString(maintenanceFile).contains("work/Checked camera"));
        assertTrue(Files.readString(maintenanceFile).contains("work/Cleaned camera"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"equipment.txt", "loans.txt", "bundles.txt"})
    void restoreCommand_otherLoadingError_recoversWithoutChangingMaintenance(String damagedFile) throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Path maintenanceFile = dataDirectory.resolve("maintenance.txt");
        manager.executeInput("maintain id/001 date/01-01-20 work/Checked camera outcome/Completed");
        byte[] maintenanceBefore = Files.readAllBytes(maintenanceFile);
        Path backupDirectory = temporaryDirectory.resolve("other-loading-error-backup");
        manager.executeInput("backup path/" + backupDirectory);
        Files.writeString(dataDirectory.resolve(damagedFile), "broken record\n");
        EqmManager broken = new EqmManager(dataDirectory);
        output.reset();

        broken.executeInput("restore path/" + backupDirectory);
        broken.executeInput("list loans");
        broken.executeInput("bye");

        assertTrue(printed().contains("[OK] Backup restored successfully"));
        assertTrue(printed().contains("1 loan."));
        assertFalse(printed().contains("Could not load saved data"));
        assertArrayEquals(maintenanceBefore, Files.readAllBytes(maintenanceFile));
    }

    /** Returns captured command output without relying on table spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
