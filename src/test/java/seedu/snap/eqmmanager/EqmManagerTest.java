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

/** Tests command routing and output for equipment deletion. */
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
