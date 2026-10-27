package seedu.snap.ui;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.Lens;
import seedu.snap.equipment.Tripod;

/** Provides command syntax, examples, defaults and restrictions without reading or changing application records. */
public final class Help {
    private static final String TEXT = createText();

    private Help() {
        // Utility class; do not instantiate.
    }

    /** Returns the complete command guide with platform line endings and a final newline. */
    public static String getText() {
        return TEXT;
    }

    /** Prints the complete command guide without changing application state. */
    public static void print() {
        System.out.print(TEXT);
    }

    /** Builds a fixed guide from equipment limits and the supported subtype damage enums. */
    private static String createText() {
        return String.format(Locale.ROOT, """
                snAp command help
                <VALUE> is a placeholder. [field/<VALUE>] is optional. Do not type angle or square brackets.
                Commands and field prefixes use exact lowercase spelling. Separate fields with whitespace.
                Equipment, loan and bundle IDs: exactly three digits, 001-999. Borrower IDs: 000-999.
                Equipment names: 1-%d characters; condition: %d-%d; remarks: 0-%d characters after trimming.
                Type details: 1-100 characters. Type details and remarks must remain on one line.
                Empty supplied details, damage, IDs or dates are invalid; defaults apply to omitted fields.

                ADD EQUIPMENT
                Format: add i/<NAME> id/<ID> type/<TYPE> cond/<N> [TYPE DETAIL] [damage/<DAMAGE>] [remarks/<TEXT>]
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

                LIST EQUIPMENT
                Format: list
                Example: list
                Shows all equipment, including status, subtype details, damage and remarks.
                Status is Available, Loaned or Unserviceable.

                DELETE EQUIPMENT
                Format: delete item/<ID>
                Example: delete item/001
                Requires an existing Item ID. Active loans and bundle membership block deletion.
                Existing loans remain in list loans; a replacement using this Item ID has separate history.

                ADD LOAN
                Format: loan id/<ITEM ID> b/<BORROWER ID> [issued/<DD-MM-YY>] due/<DD-MM-YY>
                Example: loan id/001 b/002 issued/24-10-26 due/31-10-26
                Item ID, borrower ID and due/ are required. Keep the displayed field order.
                Omitted issued/ defaults to today's date in the application's local time zone, at midnight.
                Both dates use DD-MM-YY. Issue date must be on or before the required due date.
                Equipment must exist, be serviceable and have no active loan.
                Loan IDs are assigned automatically from 001 to 999 and are never reused after deletion.

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

                DELETE LOAN
                Format: delete loan/<LOAN ID>
                Example: delete loan/001
                Removes one loan record and its history. Deleting an active loan releases its equipment.
                Deleting returned history leaves newer active loans unchanged. Deleted IDs are never reused.

                LIST LOANS
                Format: list loans
                Example: list loans
                Shows active and returned loans with Loan ID, Item ID, borrower, dates and returned status.

                VIEW LOAN ISSUE CONDITION
                Format: view loan/<LOAN ID>
                Example: view loan/001
                Shows the equipment type, condition, damage and remarks recorded when the loan was issued.
                Legacy loans without a saved snapshot report that the issue condition was not recorded.
                This read-only command does not change equipment, loans or saved records.

                LIST OVERDUE LOANS
                Format: list overdue
                Example: list overdue
                Shows active loans with due dates earlier than today's local date.
                Returned loans and loans due today or later are excluded.
                Displays Item ID, borrower ID and due date without changing any records.

                ITEM LOAN HISTORY
                Format: history id/<ITEM ID>
                Example: history id/001
                Requires an existing Item ID. Includes its physical item's active and returned loans.
                Sorted by issue date, earliest first, then Loan ID. Reused Item IDs keep separate histories.
                Older returned records without a proven item association remain available in list loans.

                FILTER EQUIPMENT
                Format: filter [id/<ID>] [i/<NAME>] [type/<TYPE>] [cond/<N|MIN-MAX>] [status/<STATUS>]
                Example: filter type/camera cond/1-3 status/available
                Supply at least one filter, in any order, with each prefix once. All filters must match (AND).
                Names use case-insensitive partial matching; types use case-insensitive full matching.
                cond/ accepts one value or an inclusive ascending range, such as 1-3.
                status/ accepts available or loaned. Available excludes active loans and unserviceable items.

                UPDATE EQUIPMENT
                Format: update id/<ID> [i/<NAME>] [type/<TYPE>] [cond/<N>]
                Example: update id/001 i/Studio camera cond/5
                Requires an existing Item ID and at least one update field, in any order, each prefix once.
                Omitted fields retain their current values. Type is fixed; type/ must match the current type.
                Updates retain subtype details, damage, remarks, serviceability, loans and bundle membership.

                CREATE BUNDLE
                Format: add-bundle name/<NAME>
                Example: add-bundle name/Studio kit
                Requires a name of 1-100 characters. Bundle IDs are assigned automatically, 001-999.
                Bundles exist only for the current session and are not saved between application runs.

                ADD EQUIPMENT TO BUNDLE
                Format: add-to-bundle id/<BUNDLE ID> <ITEM ID> [<ITEM ID> ...]
                Example: add-to-bundle id/001 001 002 003
                Requires an existing bundle and at least one existing, serviceable, unloaned equipment item.
                An item may appear only once per bundle. Each requested Item ID receives its own result.
                A complete bundle has exactly one camera, at least one lens and at least one battery.
                A tripod is optional, with at most one. Additional cameras and tripods are rejected.

                LIST BUNDLES
                Format: list bundles
                Example: list bundles
                Shows current-session bundles, their equipment and whether their composition is complete.

                HELP
                Format: help
                Example: help
                Shows this complete command guide. No arguments are accepted.

                EXIT
                Format: bye
                Example: bye
                Saves equipment and loans before exiting. A save failure keeps the application running;
                fix the reported file problem and retry bye.
                """, Equipment.MAX_NAME_LENGTH, Equipment.MIN_CONDITION, Equipment.MAX_CONDITION,
                        Equipment.MAX_REMARKS_LENGTH, damageValues(Camera.CameraDamage.values()),
                        damageValues(Lens.LensDamage.values()), damageValues(Battery.BatteryDamage.values()),
                        damageValues(Tripod.TripodDamage.values())).replace("\n", System.lineSeparator());
    }

    /** Lists the supported enum values without suggesting extra command syntax. */
    private static <T extends Enum<T>> String damageValues(T[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
    }
}
