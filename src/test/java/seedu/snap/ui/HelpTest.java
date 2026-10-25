package seedu.snap.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
    /** Supplies the user-facing syntax and example for each supported command. */
    private static Stream<Arguments> commandExamples() {
        return Stream.of(
                Arguments.of("add i/<NAME> id/<ID> type/<TYPE> cond/<N>"
                        + " [TYPE DETAIL] [damage/<DAMAGE>] [remarks/<TEXT>]",
                        "add i/Canon EOS R10 id/001 type/camera cond/1 sensor/APS-C damage/NONE"),
                Arguments.of("list", "list"),
                Arguments.of("delete item/<ID>", "delete item/001"),
                Arguments.of("loan id/<ITEM ID> b/<BORROWER ID> [issued/<DD-MM-YY>] due/<DD-MM-YY>",
                        "loan id/001 b/002 issued/24-10-26 due/31-10-26"),
                Arguments.of("return loan/<LOAN ID> serviceable/<yes|no> damage/<DAMAGE> [remarks/<TEXT>]",
                        "return loan/001 serviceable/no damage/SENSOR_DAMAGE remarks/Sensor needs repair"),
                Arguments.of("delete loan/<LOAN ID>", "delete loan/001"),
                Arguments.of("list loans", "list loans"),
                Arguments.of("history id/<ITEM ID>", "history id/001"),
                Arguments.of("filter [id/<ID>] [i/<NAME>] [type/<TYPE>] [cond/<N|MIN-MAX>] [status/<STATUS>]",
                        "filter type/camera cond/1-3 status/available"),
                Arguments.of("update id/<ID> [i/<NAME>] [type/<TYPE>] [cond/<N>]",
                        "update id/001 i/Studio camera cond/5"),
                Arguments.of("add-bundle name/<NAME>", "add-bundle name/Studio kit"),
                Arguments.of("add-to-bundle id/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]",
                        "add-to-bundle id/001 001 002 003"),
                Arguments.of("list bundles", "list bundles"),
                Arguments.of("help", "help"),
                Arguments.of("bye", "bye"));
    }

    @ParameterizedTest
    @MethodSource("commandExamples")
    void getText_eachCommand_includesSyntaxAndExample(String format, String example) {
        String text = Help.getText();

        assertTrue(text.lines().anyMatch(line -> line.equals("Format: " + format)), format);
        assertTrue(text.lines().anyMatch(line -> line.equals("Example: " + example)), example);
    }

    @Test
    void getText_notationAndLimits_distinguishOptionalFieldsAndIdentifierRanges() {
        String text = Help.getText();

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
        String text = Help.getText();

        assertTrue(text.contains("Name, unused Item ID, type and condition are required"));
        assertTrue(text.contains("Optional order: type detail, then damage/, then final remarks/."));
        assertTrue(text.contains("Omitted type detail defaults to unspecified; omitted damage defaults to NONE;"));
        assertTrue(text.contains("omitted remarks default to empty text."));
        assertTrue(text.contains("camera: sensor/<TEXT>; damage:"));
        assertTrue(text.contains("lens: zoom/<TEXT>; damage:"));
        assertTrue(text.contains("battery: capacity/<TEXT>; damage:"));
        assertTrue(text.contains("tripod: thread/<TEXT>; damage:"));
        Stream.of(Camera.CameraDamage.values(), Lens.LensDamage.values(), Battery.BatteryDamage.values(),
                Tripod.TripodDamage.values()).flatMap(Stream::of)
                .forEach(damage -> assertTrue(text.contains(damage.name()), damage.name()));
    }

    @Test
    void getText_loanAndReturn_explainDateDefaultsAssessmentAndRetainedHistory() {
        String text = Help.getText();

        assertTrue(text.contains("Item ID, borrower ID and due/ are required."));
        assertTrue(text.contains("Omitted issued/ defaults to today's date"));
        assertTrue(text.contains("local time zone, at midnight."));
        assertTrue(text.contains("Issue date must be on or before the required due date."));
        assertTrue(text.contains("be serviceable and have no active loan."));
        assertTrue(text.contains("Loan IDs are assigned automatically from 001 to 999"));
        assertTrue(text.contains("explicit serviceable/ and damage/ assessment"));
        assertTrue(text.contains("Omitted or empty remarks clear the item's previous remarks."));
        assertTrue(text.contains("Return permanently marks the retained loan returned"));
        assertTrue(text.contains("cannot be returned twice or reactivated."));
        assertTrue(text.contains("Changing its numeric condition does not repair it."));
    }

    @Test
    void getText_historyAndDeletion_distinguishPhysicalItemsAndLoanRemoval() {
        String text = Help.getText();

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
        String text = Help.getText();

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
    void getText_bundles_explainSessionLifetimeCompositionAndAvailability() {
        String text = Help.getText();

        assertTrue(text.contains("Requires a name of 1-100 characters."));
        assertTrue(text.contains("Bundles exist only for the current session"));
        assertTrue(text.contains("at least one existing, serviceable, unloaned equipment item."));
        assertTrue(text.contains("An item may appear only once per bundle."));
        assertTrue(text.contains("exactly one camera, at least one lens and at least one battery."));
        assertTrue(text.contains("A tripod is optional, with at most one."));
    }

    @Test
    void getText_helpAndExit_explainArgumentsSaveFailureAndImplementedCommandsOnly() {
        String text = Help.getText();

        assertTrue(text.contains("Shows this complete command guide. No arguments are accepted."));
        assertTrue(text.contains("Saves equipment and loans before exiting."));
        assertTrue(text.contains("A save failure keeps the application running;"));
        assertTrue(text.contains("fix the reported file problem and retry bye."));
        assertFalse(text.contains("Format: overdue"));
        assertFalse(text.contains("Format: repair"));
        assertFalse(text.contains("Format: help <"));
    }

    @Test
    void print_completeGuide_usesPlatformLineEndingsAndOneFinalNewline() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            Help.print();
        } finally {
            System.setOut(original);
        }
        String text = output.toString(StandardCharsets.UTF_8);
        String lineSeparator = System.lineSeparator();

        assertEquals(Help.getText(), text);
        assertTrue(text.endsWith(lineSeparator));
        assertFalse(text.endsWith(lineSeparator + lineSeparator));
        assertFalse(text.replace(lineSeparator, "").contains("\n"));
        assertFalse(text.replace(lineSeparator, "").contains("\r"));
    }
}
