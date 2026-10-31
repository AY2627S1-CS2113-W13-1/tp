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
import seedu.snap.ui.Help;

/** Executes the displayed help examples through the real router with each example's prerequisites. */
class HelpExamplesIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);

    /** Audited against every command branch in EqmManager.executeInput, including the member alias. */
    private static final Set<String> ROUTED_COMMANDS = Set.of("add", "list", "show", "delete item", "loan",
            "return", "delete loan", "update loan", "list loans", "view loan", "list borrower", "list member",
            "list overdue", "history", "filter", "update", "maintain", "add-bundle", "add-to-bundle",
            "remove-from-bundle", "list bundles", "history bundle", "backup", "restore", "help", "bye");
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
            manager.executeInput("add i/Added after backup id/004 type/tripod cond/1");
            field(manager, "equipment", EquipmentHandler.class).saveToStorage();
        }
        output.reset();

        manager.executeInput(input);

        assertFalse(printed().contains("Command not recognised"), example);
        assertFalse(printed().contains("Invalid format"), example);
        // Help prose describes the error label; only a line starting with it is a rejected command.
        assertFalse(printed().lines().anyMatch(line -> line.startsWith("[ERROR]")),
                example + System.lineSeparator() + printed());
        if (command.equals("bye")) {
            assertFalse(field(manager, "isRunning", Boolean.class));
            assertTrue(Files.isRegularFile(temporaryDirectory.resolve("data").resolve("equipment.txt")));
        } else {
            assertTrue(printed().contains(expectedResult(command)), example + System.lineSeparator() + printed());
        }
        assertResultingRecords(manager, command, backupDirectory);
    }

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
        return ROUTED_COMMANDS.stream()
                .filter(command -> input.equals(command) || input.startsWith(command + " ")
                        || input.startsWith(command + "/"))
                .max((left, right) -> Integer.compare(left.length(), right.length())).orElseThrow();
    }

    /** Supplies the existing IDs, active loan or bundle that an individual example requires. */
    private EqmManager prepareExample(String command) throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory.resolve("data"), FIXED_CLOCK);
        if (command.equals("add")) {
            return manager;
        }
        manager.executeInput("add i/Camera id/001 type/camera cond/1 sensor/APS-C mount/Canon RF");
        manager.executeInput("add i/Battery id/002 type/battery cond/2 capacity/2000mAh");
        manager.executeInput("add i/Lens id/003 type/lens cond/3 zoom/24-70mm mount/Canon RF sensor/APS-C");
        if (Set.of("add-to-bundle", "remove-from-bundle", "list bundles", "history bundle", "restore")
                .contains(command)) {
            manager.executeInput("add-bundle name/Studio kit");
            if (!command.equals("add-to-bundle")) {
                manager.executeInput("add-to-bundle id/001 001 002 003");
            }
        }
        if (Set.of("return", "delete loan", "update loan", "list loans", "view loan", "list borrower",
                "list member", "list overdue", "history", "history bundle", "backup", "restore")
                .contains(command)) {
            manager.executeInput("loan id/001 b/002 issued/24-10-26 due/27-10-26");
        }
        field(manager, "equipment", EquipmentHandler.class).saveToStorage();
        field(manager, "loans", LoanHandler.class).saveToStorage();
        field(manager, "bundles", BundleHandler.class).saveToStorage();
        return manager;
    }

    /** Checks successful results instead of merely accepting any response from the router. */
    private static String expectedResult(String command) {
        return switch (command) {
        case "add" -> "[OK] Equipment 001 added.";
        case "delete item" -> "[OK] Equipment 001 \"Camera\" deleted.";
        case "loan" -> "[OK] Loan 001 issued.";
        case "return" -> "[OK] Loan 001 returned.";
        case "delete loan" -> "[OK] Loan 001 deleted.";
        case "update loan" -> "[OK] Loan 001 updated.";
        case "list" -> "3 equipment items.";
        case "filter" -> "1 equipment item.";
        case "show" -> "Equipment 001";
        case "list loans" -> "LOAN ID";
        case "view loan" -> "Loan 001 condition assessments";
        case "list borrower", "list member" -> "Equipment currently held by borrower ID 002:";
        case "list overdue" -> "Overdue";
        case "history" -> "Loan history for item ID 001";
        case "update" -> "[OK] Equipment 001 updated.";
        case "maintain" -> "Maintenance recorded successfully.";
        case "add-bundle" -> "[OK] Bundle 001 \"Studio kit\" created.";
        case "add-to-bundle" -> "Equipment ID 003 added to bundle 001 successfully.";
        case "remove-from-bundle" -> "Equipment ID 001 removed from bundle 001 successfully.";
        case "list bundles" -> "Studio kit";
        case "history bundle" -> "Loan history for bundle 001 (Studio kit)";
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
        case "add" -> assertEquals("Canon EOS R10", equipment.getEquipmentById("001").getItemName());
        case "delete item" -> assertEquals(2, equipment.getEquipmentList().size());
        case "list" -> {
            assertTrue(printed().contains("Camera"));
            assertTrue(printed().contains("Battery"));
            assertTrue(printed().contains("Lens"));
        }
        case "filter" -> {
            assertTrue(printed().contains("Camera"));
            assertFalse(printed().contains("Battery"));
            assertFalse(printed().contains("Lens"));
        }
        case "list loans", "list borrower", "list member", "list overdue", "history", "history bundle" -> {
            assertTrue(printed().contains("| LOAN ID | ITEM ID | BORROWER ID | ISSUED   | DUE      | STATE"));
            assertTrue(printed().contains("| 001     | 001     | 002         | 24-10-26 | 27-10-26 | Overdue"));
            assertTrue(printed().contains("1 loan."));
        }
        case "loan" -> assertTrue(equipment.getEquipmentById("001").isLoaned());
        case "return" -> assertTrue(loans.getLoanList().getFirst().isReturned());
        case "delete loan" -> assertTrue(loans.getLoanList().isEmpty());
        case "update loan" -> {
            assertEquals("002", loans.getLoanList().getFirst().getItemId());
            assertEquals("007", loans.getLoanList().getFirst().getBorrowerId());
        }
        case "update" -> assertEquals("Studio camera", equipment.getEquipmentById("001").getItemName());
        case "maintain" -> assertEquals(1,
                field(manager, "maintenance", MaintenanceHandler.class).getMaintenanceRecords().size());
        case "add-bundle" -> assertEquals("Studio kit", bundles.getBundleList().getFirst().getBundleName());
        case "add-to-bundle" -> assertTrue(bundles.getBundleList().getFirst().isComplete());
        case "remove-from-bundle" -> assertFalse(bundles.containsEquipmentId("001"));
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
