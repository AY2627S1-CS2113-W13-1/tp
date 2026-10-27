package seedu.snap.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.Lens;
import seedu.snap.equipment.Tripod;

/** Checks the command guide covers executable syntax, meaningful defaults and important restrictions. */
class HelpTest {
    private static final List<String> TOPICS = List.of("add", "list", "show", "delete item", "loan", "return",
            "delete loan", "update loan", "list loans", "view loan", "list borrower", "list overdue", "history",
            "filter", "update", "maintain", "add-bundle", "add-to-bundle", "backup", "restore",
            "remove-from-bundle", "list bundles", "history bundle", "help", "bye");

    /** Supplies the user-facing syntax and example for each supported command. */
    private static Stream<Arguments> commandExamples() {
        return Stream.of(
                Arguments.of("add", "add i/<NAME> id/<ID> type/<TYPE> cond/<N>"
                        + " [TYPE DETAIL] [damage/<DAMAGE>] [remarks/<TEXT>]",
                        "add i/Canon EOS R10 id/001 type/camera cond/1 "
                                + "sensor/APS-C mount/Canon RF damage/NONE"),
                Arguments.of("list", "list", "list"),
                Arguments.of("show", "show id/<ID>", "show id/001"),
                Arguments.of("delete item", "delete item/<ID>", "delete item/001"),
                Arguments.of("loan", "loan id/<ITEM ID> b/<BORROWER ID> [issued/<DD-MM-YY>] due/<DD-MM-YY>",
                        "loan id/001 b/002 issued/24-10-26 due/31-10-26"),
                Arguments.of("return", "return loan/<LOAN ID> serviceable/<yes|no> damage/<DAMAGE> [remarks/<TEXT>]",
                        "return loan/001 serviceable/no damage/SENSOR_DAMAGE remarks/Sensor needs repair"),
                Arguments.of("delete loan", "delete loan/<LOAN ID>", "delete loan/001"),
                Arguments.of("update loan", "update loan/<LOAN ID> [id/<ITEM ID>] [b/<BORROWER ID>]"
                                + " [issued/DD-MM-YY] [due/DD-MM-YY]",
                        "update loan/001 id/002 b/007 due/15-10-26"),
                Arguments.of("list loans", "list loans", "list loans"),
                Arguments.of("view loan", "view loan/<LOAN ID>", "view loan/001"),
                Arguments.of("backup", "backup path/<DIRECTORY>", "backup path/backups/session-1"),
                Arguments.of("restore", "restore path/<DIRECTORY>", "restore path/backups/session-1"),
                Arguments.of("list borrower", "list borrower/<BORROWER ID>", "list borrower/002"),
                Arguments.of("list overdue", "list overdue", "list overdue"),
                Arguments.of("history", "history id/<ITEM ID>", "history id/001"),
                Arguments.of("filter", "filter [id/<ID>] [i/<NAME>] [type/<TYPE>] [cond/<N|MIN-MAX>] [status/<STATUS>]",
                        "filter type/camera cond/1-3 status/available"),
                Arguments.of("maintain", "maintain id/<ITEM ID> date/<DD-MM-YY> work/<DESCRIPTION> outcome/<OUTCOME>",
                        "maintain id/001 date/27-10-26 work/Replaced battery outcome/Completed"),
                Arguments.of("update", "update id/<ID> [i/<NAME>] [type/<TYPE>] [cond/<N>]",
                        "update id/001 i/Studio camera cond/5"),
                Arguments.of("add-bundle", "add-bundle name/<NAME>", "add-bundle name/Studio kit"),
                Arguments.of("add-to-bundle", "add-to-bundle id/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]",
                        "add-to-bundle id/001 001 002 003"),
                Arguments.of("remove-from-bundle", "remove-from-bundle id/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]",
                        "remove-from-bundle id/001 001"),
                Arguments.of("list bundles", "list bundles", "list bundles"),
                Arguments.of("history bundle", "history bundle/id/<BUNDLE ID>", "history bundle/id/001"),
                Arguments.of("history bundle", "history bundle/name/<NAME>", "history bundle/name/Studio kit"),
                Arguments.of("help", "help", "help"),
                Arguments.of("bye", "bye", "bye"));
    }

    @ParameterizedTest
    @MethodSource("commandExamples")
    void getText_eachCommand_includesSyntaxAndExample(String topic, String format, String example) {
        String text = Help.getText();

        assertTrue(labelledValues(text, "Format:").contains(format), format);
        assertTrue(labelledValues(text, "Example:").contains(example), example);
    }

    @ParameterizedTest
    @MethodSource("commandExamples")
    void getText_commandTopic_includesOnlyItsGuideAndSharedNotation(String topic, String format, String example) {
        String text = Help.getText(topic);

        assertTrue(labelledValues(text, "Format:").contains(format), topic);
        assertTrue(labelledValues(text, "Example:").contains(example), topic);
        String prose = normaliseWhitespace(text);
        assertTrue(prose.contains("<VALUE> is a placeholder. [field/<VALUE>] is optional."));
        assertTrue(prose.contains("Commands and field prefixes use exact lowercase spelling."));
        assertTrue(prose.contains("001-999. Borrower IDs: 000-999."));
        commandExamples().forEach(arguments -> {
            String otherTopic = (String) arguments.get()[0];
            String otherFormat = (String) arguments.get()[1];
            if (!topic.equals(otherTopic)) {
                assertFalse(labelledValues(text, "Format:").contains(otherFormat),
                        topic + " should not include the guide for " + otherTopic);
            }
        });
    }

    @Test
    void getText_noTopic_returnsCompactGroupedMenu() {
        String text = Help.getText("");
        String lowerCaseText = text.toLowerCase(Locale.ROOT);

        assertTrue(text.lines().count() < 20, "The default menu must fit in a small terminal.");
        for (String group : List.of("equipment", "loan", "bundle")) {
            assertTrue(lowerCaseText.contains(group), "The menu should group " + group + " commands.");
        }
        List<String> menuRows = text.lines().filter(line -> line.startsWith("| ") && line.endsWith(" |"))
                .map(line -> line.substring(2, line.length() - 2).stripTrailing())
                .dropWhile(line -> !line.equals("snAp command menu"))
                .skip(1)
                .takeWhile(line -> !line.startsWith("Type help"))
                .filter(line -> !line.isBlank())
                .toList();
        assertEquals(4, menuRows.stream().filter(line -> line.matches("(?:Equipment|Loans|Bundles|General): .*"))
                .count());
        List<String> menuTopics = menuRows.stream()
                .flatMap(line -> Arrays.stream(line.substring(line.indexOf(':') + 1).strip().split("\\s*\\|\\s*")))
                .map(String::strip)
                .filter(topic -> !topic.isEmpty())
                .sorted().toList();
        assertEquals(TOPICS.stream().sorted().toList(), menuTopics,
                "Each supported command must appear exactly once in the grouped menu.");
        assertTrue(text.contains("help <COMMAND>"));
        assertTrue(text.contains("help all"));
        assertFalse(text.contains("sensor/<TEXT>"));
        assertFalse(text.contains("[TYPE DETAIL]"));
        assertFalse(text.contains("Format:"));
        assertEquals(text, Help.getText((String) null));
        assertEquals(text, Help.getText(" \t\r\n\u2003\u00a0 "));
    }

    @Test
    void getText_menu_usesAlignedAsciiBoxWithinTerminalWidth() {
        List<String> lines = Help.getText("").lines().toList();
        String border = lines.getFirst();

        assertTrue(border.matches("\\+-+\\+"));
        assertEquals(border, lines.getLast());
        assertTrue(border.length() <= 100, "The menu should fit within 100 terminal columns.");
        for (String line : lines) {
            assertEquals(border.length(), line.length(), "All box edges must align.");
            assertTrue(line.equals(border) || line.startsWith("| ") && line.endsWith(" |"), line);
            assertTrue(line.chars().allMatch(character -> character >= 32 && character <= 126),
                    "The box must work without Unicode border characters.");
        }
    }

    @ParameterizedTest
    @MethodSource("printTopics")
    void getText_eachGuide_limitsLineWidthAndSeparatesSyntaxFromExamples(String topic) {
        String text = Help.getText(topic);
        List<String> lines = text.lines().toList();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            assertTrue(line.length() <= 100, "Line exceeds 100 columns: " + line);
            if (line.equals("Format:") || line.equals("Example:")) {
                assertTrue(index + 1 < lines.size(), "A label must have command text.");
                assertTrue(lines.get(index + 1).startsWith("  "),
                        "Syntax and examples should be indented beneath their labels.");
                assertTrue(index > 0 && lines.get(index - 1).isBlank(),
                        "Labels should have a blank line before them.");
            }
        }
    }

    @Test
    void getText_topic_placesRequestedCommandBeforeSharedRulesAndUsesHeadingSeparators() {
        String text = Help.getText("loan");
        List<String> lines = text.lines().toList();

        assertEquals("=".repeat(100), lines.getFirst());
        assertEquals("ADD LOAN", lines.get(1));
        assertEquals(lines.getFirst(), lines.get(2));
        assertTrue(lines.get(3).isBlank());
        assertTrue(text.indexOf("Format:") < text.indexOf("snAp command help"));
        assertTrue(text.contains(System.lineSeparator() + "Notes:" + System.lineSeparator()));
    }

    @Test
    void getText_all_separatesEveryCommandWithSpacedFullWidthHeadingBands() {
        List<String> lines = Help.getText("all").lines().toList();
        List<String> headings = List.of("ADD EQUIPMENT", "LIST EQUIPMENT", "SHOW EQUIPMENT", "DELETE EQUIPMENT",
                "ADD LOAN", "RETURN EQUIPMENT", "DELETE LOAN", "UPDATE LOAN", "LIST LOANS", "VIEW LOAN ISSUE CONDITION",
                "EQUIPMENT HELD BY MEMBER", "LIST OVERDUE LOANS", "ITEM LOAN HISTORY", "FILTER EQUIPMENT",
                "UPDATE EQUIPMENT", "RECORD MAINTENANCE", "CREATE BUNDLE", "ADD EQUIPMENT TO BUNDLE",
                "REMOVE EQUIPMENT FROM BUNDLE", "LIST BUNDLES", "BUNDLE LOAN HISTORY", "BACK UP DATA", "RESTORE DATA",
                "HELP", "EXIT");
        String divider = "=".repeat(100);

        assertEquals(headings.size() * 2L, lines.stream().filter(divider::equals).count());
        int previousHeading = -1;
        for (String heading : headings) {
            int index = lines.indexOf(heading);
            assertTrue(index > previousHeading, "Each command must appear once in the documented order: " + heading);
            assertTrue(index >= 3, "A command heading must follow a divider and two blank lines: " + heading);
            assertTrue(lines.get(index - 3).isBlank(), "Leave two blank lines before " + heading);
            assertTrue(lines.get(index - 2).isBlank(), "Leave two blank lines before " + heading);
            assertEquals(divider, lines.get(index - 1), heading);
            assertEquals(divider, lines.get(index + 1), heading);
            assertTrue(lines.get(index + 2).isBlank(), "Separate the heading from its command syntax: " + heading);
            previousHeading = index;
        }
    }

    @Test
    void getText_allTopic_retainsCompleteReference() {
        assertEquals(Help.getText(), Help.getText("all"));
        assertEquals(Help.getText(), Help.getText(" \tall\u2003 "));
        assertTrue(Help.getText("all").lines().count() > Help.getText("").lines().count());
    }

    @Test
    void getText_multiwordTopics_selectExactGuideAfterWhitespaceNormalisation() {
        assertEquals(Help.getText("delete item"), Help.getText(" \tdelete\u2003item\u00a0 "));
        assertEquals(Help.getText("delete loan"), Help.getText("delete  \tloan"));
        assertEquals(Help.getText("list loans"), Help.getText("list\nloans"));
        assertEquals(Help.getText("view loan"), Help.getText(" \tview\u2003loan\u00a0 "));
        assertEquals(Help.getText("list overdue"), Help.getText("list\u00a0overdue"));
        assertEquals(Help.getText("list bundles"), Help.getText("list\u2009\u202fbundles"));
        assertEquals(Help.getText("history bundle"), Help.getText(" \thistory\u2003bundle\u00a0 "));
        assertFalse(labelledValues(Help.getText("list"), "Format:").contains("list loans"));
        assertFalse(labelledValues(Help.getText("delete item"), "Format:").contains("delete loan/<LOAN ID>"));
        assertFalse(labelledValues(Help.getText("history"), "Format:").contains("history bundle/id/<BUNDLE ID>"));
    }

    /** Supplies misspellings, partial topics and aliases that must not select a command guide. */
    private static Stream<String> unknownTopics() {
        return Stream.of("unknown", "HELP", "ADD", "helpall", "helpful", "equipment", "loans", "bundles",
                "delete", "list loan", "list items", "list loans extra", "view", "view loan/001", "view loan extra",
                "history bundle/id/001", "history bundle/name/Studio kit", "remove-from-bundle id/001 001",
                "help add", "all extra");
    }

    @ParameterizedTest
    @MethodSource("unknownTopics")
    void getText_unknownTopic_reportsProblemAndListsSupportedChoices(String topic) {
        String text = Help.getText(topic);

        assertTrue(text.toLowerCase(Locale.ROOT).contains("unknown"), topic);
        assertTrue(text.contains(Help.getText("")), "The error should include the same compact menu.");
        assertTrue(labelledValues(text, "Format:").isEmpty());
    }

    @Test
    void getText_topicGuides_retainRelevantRequirementsDefaultsAndRestrictions() {
        assertTrue(proseFor("add").contains("Name, unused Item ID, type and condition are required"));
        assertTrue(proseFor("add").contains("omitted damage defaults to NONE;"));
        assertTrue(proseFor("delete item").contains("Active loans and bundle membership block deletion."));
        assertTrue(proseFor("loan").contains("Omitted issued/ defaults to today's date"));
        assertTrue(proseFor("loan").contains("Issue date must be on or before the required due date."));
        assertTrue(proseFor("return").contains("explicit serviceable/ and damage/ assessment"));
        assertTrue(proseFor("return").contains("For other damage values, omitted or empty remarks clear prior notes."));
        assertTrue(proseFor("delete loan").contains(
                "Deleting returned history leaves newer active loans unchanged."));
        assertTrue(proseFor("list overdue").contains("Returned loans and loans due today or later are excluded."));
        assertTrue(proseFor("history").contains("Reused Item IDs keep separate histories."));
        assertTrue(proseFor("filter").contains("All filters must match (AND)."));
        assertTrue(proseFor("update").contains("Omitted fields retain their current values."));
        assertTrue(proseFor("add-bundle").contains(
                "Bundles and their equipment memberships are restored between application runs"));
        assertTrue(proseFor("add-to-bundle").contains(
                "exactly one camera, at least one lens and at least one battery."));
        assertTrue(proseFor("bye").contains("A save failure keeps the application running;"));
    }

    @Test
    void getText_addAndReturnTopics_showAllCurrentSubtypeDamageValues() {
        Stream.of(Camera.CameraDamage.values(), Lens.LensDamage.values(), Battery.BatteryDamage.values(),
                Tripod.TripodDamage.values()).flatMap(Stream::of).forEach(damage -> {
                    assertTrue(Help.getText("add").contains(damage.name()), damage.name());
                    assertTrue(Help.getText("return").contains(damage.name()), damage.name());
                });
    }

    @Test
    void getText_notationAndLimits_distinguishOptionalFieldsAndIdentifierRanges() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("<VALUE> is a placeholder. [field/<VALUE>] is optional."));
        assertTrue(text.contains("Commands and field prefixes use exact lowercase spelling."));
        assertTrue(text.contains("001-999. Borrower IDs: 000-999."));
        assertTrue(text.contains("Equipment names: 1-" + Equipment.MAX_NAME_LENGTH));
        assertTrue(text.contains("condition: " + Equipment.MIN_CONDITION + "-" + Equipment.MAX_CONDITION));
        assertTrue(text.contains("remarks: 0-" + Equipment.MAX_REMARKS_LENGTH + " characters after trimming."));
        assertTrue(text.contains("Type details: 1-100 characters."));
        assertTrue(text.contains("Type details and remarks must remain on one line."));
        assertTrue(text.contains("Empty supplied details, damage, IDs or dates are invalid"));
    }

    @Test
    void getText_addEquipment_explainsIndependentDefaultsAndSubtypeDamageChoices() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Name, unused Item ID, type and condition are required"));
        assertTrue(text.contains(
                "Optional order: subtype fields in the order shown, then damage/, then final remarks/."));
        assertTrue(text.contains(
                "Each optional field may be omitted independently, except remarks/ with damage OTHER."));
        assertTrue(text.contains(
                "Omitted subtype and compatibility details default to unspecified;"));
        assertTrue(text.contains("omitted damage defaults to NONE;"));
        assertTrue(text.contains("omitted remarks default to empty text, except OTHER requires non-empty remarks."));
        assertTrue(text.contains(
                "camera: sensor/<TEXT> mount/<TEXT> thread/<TEXT> battery/<TEXT>; damage:"));
        assertTrue(text.contains(
                "lens: zoom/<TEXT> mount/<TEXT> sensor/<TEXT>; damage:"));
        assertTrue(text.contains(
                "battery: capacity/<TEXT> battery/<TEXT>; damage:"));
        assertTrue(text.contains("tripod: thread/<TEXT>; damage:"));
        assertTrue(text.contains("Compatibility comparisons ignore letter case."));
        assertTrue(text.contains("a known mismatch produces INCOMPATIBLE"));
        Stream.of(Camera.CameraDamage.values(), Lens.LensDamage.values(), Battery.BatteryDamage.values(),
                Tripod.TripodDamage.values()).flatMap(Stream::of)
                .forEach(damage -> assertTrue(text.contains(damage.name()), damage.name()));
    }

    @Test
    void getText_loanAndReturn_explainDateDefaultsAssessmentAndRetainedHistory() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Item ID, borrower ID and due/ are required."));
        assertTrue(text.contains("Omitted issued/ defaults to today's date"));
        assertTrue(text.contains("local time zone, at midnight."));
        assertTrue(text.contains("Issue date must be on or before the required due date."));
        assertTrue(text.contains("be serviceable and have no active loan."));
        assertTrue(text.contains("Loan IDs are assigned automatically from 001 to 999"));
        assertTrue(text.contains("explicit serviceable/ and damage/ assessment"));
        assertTrue(text.contains("Remarks/ must be last. For OTHER, provide non-empty text after trimming."));
        assertTrue(text.contains("For other damage values, omitted or empty remarks clear prior notes."));
        assertTrue(text.contains(
                "compatibility attributes and bundle memberships."));
        assertTrue(text.contains("Return permanently marks the retained loan returned"));
        assertTrue(text.contains("cannot be returned twice or reactivated."));
        assertTrue(text.contains("Changing its numeric condition does not repair it."));
        assertTrue(text.contains("Successful updates are saved to data/loans.txt before live records change."));
        assertTrue(text.contains("Returned loans cannot be edited."));
    }

    @Test
    void getText_viewLoan_explainsCapturedConditionLegacyRecordsAndReadOnlyBehaviour() {
        String text = proseFor("view loan");

        assertTrue(text.contains(
                "Shows the equipment type, condition, damage and remarks recorded when the loan was issued."));
        assertTrue(text.contains(
                "Legacy loans without a saved snapshot report that the issue condition was not recorded."));
        assertTrue(text.contains("This read-only command does not change equipment, loans or saved records."));
    }

    @Test
    void getText_maintenance_explainsRequiredFieldsAndPersistence() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Requires an existing Item ID, a valid maintenance date"));
        assertTrue(text.contains("Maintenance records are saved to maintenance.txt"));
        assertTrue(text.contains("Maintenance work: 1-200 characters; outcomes: 1-100 characters."));
    }

    @Test
    void getText_historyAndDeletion_distinguishPhysicalItemsAndLoanRemoval() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Active loans and bundle membership block deletion."));
        assertTrue(text.contains("Removes one loan record and its history."));
        assertTrue(text.contains("Deleting returned history leaves newer active loans unchanged."));
        assertTrue(text.contains("Includes its physical item's active and returned loans."));
        assertTrue(text.contains("Sorted by issue date, earliest first, then Loan ID."));
        assertTrue(text.contains("Reused Item IDs keep separate histories."));
        assertTrue(text.contains("Older returned records without a proven item association"));
    }

    @Test
    void getText_filterAndUpdate_explainFieldCombinationsMatchingAndPreservation() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Supply at least one filter, in any order, with each prefix once."));
        assertTrue(text.contains("All filters must match (AND)."));
        assertTrue(text.contains("Names use case-insensitive partial matching"));
        assertTrue(text.contains("types use case-insensitive full matching."));
        assertTrue(text.contains("inclusive ascending range"));
        assertTrue(text.contains("Available excludes active loans and unserviceable items."));
        assertTrue(text.contains("Requires an existing Item ID and at least one update field, in any order"));
        assertTrue(text.contains("Omitted fields retain their current values."));
        assertTrue(text.contains("Type is fixed; type/ must match the current type."));
        assertTrue(text.contains("damage, remarks, serviceability, loans and bundle membership."));
    }

    @Test
    void getText_bundles_explainPersistenceCompositionAndAvailability() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Requires a name of 1-100 characters."));
        assertTrue(text.contains("Bundles and their equipment memberships are restored between application runs"));
        assertTrue(text.contains("at least one existing, serviceable, unloaned equipment item."));
        assertTrue(text.contains("An item may appear only once per bundle."));
        assertTrue(text.contains("exactly one camera, at least one lens and at least one battery."));
        assertTrue(text.contains("A tripod is optional, with at most one."));
        assertTrue(text.contains("Format: remove-from-bundle id/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]"));
        assertTrue(text.contains("eligible for deletion after it has been removed from every bundle."));
        assertTrue(proseFor("remove-from-bundle").contains(
                "Successful removals are saved immediately; a save failure restores the previous memberships."));
        assertFalse(text.contains("Bundles exist only for the current session"));
        assertFalse(text.contains("Shows current-session bundles"));
    }

    @Test
    void getText_bundleHistory_explainsBothSelectorsOrderingAndEmptyResults() {
        String text = proseFor("history bundle");

        assertTrue(labelledValues(Help.getText("history bundle"), "Format:")
                .containsAll(List.of("history bundle/id/<BUNDLE ID>", "history bundle/name/<NAME>")));
        assertTrue(text.contains(
                "Includes active and returned loans for every equipment item in the selected bundle."));
        assertTrue(text.contains("Results are sorted by issue date, earliest first, then Loan ID."));
        assertTrue(text.contains("An empty bundle history and unknown bundle are reported clearly."));
    }

    @Test
    void getText_overdueLoans_explainsEligibilityAndReadOnlyBehaviour() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Format: list overdue"));
        assertTrue(text.contains(
                "Shows active loans with due dates earlier than today's local date."));
        assertTrue(text.contains(
                "Returned loans and loans due today or later are excluded."));
        assertTrue(text.contains(
                "Displays Item ID, borrower ID and due date without changing any records."));
    }

    @Test
    void getText_backupAndRestore_explainConsistencyValidationAndNonDestructiveFailure() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Creates a complete backup containing equipment.txt, loans.txt,"
                + " loan-sequence.txt and a manifest."));
        assertTrue(text.contains("The destination must be outside the live data directory"));
        assertTrue(text.contains("Validates the manifest and every equipment, loan and sequence record"));
        assertTrue(text.contains("A failed restore leaves the current records unchanged."));
        assertTrue(text.contains("clear session-only"));
    }

    @Test
    void getText_borrowerLoans_explainsFilteringAliasesAndReadOnlyBehaviour() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("EQUIPMENT HELD BY MEMBER"));
        assertTrue(text.contains("The equivalent alias list member/<MEMBER ID> is also accepted."));
        assertTrue(text.contains("Shows only active equipment held by the selected borrower"));
        assertTrue(text.contains("Returned loans and loans held by other borrowers are excluded."));
        assertTrue(text.contains("An unknown borrower ID and a known borrower with no active loans"));
        assertTrue(text.contains("This read-only query does not change equipment, loans or saved files."));
    }

    @Test
    void getText_helpAndExit_explainArgumentsSaveFailureAndImplementedCommandsOnly() {
        String text = Help.getText();

        assertTrue(labelledValues(text, "Format:").containsAll(List.of("help", "help <COMMAND>", "help all")));
        assertTrue(labelledValues(text, "Example:").containsAll(List.of("help", "help loan", "help all")));
        text = normaliseWhitespace(text);
        assertTrue(text.contains("Saves equipment, loans, maintenance records and bundles before exiting."));
        assertTrue(text.contains("A save failure keeps the application running;"));
        assertTrue(text.contains("fix the reported file problem and retry bye."));
        assertFalse(text.contains("Format: repair"));
        assertFalse(text.contains("No arguments are accepted."));
    }

    @Test
    void print_noArguments_printsCompactMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            Help.print();
        } finally {
            System.setOut(original);
        }
        String text = output.toString(StandardCharsets.UTF_8);

        assertEquals(Help.getText(""), text);
        assertPlatformLineEndingsAndFinalNewline(text);
    }

    /** Covers printed menu, reference, every command topic and representative invalid input. */
    private static Stream<String> printTopics() {
        return Stream.concat(Stream.of(null, "", " \t\u2003 ", "all", "unknown", "HELP", "list\u00a0loans"),
                TOPICS.stream());
    }

    @ParameterizedTest
    @MethodSource("printTopics")
    void print_topic_printsMatchingTextWithPlatformLineEndingsAndOneFinalNewline(String topic) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            Help.print(topic);
        } finally {
            System.setOut(original);
        }
        String text = output.toString(StandardCharsets.UTF_8);

        assertEquals(Help.getText(topic), text);
        assertPlatformLineEndingsAndFinalNewline(text);
    }

    /** Reassembles labelled command blocks so wrapping cannot change their executable syntax. */
    private static List<String> labelledValues(String text, String label) {
        List<String> values = new ArrayList<>();
        StringBuilder value = null;
        for (String line : text.lines().toList()) {
            if (line.equals(label)) {
                value = new StringBuilder();
            } else if (value != null && line.startsWith("  ") && !line.isBlank()) {
                if (!value.isEmpty()) {
                    value.append(' ');
                }
                value.append(line.strip());
            } else if (value != null) {
                values.add(value.toString());
                value = null;
            }
        }
        if (value != null) {
            values.add(value.toString());
        }
        return values;
    }

    /** Ignores display wrapping when checking the wording and limits for one command. */
    private static String proseFor(String topic) {
        return normaliseWhitespace(Help.getText(topic));
    }

    /** Preserves words and punctuation while treating terminal wrapping as ordinary whitespace. */
    private static String normaliseWhitespace(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    /** Rejects mixed newline conventions and accidental blank lines at the end of any guide. */
    private static void assertPlatformLineEndingsAndFinalNewline(String text) {
        String lineSeparator = System.lineSeparator();

        assertTrue(text.endsWith(lineSeparator));
        assertFalse(text.endsWith(lineSeparator + lineSeparator));
        assertFalse(text.replace(lineSeparator, "").contains("\n"));
        assertFalse(text.replace(lineSeparator, "").contains("\r"));
    }
}
