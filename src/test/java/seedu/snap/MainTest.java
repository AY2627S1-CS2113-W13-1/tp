package seedu.snap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        // @@author yarnieee
        String commands = "eqm-add name/Camera e/E001 type/camera cond/1\n"
                + "eqm-add name/Tripod e/E002 type/tripod cond/11\n"
                + "eqm-add name/\u2003 e/E002 type/camera cond/1\n"
                + "eqm-add name/Camera e/E002 type/\u2003 cond/1\nexit\n";
        // @@author
        String output = runApplication(commands, areAssertionsEnabled);
        assertTrue(output.contains("#############"));
        assertTrue(output.indexOf("#############") < output.indexOf("Welcome to snAp."));
        assertFalse(output.contains("█"), "Redirected startup output should use the ASCII fallback by default.");
        assertFalse(output.contains("\u001B"), "Redirected startup output must not contain colour escape codes.");
        // @@author yarnieee
        assertTrue(output.contains("[OK] Equipment E001 added."));
        // @@author
        assertTrue(output.contains("Condition must be a whole number from 1 to 10."));
        assertTrue(output.contains("Item name must contain 1 to 100 characters."));
        assertTrue(output.contains("Type cannot be empty."));
        assertFalse(output.contains("Added equipment with item ID"));
        assertFalse(output.contains("Equipment addition rejected:"));

        String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
        // @@author yarnieee
        assertTrue(logs.contains("Added equipment with item ID E001"));
        // @@author
        assertTrue(logs.contains("Equipment addition rejected: Condition must be a whole number from 1 to 10."));
    }

    @Test
    void main_explicitUnicodeLogo_preservesBlocksThroughRedirectedOutput() throws Exception {
        // @@author yarnieee
        String output = runApplication("exit\n", true, "unicode");
        // @@author

        assertEquals(441, output.chars().filter(character -> character == '█').count());
        assertTrue(output.indexOf("█████████████") < output.indexOf("Welcome to snAp."));
        assertFalse(output.contains("\uFFFD"), "Redirected Unicode output must decode as UTF-8.");
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void main_logDirectoryBlocked_warnsAndContinues() throws Exception {
        Files.writeString(temporaryDirectory.resolve("logs"), "This file prevents directory creation.");
        // @@author yarnieee
        String output = runApplication("eqm-add name/Camera e/E001 type/camera cond/1\nexit\n", true);
        // @@author
        assertTrue(output.contains("Warning: Application logging is unavailable:"));
        // @@author yarnieee
        assertTrue(output.contains("[OK] Equipment E001 added."));
        // @@author
        assertFalse(output.contains("Added equipment with item ID"));
    }

    @Test
    void main_equipmentAndLoanCommands_shareLogsWithoutConsoleDiagnostics() throws Exception {
        // @@author yarnieee
        String output = runApplication("eqm-add name/Camera e/E001 type/camera cond/1\n"
                + "loan-add e/E001 u/U001 issued/15-10-26 due/16-10-26\n"
                + "loan-delete l/L001\nloan-delete l/L999\nexit\n", true);
        assertTrue(output.contains("[OK] Equipment E001 added."));
        assertTrue(output.contains("[OK] Loan L001 deleted."));
        assertTrue(output.contains("No loan found for loan ID L999."));
        // @@author
        assertFalse(output.contains("Added equipment with item ID"));
        assertFalse(output.contains("Deleted loan 001 for item ID"));
        assertFalse(output.contains("WARNING:"));
        assertFalse(output.contains("INFO:"));
        String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
        // @@author yarnieee
        for (String message : new String[]{"Added equipment with item ID E001", "Deleted loan L001 for item ID E001",
            "No loan found for loan ID L999"}) {
            // @@author
            assertEquals(1L, logs.lines().filter(line -> line.contains(message)).count());
        }
    }

    @Test
    void main_restart_loadsPersistedEquipmentAndLoans() throws Exception {
        // @@author yarnieee
        String firstRun = runApplication("eqm-add name/Camera e/E001 type/camera cond/1"
                // @@author
                + " sensor/Full frame damage/SHUTTER_FAULT\n"
                // @@author yarnieee
                + "loan-add e/E001 u/U002 issued/01-10-26 due/02-10-26\nexit\n", true);

        assertTrue(firstRun.contains("[OK] Equipment E001 added."));
        assertTrue(firstRun.contains("[OK] Loan L001 issued."));
        // @@author
        assertTrue(Files.exists(temporaryDirectory.resolve("data/equipment.txt")));
        assertTrue(Files.exists(temporaryDirectory.resolve("data/loans.txt")));
        assertTrue(Files.readString(temporaryDirectory.resolve("data/equipment.txt"))
                // @@author yarnieee
                .contains("add i/Camera id/E001 type/camera cond/1 sensor/Full frame damage/SHUTTER_FAULT"));
        // @@author
        assertTrue(Files.readString(temporaryDirectory.resolve("data/loans.txt"))
                // @@author yarnieee
                .contains("loan loanId/L001 id/E001 b/U002 issued/01-10-26 due/02-10-26 returned/false"));

        String secondRun = runApplication("eqm-list\nloan-list\neqm-show e/E001\nexit\n", true);
        // @@author

        assertTrue(secondRun.contains("Welcome to snAp. Loaded 1 equipment record(s) and 1 loan(s)."));
        assertTrue(secondRun.contains("Camera"));
        assertTrue(secondRun.contains("Sensor format: Full frame"));
        assertTrue(secondRun.contains("Damage: SHUTTER_FAULT"));
        // @@author yarnieee
        assertTrue(secondRun.contains("E001"));
        assertTrue(secondRun.contains("U002"));
        // @@author
    }

    @Test
    void main_restart_restoresAvailabilityAndPersistsLoanDeletion() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("equipment.txt"),
                // @@author yarnieee
                "add i/Camera id/E001 type/camera cond/7 sensor/Full frame damage/SHUTTER_FAULT\n"
                + "add i/Lens id/E002 type/lens cond/6 zoom/24 to 70 mm damage/NONE\n"
                + "add i/Battery id/E003 type/battery cond/5 capacity/2000 mAh damage/CONTACT_DAMAGE\n"
                + "add i/Tripod id/E004 type/tripod cond/4 thread/Quarter inch damage/NONE\n");
        // @@author
        Files.writeString(dataDirectory.resolve("loans.txt"),
                // @@author yarnieee
                "loan id/E001 b/U010 issued/01-10-26 due/02-10-26 returned/false\n"
                + "loan id/E002 b/U020 issued/01-10-26 due/02-10-26 returned/true\n"
                + "loan id/E003 b/U030 issued/01-10-26 due/02-10-26 returned/false\n"
                + "loan id/E004 b/U040 issued/01-10-26 due/02-10-26 returned/true\n");

        String firstRun = runApplication("eqm-list\nloan-delete l/L001\nexit\n", true);
        // @@author

        assertTrue(firstRun.contains("Welcome to snAp. Loaded 4 equipment record(s) and 4 loan(s)."));
        // @@author yarnieee
        assertTrue(firstRun.contains(equipmentRow("Camera", "E001", "camera", 7, "Loaned")));
        assertTrue(firstRun.contains(equipmentRow("Lens", "E002", "lens", 6, "Available")));
        assertTrue(firstRun.contains(equipmentRow("Battery", "E003", "battery", 5, "Loaned")));
        assertTrue(firstRun.contains(equipmentRow("Tripod", "E004", "tripod", 4, "Available")));
        assertTrue(firstRun.contains("[OK] Loan L001 deleted."));

        String secondRun = runApplication("eqm-list\nexit\n", true);
        // @@author

        assertTrue(secondRun.contains("Welcome to snAp. Loaded 4 equipment record(s) and 3 loan(s)."));
        // @@author yarnieee
        assertTrue(secondRun.contains(equipmentRow("Camera", "E001", "camera", 7, "Available")));
        assertTrue(secondRun.contains(equipmentRow("Lens", "E002", "lens", 6, "Available")));
        assertTrue(secondRun.contains(equipmentRow("Battery", "E003", "battery", 5, "Loaned")));
        assertTrue(secondRun.contains(equipmentRow("Tripod", "E004", "tripod", 4, "Available")));
        // @@author
        String savedLoans = Files.readString(dataDirectory.resolve("loans.txt"));
        // @@author yarnieee
        assertFalse(savedLoans.contains("loan loanId/L001 "));
        assertTrue(savedLoans.contains("loan loanId/L002 id/E002 b/U020 issued/01-10-26 due/02-10-26 returned/true"));
        assertTrue(savedLoans.contains("loan loanId/L003 id/E003 b/U030 issued/01-10-26 due/02-10-26 returned/false"));
        assertTrue(savedLoans.contains("loan loanId/L004 id/E004 b/U040 issued/01-10-26 due/02-10-26 returned/true"));
        // @@author
    }

    /** Associates each compact row's status with its equipment ID after restart. */
    private String equipmentRow(String name, String id, String type, int condition, String status) {
        // @@author yarnieee
        return String.format("| %-4s | %-48s | %-7s | %-9d | %-13s |%n", id, name, type, condition, status);
        // @@author
    }

    @Test
    void main_invalidStorageFiles_reportBothLoadErrorsAndLogEachFailure() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("equipment.txt"), "not an equipment record\n");
        Files.writeString(dataDirectory.resolve("loans.txt"), "not a loan record\n");

        // @@author yarnieee
        String output = runApplication("eqm-list\nexit\n", true);
        // @@author

        assertTrue(output.contains("Could not load saved data: equipment.txt:"));
        assertTrue(output.contains("loans.txt: Invalid loan record on line 1:"));
        assertFalse(output.contains("Welcome to snAp."));
        assertFalse(output.contains("█"), "A failed startup must not print the welcome logo.");
        assertFalse(output.contains("#############"), "A failed startup must not print the ASCII logo.");

        String logs = Files.readString(temporaryDirectory.resolve("logs/snap.log.0"));
        assertTrue(logs.contains("Could not load saved equipment data"));
        assertTrue(logs.contains("Could not load saved loan data"));
    }

    @ParameterizedTest
    @CsvSource({"always,,,true", "never,,,false", "auto,,,false", "always,1,,false", "always,,dumb,false"})
    void main_unknownHelp_usesConfiguredErrorColourAndKeepsTheSessionUsable(
            String colourMode, String noColour, String term, boolean colourExpected) throws Exception {
        Map<String, String> environment = Map.of("SNAP_COLOUR", colourMode,
                "NO_COLOR", noColour == null ? "" : noColour, "TERM", term == null ? "" : term);

        // @@author yarnieee
        String output = runApplication("help unknown\nexit\n", true, null, environment);
        // @@author

        String label = colourExpected ? "\u001b[31m[ERROR]\u001b[0m" : "[ERROR]";
        String error = label + " Unknown help topic. Choose a command from the menu.";
        assertTrue(output.lines().anyMatch(error::equals), "Unknown help must use the configured error label.");
        if (!colourExpected) {
            assertFalse(output.contains("\u001b"), "Plain output must not contain colour escape codes.");
        }
        int errorPosition = output.indexOf(error);
        assertTrue(output.indexOf("snAp command menu", errorPosition) > errorPosition);
        assertTrue(output.indexOf("Goodbye.", errorPosition) > errorPosition);
    }

    /** Runs the application with isolated files and a timeout so tests cannot hang on console I/O. */
    private String runApplication(String commands, boolean areAssertionsEnabled) throws Exception {
        return runApplication(commands, areAssertionsEnabled, null);
    }

    /** Runs with a selected logo mode while removing inherited terminal settings for repeatable output. */
    private String runApplication(String commands, boolean areAssertionsEnabled, String logoMode) throws Exception {
        return runApplication(commands, areAssertionsEnabled, logoMode, Map.of());
    }

    /** Applies explicit terminal settings to a separate JVM without changing the test process environment. */
    private String runApplication(String commands, boolean areAssertionsEnabled, String logoMode,
            Map<String, String> environment) throws Exception {
        Path inputFile = temporaryDirectory.resolve("input.txt");
        Path outputFile = temporaryDirectory.resolve("output.txt");
        Files.writeString(inputFile, commands, StandardCharsets.UTF_8);
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path resources = Path.of(Main.class.getResource("/logo.txt").toURI()).getParent();
        String classpath = classes + File.pathSeparator + resources;
        String assertionOption = areAssertionsEnabled ? "-ea" : "-da";
        ProcessBuilder processBuilder = new ProcessBuilder(javaExecutable.toString(), assertionOption,
                "-cp", classpath,
                Main.class.getName()).directory(temporaryDirectory.toFile()).redirectErrorStream(true)
                .redirectInput(inputFile.toFile()).redirectOutput(outputFile.toFile());
        processBuilder.environment().remove("SNAP_COLOUR");
        processBuilder.environment().remove("SNAP_LOGO");
        processBuilder.environment().remove("TERM");
        processBuilder.environment().remove("NO_COLOR");
        if (logoMode != null) {
            processBuilder.environment().put("SNAP_LOGO", logoMode);
        }
        processBuilder.environment().putAll(environment);
        Process process = processBuilder.start();
        try {
            // @@author yarnieee
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "The application should exit after the exit command");
            // @@author
            assertEquals(0, process.exitValue(), "Application failure; inspect " + outputFile);
            return Files.readString(outputFile, StandardCharsets.UTF_8);
        } finally {
            process.destroyForcibly();
        }
    }
}
