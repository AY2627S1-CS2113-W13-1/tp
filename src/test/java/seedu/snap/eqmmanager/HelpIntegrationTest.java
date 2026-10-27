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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
import seedu.snap.loan.EquipmentConditionSnapshot;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Verifies that help and equipment views leave application records, files and counters unchanged. */
class HelpIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
    private static final List<String> HELP_COMMANDS = List.of("help", "help all", "help add", "help list", "help show",
            "help delete item", "help loan", "help return", "help delete loan", "help update loan", "help list loans",
            "help view loan",
            "help list overdue", "help history", "help filter", "help update", "help add-bundle",
            "help add-to-bundle", "help remove-from-bundle", "help list bundles", "help history bundle",
            "help help", "help bye", "help unknown",
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
        readField(manager, "bundles", BundleHandler.class).saveToStorage();
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

        executeAllReadOnlyForms(manager, before);

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

        assertTrue(printedContent().contains("Command not recognised"));
        assertTrue(printedContent().contains("Type help"));
        assertFalse(printedContent().contains("loan id/"));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(filesBefore);
        assertEquals(pathsBefore, treePaths(dataDirectory));
    }

    @Test
    void help_withoutTopic_showsCompactGroupedMenu() {
        manager.executeInput("help");

        assertMenuPrinted();
        assertFalse(printedContent().contains("Format: loan id/"));
        assertFalse(printedContent().contains("damage/SENSOR_DAMAGE"));
        assertTrue(printed().lines().count() <= 24, "The command menu should fit a normal terminal screen");
    }

    @Test
    void help_all_wrapsTextAndKeepsCommandBlocksSeparate() {
        manager.executeInput("help all");

        List<String> lines = printed().lines().toList();
        assertTrue(lines.stream().allMatch(line -> line.length() <= 100));
        assertEquals(28, lines.stream().filter("Format:"::equals).count());
        assertEquals(28, lines.stream().filter("Example:"::equals).count());
        assertTrue(printed().contains(System.lineSeparator() + System.lineSeparator() + "Format:"
                + System.lineSeparator() + "  "));
        assertTrue(printed().contains(System.lineSeparator() + System.lineSeparator() + "Example:"
                + System.lineSeparator() + "  "));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "add | Format: add i/<NAME>",
        "list | Format: list",
        "show | Format: show id/<ID>",
        "delete item | Format: delete item/<ID>",
        "loan | Format: loan id/<ITEM ID>",
        "return | Format: return loan/<LOAN ID>",
        "delete loan | Format: delete loan/<LOAN ID>",
        "update loan | Format: update loan/<LOAN ID>",
        "list loans | Format: list loans",
        "view loan | Format: view loan/<LOAN ID>",
        "backup | Format: backup path/<DIRECTORY>",
        "restore | Format: restore path/<DIRECTORY>",
        "list borrower | Format: list borrower/<BORROWER ID>",
        "list overdue | Format: list overdue",
        "history | Format: history id/<ITEM ID>",
        "filter | Format: filter [id/<ID>]",
        "update | Format: update id/<ID>",
        "maintain | Format: maintain id/<ITEM ID>",
        "add-bundle | Format: add-bundle name/<NAME>",
        "add-to-bundle | Format: add-to-bundle id/<BUNDLE ID>",
        "remove-from-bundle | Format: remove-from-bundle id/<BUNDLE ID>",
        "list bundles | Format: list bundles",
        "history bundle | Format: history bundle/id/<BUNDLE ID>",
        "help | Format: help",
        "bye | Format: bye"
    })
    void help_commandTopic_showsOnlyRequestedCommandWithExample(String topic, String expectedFormat) {
        manager.executeInput("help " + topic);

        assertTrue(printedContent().contains(expectedFormat), topic);
        assertTrue(printedContent().contains("Example: " + topic), topic);
        assertFalse(printedContent().contains("Command not recognised"), topic);
        assertFalse(printedContent().contains("Unknown help topic"), topic);
        String unrelated = topic.equals("loan") ? "Format: add i/" : "Format: loan id/";
        assertFalse(printedContent().contains(unrelated), "Topic help should omit unrelated command sections");
    }

    @ParameterizedTest
    @ValueSource(strings = {"help loan", "\u2003help\tloan\u00a0", "help\u2003loan",
        "help\u00a0loan", "help\t\tloan"})
    void help_singleWordTopicWhitespace_showsLoanSyntax(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: loan id/<ITEM ID>"));
        assertTrue(printedContent().contains("Omitted issued/ defaults to today's date"));
        assertFalse(printedContent().contains("Format: return loan/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help list overdue", "\u2003help\tlist\u00a0overdue\u2003",
        "help\u2003list\u2003overdue", "help\nlist\r\noverdue", "help  list   overdue"})
    void help_multiWordTopicWhitespace_showsExactOverdueTopic(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: list overdue"));
        assertTrue(printedContent().contains("Returned loans and loans due today or later are excluded."));
        assertFalse(printedContent().contains("Format: list loans"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help view loan", "\u2003help\tview\u00a0loan\u2003",
        "help\u2003view\u2003loan", "help\nview\r\nloan", "help  view   loan"})
    void help_viewLoanTopicWhitespace_showsGuideWithoutViewingALoan(String command) throws Exception {
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> filesBefore = savedFiles(dataDirectory);

        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: view loan/<LOAN ID>"));
        assertTrue(printedContent().contains("Example: view loan/001"));
        assertTrue(printedContent().contains("Legacy loans without a saved snapshot"));
        assertFalse(printedContent().contains("Unknown help topic"));
        assertFalse(printedContent().contains("Format: loan id/"));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(filesBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"help all", "\u2003help\tall\u00a0", "help\u2003all"})
    void help_all_showsCompleteReference(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: add i/<NAME>"));
        assertTrue(printedContent().contains("Format: loan id/<ITEM ID>"));
        assertTrue(printedContent().contains("Format: return loan/<LOAN ID>"));
        assertTrue(printedContent().contains("Format: view loan/<LOAN ID>"));
        assertTrue(printedContent().contains("Format: add-to-bundle id/<BUNDLE ID>"));
        assertTrue(printedContent().contains("Format: remove-from-bundle id/<BUNDLE ID>"));
        assertTrue(printedContent().contains("Format: history bundle/id/<BUNDLE ID>"));
        assertTrue(printedContent().contains("Format: history bundle/name/<NAME>"));
        assertTrue(printedContent().contains("Format: bye"));
        assertTrue(printedContent().contains("Omitted issued/ defaults to today's date"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help unknown", "help list overdue extra", "help add i/Camera", "help Loan",
        "help all extra", "help loan/001", "help delete", "help list over"})
    void help_unknownTopic_showsErrorAndRecoverableMenu(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Unknown help topic"));
        assertMenuPrinted();
        assertFalse(printedContent().contains("Command not recognised"));
        assertFalse(printedContent().contains("Format: loan id/"));
    }

    @Test
    void help_emptyWorkspace_createsNoDataDirectoriesOrFiles() throws Exception {
        Path missingDirectory = temporaryDirectory.resolve("empty").resolve("nested-data");
        EqmManager empty = new EqmManager(missingDirectory, FIXED_CLOCK);
        ApplicationState before = snapshot(empty);
        assertFalse(Files.exists(missingDirectory.getParent()));

        executeAllReadOnlyForms(empty, before);

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

        executeAllReadOnlyForms(manager, before);

        assertFalse(printedContent().contains("Could not save"));
        assertFalse(printedContent().contains("Could not load"));
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

        assertTrue(printedContent().contains("[OK] Loan 003 issued."));
        assertTrue(printedContent().contains("[OK] Bundle 002 \"Next kit\" created."));
        assertEquals("004", readField(manager, "loans", LoanHandler.class).getNextLoanId());
        assertEquals(3, readField(manager, "loans", LoanHandler.class).getLoanCount());
        assertEquals("003", readField(manager, "equipment", EquipmentHandler.class)
                .getEquipmentById("006").getLoanId());
    }

    @Test
    void help_followedByOtherCommands_keepsTheSessionRunning() throws Exception {
        executeAllReadOnlyForms(manager, snapshot(manager));
        output.reset();

        manager.executeInput("update id/006 i/Updated spare battery cond/9");
        manager.executeInput("list loans");
        manager.executeInput("history id/001");

        assertTrue(printedContent().contains(" updated."));
        assertTrue(printedContent().contains("BORROWER ID"));
        assertTrue(printedContent().contains("222"));
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

        assertTrue(printedContent().contains("Welcome to snAp."));
        assertTrue(printedContent().contains("Type help"));
        assertMenuPrinted();
        assertTrue(printedContent().contains("Format: loan id/<ITEM ID>"));
        assertTrue(printedContent().contains("Unknown help topic"));
        assertTrue(printedContent().contains("Goodbye."));
        assertFalse(readField(interactive, "isRunning", Boolean.class));
        assertTrue(printed().indexOf("Welcome") < printed().indexOf("loan id/"));
        assertTrue(printed().indexOf("loan id/") < printed().indexOf("Goodbye."));

        String newline = System.lineSeparator();
        String divider = "-".repeat(100);
        String inputBoundary = newline + divider + newline + "snAp > " + newline + divider + newline + newline;
        String[] responses = printed().split(Pattern.quote(inputBoundary), -1);
        assertEquals(7, responses.length, "Each of the six input prompts must have dividers and surrounding spacing.");
        assertTrue(responses[0].contains("Welcome to snAp."));
        assertTrue(responses[1].contains("snAp command menu"));
        assertTrue(responses[2].contains("ADD LOAN"));
        assertTrue(responses[3].contains("Unknown help topic"));
        assertTrue(responses[4].contains("ADD EQUIPMENT"));
        assertTrue(responses[5].contains("EXIT"));
        assertEquals("[OK] Goodbye." + newline, responses[6]);
    }

    @Test
    void run_byeOnly_printsBoxedMenuBetweenWelcomeAndFirstPrompt() throws Exception {
        System.setIn(new ByteArrayInputStream("bye\n".getBytes(StandardCharsets.UTF_8)));
        EqmManager interactive = new EqmManager(temporaryDirectory.resolve("startup-menu-data"), FIXED_CLOCK);

        interactive.run();

        assertMenuPrinted();
        String text = printed();
        String border = text.lines().filter(line -> line.matches("\\+-+\\+")).findFirst().orElseThrow();
        int welcome = text.indexOf("Welcome to snAp.");
        int menu = text.indexOf(border);
        int prompt = text.indexOf("snAp >");
        assertTrue(welcome >= 0 && welcome < menu);
        assertTrue(menu < text.indexOf("snAp command menu"));
        assertTrue(text.indexOf("snAp command menu") < prompt);
        assertTrue(text.lastIndexOf(border) < prompt, "The complete box should precede the first input prompt.");
        assertEquals(1, text.lines().filter(line -> line.contains("snAp command menu")).count());
        assertFalse(printedContent().contains("Format:"), "Startup should show the compact menu.");
        assertTrue(printedContent().contains("Goodbye."));
        assertFalse(readField(interactive, "isRunning", Boolean.class));
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

        assertTrue(printedContent().contains("Could not load saved data"));
        assertFalse(printedContent().contains("Welcome to snAp."));
        assertFalse(printedContent().contains("loan id/"));
        assertEquals(original, Files.readString(invalidLoans));
        assertFalse(Files.exists(invalidDirectory.resolve("equipment.txt")));
        assertFalse(Files.exists(invalidDirectory.resolve("loan-sequence.txt")));
        assertFalse(Files.exists(invalidDirectory.resolve("bundles.txt")));
    }

    @Test
    void equipmentViews_afterReload_preserveStatusAssociationsAndSavedData() throws Exception {
        EqmManager restarted = new EqmManager(dataDirectory, FIXED_CLOCK);
        ApplicationState before = snapshot(restarted);
        Map<Path, SavedFile> files = savedFiles(dataDirectory);
        output.reset();

        restarted.executeInput("show id/001");
        assertTrue(printed().contains("Name: Camera"));
        assertTrue(printed().contains("Sensor format: Full frame"));
        assertTrue(printed().contains("Status: Loaned"));
        assertTrue(printed().contains("Active loan ID: 002"));
        output.reset();
        restarted.executeInput("show id/005");
        assertTrue(printed().contains("Status: Unserviceable"));
        assertTrue(printed().contains("Damage: SWELLING"));
        assertTrue(printed().contains("Remarks: Needs repair"));
        assertTrue(printed().contains("Active loan ID: (none)"));
        output.reset();
        restarted.executeInput("show id/006");
        assertTrue(printed().contains("Status: Available"));
        assertTrue(printed().contains("Active loan ID: (none)"));
        executeAllReadOnlyForms(restarted, before);

        assertEquals(before, snapshot(restarted));
        assertSavedFilesUnchanged(files);
    }

    @ParameterizedTest
    @ValueSource(strings = {"show", "show id/000", "show id/01", "show id/1000", "show id/-01",
        "show id/１２３", "show id/001 unexpected", "show id/001 id/002", "show id/001 type/camera"})
    void equipmentViews_invalidShowSyntax_reportsFormatWithoutChangingState(String command) throws Exception {
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> files = savedFiles(dataDirectory);

        manager.executeInput(command);

        assertTrue(printed().contains("Invalid format. Use: show id/[ITEM ID], from 001 to 999."));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(files);
    }

    @ParameterizedTest
    @ValueSource(strings = {"show id/001", "  show\tid/001  ", "show\u2003id/001", "show\u00a0id/001"})
    void equipmentViews_showWhitespace_selectsExactItem(String command) {
        manager.executeInput(command);

        assertTrue(printed().contains("Equipment 001"));
        assertTrue(printed().contains("Name: Camera"));
        assertFalse(printed().contains("Equipment 002"));
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
                        loan.getBorrowerId(), loan.getIssuedDate(), loan.getDueDate(), loan.isReturned(),
                        loan.getIssueConditionSnapshot())).toList();
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

    /** Returns every persistent file used by the populated fixture, including bundle membership. */
    private List<Path> storagePaths() {
        return List.of(dataDirectory.resolve("equipment.txt"), dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"), dataDirectory.resolve("bundles.txt"));
    }

    /** Repeats help, lists and histories so viewing records cannot save data or consume identifiers. */
    private void executeAllReadOnlyForms(EqmManager target, ApplicationState expected) throws Exception {
        List<String> requests = new ArrayList<>(HELP_COMMANDS);
        requests.addAll(List.of("list", "filter cond/1-10", "filter id/999", "show id/001", "show id/002",
                "show id/003", "show id/004", "show id/005", "show id/006", "show id/999", "show",
                "show id/000", "show id/001 id/002", "show id/001 remarks/Unexpected"));
        requests.addAll(List.of("list loans", "list overdue", "history id/001", "history id/005",
                "history bundle/id/001", "history bundle/name/Camera kit", "list bundles",
                "list borrower/222", "list member/111", "list borrower/999"));
        for (int repetition = 0; repetition < 2; repetition++) {
            for (String command : requests) {
                output.reset();
                target.executeInput(command);
                assertFalse(printedContent().contains("Command not recognised"), command);
                assertFalse(printedContent().contains("Could not save"), command);
                assertFalse(printedContent().contains("Could not load"), command);
                if (command.startsWith("help")) {
                    assertTrue(printedContent().contains("help"), command);
                }
                assertEquals(expected, snapshot(target), command);
            }
        }
    }

    /** Checks the discovery menu independently of the production text generator. */
    private void assertMenuPrinted() {
        assertTrue(printedContent().contains("help all"));
        assertTrue(printedContent().contains("help <COMMAND>"));
        assertTrue(printedContent().contains("help list overdue"));
        assertTrue(printedContent().contains("list overdue"));
        assertTrue(printedContent().contains("view loan"));
        assertTrue(printedContent().contains("delete item"));
        assertTrue(printedContent().contains("add-to-bundle"));
        assertTrue(printedContent().contains("remove-from-bundle"));
        assertTrue(printedContent().contains("list bundles"));
        assertTrue(printedContent().contains("history bundle"));
        assertTrue(printed().lines().anyMatch(line -> line.matches("\\+-+\\+")));
        assertTrue(printed().lines().anyMatch(line -> line.startsWith("| ") && line.endsWith(" |")));
    }

    /** Reads captured application output with its original line endings and spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }

    /** Ignores display wrapping while preserving wording in help and application messages. */
    private String printedContent() {
        return printed().replaceAll("\\s+", " ").strip();
    }

    /** Includes the identity and assessment of each physical item and its current loan ownership. */
    private record EquipmentState(Equipment reference, String name, String id, String type, String recordId,
            int condition, String details, String remarks, boolean loaned, boolean serviceable, String loanId) {
    }

    /** Includes both active and returned records so displaying help cannot alter loan history. */
    private record LoanState(Loan reference, String loanId, String itemId, String equipmentRecordId,
            String borrowerId, LocalDateTime issuedDate, LocalDateTime dueDate, boolean returned,
            EquipmentConditionSnapshot issueConditionSnapshot) {
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
