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
import seedu.snap.parser.CommandAliases;

/** Checks the command guide covers executable syntax, meaningful defaults and important restrictions. */
class HelpTest {
    private static final List<String> TOPICS = List.of("eqm-add", "eqm-list", "eqm-show", "eqm-delete", "loan-add",
            "loan-return", "loan-delete", "loan-update", "loan-list", "loan-view", "loan-show", "loan-list-borrower",
            "loan-list-overdue", "eqm-loan-history", "eqm-filter", "eqm-update", "eqm-maintain", "bundle-create",
            "bundle-add-eqm", "backup", "restore", "bundle-remove-eqm", "bundle-list", "bundle-history", "help",
            "exit");

    /** Supplies the user-facing syntax and example for each supported command. */
    private static Stream<Arguments> commandExamples() {
        return Stream.of(
                Arguments.of("eqm-add", "eqm-add name/<NAME> e/<ID> type/<TYPE> cond/<N>"
                        + " [TYPE DETAIL] [damage/<DAMAGE>] [remarks/<TEXT>]",
                        "eqm-add name/Canon EOS R10 e/001 type/camera cond/1 "
                                + "sensor/APS-C mount/Canon RF damage/NONE"),
                Arguments.of("eqm-list", "eqm-list", "eqm-list"),
                Arguments.of("eqm-show", "eqm-show e/<ID>", "eqm-show e/001"),
                Arguments.of("eqm-delete", "eqm-delete e/<ID>", "eqm-delete e/001"),
                Arguments.of("loan-add", "loan-add e/<ITEM ID> u/<BORROWER ID> "
                                + "[issued/<DD-MM-YY>] due/<DD-MM-YY>",
                        "loan-add e/001 u/002 issued/24-10-26 due/31-10-26"),
                Arguments.of("loan-return", "loan-return l/<LOAN ID> serviceable/<yes|no> "
                                + "damage/<DAMAGE> [cond/<N>] [remarks/<TEXT>]",
                        "loan-return l/001 serviceable/no damage/SENSOR_DAMAGE remarks/Sensor needs repair"),
                Arguments.of("loan-delete", "loan-delete l/<LOAN ID>", "loan-delete l/001"),
                Arguments.of("loan-update", "loan-update l/<LOAN ID> [e/<ITEM ID>] [u/<BORROWER ID>]"
                                + " [issued/<DD-MM-YY>] [due/<DD-MM-YY>]",
                        "loan-update l/001 e/002 u/007 due/07-11-26"),
                Arguments.of("loan-list", "loan-list", "loan-list"),
                Arguments.of("loan-view", "loan-view l/<LOAN ID>", "loan-view l/001"),
                Arguments.of("loan-show", "loan-show l/<LOAN ID>", "loan-show l/001"),
                Arguments.of("backup", "backup path/<DIRECTORY>", "backup path/backups/session-1"),
                Arguments.of("restore", "restore path/<DIRECTORY>", "restore path/backups/session-1"),
                Arguments.of("loan-list-borrower", "loan-list-borrower u/<BORROWER ID>",
                        "loan-list-borrower u/002"),
                Arguments.of("loan-list-overdue", "loan-list-overdue", "loan-list-overdue"),
                Arguments.of("eqm-loan-history", "eqm-loan-history e/<ITEM ID>", "eqm-loan-history e/001"),
                Arguments.of("eqm-filter", "eqm-filter [e/<ID>] [name/<NAME>] [type/<TYPE>] "
                                + "[cond/<N|MIN-MAX>] [status/<STATUS>]",
                        "eqm-filter type/camera cond/1-3 status/available"),
                Arguments.of("eqm-maintain", "eqm-maintain e/<ITEM ID> date/<DD-MM-YY> "
                                + "work/<DESCRIPTION> outcome/<OUTCOME>",
                        "eqm-maintain e/001 date/27-10-26 work/Replaced battery outcome/Completed"),
                Arguments.of("eqm-update", "eqm-update e/<ID> [name/<NAME>] [type/<TYPE>] [cond/<N>]",
                        "eqm-update e/001 name/Studio camera cond/5"),
                Arguments.of("bundle-create", "bundle-create name/<NAME>", "bundle-create name/Studio kit"),
                Arguments.of("bundle-add-eqm", "bundle-add-eqm b/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]",
                        "bundle-add-eqm b/001 001 002 003"),
                Arguments.of("bundle-remove-eqm", "bundle-remove-eqm b/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]",
                        "bundle-remove-eqm b/001 001"),
                Arguments.of("bundle-list", "bundle-list", "bundle-list"),
                Arguments.of("bundle-history", "bundle-history b/<BUNDLE ID>", "bundle-history b/001"),
                Arguments.of("bundle-history", "bundle-history name/<NAME>", "bundle-history name/Studio kit"),
                Arguments.of("help", "help", "help"),
                Arguments.of("exit", "exit", "exit"));
    }

    @ParameterizedTest
    @MethodSource("commandExamples")
    void getText_eachCommand_includesSyntaxAndExample(String topic, String format, String example) {
        String text = Help.getText();

        assertTrue(labelledValues(text, "Format:").contains(format), format);
        assertTrue(labelledValues(text, "Example:").contains(example), example);
        assertTrue(text.contains("Alias: " + CommandAliases.aliasFor(topic)), topic);
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
        for (String group : List.of("equipment", "loan-add", "bundle")) {
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
                .filter(line -> line.contains(" ("))
                .map(line -> {
                    int colon = line.indexOf(':');
                    return colon < 0 ? line.strip() : line.substring(colon + 1).strip();
                })
                .flatMap(line -> Arrays.stream(line.split("\\s*\\|\\s*")))
                .map(String::strip)
                .map(label -> label.substring(0, label.indexOf(" (")))
                .filter(topic -> !topic.isEmpty())
                .sorted().toList();
        assertEquals(TOPICS.stream().sorted().toList(), menuTopics,
                "Each supported command must appear exactly once in the grouped menu.");
        for (String topic : TOPICS) {
            assertTrue(text.contains(topic + " (" + CommandAliases.aliasFor(topic) + ")"), topic);
        }
        assertTrue(text.contains("help <COMMAND OR ALIAS>"));
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
        String text = Help.getText("loan-add");
        List<String> lines = text.lines().toList();

        assertEquals("=".repeat(100), lines.getFirst());
        assertEquals("LOAN-ADD", lines.get(1));
        assertEquals(lines.getFirst(), lines.get(2));
        assertEquals("Alias: al", lines.get(3));
        assertTrue(lines.get(4).isBlank());
        assertTrue(text.indexOf("Format:") < text.indexOf("snAp command help"));
        assertTrue(text.contains(System.lineSeparator() + "Notes:" + System.lineSeparator()));
    }

    @Test
    void getText_all_separatesEveryCommandWithSpacedFullWidthHeadingBands() {
        List<String> lines = Help.getText("all").lines().toList();
        List<String> headings = List.of("EQM-ADD", "EQM-LIST", "EQM-SHOW", "EQM-DELETE", "LOAN-ADD", "LOAN-RETURN",
                "LOAN-DELETE", "LOAN-UPDATE", "LOAN-LIST", "VIEW LOAN CONDITION ASSESSMENTS", "LOAN-SHOW",
                "LOAN-LIST-BORROWER", "LOAN-LIST-OVERDUE", "EQM-LOAN-HISTORY", "EQM-FILTER", "EQM-UPDATE",
                "EQM-MAINTAIN", "BUNDLE-CREATE", "BUNDLE-ADD-EQM", "BUNDLE-REMOVE-EQM", "BUNDLE-LIST",
                "BUNDLE-HISTORY", "BACKUP", "RESTORE", "HELP",
                "EXIT");
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
            assertTrue(lines.get(index + 2).startsWith("Alias: "), heading);
            assertTrue(lines.get(index + 3).isBlank(), "Separate the heading from its command syntax: " + heading);
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
        assertEquals(Help.getText("eqm-delete"), Help.getText(" \teqm-delete\u00a0 "));
        assertEquals(Help.getText("loan-delete"), Help.getText(" \tloan-delete\u00a0 "));
        assertEquals(Help.getText("loan-list"), Help.getText(" \tloan-list\u00a0 "));
        assertEquals(Help.getText("loan-show"), Help.getText(" \tloan-show\u00a0 "));
        assertEquals(Help.getText("loan-list-overdue"), Help.getText(" \tloan-list-overdue\u00a0 "));
        assertEquals(Help.getText("bundle-list"), Help.getText(" \tbundle-list\u00a0 "));
        assertEquals(Help.getText("bundle-history"), Help.getText(" \tbundle-history\u00a0 "));
        assertEquals(Help.getText("loan-list-borrower"), Help.getText(" \tloan-list-borrower\u00a0 "));
        for (String topic : TOPICS) {
            assertEquals(Help.getText(topic), Help.getText(CommandAliases.aliasFor(topic)), topic);
        }
        assertFalse(labelledValues(Help.getText("eqm-list"), "Format:").contains("loan-list"));
        assertFalse(labelledValues(Help.getText("eqm-delete"), "Format:").contains("loan-delete l/<LOAN ID>"));
        assertFalse(labelledValues(Help.getText("eqm-loan-history"), "Format:")
                .contains("bundle-history b/<BUNDLE ID>"));
    }

    /** Supplies misspellings, partial topics and aliases that must not select a command guide. */
    private static Stream<String> unknownTopics() {
        return Stream.of("unknown", "HELP", "ADD", "helpall", "helpful", "equipment", "loans", "bundles",
                "delete", "list loan", "list items", "loan-list extra", "view", "loan-show l/001", "loan-view extra",
                "bundle-history b/001", "bundle-history name/Studio kit", "bundle-remove-eqm b/001 001",
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
        assertTrue(proseFor("eqm-add").contains("Name, unused Item ID, type and condition are required"));
        assertTrue(proseFor("eqm-add").contains("omitted damage defaults to NONE;"));
        assertTrue(proseFor("eqm-delete").contains("Active loans and bundle membership block deletion."));
        assertTrue(proseFor("loan-add").contains("Omitted issued/ defaults to today's date"));
        assertTrue(proseFor("loan-add").contains("Issue date must be on or before the required due date."));
        assertTrue(proseFor("loan-return").contains("explicit serviceable/ and damage/ assessment"));
        assertTrue(proseFor("loan-return").contains(
                "For other damage values, omitted or empty remarks clear prior notes."));
        assertTrue(proseFor("loan-delete").contains(
                "Deleting returned history leaves newer active loans unchanged."));
        assertTrue(proseFor("loan-list-overdue").contains("Returned loans and loans due today or later are excluded."));
        assertTrue(proseFor("eqm-loan-history").contains("Reused Item IDs keep separate histories."));
        assertTrue(proseFor("eqm-filter").contains("All filters must match (AND)."));
        assertTrue(proseFor("eqm-update").contains("Omitted fields retain their current values."));
        assertTrue(proseFor("bundle-create").contains(
                "Bundles and their equipment memberships are restored between application runs"));
        assertTrue(proseFor("bundle-add-eqm").contains(
                "exactly one camera, at least one lens and at least one battery."));
        assertTrue(proseFor("exit").contains("A save failure keeps the application running;"));
    }

    @ParameterizedTest
    @MethodSource("subtypeDamageChoices")
    void getText_addAndReturnTopics_showDamageValuesUnderTheCorrectType(
            String type, String fields, Enum<?>[] damages) {
        String choices = String.join(", ", Arrays.stream(damages).map(Enum::name).toList());

        assertTrue(proseFor("eqm-add").contains(type + ": " + fields + "; damage: " + choices), type);
        assertTrue(proseFor("loan-return").contains(type + ": " + choices), type);
    }

    /** Checks each subtype separately so damage listed under the wrong type cannot satisfy the test. */
    private static Stream<Arguments> subtypeDamageChoices() {
        return Stream.of(
                Arguments.of("camera", "sensor/<TEXT> mount/<TEXT> thread/<TEXT> battery/<TEXT>",
                        Camera.CameraDamage.values()),
                Arguments.of("lens", "zoom/<TEXT> mount/<TEXT> sensor/<TEXT>", Lens.LensDamage.values()),
                Arguments.of("battery", "capacity/<TEXT> battery/<TEXT>", Battery.BatteryDamage.values()),
                Arguments.of("tripod", "thread/<TEXT>", Tripod.TripodDamage.values()));
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
        assertTrue(proseFor("loan-update").contains(
                "Reassigning to another item releases the old item and captures the replacement's condition."));
        assertTrue(proseFor("loan-update").contains(
                "Changing only the borrower or dates preserves the recorded issue condition."));
    }

    @Test
    void getText_viewLoan_explainsCapturedConditionLegacyRecordsAndReadOnlyBehaviour() {
        String text = proseFor("loan-show");

        assertTrue(text.contains(
                "Shows the equipment type, condition, damage and remarks recorded when the loan was issued."));
        assertTrue(text.contains(
                "Legacy loans without a saved snapshot report that the issue condition was not recorded."));
        assertTrue(text.contains("this shows the replacement's condition at reassignment."));
        assertTrue(text.contains("This read-only command does not change equipment, loans or saved records."));
    }

    @Test
    void getText_maintenance_explainsRequiredFieldsAndPersistence() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Requires an existing Item ID, a valid maintenance date"));
        assertTrue(text.contains("Maintenance records are saved to maintenance.txt"));
        assertTrue(text.contains("Maintenance work: 1-200 characters; outcomes: 1-100 characters."));
        assertTrue(proseFor("eqm-maintain").contains("in the displayed field order."));
        assertTrue(proseFor("eqm-maintain").contains("cannot contain tabs or line breaks."));
        assertTrue(proseFor("eqm-maintain").contains(
                "Recording maintenance does not change condition, damage, remarks or serviceability."));
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
        assertTrue(text.contains("Type is fixed; type/ must match the current type, ignoring letter case."));
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
        assertTrue(text.contains("Names must be unique; name comparisons are case-sensitive."));
        assertTrue(text.contains("Known compatibility mismatches are rejected."));
        assertTrue(text.contains(
                "Missing compatibility details or unrelated equipment types do not block addition."));
        assertTrue(text.contains("Format: bundle-remove-eqm b/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]"));
        assertTrue(text.contains("eligible for deletion after it has been removed from every bundle."));
        assertTrue(proseFor("bundle-remove-eqm").contains(
                "Successful removals are saved immediately; a save failure restores the previous memberships."));
        assertTrue(proseFor("bundle-add-eqm").contains(
                "A rejected item does not undo earlier additions."));
        assertTrue(proseFor("bundle-add-eqm").contains("Save additions with exit before exiting."));
        assertTrue(proseFor("bundle-remove-eqm").contains(
                "Loaned and unserviceable items may be removed from a bundle."));
        assertFalse(text.contains("Bundles exist only for the current session"));
        assertFalse(text.contains("Shows current-session bundles"));
    }

    @Test
    void getText_bundleHistory_explainsBothSelectorsOrderingAndEmptyResults() {
        String text = proseFor("bundle-history");

        assertTrue(labelledValues(Help.getText("bundle-history"), "Format:")
                .containsAll(List.of("bundle-history b/<BUNDLE ID>", "bundle-history name/<NAME>")));
        assertTrue(text.contains(
                "Includes active and returned loans for every equipment item in the selected bundle."));
        assertTrue(text.contains("Results are sorted by issue date, earliest first, then Loan ID."));
        assertTrue(text.contains("An empty bundle history and unknown bundle are reported clearly."));
        assertTrue(text.contains("Uses current bundle membership, including loans issued before an item joined"));
        assertTrue(text.contains("Removing an item excludes its loans from this view."));
        assertTrue(text.contains("Names must match exactly, including case."));
    }

    @Test
    void getText_overdueLoans_explainsEligibilityAndReadOnlyBehaviour() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Format: loan-list-overdue"));
        assertTrue(text.contains(
                "Shows active loans with due dates earlier than today's local date."));
        assertTrue(text.contains(
                "Returned loans and loans due today or later are excluded."));
        assertTrue(text.contains(
                "Uses the same columns and DD-MM-YY dates as loan-list, with Overdue state and a count."));
    }

    @Test
    void getText_backupAndRestore_explainSavedFilesExclusionsAndReplacement() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("Copies saved equipment.txt, loans.txt and loan-sequence.txt,"
                + " plus backup-manifest.txt."));
        assertTrue(proseFor("backup").contains("Bundles and maintenance records are not included."));
        assertTrue(proseFor("backup").contains("Backup does not save unsaved changes."));
        assertTrue(text.contains("The destination must be outside the live data directory"));
        assertTrue(text.contains("Validates the manifest and every equipment, loan and sequence record"));
        assertTrue(proseFor("restore").contains("All bundles and their saved memberships are cleared."));
        assertTrue(proseFor("restore").contains("Maintenance records remain unchanged."));
        assertTrue(proseFor("restore").contains("A failed restore attempts to restore the previous saved records"));
        assertTrue(proseFor("restore").contains("Restore is available after a startup loading error."));
        assertFalse(text.contains("session-only"));
    }

    @Test
    void getText_borrowerLoans_explainsFilteringAliasesAndReadOnlyBehaviour() {
        String text = normaliseWhitespace(Help.getText());

        assertTrue(text.contains("LOAN-LIST-BORROWER"));
        assertFalse(text.contains("list member"));
        assertTrue(text.contains("Shows only active equipment held by the selected borrower"));
        assertTrue(text.contains("Returned loans and loans held by other borrowers are excluded."));
        assertTrue(text.contains("An unknown borrower ID and a known borrower with no active loans"));
        assertTrue(proseFor("loan-list-borrower").contains(
                "A borrower is known when at least one active or returned loan uses that ID."));
        assertTrue(text.contains("This read-only query does not change equipment, loans or saved files."));
    }

    @Test
    void getText_helpAndExit_explainArgumentsSaveFailureAndImplementedCommandsOnly() {
        String text = Help.getText();

        assertTrue(labelledValues(text, "Format:").containsAll(List.of("help", "help <COMMAND>", "help all")));
        assertTrue(labelledValues(text, "Example:").containsAll(List.of("help", "help loan-add", "help all")));
        text = normaliseWhitespace(text);
        assertTrue(text.contains("Saves equipment, loans, maintenance records and bundles before exiting."));
        assertTrue(text.contains("A save failure keeps the application running;"));
        assertTrue(text.contains("fix the reported file problem and retry exit."));
        assertTrue(proseFor("help").contains("either a full command name or its alias"));
        assertFalse(proseFor("help").contains("list member"));
        assertTrue(proseFor("exit").contains(
                "After a startup loading error, exit exits without saving the failed data set."));
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
        return Stream.concat(Stream.of(null, "", " \t\u2003 ", "all", "unknown", "HELP", "list\u00a0loans",
                "list member"), Stream.concat(TOPICS.stream(), TOPICS.stream().map(CommandAliases::aliasFor)));
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
