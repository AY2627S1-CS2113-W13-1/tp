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
import seedu.snap.maintenance.MaintenanceHandler;
import seedu.snap.maintenance.MaintenanceRecord;
import seedu.snap.ui.Help;

/** Verifies that help and equipment views leave application records, files and counters unchanged. */
class HelpIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
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
        manager.executeInput("add-eqm name/Camera E-id/001 type/camera cond/4 sensor/Full frame "
                + "remarks/Camera checked");
        manager.executeInput("add-eqm name/Battery E-id/002 type/battery cond/5 capacity/2000mAh");
        manager.executeInput("add-eqm name/Lens E-id/003 type/lens cond/6 zoom/24-70mm");
        manager.executeInput("add-eqm name/Tripod E-id/004 type/tripod cond/7 thread/1-4 inch");
        manager.executeInput("add-eqm name/Workshop battery E-id/005 type/battery cond/3 capacity/1500mAh");
        manager.executeInput("add-eqm name/Spare battery E-id/006 type/battery cond/8 capacity/1800mAh");
        manager.executeInput("add-bundle name/Camera kit");
        manager.executeInput("bundle-add-eqm B-id/001 001 002 003 004");
        manager.executeInput("add-loan E-id/005 U-id/111 due/25-10-26");
        manager.executeInput("loan-return L-id/001 serviceable/no damage/SWELLING remarks/Needs repair");
        manager.executeInput("add-loan E-id/001 U-id/222 due/25-10-26");
        manager.executeInput("eqm-maintain E-id/005 date/24-10-26 work/Inspected swelling outcome/Repair required");
        readField(manager, "bundles", BundleHandler.class).saveToStorage();
        assertTrue(readField(manager, "bundles", BundleHandler.class).getBundleList().getFirst().isComplete());
        assertTrue(readField(manager, "equipment", EquipmentHandler.class).getEquipmentById("001").isLoaned());
        assertFalse(readField(manager, "equipment", EquipmentHandler.class)
                .getEquipmentById("005").isServiceable());
        assertEquals(1, readField(manager, "maintenance", MaintenanceHandler.class).getMaintenanceRecords().size());
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
        assertFalse(printedContent().contains("add-loan E-id/"));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(filesBefore);
        assertEquals(pathsBefore, treePaths(dataDirectory));
    }

    @Test
    void help_withoutTopic_showsCompactGroupedMenu() {
        manager.executeInput("help");

        assertMenuPrinted();
        assertFalse(printedContent().contains("Format: add-loan E-id/"));
        assertFalse(printedContent().contains("damage/SENSOR_DAMAGE"));
        assertTrue(printed().lines().count() <= 24, "The command menu should fit a normal terminal screen");
    }

    @Test
    void help_all_wrapsTextAndKeepsCommandBlocksSeparate() {
        manager.executeInput("help all");

        List<String> lines = printed().lines().toList();
        assertTrue(lines.stream().allMatch(line -> line.length() <= 100));
        assertEquals(29, lines.stream().filter("Format:"::equals).count());
        assertEquals(29, lines.stream().filter("Example:"::equals).count());
        assertTrue(printed().contains(System.lineSeparator() + System.lineSeparator() + "Format:"
                + System.lineSeparator() + "  "));
        assertTrue(printed().contains(System.lineSeparator() + System.lineSeparator() + "Example:"
                + System.lineSeparator() + "  "));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "add-eqm | Format: add-eqm name/<NAME>",
        "list-eqm | Format: list-eqm",
        "eqm-show | Format: eqm-show E-id/<ID>",
        "eqm-delete | Format: eqm-delete E-id/<ID>",
        "add-loan | Format: add-loan E-id/<ITEM ID>",
        "loan-return | Format: loan-return L-id/<LOAN ID>",
        "loan-delete | Format: loan-delete L-id/<LOAN ID>",
        "loan-update | Format: loan-update L-id/<LOAN ID>",
        "list-loan | Format: list-loan",
        "loan-show | Format: loan-show L-id/<LOAN ID>",
        "backup | Format: backup path/<DIRECTORY>",
        "restore | Format: restore path/<DIRECTORY>",
        "list-loan-borrower | Format: list-loan-borrower U-id/<BORROWER ID>",
        "list-loan-overdue | Format: list-loan-overdue",
        "eqm-loan-history | Format: eqm-loan-history E-id/<ITEM ID>",
        "filter-eqm | Format: filter-eqm [E-id/<ID>]",
        "eqm-update | Format: eqm-update E-id/<ID>",
        "eqm-maintain | Format: eqm-maintain E-id/<ITEM ID>",
        "add-bundle | Format: add-bundle name/<NAME>",
        "bundle-add-eqm | Format: bundle-add-eqm B-id/<BUNDLE ID>",
        "bundle-remove-eqm | Format: bundle-remove-eqm B-id/<BUNDLE ID>",
        "list-bundle | Format: list-bundle",
        "bundle-history | Format: bundle-history B-id/<BUNDLE ID>",
        "help | Format: help",
        "exit | Format: exit"
    })
    void help_commandTopic_showsOnlyRequestedCommandWithExample(String topic, String expectedFormat) {
        manager.executeInput("help " + topic);

        assertTrue(printedContent().contains(expectedFormat), topic);
        assertTrue(printedContent().contains("Example: " + topic), topic);
        assertFalse(printedContent().contains("Command not recognised"), topic);
        assertFalse(printedContent().contains("Unknown help topic"), topic);
        String unrelated = topic.equals("add-loan") ? "Format: add-eqm name/" : "Format: add-loan E-id/";
        assertFalse(printedContent().contains(unrelated), "Topic help should omit unrelated command sections");
    }

    @ParameterizedTest
    @ValueSource(strings = {"help add-loan", "\u2003help\tadd-loan\u00a0", "help\u2003add-loan",
        "help\u00a0add-loan", "help\t\tadd-loan"})
    void help_singleWordTopicWhitespace_showsLoanSyntax(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: add-loan E-id/<ITEM ID>"));
        assertTrue(printedContent().contains("Omitted issued/ defaults to today's date"));
        assertFalse(printedContent().contains("Format: loan-return L-id/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help list-loan-overdue", "\u2003help\tlist-loan-overdue\u00a0",
        "help\u2003list-loan-overdue", "help  list-loan-overdue"})
    void help_multiWordTopicWhitespace_showsExactOverdueTopic(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: list-loan-overdue"));
        assertTrue(printedContent().contains("Returned loans and loans due today or later are excluded."));
        assertFalse(printedContent().contains("Format: list-loan"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help loan-show", "\u2003help\tloan-show\u00a0",
        "help\u2003loan-show", "help  loan-show"})
    void help_viewLoanTopicWhitespace_showsGuideWithoutViewingALoan(String command) throws Exception {
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> filesBefore = savedFiles(dataDirectory);

        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: loan-show L-id/<LOAN ID>"));
        assertTrue(printedContent().contains("Example: loan-show L-id/001"));
        assertTrue(printedContent().contains("Legacy loans without a saved snapshot"));
        assertFalse(printedContent().contains("Unknown help topic"));
        assertFalse(printedContent().contains("Format: add-loan E-id/"));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(filesBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"help all", "\u2003help\tall\u00a0", "help\u2003all"})
    void help_all_showsCompleteReference(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Format: add-eqm name/<NAME>"));
        assertTrue(printedContent().contains("Format: add-loan E-id/<ITEM ID>"));
        assertTrue(printedContent().contains("Format: loan-return L-id/<LOAN ID>"));
        assertTrue(printedContent().contains("Format: loan-show L-id/<LOAN ID>"));
        assertTrue(printedContent().contains("Format: bundle-add-eqm B-id/<BUNDLE ID>"));
        assertTrue(printedContent().contains("Format: bundle-remove-eqm B-id/<BUNDLE ID>"));
        assertTrue(printedContent().contains("Format: bundle-history B-id/<BUNDLE ID>"));
        assertTrue(printedContent().contains("Format: bundle-history name/<NAME>"));
        assertTrue(printedContent().contains("Format: exit"));
        assertTrue(printedContent().contains("Omitted issued/ defaults to today's date"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"help unknown", "help list-loan-overdue extra", "help add-eqm name/Camera", "help Loan",
        "help all extra", "help loan/001", "help delete", "help list over"})
    void help_unknownTopic_showsErrorAndRecoverableMenu(String command) {
        manager.executeInput(command);

        assertTrue(printedContent().contains("Unknown help topic"));
        assertMenuPrinted();
        assertFalse(printedContent().contains("Command not recognised"));
        assertFalse(printedContent().contains("Format: add-loan E-id/"));
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

        manager.executeInput("add-loan E-id/006 U-id/333 due/25-10-26");
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

        manager.executeInput("eqm-update E-id/006 name/Updated spare battery cond/9");
        manager.executeInput("list-loan");
        manager.executeInput("eqm-loan-history E-id/001");

        assertTrue(printedContent().contains(" updated."));
        assertTrue(printedContent().contains("BORROWER ID"));
        assertTrue(printedContent().contains("222"));
        assertEquals("Updated spare battery", readField(manager, "equipment", EquipmentHandler.class)
                .getEquipmentById("006").getItemName());
        assertTrue(readField(manager, "isRunning", Boolean.class));
    }

    @Test
    void run_helpThenBye_showsDiscoveryAndEndsOnlyAfterBye() throws Exception {
        System.setIn(new ByteArrayInputStream("help\nhelp add-loan\nhelp unknown\nhelp all\nhelp exit\nexit\n"
                .getBytes(StandardCharsets.UTF_8)));
        EqmManager interactive = new EqmManager(temporaryDirectory.resolve("interactive-data"), FIXED_CLOCK);

        interactive.run();

        assertTrue(printedContent().contains("Welcome to snAp."));
        assertTrue(printedContent().contains("Type help"));
        assertMenuPrinted();
        assertTrue(printedContent().contains("Format: add-loan E-id/<ITEM ID>"));
        assertTrue(printedContent().contains("Unknown help topic"));
        assertTrue(printedContent().contains("Goodbye."));
        assertFalse(readField(interactive, "isRunning", Boolean.class));
        assertTrue(printed().indexOf("Welcome") < printed().indexOf("add-loan E-id/"));
        assertTrue(printed().indexOf("add-loan E-id/") < printed().indexOf("Goodbye."));

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
        System.setIn(new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8)));
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
        System.setIn(new ByteArrayInputStream("help\nexit\n".getBytes(StandardCharsets.UTF_8)));
        EqmManager invalid = new EqmManager(invalidDirectory, FIXED_CLOCK);

        invalid.run();

        assertTrue(printedContent().contains("Could not load saved data"));
        assertFalse(printedContent().contains("Welcome to snAp."));
        assertFalse(printedContent().contains("add-loan E-id/"));
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

        restarted.executeInput("eqm-show E-id/001");
        assertTrue(printed().contains("Name: Camera"));
        assertTrue(printed().contains("Sensor format: Full frame"));
        assertTrue(printed().contains("Status: Loaned"));
        assertTrue(printed().contains("Active loan ID: 002"));
        output.reset();
        restarted.executeInput("eqm-show E-id/005");
        assertTrue(printed().contains("Status: Unserviceable"));
        assertTrue(printed().contains("Damage: SWELLING"));
        assertTrue(printed().contains("Remarks: Needs repair"));
        assertTrue(printed().contains("Active loan ID: (none)"));
        output.reset();
        restarted.executeInput("eqm-show E-id/006");
        assertTrue(printed().contains("Status: Available"));
        assertTrue(printed().contains("Active loan ID: (none)"));
        executeAllReadOnlyForms(restarted, before);

        assertEquals(before, snapshot(restarted));
        assertSavedFilesUnchanged(files);
    }

    @ParameterizedTest
    @ValueSource(strings = {"eqm-show", "eqm-show E-id/000", "eqm-show E-id/01", "eqm-show E-id/1000",
        "eqm-show E-id/-01", "eqm-show E-id/１２３", "eqm-show E-id/001 unexpected",
        "eqm-show E-id/001 E-id/002", "eqm-show E-id/001 type/camera"})
    void equipmentViews_invalidShowSyntax_reportsFormatWithoutChangingState(String command) throws Exception {
        ApplicationState before = snapshot(manager);
        Map<Path, SavedFile> files = savedFiles(dataDirectory);

        manager.executeInput(command);

        assertTrue(printed().contains("Invalid format. Use: eqm-show E-id/[ITEM ID], from 001 to 999."));
        assertEquals(before, snapshot(manager));
        assertSavedFilesUnchanged(files);
    }

    @ParameterizedTest
    @ValueSource(strings = {"eqm-show E-id/001", "  eqm-show\tE-id/001  ", "eqm-show\u2003E-id/001",
        "eqm-show\u00a0E-id/001"})
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
        MaintenanceHandler maintenanceHandler = readField(target, "maintenance", MaintenanceHandler.class);
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
        return new ApplicationState(inventory, loanHandler, bundleHandler, maintenanceHandler,
                items, loanRecords, bundleRecords, List.copyOf(maintenanceHandler.getMaintenanceRecords()),
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
                dataDirectory.resolve("loan-sequence.txt"), dataDirectory.resolve("bundles.txt"),
                dataDirectory.resolve("maintenance.txt"));
    }

    /** Repeats help, lists and histories so viewing records cannot save data or consume identifiers. */
    private void executeAllReadOnlyForms(EqmManager target, ApplicationState expected) throws Exception {
        List<String> requests = new ArrayList<>(helpRequests());
        requests.addAll(List.of("list-eqm", "filter-eqm cond/1-10", "filter-eqm E-id/999", "eqm-show E-id/001",
                "eqm-show E-id/002", "eqm-show E-id/003", "eqm-show E-id/004", "eqm-show E-id/005",
                "eqm-show E-id/006", "eqm-show E-id/999", "eqm-show",
                "eqm-show E-id/000", "eqm-show E-id/001 E-id/002", "eqm-show E-id/001 remarks/Unexpected"));
        requests.addAll(List.of("list-loan", "list-loan-overdue", "eqm-loan-history E-id/001",
                "eqm-loan-history E-id/005",
                "bundle-history B-id/001", "bundle-history name/Camera kit", "list-bundle",
                "list-loan-borrower U-id/222", "list-loan-borrower U-id/999", "loan-show L-id/001"));
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

    /** Exercises every advertised topic as the menu grows, plus aliases and malformed requests. */
    private static List<String> helpRequests() {
        List<String> requests = new ArrayList<>(List.of("help", "help all", "help list member", "help unknown",
                "help list-loan-overdue extra", "help add-eqm name/Camera", "help Loan", "help all extra"));
        for (String line : Help.getText("").lines().toList()) {
            if (!line.startsWith("| ")) {
                continue;
            }
            String content = line.substring(2, line.length() - 2).stripTrailing();
            if (content.matches("(?:Equipment|Loans|Bundles|General): .*")) {
                content = content.substring(content.indexOf(':') + 1).strip();
            } else if (!content.isBlank() && content.startsWith(" ")) {
                content = content.strip();
            } else {
                continue;
            }
            for (String topic : content.split(" \\| ")) {
                requests.add("help " + topic);
            }
        }
        return requests;
    }

    /** Checks the discovery menu independently of the production text generator. */
    private void assertMenuPrinted() {
        assertTrue(printedContent().contains("help all"));
        assertTrue(printedContent().contains("help <COMMAND>"));
        assertTrue(printedContent().contains("help list-loan-overdue"));
        assertTrue(printedContent().contains("list-loan-overdue"));
        assertTrue(printedContent().contains("loan-show"));
        assertTrue(printedContent().contains("eqm-delete"));
        assertTrue(printedContent().contains("bundle-add-eqm"));
        assertTrue(printedContent().contains("bundle-remove-eqm"));
        assertTrue(printedContent().contains("list-bundle"));
        assertTrue(printedContent().contains("bundle-history"));
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
            MaintenanceHandler maintenance,
            List<EquipmentState> items, List<LoanState> loanRecords, List<BundleState> bundleRecords,
            List<MaintenanceRecord> maintenanceRecords,
            int loanCount, String nextLoanId, int nextBundleNumber, boolean running) {
    }

    /** Retains file bytes and modification time before help is displayed. */
    private record SavedFile(byte[] contents, FileTime modified) {
    }
}
