package seedu.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks command validation, deletion confirmation and persistence across application runs.
 */
class DukeTest {
    private static final String ADD_CAMERA = "add i/Canon EOS R10 id/001 type/camera cond/1";

    @TempDir
    Path directory;

    private String runCommands(String commands, Path file) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Duke.run(new Scanner(commands), new PrintStream(output, true, StandardCharsets.UTF_8), file);
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void addAndRestart_preservesEveryFieldAndRejectsDuplicateId() throws IOException {
        Path file = directory.resolve("data/equipment.txt");
        assertTrue(runCommands(ADD_CAMERA + "\nbye\n", file).contains("Added equipment:"));
        assertEquals(List.of(ADD_CAMERA), Files.readAllLines(file));

        String output = runCommands("add i/Tripod id/001 type/accessories cond/10\nbye\n", file);
        assertTrue(output.contains("Loaded 1 equipment record(s)."));
        assertTrue(output.contains("Equipment ID 001 already exists."));
        assertEquals(List.of(ADD_CAMERA), Files.readAllLines(file));
    }

    @Test
    void add_allowsSameNameWithDifferentIds() throws IOException {
        Path file = directory.resolve("equipment.txt");
        runCommands(ADD_CAMERA + "\n" + ADD_CAMERA.replace("001", "002") + "\nbye\n", file);
        assertEquals(2, EquipmentStorage.load(file).size());
    }

    @Test
    void add_acceptsBoundaryValuesAndMultiwordType() {
        String command = "add i/" + "a".repeat(100) + " id/999 type/audio accessories cond/10";
        assertEquals(command, Equipment.parse(command).toFileLine());
    }

    @Test
    void add_rejectsInvalidFieldsAndMissingPrefixes() {
        List<String> invalidCommands = List.of(
                "add i/" + "a".repeat(101) + " id/001 type/camera cond/1",
                "add i/ id/001 type/camera cond/1",
                "add i/Camera id/001 type/ cond/1",
                "add i/Camera id/001 cond/1",
                ADD_CAMERA.replace("id/001", "id/000"),
                ADD_CAMERA.replace("id/001", "id/1000"),
                ADD_CAMERA.replace("id/001", "id/1"),
                ADD_CAMERA.replace("id/001", "id/abc"),
                ADD_CAMERA.replace("cond/1", "cond/0"),
                ADD_CAMERA.replace("cond/1", "cond/11"),
                ADD_CAMERA.replace("cond/1", "cond/good"));
        for (String command : invalidCommands) {
            assertThrows(IllegalArgumentException.class, () -> Equipment.parse(command), command);
        }
    }

    @Test
    void deleteConfirmed_remainsDeletedAfterRestartAndAllowsIdReuse() throws IOException {
        Path file = directory.resolve("equipment.txt");
        runCommands(ADD_CAMERA + "\nbye\n", file);
        String output = runCommands("delete item/001\nyes\nbye\n", file);
        assertTrue(output.contains("Type yes to confirm."));
        assertTrue(output.contains("Deleted equipment:"));
        assertTrue(EquipmentStorage.load(file).isEmpty());
        assertTrue(runCommands("bye\n", file).contains("Loaded 0 equipment record(s)."));
        assertTrue(runCommands(ADD_CAMERA + "\nbye\n", file).contains("Added equipment:"));
    }

    @Test
    void deleteCancelledOrUnconfirmed_keepsSavedItem() throws IOException {
        Path file = directory.resolve("equipment.txt");
        runCommands(ADD_CAMERA + "\nbye\n", file);
        assertTrue(runCommands("delete item/001\nno\nbye\n", file).contains("Deletion cancelled."));
        assertTrue(runCommands("delete item/001\n", file).contains("Deletion cancelled."));
        assertEquals(List.of(ADD_CAMERA), Files.readAllLines(file));
    }

    @Test
    void invalidDelete_printsErrorThenSyntaxWithoutDeleting() throws IOException {
        Path file = directory.resolve("equipment.txt");
        runCommands(ADD_CAMERA + "\nbye\n", file);
        for (String command : List.of("delete", "delete loan/001", "delete item/1", "delete item/000",
                "delete item/1000", "delete item/abc", "delete item/001 extra")) {
            String output = runCommands(command + "\nbye\n", file);
            assertTrue(output.contains("[Error] Invalid delete syntax. ITEM ID must be from 001 to 999.\n"
                    .replace("\n", System.lineSeparator())
                    + "Delete: delete item/[ITEM ID]"));
            assertFalse(output.contains("Type yes to confirm."));
            assertEquals(List.of(ADD_CAMERA), Files.readAllLines(file));
        }
    }

    @Test
    void deleteUnknownId_reportsErrorWithoutChangingFile() throws IOException {
        Path file = directory.resolve("equipment.txt");
        runCommands(ADD_CAMERA + "\nbye\n", file);
        assertTrue(runCommands("delete item/999\nbye\n", file).contains("Equipment ID 999 does not exist."));
        assertEquals(List.of(ADD_CAMERA), Files.readAllLines(file));
    }

    @Test
    void damagedOrDuplicateSavedRecords_stopStartupWithoutOverwritingFile() throws IOException {
        Path file = directory.resolve("equipment.txt");
        for (String content : List.of("invalid record\n", ADD_CAMERA + "\n" + ADD_CAMERA + "\n")) {
            Files.writeString(file, content);
            String output = runCommands(ADD_CAMERA + "\nbye\n", file);
            assertTrue(output.contains("[Error] Could not load equipment."));
            assertEquals(content, Files.readString(file));
        }
    }

    @Test
    void saveFailure_doesNotReportSuccessOrKeepUnsavedItem() throws IOException {
        Path parent = directory.resolve("blocked");
        Path file = parent.resolve("equipment.txt");
        Files.writeString(parent, "This is a file, not a directory.");
        String output = runCommands(ADD_CAMERA + "\ndelete item/001\nbye\n", file);
        assertTrue(output.contains("[Error] Could not save equipment."));
        assertFalse(output.contains("Added equipment:"));
        assertTrue(output.contains("Equipment ID 001 does not exist."));
    }

    @Test
    void emptyUnknownAndInvalidCommands_showHelpAndAllowNextValidCommand() throws IOException {
        Path file = directory.resolve("equipment.txt");
        String output = runCommands("   \nunknown\ndeleteitem/001\nadd\n" + ADD_CAMERA + "\nbye\n", file);
        assertTrue(output.contains("[Error] No command entered."));
        assertTrue(output.contains("[Error] Unknown command."));
        assertFalse(output.lines().anyMatch(line -> line.startsWith("Error:")));
        assertTrue(output.contains("[Error] Invalid add syntax."));
        assertTrue(output.contains("Delete: delete item/[ITEM ID]"));
        assertTrue(output.contains("Added equipment:"));
        assertEquals(List.of(ADD_CAMERA), Files.readAllLines(file));
    }

    @Test
    void normalEndOfInput_exitsWithGoodbye() {
        assertTrue(runCommands("", directory.resolve("equipment.txt")).contains("Goodbye."));
    }

    @Test
    void inputReadFailure_reportsErrorRatherThanNormalExit() {
        Readable brokenInput = buffer -> {
            throw new IOException("Input connection lost.");
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Duke.run(new Scanner(brokenInput), new PrintStream(output, true, StandardCharsets.UTF_8),
                directory.resolve("equipment.txt"));
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("[Error] Could not read input. Input connection lost."));
        assertFalse(text.contains("Goodbye."));
    }

    @Test
    void unexpectedInputError_stopsCleanlyAndKeepsSavedRecords() throws IOException {
        Path file = directory.resolve("equipment.txt");
        runCommands(ADD_CAMERA + "\nbye\n", file);
        Scanner closedInput = new Scanner("delete item/001\nyes\n");
        closedInput.close();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Duke.run(closedInput, new PrintStream(output, true, StandardCharsets.UTF_8), file);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[Error] An unexpected error occurred."));
        assertEquals(List.of(ADD_CAMERA), Files.readAllLines(file));
    }

    @Test
    void failedFileReplacement_keepsExistingContentsAndRemovesTemporaryFile() throws IOException {
        Path file = directory.resolve("equipment.txt");
        Files.createDirectory(file);
        Path existingFile = file.resolve("existing.txt");
        Files.writeString(existingFile, "Keep this content.");
        assertThrows(IOException.class, () -> EquipmentStorage.save(file, List.of(Equipment.parse(ADD_CAMERA))));
        assertEquals("Keep this content.", Files.readString(existingFile));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    void unexpectedCommandError_allowsAnotherCommandAndKeepsOriginalItem() throws IOException {
        Path file = directory.resolve("equipment.txt");
        String secondItem = ADD_CAMERA.replace("001", "002");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // Simulate a one-off unexpected failure before deletion can modify the records.
        PrintStream failingOutput = new PrintStream(output, true, StandardCharsets.UTF_8) {
            private boolean failed;

            @Override
            public void println(String message) {
                if (!failed && message.startsWith("Delete ")) {
                    failed = true;
                    throw new IllegalStateException("Temporary confirmation display failure.");
                }
                super.println(message);
            }
        };
        String commands = ADD_CAMERA + "\ndelete item/001\n" + secondItem + "\nbye\n";
        Duke.run(new Scanner(commands), failingOutput, file);
        String text = output.toString(StandardCharsets.UTF_8);
        int errorPosition = text.indexOf("[Error] Could not complete the command. Please try again.");
        assertTrue(errorPosition >= 0);
        assertTrue(text.indexOf("> ", errorPosition) > errorPosition);
        assertTrue(text.contains("Added equipment: Canon EOS R10 (ID: 002)."), text);
        assertTrue(text.contains("Goodbye."));
        assertEquals(List.of(ADD_CAMERA, secondItem), Files.readAllLines(file));
    }

    @Test
    void invalidDelete_allowsNextValidAddAndDeleteInSameSession() throws IOException {
        Path file = directory.resolve("equipment.txt");
        String output = runCommands("delete item/abc\n" + ADD_CAMERA + "\ndelete item/001\nyes\nbye\n", file);
        assertTrue(output.contains("[Error] Invalid delete syntax."));
        assertTrue(output.contains("Added equipment:"));
        assertTrue(output.contains("Deleted equipment:"));
        assertTrue(output.contains("Goodbye."));
        assertTrue(EquipmentStorage.load(file).isEmpty());
    }
}
