package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void addEquipment_duplicateId_printsErrorAndAcceptsNextValidCommand() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("add i/Tripod id/001 type/accessories cond/5");
        manager.executeInput("add i/Tripod id/002 type/accessories cond/5");

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
            "add i/Tripod id/002 type/accessories cond/11",
            "add i/Tripodid/002 type/accessories cond/5",
            "add i/Tripod i/Lens id/002 type/accessories cond/5",
            "add i/\u2003 id/002 type/accessories cond/5",
            "add i/Tripod id/001 type/accessories cond/5"
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
        manager.executeInput("add i/Tripod id/002 type/accessories cond/5");
        assertEquals("Equipment added successfully." + System.lineSeparator(), output.toString(StandardCharsets.UTF_8));
        output.reset();

        manager.executeInput("list");
        String expected = String.format("%-4s %-24s | %-6s | %-15s | %-9s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION")
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d%n",
                        "1)", "Canon EOS R10", "001", "camera", 1)
                + String.format("%-4s %-24s | %-6s | %-15s | %-9d%n",
                        "2)", "Tripod", "002", "accessories", 5);
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
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
            "delete loan/001",
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
    void bye_withoutMutations_createsBothStorageFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("bye");

        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        assertTrue(Files.isRegularFile(equipmentFile));
        assertTrue(Files.isRegularFile(loansFile));
        assertEquals("", Files.readString(equipmentFile));
        assertEquals("", Files.readString(loansFile));
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
