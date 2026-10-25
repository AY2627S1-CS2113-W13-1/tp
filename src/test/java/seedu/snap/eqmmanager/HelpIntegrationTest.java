package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.bundle.Bundle;
import seedu.snap.bundle.BundleHandler;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Verifies that help is discoverable and leaves application records, files and counters unchanged. */
class HelpIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
    private static final List<String> HELP_COMMANDS = List.of("help", "help all", "help add", "help list",
            "help delete item", "help loan", "help return", "help delete loan", "help list loans",
            "help list overdue", "help history", "help filter", "help update", "help add-bundle",
            "help add-to-bundle", "help list bundles", "help help", "help bye", "help unknown",
            "help list overdue extra", "help add i/Camera", "help Loan", "help all extra");
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private InputStream originalInput;
    private EqmManager manager;
    private Path dataDirectory;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() throws Exception {
        originalOutput = System.out;
        originalInput = System.in;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        dataDirectory = temporaryDirectory.resolve("data");
        manager = new EqmManager(dataDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/4 sensor/Full frame remarks/Camera checked");
        manager.executeInput("add i/Battery id/002 type/battery cond/5 capacity/2000mAh");
        manager.executeInput("add i/Lens id/003 type/lens cond/6 zoom/24-70mm");
        manager.executeInput("add i/Tripod id/004 type/tripod cond/7 thread/1-4 inch");
        manager.executeInput("add i/Workshop battery id/005 type/battery cond/3 capacity/1500mAh");
        manager.executeInput("add i/Spare battery id/006 type/battery cond/8 capacity/1800mAh");
        manager.executeInput("add-bundle name/Camera kit");
        manager.executeInput("add-to-bundle id/001 001 002 003 004");
        manager.executeInput("loan id/005 b/111 due/25-10-26");
        manager.executeInput("return loan/001 serviceable/no damage/SWELLING remarks/Needs repair");
        manager.executeInput("loan id/001 b/222 due/25-10-26");
        assertTrue(readField(manager, "bundles", BundleHandler.class).getBundleList().getFirst().isComplete());
        assertTrue(readField(manager, "equipment", EquipmentHandler.class).getEquipmentById("001").isLoaned());
        assertFalse(readField(manager, "equipment", EquipmentHandler.class)
                .getEquipmentById("005").isServiceable());
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
        System.setIn(originalInput);
    }

    @Test
    void help_populatedApplication_preservesEveryRecordReferenceValueAndSavedFile() throws Exception {
        for (Path path : storagePaths()) {
            Files.setLastModifiedTime(path, FileTime.fromMillis(1000));
        }
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> filesBefore = savedFiles(dataDirectory);
        List<Path> pathsBefore = treePaths(dataDirectory);

        executeAllHelpForms(manager, before);

        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(filesBefore);
        assertEquals(pathsBefore, treePaths(dataDirectory));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u2003\t help \u00a0\u2003", "help\u00a0", "help\t\u2003\n"})
    void help_outerUnicodeWhitespace_isNormalisedWithoutChangingRecords(String command) throws Exception {
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> filesBefore = savedFiles(dataDirectory);

        manager.executeInput(command);

        assertMenuPrinted();
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(filesBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HELP", "helper", "helpall", "helpful", "help/all", "help-loan"})
    void help_similarCommands_followUnknownCommandPathAndPreserveRecords(String command) throws Exception {
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> filesBefore = savedFiles(dataDirectory);
        List<Path> pathsBefore = treePaths(dataDirectory);

        manager.executeInput(command);

        assertTrue(printed().contains("Command not recognised"));
        assertTrue(printed().contains("Type help"));
        assertFalse(printed().contains("loan id/"));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(filesBefore);
        assertEquals(pathsBefore, treePaths(dataDirectory));
    }

    @Test
    void help_withoutTopic_showsCompactGroupedMenu() {
        manager.executeInput("help");

        assertMenuPrinted();
        assertFalse(printed().contains("Format: loan id/"));
        assertFalse(printed().contains("damage/SENSOR_DAMAGE"));
        assertTrue(printed().lines().count() <= 24, "The command menu should fit a normal terminal screen");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "add | Format: add i/<NAME>",
        "list | Format: list",
        "delete item | Format: delete item/<ID>",
        "loan | Format: loan id/<ITEM ID>",
        "return | Format: return loan/<LOAN ID>",
        "delete loan | Format: delete loan/<LOAN ID>",
        "list loans | Format: list loans",
        "list overdue | Format: list overdue",
        "history | Format: history id/<ITEM ID>",
        "filter | Format: filter [id/<ID>]",
        "update | Format: update id/<ID>",
        "add-bundle | Format: add-bundle name/<NAME>",
        "add-to-bundle | Format: add-to-bundle id/<BUNDLE ID>",
        "list bundles | Format: list bundles",
        "help | Format: help",
        "bye | Format: bye"
    })
    void help_commandTopic_showsOnlyRequestedCommandWithExample(String topic, String expectedFormat) {
        manager.executeInput("help " + topic);

        assertTrue(printed().contains(expectedFormat), topic);
        assertTrue(printed().contains("Example: " + topic), topic);
        assertFalse(printed().contains("Command not recognised"), topic);
        assertFalse(printed().contains("Unknown help topic"), topic);
        String unrelated = topic.equals("loan") ? "Format: add i/" : "Format: loan id/";
        assertFalse(printed().contains(unrelated), "Topic help should omit unrelated command sections");
    }

    @ParameterizedTest
    @ValueSource(strings = {"help loan", "\u2003help\tloan\u00a0", "help\u2003loan",
        "help\u00a0loan", "help\t\tloan"})
    void help_singleWordTopicWhitespace_showsLoanSyntax(String command) {
        manager.executeInput(command);

        assertTrue(printed().contains("Format: loan id/<ITEM ID>"));
        assertTrue(printed().contains("Omitted issued/ defaults to today's date"));
        assertFalse(printed().contains("Format: return loan/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help list overdue", "\u2003help\tlist\u00a0overdue\u2003",
        "help\u2003list\u2003overdue", "help\nlist\r\noverdue", "help  list   overdue"})
    void help_multiWordTopicWhitespace_showsExactOverdueTopic(String command) {
        manager.executeInput(command);

        assertTrue(printed().contains("Format: list overdue"));
        assertTrue(printed().contains("Returned loans and loans due today or later are excluded."));
        assertFalse(printed().contains("Format: list loans"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help all", "\u2003help\tall\u00a0", "help\u2003all"})
    void help_all_showsCompleteReference(String command) {
        manager.executeInput(command);

        assertTrue(printed().contains("Format: add i/<NAME>"));
        assertTrue(printed().contains("Format: loan id/<ITEM ID>"));
        assertTrue(printed().contains("Format: return loan/<LOAN ID>"));
        assertTrue(printed().contains("Format: add-to-bundle id/<BUNDLE ID>"));
        assertTrue(printed().contains("Format: bye"));
        assertTrue(printed().contains("Omitted issued/ defaults to today's date"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help unknown", "help list overdue extra", "help add i/Camera", "help Loan",
        "help all extra", "help loan/001", "help delete", "help list over"})
    void help_unknownTopic_showsErrorAndRecoverableMenu(String command) {
        manager.executeInput(command);

        assertTrue(printed().contains("Unknown help topic"));
        assertMenuPrinted();
        assertFalse(printed().contains("Command not recognised"));
        assertFalse(printed().contains("Format: loan id/"));
    }

    @Test
    void help_emptyWorkspace_createsNoDataDirectoriesOrFiles() throws Exception {
        Path missingDirectory = temporaryDirectory.resolve("empty").resolve("nested-data");
        EqmManager empty = new EqmManager(missingDirectory, FIXED_CLOCK);
        ApplicationState before = snapshot(empty);
        assertFalse(Files.exists(missingDirectory.getParent()));

        executeAllHelpForms(empty, before);

        assertEquals(before, snapshot(empty));
        assertFalse(Files.exists(missingDirectory));
        assertFalse(Files.exists(missingDirectory.getParent()));
        assertTrue(readField(empty, "loans", LoanHandler.class).getLoanList().isEmpty());
        assertTrue(readField(empty, "equipment", EquipmentHandler.class).getEquipmentList().isEmpty());
        assertTrue(readField(empty, "bundles", BundleHandler.class).getBundleList().isEmpty());
    }

    @Test
    void help_blockedStorage_usesOnlyCurrentMemoryAndKeepsNextIdentifiers() throws Exception {
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> originals = savedFiles(dataDirectory);
        for (Path path : storagePaths()) {
            Files.delete(path);
            Files.createDirectory(path);
            Files.writeString(path.resolve("keep.txt"), "Storage is blocked at " + path.getFileName());
        }
        Map<Path, SavedFile> blockedFiles = savedFiles(dataDirectory);
        List<Path> blockedPaths = treePaths(dataDirectory);

        executeAllHelpForms(manager, before);

        assertFalse(printed().contains("Could not save"));
        assertFalse(printed().contains("Could not load"));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(blockedFiles);
        assertEquals(blockedPaths, treePaths(dataDirectory));
        for (Path path : storagePaths()) {
            Files.delete(path.resolve("keep.txt"));
            Files.delete(path);
            Files.write(path, originals.get(path).contents());
            Files.setLastModifiedTime(path, originals.get(path).modified());
        }
        assertSavedFilesUnchanged(originals);
        output.reset();

        manager.executeInput("loan id/006 b/333 due/25-10-26");
        manager.executeInput("add-bundle name/Next kit");

        assertTrue(printed().contains("Loan 003 added successfully."));
        assertTrue(printed().contains("Bundle 002 added successfully."));
        assertEquals("004", readField(manager, "loans", LoanHandler.class).getNextLoanId());
        assertEquals(3, readField(manager, "loans", LoanHandler.class).getLoanCount());
        assertEquals("003", readField(manager, "equipment", EquipmentHandler.class)
                .getEquipmentById("006").getLoanId());
    }

    @Test
    void help_followedByOtherCommands_keepsTheSessionRunning() throws Exception {
        executeAllHelpForms(manager, snapshot(manager));
        output.reset();

        manager.executeInput("update id/006 i/Updated spare battery cond/9");
        manager.executeInput("list loans");
        manager.executeInput("history id/001");

        assertTrue(printed().contains("updated successfully"));
        assertTrue(printed().contains("BORROWER ID"));
        assertTrue(printed().contains("222"));
        assertEquals("Updated spare battery", readField(manager, "equipment", EquipmentHandler.class)
                .getEquipmentById("006").getItemName());
        assertTrue(readField(manager, "isRunning", Boolean.class));
    }

    @Test
    void run_helpThenBye_showsDiscoveryAndEndsOnlyAfterBye() throws Exception {
        System.setIn(new ByteArrayInputStream("help\nhelp loan\nhelp unknown\nhelp all\nhelp bye\nbye\n"
                .getBytes(StandardCharsets.UTF_8)));
        EqmManager interactive = new EqmManager(temporaryDirectory.resolve("interactive-data"), FIXED_CLOCK);

        interactive.run();

        assertTrue(printed().contains("Welcome to snAp."));
        assertTrue(printed().contains("Type help"));
        assertMenuPrinted();
        assertTrue(printed().contains("Format: loan id/<ITEM ID>"));
        assertTrue(printed().contains("Unknown help topic"));
        assertTrue(printed().contains("Goodbye."));
        assertFalse(readField(interactive, "isRunning", Boolean.class));
        assertTrue(printed().indexOf("Welcome") < printed().indexOf("loan id/"));
        assertTrue(printed().indexOf("loan id/") < printed().indexOf("Goodbye."));
    }

    @Test
    void run_invalidSavedData_preservesTheExistingStartupAbort() throws Exception {
        Path invalidDirectory = temporaryDirectory.resolve("invalid-data");
        Files.createDirectories(invalidDirectory);
        Path invalidLoans = invalidDirectory.resolve("loans.txt");
        String original = "This is not a loan record.\n";
        Files.writeString(invalidLoans, original);
        System.setIn(new ByteArrayInputStream("help\nbye\n".getBytes(StandardCharsets.UTF_8)));
        EqmManager invalid = new EqmManager(invalidDirectory, FIXED_CLOCK);

        invalid.run();

        assertTrue(printed().contains("Could not load saved data"));
        assertFalse(printed().contains("Welcome to snAp."));
        assertFalse(printed().contains("loan id/"));
        assertEquals(original, Files.readString(invalidLoans));
        assertFalse(Files.exists(invalidDirectory.resolve("equipment.txt")));
        assertFalse(Files.exists(invalidDirectory.resolve("loan-sequence.txt")));
    }

    /** Captures object references as well as their values, including counters that help must not consume. */
    private ApplicationState snapshot(EqmManager target) throws Exception {
        EquipmentHandler inventory = readField(target, "equipment", EquipmentHandler.class);
        LoanHandler loanHandler = readField(target, "loans", LoanHandler.class);
        BundleHandler bundleHandler = readField(target, "bundles", BundleHandler.class);
        List<EquipmentState> items = inventory.getEquipmentList().stream()
                .map(item -> new EquipmentState(item, item.getItemName(), item.getItemId(), item.getType(),
                        item.getRecordId(), item.getCondition(), item.getDetails(), item.getRemarks(),
                        item.isLoaned(), item.isServiceable(), item.getLoanId())).toList();
        List<LoanState> loanRecords = loanHandler.getLoanList().stream()
                .map(loan -> new LoanState(loan, loan.getLoanId(), loan.getItemId(), loan.getEquipmentRecordId(),
                        loan.getBorrowerId(), loan.getIssuedDate(), loan.getDueDate(), loan.isReturned())).toList();
        List<BundleState> bundleRecords = bundleHandler.getBundleList().stream()
                .map(bundle -> new BundleState(bundle, bundle.getBundleId(), bundle.getBundleName(),
                        bundle.isComplete(), List.copyOf(bundle.getEquipmentList()))).toList();
        return new ApplicationState(inventory, loanHandler, bundleHandler, items, loanRecords, bundleRecords,
                loanHandler.getLoanCount(), loanHandler.getNextLoanId(),
                readField(bundleHandler, "nextBundleNumber", Integer.class),
                readField(target, "isRunning", Boolean.class));
    }

    /** Reads implementation state for regression tests without adding production accessors. */
    private static <T> T readField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    /** Captures all regular files so help cannot silently save or create a replacement file. */
    private Map<Path, SavedFile> savedFiles(Path root) throws Exception {
        Map<Path, SavedFile> result = new LinkedHashMap<>();
        for (Path path : treePaths(root)) {
            if (Files.isRegularFile(path)) {
                result.put(path, new SavedFile(Files.readAllBytes(path), Files.getLastModifiedTime(path)));
            }
        }
        return result;
    }

    /** Compares exact bytes and timestamps, including sentinels when storage has been blocked. */
    private void assertSavedFilesUnchanged(Map<Path, SavedFile> expected) throws Exception {
        for (Map.Entry<Path, SavedFile> entry : expected.entrySet()) {
            assertArrayEquals(entry.getValue().contents(), Files.readAllBytes(entry.getKey()),
                    entry.getKey().toString());
            assertEquals(entry.getValue().modified(), Files.getLastModifiedTime(entry.getKey()),
                    entry.getKey().toString());
        }
    }

    /** Returns the full directory tree in a stable order to detect unexpected files or directories. */
    private List<Path> treePaths(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.sorted().toList();
        }
    }

    /** Returns the three persistent files used by the populated fixture. */
    private List<Path> storagePaths() {
        return List.of(dataDirectory.resolve("equipment.txt"), dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
    }

    /** Repeats every help form so command names in a topic cannot execute or consume identifiers. */
    private void executeAllHelpForms(EqmManager target, ApplicationState expected) throws Exception {
        for (int repetition = 0; repetition < 2; repetition++) {
            for (String command : HELP_COMMANDS) {
                output.reset();
                target.executeInput(command);
                assertFalse(printed().contains("Command not recognised"), command);
                assertFalse(printed().contains("Could not save"), command);
                assertFalse(printed().contains("Could not load"), command);
                assertTrue(printed().contains("help"), command);
                assertEquals(expected, snapshot(target), command);
            }
        }
    }

    /** Checks the discovery menu independently of the production text generator. */
    private void assertMenuPrinted() {
        assertTrue(printed().contains("help all"));
        assertTrue(printed().contains("help loan"));
        assertTrue(printed().contains("list overdue"));
        assertTrue(printed().contains("delete item"));
        assertTrue(printed().contains("add-to-bundle"));
        assertTrue(printed().contains("list bundles"));
    }

    /** Reads captured application output independently of line endings or table spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    /** Includes the identity and assessment of each physical item and its current loan ownership. */
    private record EquipmentState(Equipment reference, String name, String id, String type, String recordId,
            int condition, String details, String remarks, boolean loaned, boolean serviceable, String loanId) {
    }

    /** Includes both active and returned records so displaying help cannot alter loan history. */
    private record LoanState(Loan reference, String loanId, String itemId, String equipmentRecordId,
            String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate, boolean returned) {
    }

    /** Retains bundle item references and the derived composition state. */
    private record BundleState(Bundle reference, String id, String name, boolean complete, List<Equipment> items) {
    }

    /** Keeps complete in-memory state separate from file snapshots for precise failures. */
    private record ApplicationState(EquipmentHandler equipment, LoanHandler loans, BundleHandler bundles,
            List<EquipmentState> items, List<LoanState> loanRecords, List<BundleState> bundleRecords,
            int loanCount, String nextLoanId, int nextBundleNumber, boolean running) {
    }

    /** Retains file bytes and modification time before help is displayed. */
    private record SavedFile(byte[] contents, FileTime modified) {
    }
}
