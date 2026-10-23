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

/** Tests command routing and output for equipment additions and deletions. */
class EqmManagerTest {
    private static final String ADD_CAMERA = "add i/Canon EOS R10 id/001 type/camera cond/1";
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
        manager.executeInput("add i/Lens id/002 type/lens cond/1");
        manager.executeInput("add i/Battery id/003 type/battery cond/1");
        manager.executeInput("add-bundle name/Camera kit");
        manager.executeInput("add-to-bundle id/001 001 002 003");
        String saved = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        output.reset();

        manager.executeInput("delete item/001");
        manager.executeInput("list bundles");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("Cannot delete item ID 001 while it belongs to a bundle."));
        assertTrue(actual.contains("Canon EOS R10"));
        assertTrue(actual.contains("true"));
        assertEquals(saved, Files.readString(temporaryDirectory.resolve("equipment.txt")));
    }

    @Test
    void addEquipment_duplicateId_printsErrorAndAcceptsNextValidCommand() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("add i/Tripod id/001 type/tripod cond/5");
        manager.executeInput("add i/Tripod id/002 type/tripod cond/5");

        assertEquals("Item ID 001 already exists." + System.lineSeparator()
                + "Equipment added successfully." + System.lineSeparator(), output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void addEquipment_malformedCommands_recoversAndPreservesInventory() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();
        String[] commands = {
            "add i/Tripod id/002",
            "add i/Tripod id/002 type/tripod cond/11",
            "add i/Tripodid/002 type/tripod cond/5",
            "add i/Tripod i/Lens id/002 type/tripod cond/5",
            "add i/\u2003 id/002 type/tripod cond/5",
            "add i/Tripod id/001 type/tripod cond/5"
        };
        String formatError = "Invalid format. Use: add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]";
        String[] errors = {
            formatError, "Condition must be a whole number from 1 to 10.", formatError, formatError,
            "Item name must contain 1 to 100 characters.", "Item ID 001 already exists."
        };
        for (int i = 0; i < commands.length; i++) {
            manager.executeInput(commands[i]);
            assertEquals(errors[i] + System.lineSeparator(), output.toString(StandardCharsets.UTF_8), commands[i]);
            output.reset();
        }
        manager.executeInput("add i/Tripod id/002 type/tripod cond/5");
        assertEquals("Equipment added successfully." + System.lineSeparator(), output.toString(StandardCharsets.UTF_8));
        output.reset();

        manager.executeInput("list");
        String expected = String.format("%-4s %-24s | %-6s | %-15s | %-9s | %-10s | %-30s | %-30s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION", "STATUS", "DETAILS", "REMARKS")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                        "1)", "Canon EOS R10", "001", "camera", 1, "Available",
                        "sensor/unspecified damage/NONE", "")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                        "2)", "Tripod", "002", "tripod", 5, "Available",
                        "thread/unspecified damage/NONE", "");
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void addEquipment_explicitSubtypeDetails_listsSavedDetails() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/CAMERA cond/7 sensor/Full frame damage/sensor_damage");
        manager.executeInput("add i/Lens id/002 type/lens cond/6 zoom/24 to 70 mm damage/scratched_glass");
        manager.executeInput("add i/Battery id/003 type/battery cond/5 capacity/2000 mAh damage/swelling");
        manager.executeInput("add i/Tripod id/004 type/tripod cond/4 thread/Quarter inch damage/bent_leg");
        output.reset();

        manager.executeInput("list");

        String expected = String.format("%-4s %-24s | %-6s | %-15s | %-9s | %-10s | %-30s | %-30s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION", "STATUS", "DETAILS", "REMARKS")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                        "1)", "Camera", "001", "camera", 7, "Available",
                        "sensor/Full frame damage/SENSOR_DAMAGE", "")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                        "2)", "Lens", "002", "lens", 6, "Available",
                        "zoom/24 to 70 mm damage/SCRATCHED_GLASS", "")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                        "3)", "Battery", "003", "battery", 5, "Available",
                        "capacity/2000 mAh damage/SWELLING", "")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                        "4)", "Tripod", "004", "tripod", 4, "Available",
                        "thread/Quarter inch damage/BENT_LEG", "");
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void addEquipment_withRemarks_displaysAndPersistsRemarks() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Lens id/001 type/lens cond/3 remarks/Handle with care");
        output.reset();

        manager.executeInput("list");

        String expected = String.format("%-4s %-24s | %-6s | %-15s | %-9s | %-10s | %-30s | %-30s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION", "STATUS", "DETAILS", "REMARKS")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                        "1)", "Lens", "001", "lens", 3, "Available",
                        "zoom/unspecified damage/NONE", "Handle with care");
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("equipment.txt"))
                .contains("remarks/Handle with care"));
    }

    @Test
    void deleteEquipment_validCommand_printsSuccessMessage() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("delete item/001");

        assertEquals("Equipment with item ID 001 deleted successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_invalidCommands_printExpectedErrors() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();
        String expectedMessage = "Invalid delete syntax. ITEM ID must be from 001 to 999.";
        String[] invalidCommands = {
            "delete",
            "delete camera/001",
            "delete item/1",
            "delete item/000",
            "delete item/1000",
            "delete item/abc",
            "delete item/001 extra"
        };

        for (String command : invalidCommands) {
            manager.executeInput(command);
            assertEquals(expectedMessage + System.lineSeparator(),
                    output.toString(StandardCharsets.UTF_8), command);
            output.reset();
        }
    }

    @Test
    void deleteEquipment_unknownId_printsExpectedError() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("delete item/999");

        assertEquals("Equipment ID 999 does not exist." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_repeatedCommand_printsMissingIdError() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("delete item/001");
        output.reset();

        manager.executeInput("delete item/001");

        assertEquals("Equipment ID 001 does not exist." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_afterInvalidCommands_stillDeletesEquipment() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("delete item/000");
        manager.executeInput("delete item/999");
        output.reset();

        manager.executeInput("delete item/001");

        assertEquals("Equipment with item ID 001 deleted successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_sameNameDifferentIds_deletesOnlyRequestedId() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("add i/Canon EOS R10 id/002 type/camera cond/5");
        output.reset();

        manager.executeInput("delete item/001");
        manager.executeInput("delete item/001");
        manager.executeInput("delete item/002");

        assertEquals("Equipment with item ID 001 deleted successfully." + System.lineSeparator()
                + "Equipment ID 001 does not exist." + System.lineSeparator()
                + "Equipment with item ID 002 deleted successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeInput_unrecognizedCommand_printsErrorMessage() {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("gibberish");

        assertEquals(
                "Command not recognised. Please try again." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8)
        );
    }

    @Test
    void bye_withoutMutations_createsAllStorageFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("bye");

        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        assertTrue(Files.isRegularFile(equipmentFile));
        assertTrue(Files.isRegularFile(loansFile));
        assertTrue(Files.isRegularFile(sequenceFile));
        assertEquals("", Files.readString(equipmentFile));
        assertEquals("", Files.readString(loansFile));
        assertEquals("nextLoanId/001" + System.lineSeparator(), Files.readString(sequenceFile));
    }

    @Test
    void deleteLoan_failedSave_keepsEquipmentLoanedUntilSuccessfulRetry() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/02-10-26");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        Files.delete(loansFile);
        Files.createDirectory(loansFile);
        Path sentinel = loansFile.resolve("keep.txt");
        Files.writeString(sentinel, "keep loan data");
        output.reset();

        manager.executeInput("delete loan/001");
        manager.executeInput("delete item/001");

        String rejected = output.toString(StandardCharsets.UTF_8);
        assertTrue(rejected.contains("Could not save loan deletion:"));
        assertTrue(rejected.contains("Cannot delete item ID 001 while it has an active loan."));
        assertFalse(rejected.contains("deleted successfully."));
        assertEquals("keep loan data", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(loansFile);
        output.reset();

        manager.executeInput("delete loan/001");
        manager.executeInput("filter id/001 status/available");

        String recovered = output.toString(StandardCharsets.UTF_8);
        assertTrue(recovered.contains("Loan 001 for item ID 001 deleted successfully."));
        assertTrue(recovered.contains("Canon EOS R10"));
        assertEquals("", Files.readString(loansFile));
        output.reset();
        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("filter id/001 status/available");
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Canon EOS R10"));
    }

    @Test
    void bye_saveFailure_reportsBothFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/02-10-26");

        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        Files.delete(equipmentFile);
        Files.delete(loansFile);
        Files.createDirectory(equipmentFile);
        Files.createDirectory(loansFile);
        Files.writeString(equipmentFile.resolve("keep.txt"), "keep equipment data");
        Files.writeString(loansFile.resolve("keep.txt"), "keep loan data");
        output.reset();

        manager.executeInput("bye");

        String message = output.toString(StandardCharsets.UTF_8);
        assertTrue(message.contains("Could not save data: equipment.txt:"));
        assertTrue(message.contains("loans.txt:"));
    }
}
