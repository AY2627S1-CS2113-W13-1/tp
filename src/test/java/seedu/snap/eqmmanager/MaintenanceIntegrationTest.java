package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

/** Tests maintenance commands through the application manager and persistent data directory. */
class MaintenanceIntegrationTest {
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
        EqmManager manager = new EqmManager(temporaryDirectory);
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
        EqmManager manager = new EqmManager(temporaryDirectory);
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
        EqmManager manager = new EqmManager(temporaryDirectory);

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
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("maintain id/001 date/27-10-26 work/Repair outcome/Completed");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Could not load saved data: maintenance.txt:"));
        assertFalse(printed.contains("Maintenance recorded successfully."));
        assertEquals(original, Files.readString(file));
    }

    @Test
    void historyMaintenance_validCommand_displaysChronologicalRecordsWithoutWriting() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("maintain id/001 date/27-10-26 work/Later repair outcome/Completed");
        manager.executeInput("maintain id/001 date/24-10-26 work/Earlier repair outcome/Completed");
        Path maintenanceFile = temporaryDirectory.resolve("maintenance.txt");
        byte[] savedBeforeView = Files.readAllBytes(maintenanceFile);
        output.reset();

        manager.executeInput("history maintenance/id/001");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Maintenance history for item ID 001:"));
        assertTrue(printed.contains("DATE"));
        assertTrue(printed.contains("Earlier repair"));
        assertTrue(printed.contains("Later repair"));
        assertTrue(printed.indexOf("2026-10-24") < printed.indexOf("2026-10-27"));
        assertArrayEquals(savedBeforeView, Files.readAllBytes(maintenanceFile));
    }

    @Test
    void historyMaintenance_emptyUnknownAndInvalidQueries_areHandled() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        output.reset();

        manager.executeInput("history maintenance/id/001");
        manager.executeInput("history maintenance/id/999");
        manager.executeInput("history maintenance/id/000");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("No maintenance history for item ID 001."));
        assertTrue(printed.contains("Equipment ID 999 does not exist."));
        assertTrue(printed.contains("Invalid format. Use: history maintenance/id/[ITEM ID], from 001 to 999."));
    }

    @Test
    void historyMaintenance_afterRestart_displaysPersistedRecords() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("maintain id/001 date/27-10-26 work/Replaced battery outcome/Completed");
        output.reset();

        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("history maintenance/id/001");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Replaced battery"));
        assertTrue(printed.contains("Completed"));
    }

    @Test
    void maintenanceDue_afterUnserviceableReturn_listsEquipment() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("loan id/001 b/002 issued/24-10-26 due/27-10-26");
        manager.executeInput("return loan/001 serviceable/no damage/SHUTTER_FAULT");
        output.reset();

        manager.executeInput("list maintenance-due");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Equipment due for maintenance or inspection:"));
        assertTrue(printed.contains("001"));
        assertTrue(printed.contains("Needs inspection"));
    }

    @Test
    void inspect_pass_updatesAvailabilityAndPersistsOutcomeAfterRestart() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("loan id/001 b/002 issued/24-10-26 due/27-10-26");
        manager.executeInput("return loan/001 serviceable/no damage/SHUTTER_FAULT");
        output.reset();

        manager.executeInput("inspect id/001 date/27-10-26 outcome/PASS");

        assertEquals("Inspection recorded successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("equipment.txt")).contains("serviceable/yes"));
        assertTrue(Files.readString(temporaryDirectory.resolve("maintenance.txt"))
                .contains("work/Inspection\toutcome/PASSED"));

        output.reset();
        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("list maintenance-due");
        restarted.executeInput("history maintenance/id/001");
        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("No equipment is due for maintenance or inspection."));
        assertTrue(printed.contains("PASSED"));
    }

    @Test
    void inspect_fail_keepsEquipmentDueAndPersistsFailedOutcome() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("loan id/001 b/002 issued/24-10-26 due/27-10-26");
        manager.executeInput("return loan/001 serviceable/no damage/SHUTTER_FAULT");
        output.reset();

        manager.executeInput("inspect id/001 date/27-10-26 outcome/FAIL");
        manager.executeInput("list maintenance-due");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Inspection recorded successfully."));
        assertTrue(printed.contains("Needs inspection"));
        assertTrue(Files.readString(temporaryDirectory.resolve("maintenance.txt"))
                .contains("work/Inspection\toutcome/FAILED"));
        assertTrue(Files.readString(temporaryDirectory.resolve("equipment.txt")).contains("serviceable/no"));
    }

    @Test
    void inspect_activeLoan_isRejectedWithoutChangingLoan() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("loan id/001 b/002 issued/24-10-26 due/27-10-26");
        output.reset();

        manager.executeInput("inspect id/001 date/27-10-26 outcome/PASS");

        assertEquals("Cannot inspect item ID 001 while it has an active loan." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.notExists(temporaryDirectory.resolve("maintenance.txt")));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt")).contains("returned/false"));
    }

    @Test
    void summaryMaintenance_afterRestart_identifiesOverdueAndFrequentRepairs() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Frequent id/001 type/camera cond/5");
        manager.executeInput("add i/Overdue id/002 type/lens cond/5");
        manager.executeInput("maintain id/001 date/25-10-26 work/Repair A outcome/Completed");
        manager.executeInput("maintain id/001 date/26-10-26 work/Repair B outcome/Completed");
        manager.executeInput("loan id/002 b/002 issued/24-10-26 due/27-10-26");
        manager.executeInput("return loan/001 serviceable/no damage/AUTOFOCUS_FAULT");
        EqmManager restarted = new EqmManager(temporaryDirectory);
        byte[] equipmentBefore = Files.readAllBytes(temporaryDirectory.resolve("equipment.txt"));
        byte[] maintenanceBefore = Files.readAllBytes(temporaryDirectory.resolve("maintenance.txt"));
        output.reset();

        restarted.executeInput("summary maintenance");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Maintenance summary:"));
        assertTrue(printed.contains("Frequently repaired threshold: at least 2 repairs."));
        assertTrue(printed.contains("Frequent"));
        assertTrue(printed.contains("Overdue"));
        assertTrue(printed.contains("YES"));
        assertTrue(printed.contains("2026-10-26"));
        assertTrue(printed.indexOf("001") < printed.indexOf("002"));
        assertArrayEquals(equipmentBefore, Files.readAllBytes(temporaryDirectory.resolve("equipment.txt")));
        assertArrayEquals(maintenanceBefore, Files.readAllBytes(temporaryDirectory.resolve("maintenance.txt")));
    }

    @Test
    void summaryMaintenance_emptyData_displaysClearMessage() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        output.reset();

        manager.executeInput("summary maintenance");

        assertEquals("No overdue maintenance or frequent repair records found." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void summaryMaintenance_invalidCommand_reportsErrorAndPreservesRecords() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("maintain id/001 date/27-10-26 work/Repair outcome/Completed");
        byte[] maintenanceBefore = Files.readAllBytes(temporaryDirectory.resolve("maintenance.txt"));
        output.reset();

        manager.executeInput("summary maintenance extra");

        assertEquals("Invalid format. Use: summary maintenance." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertArrayEquals(maintenanceBefore, Files.readAllBytes(temporaryDirectory.resolve("maintenance.txt")));
    }
}
