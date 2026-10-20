package seedu.snap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests shared application logging and equipment diagnostics in separate JVM processes. */
class MainTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void main_validAndInvalidAdditions_writesLogsWithoutConsoleDiagnostics() throws Exception {
        verifyEquipmentDiagnostics(true);
    }

    @Test
    void main_assertionsDisabled_stillValidatesCommandsAndWritesLogs() throws Exception {
        verifyEquipmentDiagnostics(false);
    }

    /** Verifies that user validation and diagnostic logging work independently of Java assertions. */
    private void verifyEquipmentDiagnostics(boolean areAssertionsEnabled) throws Exception {
        String commands = "add i/Camera id/001 type/camera cond/1\n"
                + "add i/Tripod id/002 type/accessories cond/11\n"
                + "add i/\u2003 id/002 type/camera cond/1\n"
                + "add i/Camera id/002 type/\u2003 cond/1\nbye\n";
        String output = runApplication(commands, areAssertionsEnabled);
        assertTrue(output.contains("Equipment added successfully."));
        assertTrue(output.contains("Condition must be a whole number from 1 to 10."));
        assertTrue(output.contains("Item name must contain 1 to 100 characters."));
        assertTrue(output.contains("Type cannot be empty."));
        assertFalse(output.contains("Added equipment with item ID"));
        assertFalse(output.contains("Equipment addition rejected:"));

        String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
        assertTrue(logs.contains("Added equipment with item ID 001"));
        assertTrue(logs.contains("Equipment addition rejected: Condition must be a whole number from 1 to 10."));
    }

    @Test
    void main_logDirectoryBlocked_warnsAndContinues() throws Exception {
        Files.writeString(temporaryDirectory.resolve("logs"), "This file prevents directory creation.");
        String output = runApplication("add i/Camera id/001 type/camera cond/1\nbye\n", true);
        assertTrue(output.contains("Warning: Application logging is unavailable:"));
        assertTrue(output.contains("Equipment added successfully."));
        assertFalse(output.contains("Added equipment with item ID"));
    }

    @Test
    void main_equipmentAndLoanCommands_shareLogsWithoutConsoleDiagnostics() throws Exception {
        String output = runApplication("add i/Camera id/001 type/camera cond/1\n"
                + "loan id/001 b/001 issued/15-10-26 due/16-10-26\n"
                + "delete-loan id/001\ndelete-loan id/999\nbye\n", true);
        assertTrue(output.contains("Equipment added successfully."));
        assertTrue(output.contains("Loan for item ID 001 deleted successfully."));
        assertTrue(output.contains("No loan found for item ID 999."));
        assertFalse(output.contains("Added equipment with item ID"));
        assertFalse(output.contains("Deleted loan for item ID"));
        assertFalse(output.contains("WARNING:"));
        assertFalse(output.contains("INFO:"));
        String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
        for (String message : new String[]{"Added equipment with item ID 001", "Deleted loan for item ID 001",
            "No loan found for item ID 999"}) {
            assertEquals(1L, logs.lines().filter(line -> line.contains(message)).count());
        }
    }

    /** Runs the application with isolated files and a timeout so tests cannot hang on console I/O. */
    private String runApplication(String commands, boolean areAssertionsEnabled) throws Exception {
        Path inputFile = temporaryDirectory.resolve("input.txt");
        Path outputFile = temporaryDirectory.resolve("output.txt");
        Files.writeString(inputFile, commands, StandardCharsets.UTF_8);
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String assertionOption = areAssertionsEnabled ? "-ea" : "-da";
        Process process = new ProcessBuilder(javaExecutable.toString(), assertionOption, "-cp", classes.toString(),
                Main.class.getName()).directory(temporaryDirectory.toFile()).redirectErrorStream(true)
                .redirectInput(inputFile.toFile()).redirectOutput(outputFile.toFile()).start();
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "The application should exit after bye");
            assertEquals(0, process.exitValue(), "Application failure; inspect " + outputFile);
            return Files.readString(outputFile, StandardCharsets.UTF_8);
        } finally {
            process.destroyForcibly();
        }
    }
}
