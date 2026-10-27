package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

/** Tests storage-location commands, display output, loan preservation, and restart behaviour. */
class EquipmentStorageLocationIntegrationTest {
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
    void updateLocation_displayPreservesLoanAndSurvivesRestart() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("loan id/001 b/002 issued/24-10-26 due/31-10-26");
        output.reset();

        manager.executeInput("update id/001 location/Cabinet A-3");
        manager.executeInput("list");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Equipment with item ID 001 updated successfully."));
        assertTrue(printed.contains("LOCATION"));
        assertTrue(printed.contains("Cabinet A-3"));
        assertTrue(printed.contains("Loaned"));
        byte[] saved = Files.readAllBytes(temporaryDirectory.resolve("equipment.txt"));

        output.reset();
        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("list");

        String restartedOutput = output.toString(StandardCharsets.UTF_8);
        assertTrue(restartedOutput.contains("Cabinet A-3"));
        assertTrue(restartedOutput.contains("Loaned"));
        assertArrayEquals(saved, Files.readAllBytes(temporaryDirectory.resolve("equipment.txt")));
    }

    @Test
    void updateLocation_clearAndInvalidValue_preserveSavedStateUntilValidCommand() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("update id/001 location/Cabinet A-3");
        byte[] savedBeforeInvalid = Files.readAllBytes(temporaryDirectory.resolve("equipment.txt"));
        output.reset();

        manager.executeInput("update id/001 location/");
        assertArrayEquals(savedBeforeInvalid, Files.readAllBytes(temporaryDirectory.resolve("equipment.txt")));
        manager.executeInput("update id/001 location/NONE");
        manager.executeInput("list");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Field location/ cannot be empty."));
        assertTrue(printed.contains("Equipment with item ID 001 updated successfully."));
        assertFalse(printed.contains("LOCATION"), "Cleared locations must not remain in list output");
        assertFalse(printed.contains("Cabinet A-3"));
        assertTrue(Files.readString(temporaryDirectory.resolve("equipment.txt")).contains("location/\tadd"));
    }
}
