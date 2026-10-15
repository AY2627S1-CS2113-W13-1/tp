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
    private void verifyEquipmentDiagnostics(boolean assertionsEnabled) throws Exception {
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String assertionOption = assertionsEnabled ? "-ea" : "-da";
        Process process = new ProcessBuilder(javaExecutable.toString(), assertionOption, "-cp", classes.toString(),
                Main.class.getName()).directory(temporaryDirectory.toFile()).redirectErrorStream(true).start();
        try {
            String commands = "add i/Camera id/001 type/camera cond/1\n"
                    + "add i/Tripod id/002 type/accessories cond/11\n"
                    + "add i/\u2003 id/002 type/camera cond/1\n"
                    + "add i/Camera id/002 type/\u2003 cond/1\nbye\n";
            try (var input = process.getOutputStream()) {
                input.write(commands.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "The application should exit after bye");
            assertEquals(0, process.exitValue());
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(output.contains("Equipment added successfully."));
            assertTrue(output.contains("Condition must be a whole number from 1 to 10."));
            assertTrue(output.contains("Item name must contain 1 to 100 characters."));
            assertTrue(output.contains("Type cannot be empty."));
            assertFalse(output.contains("Added equipment with item ID"));
            assertFalse(output.contains("Equipment addition rejected:"));

            String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
            assertTrue(logs.contains("Added equipment with item ID 001"));
            assertTrue(logs.contains("Equipment addition rejected: Condition must be a whole number from 1 to 10."));
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    void main_logDirectoryBlocked_warnsAndContinues() throws Exception {
        Files.writeString(temporaryDirectory.resolve("logs"), "This file prevents directory creation.");
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(javaExecutable.toString(), "-ea", "-cp", classes.toString(),
                Main.class.getName()).directory(temporaryDirectory.toFile()).redirectErrorStream(true).start();
        try {
            try (var input = process.getOutputStream()) {
                input.write("add i/Camera id/001 type/camera cond/1\nbye\n".getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Logging failure must not prevent normal exit");
            assertEquals(0, process.exitValue());
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(output.contains("Warning: Application logging is unavailable:"));
            assertTrue(output.contains("Equipment added successfully."));
            assertFalse(output.contains("Added equipment with item ID"));
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    void main_repeatedRuns_appendLogsAndReleaseLocks() throws Exception {
        runApplication("add i/Camera id/001 type/camera cond/1\nbye\n", "first");
        Path logFile = temporaryDirectory.resolve("logs/snap.log.0");
        String firstLog = Files.readString(logFile);
        runApplication("add i/Lens id/999 type/lens cond/10\nbye\n", "second");
        String secondLog = Files.readString(logFile);
        assertTrue(secondLog.startsWith(firstLog), "Restarting must append rather than erase existing logs");
        assertTrue(secondLog.contains("Added equipment with item ID 999"));
        try (var files = Files.list(temporaryDirectory.resolve("logs"))) {
            assertEquals(0L, files.filter(path -> path.toString().endsWith(".lck")).count());
        }
    }

    @Test
    void main_equipmentAndLoanCommands_shareLogsWithoutConsoleDiagnostics() throws Exception {
        String output = runApplication("add i/Camera id/001 type/camera cond/1\n"
                + "loan id/001 b/001 issued/15-10-26 due/16-10-26\n"
                + "delete-loan id/001\ndelete-loan id/999\nbye\n", "shared");
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

    @Test
    void main_manyCommands_rotatesLogsAndKeepsConsoleClean() throws Exception {
        StringBuilder commands = new StringBuilder();
        for (int i = 0; i < 12000; i++) {
            commands.append("add i/Camera id/001 type/camera cond/1\ndelete item/001\n");
        }
        commands.append("add i/Last camera id/999 type/camera cond/10\nbye\n");
        String output = runApplication(commands.toString(), "rotation");
        assertFalse(output.contains("Added equipment with item ID"));
        assertFalse(output.contains("Exception"));
        assertFalse(output.contains("AssertionError"));
        assertEquals(12001L, output.lines().filter(line -> line.contains("Equipment added successfully.")).count());

        Path logs = temporaryDirectory.resolve("logs");
        assertTrue(Files.size(logs.resolve("snap.log.1")) >= 1_000_000);
        assertTrue(Files.size(logs.resolve("snap.log.0")) < 1_001_000);
        assertTrue(Files.readString(logs.resolve("snap.log.0")).contains("Added equipment with item ID 999"));
        try (var files = Files.list(logs)) {
            assertEquals(2L, files.count(), "Only the two rotating files should remain after shutdown");
        }
    }

    /** Uses files for process input/output so a large command batch cannot fill a pipe and deadlock. */
    private String runApplication(String commands, String filePrefix) throws Exception {
        Path inputFile = temporaryDirectory.resolve(filePrefix + "-input.txt");
        Path outputFile = temporaryDirectory.resolve(filePrefix + "-output.txt");
        Files.writeString(inputFile, commands, StandardCharsets.UTF_8);
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(javaExecutable.toString(), "-ea", "-cp", classes.toString(),
                Main.class.getName()).directory(temporaryDirectory.toFile()).redirectErrorStream(true)
                .redirectInput(inputFile.toFile()).redirectOutput(outputFile.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "The command batch should finish within 30 seconds");
            assertEquals(0, process.exitValue(), "Application failure; inspect " + outputFile);
            return Files.readString(outputFile, StandardCharsets.UTF_8);
        } finally {
            process.destroyForcibly();
        }
    }
}
