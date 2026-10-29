package seedu.snap.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.Lens;
import seedu.snap.equipment.Tripod;
import seedu.snap.maintenance.MaintenanceRecord;

/** Provides command help without reading or changing application records. */
public final class Help {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final int TEXT_WIDTH = 100;
    private static final String COMMAND_DIVIDER = "=".repeat(TEXT_WIDTH);
    private static final String OVERVIEW = createOverview();
    private static final List<CommandHelp> COMMANDS = createCommands();
    private static final String TEXT = createText();
    private static final String MENU = createMenu();

    private Help() {
        // Utility class; do not instantiate.
    }

    /** Returns the complete command guide with platform line endings and a final newline. */
    public static String getText() {
        return TEXT;
    }

    /**
     * Selects the menu, complete reference or an exact command's reference.
     *
     * @param topic command name, all, or an empty value for the compact menu
     * @return plain help text, or an error followed by the menu for an unknown topic
     */
    public static String getText(String topic) {
        return getText(topic, false);
    }

    /** Applies colour only to unknown-topic errors when building text for terminal output. */
    private static String getText(String topic, boolean colourErrors) {
        String normalised = topic == null ? "" : topic.replaceAll("(?U)^\\s+|\\s+$", "")
                .replaceAll("(?U)\\s+", " ");
        if (normalised.isEmpty()) {
            return MENU;
        }
        if (normalised.equals("all")) {
            return TEXT;
        }
        if (normalised.equals("list member")) {
            normalised = "list borrower";
        }
        for (CommandHelp command : COMMANDS) {
            if (command.command().equals(normalised)) {
                return formatReference(command.text(), true) + LINE_SEPARATOR + OVERVIEW;
            }
        }
        return ConsoleOutput.formatError("Unknown help topic. Choose a command from the menu.", colourErrors)
                + LINE_SEPARATOR + LINE_SEPARATOR + MENU;
    }

    /** Prints the compact command menu without changing application state. */
    public static void print() {
        print("");
    }

    /** Prints the selected reference or a useful response for an unknown topic. */
    public static void print(String topic) {
        System.out.print(getText(topic, ConsoleOutput.usesColour()));
    }

    /** Builds common notation and limits from the equipment model. */
    private static String createOverview() {
        return formatReference(lineEndings(String.format(Locale.ROOT, """
                snAp command help
                <VALUE> is a placeholder. [field/<VALUE>] is optional. Do not type angle or square brackets.

                Commands and field prefixes use exact lowercase spelling. Use spaces as shown in each format.

                Equipment, loan and bundle IDs: exactly three digits, 001-999. Borrower IDs: 000-999.
                Equipment names: 1-%d characters; condition: %d-%d; remarks: 0-%d characters after trimming.
                Maintenance work: 1-%d characters; outcomes: 1-%d characters. Both must remain on one line.
                Type details: 1-100 characters. Type details and remarks must remain on one line.

                Empty supplied details, damage, IDs or dates are invalid; defaults apply to omitted fields.
                Wrapped formats and examples must be entered as a single line.
                """,
                Equipment.MAX_NAME_LENGTH, Equipment.MIN_CONDITION, Equipment.MAX_CONDITION,
                Equipment.MAX_REMARKS_LENGTH, MaintenanceRecord.MAX_WORK_LENGTH,
                MaintenanceRecord.MAX_OUTCOME_LENGTH)), false);
    }

    /** Keeps each command's description in one immutable entry for every help view. */
    private static List<CommandHelp> createCommands() {
        return List.of(
                new CommandHelp("add", "Equipment", lineEndings(String.format(Locale.ROOT, """
                        ADD EQUIPMENT
                        Format: add i/<NAME> id/<ID> type/<TYPE> cond/<N> [TYPE DETAIL] [damage/<DAMAGE>] \
                        [remarks/<TEXT>]
                        Example: add i/Canon EOS R10 id/001 type/camera cond/1 sensor/APS-C mount/Canon RF damage/NONE
                        Name, unused Item ID, type and condition are required, in that order.

                        Type and damage values are case-insensitive. Use the field and damage list for the type:
                          camera: sensor/<TEXT> mount/<TEXT> thread/<TEXT> battery/<TEXT>; damage: %s
                          lens: zoom/<TEXT> mount/<TEXT> sensor/<TEXT>; damage: %s
                          battery: capacity/<TEXT> battery/<TEXT>; damage: %s
                          tripod: thread/<TEXT>; damage: %s

                        Optional order: subtype fields in the order shown, then damage/, then final remarks/.
                        Each optional field may be omitted independently, except remarks/ with damage OTHER.

                        Omitted subtype and compatibility details default to unspecified;
                        omitted damage defaults to NONE;
                        omitted remarks default to empty text, except OTHER requires non-empty remarks.
                        Text after final remarks/ is kept as remarks.

                        Compatibility comparisons ignore letter case. Missing comparison data produces UNKNOWN;
                        a known mismatch produces INCOMPATIBLE, even when another comparison value is unknown.
                        """,
                        damageValues(Camera.CameraDamage.values()),
                        damageValues(Lens.LensDamage.values()), damageValues(Battery.BatteryDamage.values()),
                        damageValues(Tripod.TripodDamage.values())))),
                new CommandHelp("list", "Equipment", lineEndings("""
                        LIST EQUIPMENT
                        Format: list
                        Example: list
                        Shows all equipment with Item ID, name, type, numeric condition and status in a compact table.
                        Long names end in ...; use show id/<ID> for full values, subtype details, damage and remarks.
                        Status is Available, Loaned or Unserviceable.
                        """)),
                new CommandHelp("show", "Equipment", lineEndings("""
                        SHOW EQUIPMENT
                        Format: show id/<ID>
                        Example: show id/001
                        Requires an existing Item ID from 001 to 999. Extra fields are not accepted.
                        Shows the full name, type, numeric condition, status, serviceability and active Loan ID.
                        Includes all subtype and compatibility fields, damage and remarks, without truncation.
                        Long values wrap onto indented lines; (none) marks an empty value.
                        Tabs and control characters are shown as escapes so they cannot change the layout.
                        This read-only command does not change records, identifiers or saved data.
                        """)),
                new CommandHelp("delete item", "Equipment", lineEndings("""
                        DELETE EQUIPMENT
                        Format: delete item/<ID>
                        Example: delete item/001
                        Requires an existing Item ID. Active loans and bundle membership block deletion.

                        Existing loans remain in list loans; a replacement using this Item ID has separate history.
                        """)),
                new CommandHelp("loan", "Loans", lineEndings("""
                        ADD LOAN
                        Format: loan id/<ITEM ID> b/<BORROWER ID> [issued/<DD-MM-YY>] due/<DD-MM-YY>
                        Example: loan id/001 b/002 issued/24-10-26 due/31-10-26
                        Item ID, borrower ID and due/ are required. Keep the displayed field order.

                        Omitted issued/ defaults to today's date in the application's local time zone, at midnight.
                        Both dates use DD-MM-YY. Issue date must be on or before the required due date.

                        Equipment must exist, be serviceable and have no active loan.

                        Loan IDs are assigned automatically from 001 to 999 and are never reused after deletion.
                        Success shows the assigned Loan ID, item ID and name, borrower, issue date and due date.
                        """)),
                new CommandHelp("return", "Loans", lineEndings(String.format(Locale.ROOT, """
                        RETURN EQUIPMENT
                        Format: return loan/<LOAN ID> serviceable/<yes|no> damage/<DAMAGE> [remarks/<TEXT>]
                        Example: return loan/001 serviceable/no damage/SENSOR_DAMAGE remarks/Sensor needs repair
                        Requires an active Loan ID and explicit serviceable/ and damage/ assessment, in that order.
                        Serviceable yes/no and subtype damage values are case-insensitive; use NONE for no damage.

                        Serviceability determines whether the item may be borrowed again; damage alone does not.

                        Remarks/ must be last. For OTHER, provide non-empty text after trimming.
                        For other damage values, omitted or empty remarks clear prior notes.
                        The assessment replaces damage and remarks, preserving subtype details,
                        compatibility attributes and bundle memberships.

                        Return permanently marks the retained loan returned; it cannot be returned twice or reactivated.
                        Unserviceable equipment remains unavailable. Changing its numeric condition does not repair it.
                        Success shows the Loan ID, item ID and name, and resulting Available or Unserviceable status.

                        Damage choices by equipment type:
                          camera: %s
                          lens: %s
                          battery: %s
                          tripod: %s
                        """,
                        damageValues(Camera.CameraDamage.values()),
                        damageValues(Lens.LensDamage.values()), damageValues(Battery.BatteryDamage.values()),
                        damageValues(Tripod.TripodDamage.values())))),
                new CommandHelp("delete loan", "Loans", lineEndings("""
                        DELETE LOAN
                        Format: delete loan/<LOAN ID>
                        Example: delete loan/001
                        Removes one loan record and its history. Deleting an active loan releases its equipment.

                        Deleting returned history leaves newer active loans unchanged. Deleted IDs are never reused.
                        """)),
                new CommandHelp("update loan", "Loans", lineEndings("""
                        UPDATE LOAN
                        Format: update loan/<LOAN ID> [id/<ITEM ID>] [b/<BORROWER ID>] \
                        [issued/<DD-MM-YY>] [due/<DD-MM-YY>]
                        Example: update loan/001 id/002 b/007 due/07-11-26
                        Requires one existing active Loan ID and at least one update field,
                        in any order, each prefix once.
                        The Loan ID remains unchanged; omitted fields retain their current values.
                        A replacement item must exist, be serviceable and have no active loan.
                        Reassigning to another item releases the old item and captures the replacement's condition.
                        Changing only the borrower or dates preserves the recorded issue condition.
                        Returned loans cannot be edited.
                        The issue date must not be later than the due date. Invalid updates leave records unchanged.
                        Successful updates are saved to data/loans.txt before live records change.
                        """)),
                new CommandHelp("list loans", "Loans", lineEndings("""
                        LIST LOANS
                        Format: list loans
                        Example: list loans
                        Shows Loan ID, Item ID, borrower, DD-MM-YY dates and Active, Returned or Overdue state.
                        Includes a result count and guidance when no loans are recorded.
                        """)),
                new CommandHelp("view loan", "Loans", lineEndings("""
                        VIEW LOAN ISSUE CONDITION
                        Format: view loan/<LOAN ID>
                        Example: view loan/001
                        Shows the equipment type, condition, damage and remarks recorded when the loan was issued.
                        If update loan assigns another item, this shows the replacement's condition at reassignment.
                        Legacy loans without a saved snapshot report that the issue condition was not recorded.
                        This read-only command does not change equipment, loans or saved records.
                        """)),
                new CommandHelp("list borrower", "Loans", lineEndings("""
                        EQUIPMENT HELD BY MEMBER
                        Format: list borrower/<BORROWER ID>
                        Example: list borrower/002
                        Format: list member/<MEMBER ID>
                        Example: list member/002
                        The equivalent alias list member/<MEMBER ID> is also accepted.
                        Shows only active equipment held by the selected borrower, with loan details.
                        Returned loans and loans held by other borrowers are excluded.
                        A borrower is known when at least one active or returned loan uses that ID.
                        An unknown borrower ID and a known borrower with no active loans receive separate messages.
                        Uses the same dates, states, columns and result counts as list loans.
                        This read-only query does not change equipment, loans or saved files.
                        """)),
                new CommandHelp("list overdue", "Loans", lineEndings("""
                        LIST OVERDUE LOANS
                        Format: list overdue
                        Example: list overdue
                        Shows active loans with due dates earlier than today's local date.
                        Returned loans and loans due today or later are excluded.
                        Uses the same columns and DD-MM-YY dates as list loans, with Overdue state and a count.
                        This read-only command does not change any records.
                        """)),
                new CommandHelp("history", "Loans", lineEndings("""
                        ITEM LOAN HISTORY
                        Format: history id/<ITEM ID>
                        Example: history id/001
                        Requires an existing Item ID. Includes its physical item's active and returned loans.
                        Sorted by issue date, earliest first, then Loan ID. Reused Item IDs keep separate histories.
                        Uses the same dates, states, columns and result counts as list loans.

                        Older returned records without a proven item association remain available in list loans.
                        """)),
                new CommandHelp("summary", "Equipment", lineEndings("""
                        EQUIPMENT SUMMARY
                        Format: summary id/<ITEM ID>
                        Example: summary id/001
                        Displays one item's identity, current details, status, remarks, subtype details and
                        loan history.
                        Maintenance, status-history and storage-location sections are shown when recorded;
                        otherwise a clear empty-section message is displayed. The command is read-only and
                        does not change saved records.
                        """)),
                new CommandHelp("filter", "Equipment", lineEndings("""
                        FILTER EQUIPMENT
                        Format: filter [id/<ID>] [i/<NAME>] [type/<TYPE>] [cond/<N|MIN-MAX>] [status/<STATUS>]
                        Example: filter type/camera cond/1-3 status/available
                        Supply at least one filter, in any order, with each prefix once. All filters must match (AND).

                        Names use case-insensitive partial matching; types use case-insensitive full matching.
                        cond/ accepts one value or an inclusive ascending range, such as 1-3.

                        status/ accepts available or loaned, ignoring letter case.
                        Available excludes active loans and unserviceable items.
                        Uses the same compact table as list. Use show id/<ID> for full equipment values.
                        """)),
                new CommandHelp("update", "Equipment", lineEndings("""
                        UPDATE EQUIPMENT
                        Format: update id/<ID> [i/<NAME>] [type/<TYPE>] [cond/<N>]
                        Example: update id/001 i/Studio camera cond/5
                        Requires an existing Item ID and at least one update field, in any order, each prefix once.

                        Omitted fields retain their current values. Type is fixed; type/ must match the current type,
                        ignoring letter case.
                        Updates retain subtype details, damage, remarks, serviceability, loans and bundle membership.
                        """)),
                new CommandHelp("maintain", "Equipment", lineEndings(String.format(Locale.ROOT, """
                        RECORD MAINTENANCE
                        Format: maintain id/<ITEM ID> date/<DD-MM-YY> work/<DESCRIPTION> outcome/<OUTCOME>
                        Example: maintain id/001 date/27-10-26 work/Replaced battery outcome/Completed
                        Requires an existing Item ID, a valid maintenance date that is not in the future,
                        a non-empty work description and outcome, in the displayed field order.
                        Work descriptions and outcomes are trimmed and cannot contain tabs or line breaks.
                        Maintenance work: 1-%d characters; outcomes: 1-%d characters.

                        Maintenance records are saved to maintenance.txt immediately and remain associated
                        with the physical item. A failed save leaves maintenance records unchanged.
                        Recording maintenance does not change condition, damage, remarks or serviceability.
                        Success shows the Item ID, name, maintenance date, work description and outcome.
                        """, MaintenanceRecord.MAX_WORK_LENGTH, MaintenanceRecord.MAX_OUTCOME_LENGTH))),
                new CommandHelp("add-bundle", "Bundles", lineEndings("""
                        CREATE BUNDLE
                        Format: add-bundle name/<NAME>
                        Example: add-bundle name/Studio kit
                        Requires a name of 1-100 characters. Bundle IDs are assigned automatically, 001-999.
                        Names must be unique; name comparisons are case-sensitive.

                        Save new bundles and additions with bye before exiting.
                        Bundles and their equipment memberships are restored between application runs after bye.
                        """)),
                new CommandHelp("add-to-bundle", "Bundles", lineEndings("""
                        ADD EQUIPMENT TO BUNDLE
                        Format: add-to-bundle id/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]
                        Example: add-to-bundle id/001 001 002 003
                        Requires an existing bundle and at least one existing, serviceable, unloaned equipment item.
                        An item may appear only once per bundle. Each requested Item ID receives its own result.
                        Results use [OK] or [ERROR], followed by totals and the resulting bundle composition.

                        A complete bundle has exactly one camera, at least one lens and at least one battery.
                        A tripod is optional, with at most one. Additional cameras and tripods are rejected.
                        Known compatibility mismatches are rejected. Missing compatibility details or unrelated
                        equipment types do not block addition.
                        Camera/lens comparisons use mount/ and sensor/; camera/battery uses battery/;
                        camera/tripod uses thread/. Comparisons ignore letter case.

                        Items are checked in the supplied order. A rejected item does not undo earlier additions.
                        Save additions with bye before exiting.
                        """)),
                new CommandHelp("remove-from-bundle", "Bundles", lineEndings("""
                        REMOVE EQUIPMENT FROM BUNDLE
                        Format: remove-from-bundle id/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]
                        Example: remove-from-bundle id/001 001
                        Requires an existing bundle and at least one Item ID. Each ID receives its own result.
                        Removes the requested memberships without deleting the equipment. This can make an item
                        eligible for deletion after it has been removed from every bundle.
                        Loaned and unserviceable items may be removed from a bundle.
                        Successful removals are saved immediately; a save failure restores the previous memberships.
                        """)),
                new CommandHelp("list bundles", "Bundles", lineEndings("""
                        LIST BUNDLES
                        Format: list bundles
                        Example: list bundles
                        Shows saved and current bundles, their equipment and whether their composition is complete.
                        Incomplete bundles list missing required types. Includes bundle and member counts.
                        Save new bundles and additions with bye; successful removals are saved immediately.
                        """)),
                new CommandHelp("history bundle", "Bundles", lineEndings("""
                        BUNDLE LOAN HISTORY
                        Format: history bundle/id/<BUNDLE ID>
                        Example: history bundle/id/001
                        Format: history bundle/name/<NAME>
                        Example: history bundle/name/Studio kit
                        Includes active and returned loans for every equipment item in the selected bundle.
                        Uses current bundle membership, including loans issued before an item joined the bundle.
                        Removing an item excludes its loans from this view. Names must match exactly, including case.
                        Results are sorted by issue date, earliest first, then Loan ID.
                        Uses the same DD-MM-YY dates, states, columns and result counts as list loans.
                        An empty bundle history and unknown bundle are reported clearly.
                        """)),
                new CommandHelp("backup", "General", lineEndings("""
                        BACK UP DATA
                        Format: backup path/<DIRECTORY>
                        Example: backup path/backups/session-1
                        Copies saved equipment.txt, loans.txt and loan-sequence.txt, plus backup-manifest.txt.
                        Bundles and maintenance records are not included. Backup does not save unsaved changes.
                        The destination must be outside the live data directory and must not already exist.
                        Neither backup nor restore paths may be a parent of the live data directory.
                        Backup creation does not change live records. Paths may contain spaces after path/.
                        """)),
                new CommandHelp("restore", "General", lineEndings("""
                        RESTORE DATA
                        Format: restore path/<DIRECTORY>
                        Example: restore path/backups/session-1
                        Requires an existing backup directory outside the live data directory.
                        Validates the manifest and every equipment, loan and sequence record before replacing live data.
                        Successful restores replace equipment, loans and the next Loan ID with the saved backup.
                        All bundles and their saved memberships are cleared. Maintenance records remain unchanged.
                        A failed restore attempts to restore the previous saved records; report any rollback error.
                        Restore is available after a startup loading error. Other commands are blocked except bye.
                        """)),
                new CommandHelp("help", "General", lineEndings("""
                        HELP
                        Format: help
                        Example: help
                        Shows a compact command menu grouped by equipment, loans, bundles and general commands.
                        Format: help <COMMAND>
                        Example: help loan
                        Shows one command's syntax, examples, required and optional fields, defaults and restrictions.
                        Use the full command name for multiword topics, such as help list overdue or help delete loan.
                        help list member is an alias for help list borrower.
                        Format: help all
                        Example: help all
                        Shows the complete command guide.

                        Unknown topics report an error and show the command menu.
                        Help never changes records, identifiers or saved data, and never ends the session.
                        """)),
                new CommandHelp("bye", "General", lineEndings("""
                        EXIT
                        Format: bye
                        Example: bye
                        Saves equipment, loans, maintenance records and bundles before exiting. A save failure keeps
                        the application running; fix the reported file problem and retry bye.
                        After a startup loading error, bye exits without saving the failed data set.
                        """)));
    }

    /** Builds the full reference from the same descriptions used for topic help. */
    private static String createText() {
        String commandGap = LINE_SEPARATOR.repeat(2);
        return OVERVIEW + commandGap + COMMANDS.stream()
                .map(command -> formatReference(command.text(), true)).collect(Collectors.joining(commandGap));
    }

    /** Groups command names from the reference so the menu cannot omit an existing entry. */
    private static String createMenu() {
        String border = "+" + "-".repeat(TEXT_WIDTH - 2) + "+" + LINE_SEPARATOR;
        StringBuilder menu = new StringBuilder(border);
        appendMenuRow(menu, "snAp command menu");
        menu.append(border);
        for (String group : List.of("Equipment", "Loans", "Bundles", "General")) {
            List<String> groupCommands = COMMANDS.stream().filter(command -> command.group().equals(group))
                    .map(CommandHelp::command).toList();
            appendMenuGroup(menu, group, groupCommands);
        }
        appendMenuRow(menu, "");
        appendMenuRow(menu, "Type help <COMMAND> for details (for example, help loan or help list overdue).");
        appendMenuRow(menu, "Type help all for the complete reference.");
        return menu.append(border).toString();
    }

    /** Adds a command group to the menu, wrapping long groups without splitting command names. */
    private static void appendMenuGroup(StringBuilder menu, String group, List<String> commands) {
        String prefix = group + ": ";
        String row = prefix;
        for (String command : commands) {
            String separator = row.equals(prefix) ? "" : " | ";
            String candidate = row + separator + command;
            if (candidate.length() > TEXT_WIDTH - 4 && !row.equals(prefix)) {
                appendMenuRow(menu, row);
                row = " ".repeat(prefix.length()) + command;
            } else {
                row = candidate;
            }
        }
        appendMenuRow(menu, row);
    }

    /** Pads each menu row to the same width, including its ASCII borders. */
    private static void appendMenuRow(StringBuilder menu, String text) {
        menu.append("| ").append(text).append(" ".repeat(TEXT_WIDTH - 4 - text.length()))
                .append(" |").append(LINE_SEPARATOR);
    }

    /** Separates headings, command examples and note paragraphs while preserving their content. */
    private static String formatReference(String text, boolean showNotesHeading) {
        List<String> lines = text.lines().toList();
        String title = lines.getFirst();
        StringBuilder formatted = new StringBuilder();
        if (showNotesHeading) {
            formatted.append(COMMAND_DIVIDER).append(LINE_SEPARATOR);
        }
        formatted.append(title).append(LINE_SEPARATOR)
                .append(showNotesHeading ? COMMAND_DIVIDER : "-".repeat(title.length()))
                .append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        boolean needsNotesHeading = showNotesHeading;
        int index = 1;
        while (index < lines.size()) {
            String line = lines.get(index);
            if (line.isBlank()) {
                index++;
                continue;
            }
            if (isCommandExample(line)) {
                int colon = line.indexOf(':');
                formatted.append(line, 0, colon + 1).append(LINE_SEPARATOR);
                appendWrapped(formatted, line.substring(colon + 1).strip(), "  ", "  ");
                formatted.append(LINE_SEPARATOR);
                needsNotesHeading = showNotesHeading;
                index++;
                continue;
            }
            if (needsNotesHeading) {
                formatted.append("Notes:").append(LINE_SEPARATOR);
                needsNotesHeading = false;
            }
            if (line.startsWith("  ")) {
                while (index < lines.size() && lines.get(index).startsWith("  ")) {
                    appendWrapped(formatted, lines.get(index++).strip(), "    ", "      ");
                }
            } else {
                StringBuilder paragraph = new StringBuilder();
                while (index < lines.size() && !lines.get(index).isBlank()
                        && !isCommandExample(lines.get(index)) && !lines.get(index).startsWith("  ")) {
                    if (!paragraph.isEmpty()) {
                        paragraph.append(' ');
                    }
                    paragraph.append(lines.get(index++));
                }
                appendWrapped(formatted, paragraph.toString(), "  ", "  ");
            }
            formatted.append(LINE_SEPARATOR);
        }
        return formatted.toString().stripTrailing() + LINE_SEPARATOR;
    }

    /** Identifies syntax and example lines before their display labels are separated. */
    private static boolean isCommandExample(String line) {
        return line.startsWith("Format: ") || line.startsWith("Example: ");
    }

    /** Wraps at spaces so words and field values are retained, with indented continuation lines. */
    private static void appendWrapped(StringBuilder output, String text, String indent, String continuation) {
        StringBuilder line = new StringBuilder(indent);
        int contentStart = indent.length();
        for (String word : text.split(" +")) {
            if (line.length() > contentStart && line.length() + 1 + word.length() > TEXT_WIDTH) {
                output.append(line).append(LINE_SEPARATOR);
                line = new StringBuilder(continuation);
                contentStart = continuation.length();
            }
            if (line.length() > contentStart) {
                line.append(' ');
            }
            line.append(word);
        }
        output.append(line).append(LINE_SEPARATOR);
    }

    /** Converts the fixed text blocks to platform line endings. */
    private static String lineEndings(String text) {
        return text.replace("\n", LINE_SEPARATOR);
    }

    /** Lists the supported enum values without suggesting extra command syntax. */
    private static <T extends Enum<T>> String damageValues(T[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
    }

    /** Associates one exact command with its menu group and complete reference. */
    private record CommandHelp(String command, String group, String text) {
    }
}
