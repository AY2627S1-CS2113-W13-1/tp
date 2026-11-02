package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.bundle.BundleHandler;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.loan.LoanHandler;
import seedu.snap.maintenance.MaintenanceHandler;
// @@author yarnieee
import seedu.snap.parser.CommandAliases;
// @@author
import seedu.snap.ui.Help;

/** Executes the displayed help examples through the real router with each example's prerequisites. */
class HelpExamplesIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);

    // @@author yarnieee
    /** Audited against every documented command branch in EqmManager.executeInput. */
    private static final Set<String> ROUTED_COMMANDS = Set.of("eqm-add", "eqm-list", "eqm-show", "eqm-delete",
            "loan-add",
            "loan-return", "loan-delete", "loan-update", "loan-list", "loan-view", "loan-show", "loan-list-borrower",
            "loan-list-overdue", "eqm-loan-history", "eqm-filter", "eqm-update", "eqm-maintain", "bundle-create",
            "bundle-add-eqm", "bundle-remove-eqm", "bundle-list", "bundle-history", "backup", "restore", "help",
            "exit");
    // @@author
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void captureOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void examples_coverEveryAuditedCommandRouteAndAlias() {
        Set<String> documented = examples().map(HelpExamplesIntegrationTest::commandName).collect(Collectors.toSet());

        assertEquals(ROUTED_COMMANDS, documented,
                "Each command route and alias needs an executable example, including newly merged commands.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void helpExample_withRequiredRecords_executesSuccessfully(String example) throws Exception {
        // @@author yarnieee
        executeExample(example);
        // @@author
    }

    @ParameterizedTest(name = "{0}")
    // @@author yarnieee
    @MethodSource("aliasExamples")
    void shortAlias_withRequiredRecords_executesSuccessfully(String example) throws Exception {
        executeExample(example);
    }

    /** Executes a canonical or alias example through the real command router. */
    private void executeExample(String example) throws Exception {
        // @@author
        String command = commandName(example);
        EqmManager manager = prepareExample(command);
        Path backupDirectory = temporaryDirectory.resolve("backups").resolve("session-1");
        String input = example;
        if (command.equals("backup") || command.equals("restore")) {
            // Keep the displayed path syntax but put file operations inside this test's temporary directory.
            input = example.substring(0, example.indexOf("path/") + "path/".length()) + backupDirectory;
        }
        if (command.equals("restore")) {
            manager.executeInput("backup path/" + backupDirectory);
            assertTrue(printed().contains("Backup created successfully"));
            // @@author yarnieee
            manager.executeInput("eqm-add name/Added after backup e/E004 type/tripod cond/1");
            // @@author
            field(manager, "equipment", EquipmentHandler.class).saveToStorage();
        }
        output.reset();

        manager.executeInput(input);

        assertFalse(printed().contains("Command not recognised"), example);
        assertFalse(printed().contains("Invalid format"), example);
        // Help prose describes the error label; only a line starting with it is a rejected command.
        assertFalse(printed().lines().anyMatch(line -> line.startsWith("[ERROR]")),
                example + System.lineSeparator() + printed());
        // @@author yarnieee
        if (command.equals("exit")) {
            // @@author
            assertFalse(field(manager, "isRunning", Boolean.class));
            assertTrue(Files.isRegularFile(temporaryDirectory.resolve("data").resolve("equipment.txt")));
        } else {
            assertTrue(printed().contains(expectedResult(command)), example + System.lineSeparator() + printed());
        }
        assertResultingRecords(manager, command, backupDirectory);
    }

    // @@author yarnieee
    /** Replaces the command token in each displayed example with its exact short alias. */
    private static Stream<String> aliasExamples() {
        return examples().map(example -> {
            // @@author
            String command = commandName(example);
            // @@author yarnieee
            return CommandAliases.aliasFor(command) + example.substring(command.length());
        });
    }

    // @@author
    /** Rejoins wrapped examples exactly as users are instructed to enter them, stopping at the blank line. */
    private static Stream<String> examples() {
        List<String> examples = new ArrayList<>();
        List<String> lines = Help.getText().lines().toList();
        for (int index = 0; index < lines.size(); index++) {
            if (!lines.get(index).equals("Example:")) {
                continue;
            }
            StringBuilder example = new StringBuilder();
            while (++index < lines.size() && !lines.get(index).isBlank()) {
                if (!example.isEmpty()) {
                    example.append(' ');
                }
                example.append(lines.get(index).strip());
            }
            examples.add(example.toString());
        }
        return examples.stream();
    }

    /** Matches the longest audited command name so specialised routes are distinct from list and update. */
    private static String commandName(String input) {
        // @@author yarnieee
        String canonicalInput = CommandAliases.normalizeInput(input);
        // @@author
        return ROUTED_COMMANDS.stream()
                // @@author yarnieee
                .filter(command -> canonicalInput.equals(command) || canonicalInput.startsWith(command + " ")
                        || canonicalInput.startsWith(command + "/"))
                // @@author
                .max((left, right) -> Integer.compare(left.length(), right.length())).orElseThrow();
    }

    /** Supplies the existing IDs, active loan or bundle that an individual example requires. */
    private EqmManager prepareExample(String command) throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory.resolve("data"), FIXED_CLOCK);
        // @@author yarnieee
        if (command.equals("eqm-add")) {
            // @@author
            return manager;
        }
        // @@author yarnieee
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1 sensor/APS-C mount/Canon RF");
        manager.executeInput("eqm-add name/Battery e/E002 type/battery cond/2 capacity/2000mAh");
        manager.executeInput("eqm-add name/Lens e/E003 type/lens cond/3 zoom/24-70mm mount/Canon RF sensor/APS-C");
        if (Set.of("bundle-add-eqm", "bundle-remove-eqm", "bundle-list", "bundle-history", "restore")
                // @@author
                .contains(command)) {
            // @@author yarnieee
            manager.executeInput("bundle-create name/Studio kit");
            if (!command.equals("bundle-add-eqm")) {
                manager.executeInput("bundle-add-eqm b/B001 E001 E002 E003");
                // @@author
            }
        }
        // @@author yarnieee
        if (Set.of("loan-return", "loan-delete", "loan-update", "loan-list", "loan-view", "loan-show",
                "loan-list-borrower",
                "loan-list-overdue", "eqm-loan-history", "bundle-history", "backup", "restore")
                // @@author
                .contains(command)) {
            // @@author yarnieee
            manager.executeInput("loan-add e/E001 u/U002 issued/24-10-26 due/27-10-26");
            // @@author
        }
        field(manager, "equipment", EquipmentHandler.class).saveToStorage();
        field(manager, "loans", LoanHandler.class).saveToStorage();
        field(manager, "bundles", BundleHandler.class).saveToStorage();
        return manager;
    }

    /** Checks successful results instead of merely accepting any response from the router. */
    private static String expectedResult(String command) {
        return switch (command) {
        // @@author yarnieee
        case "eqm-add" -> "[OK] Equipment E001 added.";
        case "eqm-delete" -> "[OK] Equipment E001 \"Camera\" deleted.";
        case "loan-add" -> "[OK] Loan L001 issued.";
        case "loan-return" -> "[OK] Loan L001 returned.";
        case "loan-delete" -> "[OK] Loan L001 deleted.";
        case "loan-update" -> "[OK] Loan L001 updated.";
        case "eqm-list" -> "3 equipment items.";
        case "eqm-filter" -> "1 equipment item.";
        case "eqm-show" -> "Equipment E001";
        case "loan-list" -> "LOAN ID";
        case "loan-view" -> "Loan L001 condition assessments";
        case "loan-show" -> "Loan L001 issue-time condition:";
        case "loan-list-borrower", "list member" -> "Equipment currently held by borrower ID U002:";
        case "loan-list-overdue" -> "Overdue";
        case "eqm-loan-history" -> "Loan history for item ID E001";
        case "eqm-update" -> "[OK] Equipment E001 updated.";
        case "eqm-maintain" -> "Maintenance recorded successfully.";
        case "bundle-create" -> "[OK] Bundle B001 \"Studio kit\" created.";
        case "bundle-add-eqm" -> "Equipment ID E003 added to bundle B001 successfully.";
        case "bundle-remove-eqm" -> "Equipment ID E001 removed from bundle B001 successfully.";
        case "bundle-list" -> "Studio kit";
        case "bundle-history" -> "Loan history for bundle B001 (Studio kit)";
        // @@author
        case "backup" -> "Backup created successfully";
        case "restore" -> "Backup restored successfully";
        case "help" -> "snAp command";
        default -> throw new IllegalArgumentException("Missing result check for " + command);
        };
    }

    /** Confirms the important mutations and backup artefacts in addition to the visible success message. */
    private void assertResultingRecords(EqmManager manager, String command, Path backupDirectory) throws Exception {
        EquipmentHandler equipment = field(manager, "equipment", EquipmentHandler.class);
        LoanHandler loans = field(manager, "loans", LoanHandler.class);
        BundleHandler bundles = field(manager, "bundles", BundleHandler.class);
        switch (command) {
        // @@author yarnieee
        case "eqm-add" -> assertEquals("Canon EOS R10", equipment.getEquipmentById("E001").getItemName());
        case "eqm-delete" -> assertEquals(2, equipment.getEquipmentList().size());
        case "eqm-list" -> {
            // @@author
            assertTrue(printed().contains("Camera"));
            assertTrue(printed().contains("Battery"));
            assertTrue(printed().contains("Lens"));
        }
        // @@author yarnieee
        case "eqm-filter" -> {
            // @@author
            assertTrue(printed().contains("Camera"));
            assertFalse(printed().contains("Battery"));
            assertFalse(printed().contains("Lens"));
        }
        // @@author yarnieee
        case "loan-list", "loan-list-borrower", "loan-list-overdue", "eqm-loan-history", "bundle-history" -> {
            // @@author
            assertTrue(printed().contains("| LOAN ID | ITEM ID | BORROWER ID | ISSUED   | DUE      | STATE"));
            // @@author yarnieee
            assertTrue(printed().contains("| L001    | E001    | U002        | 24-10-26 | 27-10-26 | Overdue"));
            // @@author
            assertTrue(printed().contains("1 loan."));
        }
        // @@author yarnieee
        case "loan-add" -> assertTrue(equipment.getEquipmentById("E001").isLoaned());
        case "loan-return" -> assertTrue(loans.getLoanList().getFirst().isReturned());
        case "loan-delete" -> assertTrue(loans.getLoanList().isEmpty());
        case "loan-update" -> {
            assertEquals("E002", loans.getLoanList().getFirst().getItemId());
            assertEquals("U007", loans.getLoanList().getFirst().getBorrowerId());
            // @@author
        }
        // @@author yarnieee
        case "eqm-update" -> assertEquals("Studio camera", equipment.getEquipmentById("E001").getItemName());
        case "eqm-maintain" -> assertEquals(1,
                // @@author
                field(manager, "maintenance", MaintenanceHandler.class).getMaintenanceRecords().size());
        // @@author yarnieee
        case "bundle-create" -> assertEquals("Studio kit", bundles.getBundleList().getFirst().getBundleName());
        case "bundle-add-eqm" -> assertTrue(bundles.getBundleList().getFirst().isComplete());
        case "bundle-remove-eqm" -> assertFalse(bundles.containsEquipmentId("E001"));
        // @@author
        case "backup" -> assertTrue(Files.isRegularFile(backupDirectory.resolve("backup-manifest.txt")));
        case "restore" -> {
            assertEquals(3, equipment.getEquipmentList().size());
            assertTrue(bundles.getBundleList().isEmpty());
            assertEquals(1, loans.getLoanCount());
        }
        default -> { }
        }
    }

    /** Reads state for assertions without adding production accessors solely for this audit. */
    private static <T> T field(EqmManager manager, String name, Class<T> type) throws Exception {
        Field field = EqmManager.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(manager));
    }

    /** Returns the exact captured command output. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
