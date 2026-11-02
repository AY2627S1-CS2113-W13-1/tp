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

/** Tests command routing and output for equipment additions and deletions. */
class EqmManagerTest {
    private static final String ADD_CAMERA = "eqm-add name/Canon EOS R10 e/E001 type/camera cond/1";
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
    void deleteEquipment_bundledItem_preservesInventoryAndCompletion() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("eqm-add name/Lens e/E002 type/lens cond/1");
        manager.executeInput("eqm-add name/Battery e/E003 type/battery cond/1");
        manager.executeInput("bundle-create name/Camera kit");
        manager.executeInput("bundle-add-eqm b/B001 E001 E002 E003");
        String saved = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        output.reset();

        manager.executeInput("eqm-delete e/E001");
        manager.executeInput("bundle-list");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("Cannot delete item ID E001 while it belongs to a bundle."));
        assertTrue(actual.contains("Canon EOS R10"));
        assertTrue(actual.contains("Composition: Complete"));
        assertEquals(saved, Files.readString(temporaryDirectory.resolve("equipment.txt")));
    }

    @Test
    void removeFromBundle_afterRestart_allowsEquipmentDeletion() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("bundle-create name/Camera kit");
        manager.executeInput("bundle-add-eqm b/B001 E001");
        manager.executeInput("exit");

        output.reset();
        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("bundle-remove-eqm b/B001 E001");
        restarted.executeInput("eqm-delete e/E001");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("Equipment ID E001 removed from bundle B001 successfully."));
        assertTrue(actual.contains("[OK] Equipment E001 \"Canon EOS R10\" deleted."));
        assertEquals("", Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertEquals("bundle id/B001 name64/Q2FtZXJhIGtpdA items/" + System.lineSeparator(),
                Files.readString(temporaryDirectory.resolve("bundles.txt")));

        output.reset();
        EqmManager afterRestart = new EqmManager(temporaryDirectory);
        afterRestart.executeInput("bundle-list");
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("missing equipment"));
    }

    @Test
    void addEquipment_duplicateId_printsErrorAndAcceptsNextValidCommand() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("eqm-add name/Tripod e/E001 type/tripod cond/5");
        manager.executeInput("eqm-add name/Tripod e/E002 type/tripod cond/5");

        assertEquals("[ERROR] Item ID E001 already exists." + System.lineSeparator()
                + "[OK] Equipment E002 added." + System.lineSeparator()
                + "  Name: \"Tripod\"" + System.lineSeparator()
                + "  Type: tripod | Condition: 5 | Status: Available" + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void addEquipment_malformedCommands_recoversAndPreservesInventory() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();
        String[] commands = {
            "eqm-add name/Tripod e/E002",
            "eqm-add name/Tripod e/E002 type/tripod cond/11",
            "eqm-add name/Tripode/E002 type/tripod cond/5",
            "eqm-add name/Tripod name/Lens e/E002 type/tripod cond/5",
            "eqm-add name/\u2003 e/E002 type/tripod cond/5",
            "eqm-add name/Tripod e/E001 type/tripod cond/5"
        };
        String formatError = "Invalid format. Use: eqm-add name/[ITEM] e/[ITEM ID] type/[TYPE] cond/[CONDITION]";
        String[] errors = {
            formatError, "Condition must be a whole number from 1 to 10.", formatError, formatError,
            "Item name must contain 1 to 100 characters.", "Item ID E001 already exists."
        };
        for (int i = 0; i < commands.length; i++) {
            manager.executeInput(commands[i]);
            assertEquals("[ERROR] " + errors[i] + System.lineSeparator(),
                    output.toString(StandardCharsets.UTF_8), commands[i]);
            output.reset();
        }
        manager.executeInput("eqm-add name/Tripod e/E002 type/tripod cond/5");
        assertEquals("[OK] Equipment E002 added." + System.lineSeparator()
                + "  Name: \"Tripod\"" + System.lineSeparator()
                + "  Type: tripod | Condition: 5 | Status: Available" + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        output.reset();

        manager.executeInput("eqm-list");
        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("| E001 | Canon EOS R10"));
        assertTrue(actual.contains("| E002 | Tripod"));
        assertTrue(actual.contains("2 equipment items."));
        assertTrue(actual.lines().allMatch(line -> line.length() <= 100));
    }

    @Test
    void addEquipment_explicitSubtypeDetails_showsSavedDetails() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Camera e/E001 type/CAMERA cond/7 sensor/Full frame damage/sensor_damage");
        manager.executeInput("eqm-add name/Lens e/E002 type/lens cond/6 zoom/24 to 70 mm damage/scratched_glass");
        manager.executeInput("eqm-add name/Battery e/E003 type/battery cond/5 capacity/2000 mAh damage/swelling");
        manager.executeInput("eqm-add name/Tripod e/E004 type/tripod cond/4 thread/Quarter inch damage/bent_leg");
        output.reset();

        manager.executeInput("eqm-list");

        String table = output.toString(StandardCharsets.UTF_8);
        assertEquals(4, table.lines().filter(line -> line.contains("Available")).count());
        assertTrue(table.contains("4 equipment items."));
        output.reset();
        for (String id : java.util.List.of("E001", "E002", "E003", "E004")) {
            manager.executeInput("eqm-show e/" + id);
        }
        String details = output.toString(StandardCharsets.UTF_8);
        assertTrue(details.contains("Sensor format: Full frame"));
        assertTrue(details.contains("Zoom range: 24 to 70 mm"));
        assertTrue(details.contains("Capacity: 2000 mAh"));
        assertTrue(details.contains("Thread: Quarter inch"));
        for (String damage : java.util.List.of("SENSOR_DAMAGE", "SCRATCHED_GLASS", "SWELLING", "BENT_LEG")) {
            assertTrue(details.contains("Damage: " + damage));
        }
    }

    @Test
    void addEquipment_withRemarks_displaysAndPersistsRemarks() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Lens e/E001 type/lens cond/3 remarks/Handle with care");
        output.reset();

        manager.executeInput("eqm-show e/E001");

        String details = output.toString(StandardCharsets.UTF_8);
        assertTrue(details.contains("Equipment E001"));
        assertTrue(details.contains("Name: Lens"));
        assertTrue(details.contains("Remarks: Handle with care"));
        assertTrue(Files.readString(temporaryDirectory.resolve("equipment.txt"))
                .contains("remarks/Handle with care"));
    }

    @Test
    void deleteEquipment_validCommand_printsSuccessMessage() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("eqm-delete e/E001");

        assertEquals("[OK] Equipment E001 \"Canon EOS R10\" deleted." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_invalidCommands_printExpectedErrors() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();
        String expectedMessage = "Invalid delete syntax. ITEM ID must be from E001 to E999.";
        String[] invalidCommands = {
            "eqm-delete",
            "eqm-delete camera/001",
            "eqm-delete e/1",
            "eqm-delete e/000",
            "eqm-delete e/1000",
            "eqm-delete e/abc",
            "eqm-delete e/E001 extra"
        };

        for (String command : invalidCommands) {
            manager.executeInput(command);
            assertEquals("[ERROR] " + expectedMessage + System.lineSeparator(),
                    output.toString(StandardCharsets.UTF_8), command);
            output.reset();
        }
    }

    @Test
    void deleteEquipment_unknownId_printsExpectedError() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("eqm-delete e/E999");

        assertEquals("[ERROR] Equipment ID E999 does not exist." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_repeatedCommand_printsMissingIdError() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("eqm-delete e/E001");
        output.reset();

        manager.executeInput("eqm-delete e/E001");

        assertEquals("[ERROR] Equipment ID E001 does not exist." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_afterInvalidCommands_stillDeletesEquipment() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("eqm-delete e/000");
        manager.executeInput("eqm-delete e/999");
        output.reset();

        manager.executeInput("eqm-delete e/E001");

        assertEquals("[OK] Equipment E001 \"Canon EOS R10\" deleted." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_sameNameDifferentIds_deletesOnlyRequestedId() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("eqm-add name/Canon EOS R10 e/E002 type/camera cond/5");
        output.reset();

        manager.executeInput("eqm-delete e/E001");
        manager.executeInput("eqm-delete e/E001");
        manager.executeInput("eqm-delete e/E002");

        assertEquals("[OK] Equipment E001 \"Canon EOS R10\" deleted." + System.lineSeparator()
                + "[ERROR] Equipment ID E001 does not exist." + System.lineSeparator()
                + "[OK] Equipment E002 \"Canon EOS R10\" deleted." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeInput_unrecognizedCommand_printsErrorMessage() {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("gibberish");

        assertEquals(
                "[ERROR] Command not recognised. Type help for available commands." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8)
        );
    }

    @Test
    void bye_withoutMutations_createsAllStorageFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("exit");

        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        assertTrue(Files.isRegularFile(equipmentFile));
        assertTrue(Files.isRegularFile(loansFile));
        assertTrue(Files.isRegularFile(sequenceFile));
        assertEquals("", Files.readString(equipmentFile));
        assertEquals("", Files.readString(loansFile));
        assertEquals("nextLoanId/L001" + System.lineSeparator(), Files.readString(sequenceFile));
    }

    @Test
    void deleteLoan_failedSave_keepsEquipmentLoanedUntilSuccessfulRetry() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("loan-add e/E001 u/U002 issued/01-10-26 due/02-10-26");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        Files.delete(loansFile);
        Files.createDirectory(loansFile);
        Path sentinel = loansFile.resolve("keep.txt");
        Files.writeString(sentinel, "keep loan data");
        output.reset();

        manager.executeInput("loan-delete l/L001");
        manager.executeInput("eqm-delete e/E001");

        String rejected = output.toString(StandardCharsets.UTF_8);
        assertTrue(rejected.contains("Could not save loan deletion:"));
        assertTrue(rejected.contains("Cannot delete item ID E001 while it has an active loan."));
        assertFalse(rejected.contains("[OK]"));
        assertEquals("keep loan data", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(loansFile);
        output.reset();

        manager.executeInput("loan-delete l/L001");
        manager.executeInput("eqm-filter e/E001 status/available");

        String recovered = output.toString(StandardCharsets.UTF_8);
        assertTrue(recovered.contains("[OK] Loan L001 deleted."));
        assertTrue(recovered.contains("Canon EOS R10"));
        assertEquals("", Files.readString(loansFile));
        output.reset();
        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("eqm-filter e/E001 status/available");
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Canon EOS R10"));
    }

    @Test
    void bye_saveFailure_reportsBothFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("loan-add e/E001 u/U002 issued/01-10-26 due/02-10-26");

        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        Files.delete(equipmentFile);
        Files.delete(loansFile);
        Files.createDirectory(equipmentFile);
        Files.createDirectory(loansFile);
        Files.writeString(equipmentFile.resolve("keep.txt"), "keep equipment data");
        Files.writeString(loansFile.resolve("keep.txt"), "keep loan data");
        output.reset();

        manager.executeInput("exit");

        String message = output.toString(StandardCharsets.UTF_8);
        assertTrue(message.contains("Could not save data: equipment.txt:"));
        assertTrue(message.contains("loans.txt:"));
    }

    // @@author Rohita-29

    @Test
    void listOverdue_noLoans_printsClearMessage() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        output.reset();

        manager.executeInput("loan-list-overdue");

        assertEquals(
                "Overdue loans" + System.lineSeparator() + "-------------" + System.lineSeparator()
                        + "No overdue loans found." + System.lineSeparator() + "0 loans." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void listOverdue_activePastLoan_printsLoanWithoutChangingSavedData() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput(
                "loan-add e/E001 u/U002 issued/01-01-20 due/02-01-20");

        String savedEquipment = Files.readString(
                temporaryDirectory.resolve("equipment.txt"));
        String savedLoans = Files.readString(
                temporaryDirectory.resolve("loans.txt"));
        output.reset();

        manager.executeInput("loan-list-overdue");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("ITEM ID"));
        assertTrue(actual.contains("BORROWER ID"));
        assertTrue(actual.contains("DUE"));
        assertTrue(actual.contains("E001"));
        assertTrue(actual.contains("U002"));
        assertTrue(actual.contains("02-01-20"));
        assertEquals(
                savedEquipment,
                Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertEquals(
                savedLoans,
                Files.readString(temporaryDirectory.resolve("loans.txt")));
    }

    @Test
    void listOverdue_returnedPastLoan_printsNoResultsMessage() throws Exception {
        Files.writeString(
                temporaryDirectory.resolve("equipment.txt"),
                "add i/Camera id/E001 type/camera cond/1"
                        + System.lineSeparator());
        Files.writeString(
                temporaryDirectory.resolve("loans.txt"),
                "loan loanId/L001 id/E001 b/U002 issued/01-01-20 "
                        + "due/02-01-20 returned/true"
                        + System.lineSeparator());

        EqmManager manager = new EqmManager(temporaryDirectory);
        output.reset();

        manager.executeInput("loan-list-overdue");

        assertEquals(
                "Overdue loans" + System.lineSeparator() + "-------------" + System.lineSeparator()
                        + "No overdue loans found." + System.lineSeparator() + "0 loans." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void listOverdue_usesInjectedClockForReferenceDate() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-25T12:00:00Z"),
                ZoneOffset.UTC);
        EqmManager manager =
                new EqmManager(temporaryDirectory, clock);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput(
                "loan-add e/E001 u/U002 "
                        + "issued/24-10-26 due/25-10-26");
        output.reset();

        manager.executeInput("loan-list-overdue");

        assertEquals(
                "Overdue loans" + System.lineSeparator() + "-------------" + System.lineSeparator()
                        + "No overdue loans found." + System.lineSeparator() + "0 loans." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void oldInteractiveCommandKeywords_areRejected() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        String[] oldCommands = {
            "add", "list", "show e/001", "delete item e/001", "filter type/camera", "update e/001 name/New",
            "maintain e/001 date/27-10-26 work/Repair outcome/Done", "loan e/001 u/002 issued/01-10-26 due/02-10-26",
            "return loan l/001 serviceable/yes damage/NONE", "delete loan l/001", "update loan l/001 u/002",
            "list loans", "list borrower u/002", "view loan l/001", "list overdue", "history e/001",
            "add-eqm name/Camera e/001 type/camera cond/1", "list-eqm", "filter-eqm type/camera",
            "add-loan e/001 u/002 issued/01-10-26 due/02-10-26", "list-loan", "list-loan-borrower u/002",
            "list-loan-overdue", "add-bundle name/Kit", "list-bundle", "history bundle b/001", "bye"
        };

        for (String command : oldCommands) {
            output.reset();
            manager.executeInput(command);
            assertEquals("[ERROR] Command not recognised. Type help for available commands."
                    + System.lineSeparator(), output.toString(StandardCharsets.UTF_8), command);
        }
    }

    // @@author
}
