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
                + "add i/Tripod id/002 type/tripod cond/11\n"
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
                + "delete loan/001\ndelete loan/999\nbye\n", true);
        assertTrue(output.contains("Equipment added successfully."));
        assertTrue(output.contains("Loan 001 for item ID 001 deleted successfully."));
        assertTrue(output.contains("No loan found for loan ID 999."));
        assertFalse(output.contains("Added equipment with item ID"));
        assertFalse(output.contains("Deleted loan 001 for item ID"));
        assertFalse(output.contains("WARNING:"));
        assertFalse(output.contains("INFO:"));
        String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
        for (String message : new String[]{"Added equipment with item ID 001", "Deleted loan 001 for item ID 001",
            "No loan found for loan ID 999"}) {
            assertEquals(1L, logs.lines().filter(line -> line.contains(message)).count());
        }
    }

    @Test
    void main_restart_loadsPersistedEquipmentAndLoans() throws Exception {
        String firstRun = runApplication("add i/Camera id/001 type/camera cond/1"
                + " sensor/Full frame damage/SHUTTER_FAULT\n"
                + "loan id/001 b/002 issued/01-10-26 due/02-10-26\nbye\n", true);

        assertTrue(firstRun.contains("Equipment added successfully."));
        assertTrue(firstRun.contains("Loan 001 added successfully."));
        assertTrue(Files.exists(temporaryDirectory.resolve("data/equipment.txt")));
        assertTrue(Files.exists(temporaryDirectory.resolve("data/loans.txt")));
        assertTrue(Files.readString(temporaryDirectory.resolve("data/equipment.txt"))
                .contains("add i/Camera id/001 type/camera cond/1 sensor/Full frame damage/SHUTTER_FAULT"));
        assertTrue(Files.readString(temporaryDirectory.resolve("data/loans.txt"))
                .contains("loan loanId/001 id/001 b/002 issued/01-10-26 due/02-10-26 returned/false"));

        String secondRun = runApplication("list\nlist loans\nbye\n", true);

        assertTrue(secondRun.contains("Welcome to snAp. Loaded 1 equipment record(s) and 1 loan(s)."));
        assertTrue(secondRun.contains("Camera"));
        assertTrue(secondRun.contains("sensor/Full frame damage/SHUTTER_FAULT"));
        assertTrue(secondRun.contains("001"));
        assertTrue(secondRun.contains("002"));
    }

    @Test
    void main_restart_restoresAvailabilityAndPersistsLoanDeletion() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("equipment.txt"),
                "add i/Camera id/001 type/camera cond/7 sensor/Full frame damage/SHUTTER_FAULT\n"
                + "add i/Lens id/002 type/lens cond/6 zoom/24 to 70 mm damage/NONE\n"
                + "add i/Battery id/003 type/battery cond/5 capacity/2000 mAh damage/CONTACT_DAMAGE\n"
                + "add i/Tripod id/004 type/tripod cond/4 thread/Quarter inch damage/NONE\n");
        Files.writeString(dataDirectory.resolve("loans.txt"),
                "loan id/001 b/010 issued/01-10-26 due/02-10-26 returned/false\n"
                + "loan id/002 b/020 issued/01-10-26 due/02-10-26 returned/true\n"
                + "loan id/003 b/030 issued/01-10-26 due/02-10-26 returned/false\n"
                + "loan id/004 b/040 issued/01-10-26 due/02-10-26 returned/true\n");

        String firstRun = runApplication("list\ndelete loan/001\nbye\n", true);

        assertTrue(firstRun.contains("Welcome to snAp. Loaded 4 equipment record(s) and 4 loan(s)."));
        assertTrue(firstRun.contains(equipmentRow("1)", "Camera", "001", "camera", 7,
                "Loaned", "sensor/Full frame damage/SHUTTER_FAULT")));
        assertTrue(firstRun.contains(equipmentRow("2)", "Lens", "002", "lens", 6,
                "Available", "zoom/24 to 70 mm damage/NONE")));
        assertTrue(firstRun.contains(equipmentRow("3)", "Battery", "003", "battery", 5,
                "Loaned", "capacity/2000 mAh damage/CONTACT_DAMAGE")));
        assertTrue(firstRun.contains(equipmentRow("4)", "Tripod", "004", "tripod", 4,
                "Available", "thread/Quarter inch damage/NONE")));
        assertTrue(firstRun.contains("Loan 001 for item ID 001 deleted successfully."));

        String secondRun = runApplication("list\nbye\n", true);

        assertTrue(secondRun.contains("Welcome to snAp. Loaded 4 equipment record(s) and 3 loan(s)."));
        assertTrue(secondRun.contains(equipmentRow("1)", "Camera", "001", "camera", 7,
                "Available", "sensor/Full frame damage/SHUTTER_FAULT")));
        assertTrue(secondRun.contains(equipmentRow("2)", "Lens", "002", "lens", 6,
                "Available", "zoom/24 to 70 mm damage/NONE")));
        assertTrue(secondRun.contains(equipmentRow("3)", "Battery", "003", "battery", 5,
                "Loaned", "capacity/2000 mAh damage/CONTACT_DAMAGE")));
        assertTrue(secondRun.contains(equipmentRow("4)", "Tripod", "004", "tripod", 4,
                "Available", "thread/Quarter inch damage/NONE")));
        String savedLoans = Files.readString(dataDirectory.resolve("loans.txt"));
        assertFalse(savedLoans.contains("loan loanId/001 "));
        assertTrue(savedLoans.contains("loan loanId/002 id/002 b/020 issued/01-10-26 due/02-10-26 returned/true"));
        assertTrue(savedLoans.contains("loan loanId/003 id/003 b/030 issued/01-10-26 due/02-10-26 returned/false"));
        assertTrue(savedLoans.contains("loan loanId/004 id/004 b/040 issued/01-10-26 due/02-10-26 returned/true"));
    }

    /** Formats one complete row so restart checks associate each status with its equipment ID. */
    private String equipmentRow(String number, String name, String id, String type, int condition,
            String status, String details) {
        return String.format("%-4s %-24s | %-6s | %-15s | %-9d | %-10s | %-30s | %-30s%n",
                number, name, id, type, condition, status, details, "");
    }

    @Test
    void main_invalidStorageFiles_reportBothLoadErrorsAndLogEachFailure() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("equipment.txt"), "not an equipment record\n");
        Files.writeString(dataDirectory.resolve("loans.txt"), "not a loan record\n");

        String output = runApplication("list\n", true);

        assertTrue(output.contains("Could not load saved data: equipment.txt:"));
        assertTrue(output.contains("loans.txt: Invalid loan record on line 1:"));
        assertFalse(output.contains("Welcome to snAp."));

        String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
        assertTrue(logs.contains("Could not load saved equipment data"));
        assertTrue(logs.contains("Could not load saved loan data"));
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
