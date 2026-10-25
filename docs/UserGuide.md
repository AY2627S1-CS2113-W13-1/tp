# User Guide

## Introduction

snAp tracks individual pieces of equipment and their loans. Equipment and loan records are retained between
application sessions. Bundles group equipment during the current session.

## Quick Start

1. Install Java 25.
2. From the project directory, run `./gradlew run` on macOS/Linux or `.\gradlew.bat run` on Windows.
3. Enter one command per line. Type `bye` to exit.

## Getting help: `help`

Format: `help`

Example: `help`

This displays every available command with its format, an example, required and optional fields, defaults and
important restrictions. Help uses `<VALUE>` for a value you must replace and `[field/<VALUE>]` for a field you
can omit. Enter `help` without arguments. The welcome message and unrecognised-command errors point to it.

Requesting help leaves equipment, loans, bundles, availability, identifiers and saved records unchanged.
You can then enter another command normally. Help does not save data or end the session.

## Optional fields and defaults

Square brackets around a field mean that the whole field can be omitted. Defaults apply only when a field is
absent. Supplying an empty or invalid value produces an error, except that empty remarks are valid.

| Command | Optional field | When omitted |
| --- | --- | --- |
| `add` | Type-specific detail (`sensor/`, `zoom/`, `capacity/` or `thread/`) | `unspecified` |
| `add` | `damage/` | `NONE` |
| `add` | `remarks/` | Empty text |
| `loan` | `issued/` | Today's date in the application's local time zone, at midnight |
| `return` | `remarks/` | Empty text, replacing any previous remarks |
| `update` | Name, type or condition | Keep the existing value; supply an ID and at least one field |
| `filter` | Any individual criterion | No restriction for that criterion; supply at least one criterion |

Equipment name, Item ID, type and numeric condition remain required when adding equipment.
Loans require Item ID, borrower ID and due date. Returns require loan ID, serviceability and damage.
There is no default due date or serviceability decision.

## Adding equipment: `add`

Format: `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION] [TYPE DETAIL] [damage/[DAMAGE]] [remarks/[REMARKS]]`

Example: `add i/Canon EOS R10 id/001 type/camera cond/1 remarks/Handle with care`

* `ITEM` is a non-empty name of up to 100 characters.
* `ITEM ID` uses exactly three digits from `001` to `999` and must not already exist.
* `TYPE` is `camera`, `lens`, `battery` or `tripod`. Type names are case-insensitive and stored in lowercase.
* `CONDITION` is a whole number from `1` to `10`. We recommend `1` for pristine and `10` for non-functional.
* `REMARKS` is optional free-form text of up to 200 characters. It must remain on one line and is trimmed at both ends.
* Put `remarks/` last when remarks are provided. Omit it to add equipment without remarks.
* Enter the fields in the order shown above. Names can contain spaces.
* Equipment with the same name can be added separately using different IDs.

After `cond/`, you can add the fields for the equipment type, followed by
`damage/[DAMAGE]`:

| Type | Optional fields, in command order | Damage values |
| --- | --- | --- |
| `camera` | `sensor/[SENSOR FORMAT]`, `mount/[LENS MOUNT]`, `thread/[TRIPOD THREAD]`, `battery/[BATTERY TYPE]` | `NONE`, `SENSOR_DAMAGE`, `SHUTTER_FAULT` |
| `lens` | `zoom/[ZOOM RANGE]`, `mount/[LENS MOUNT]`, `sensor/[SENSOR FORMAT]` | `NONE`, `SCRATCHED_GLASS`, `AUTOFOCUS_FAULT` |
| `battery` | `capacity/[CAPACITY]`, `battery/[BATTERY TYPE]` | `NONE`, `SWELLING`, `CONTACT_DAMAGE` |
| `tripod` | `thread/[TRIPOD THREAD]` | `NONE`, `BENT_LEG`, `HEAD_FAULT` |

* Each type-specific value is free text containing 1 to 100 characters
  on one line.
* Type-specific fields must appear in the order shown in the table.
  `damage/` follows those fields when supplied.
* Every optional field may be omitted. Omitted compatibility values
  default to `unspecified`, and damage defaults to `NONE`.
* Damage values are case-insensitive and stored in uppercase. Use a
  damage value from the row belonging to the selected equipment type.
* Fields belonging to another equipment type, invalid damage values,
  repeated fields and fields in the wrong order are rejected.
* Compatibility comparisons ignore letter case.
* A result is reported as unknown when an attribute needed for comparison
  was omitted. A known conflicting attribute is considered incompatible.
* Compatibility attributes are saved, restored and preserved when the
  equipment name or condition is updated.
* There is currently no separate user command that displays a compatibility result.

Examples:

```text
add i/Canon EOS R10 id/001 type/camera cond/1 sensor/APS-C mount/Canon RF thread/1/4"-20 battery/Li-ion damage/NONE
add i/Standard Zoom id/002 type/lens cond/2 zoom/24-70 mm mount/Canon RF sensor/APS-C damage/NONE
add i/Spare Battery id/003 type/battery cond/3 capacity/2000 mAh battery/Li-ion damage/NONE
add i/Travel Tripod id/004 type/tripod cond/2 thread/1/4"-20 damage/NONE
```

* Names can contain text such as `USB id/card reader` or `fluid/head tripod`.
  The actual `id/` field is the last one before `type/`. Earlier whitespace-prefixed `id/` text in the name must
  have a non-empty value starting with a non-whitespace character other than a digit, `+`, or `-`.
  Whitespace-prefixed numeric ID-like text such as `id/001` is reserved and cannot be part of the name.
* Prefixes `i/`, `type/` and `cond/` at the start or after whitespace are reserved command delimiters.
  Prefixes `sensor/`, `mount/`, `zoom/`, `capacity/`, `thread/`, `battery/`, `damage/` and `remarks/`
  are delimiters only after `cond/`.
  Names can contain these optional prefixes, such as `Camera sensor/cleaning kit` or `Camera remarks/adapter`.
  The first `remarks/` after `cond/` starts the remarks field.
  Required fields must occur once each; optional fields may occur at most once. Fields must follow the
  documented order and be separated by whitespace. `remarks/` must be the final optional field.
  There is no quoting or escaping syntax. Prefix-like text inside words remains valid.

## Viewing equipment: `list`

Format: `list`

This displays all equipment with its name, ID, type and condition. The `STATUS` column, before `DETAILS`, shows
`Available`, `Loaned` or `Unserviceable`. The `DETAILS` column shows the type-specific value and damage, including the defaults
when optional fields were omitted. The `REMARKS` column follows `DETAILS` and is empty when no remarks were given.

## Deleting equipment: `delete`

Format: `delete item/[ITEM ID]`

Example: `delete item/001`

The item is deleted immediately and a success message is printed. The deleted ID can then be used for another item.

Equipment with an active (unreturned) loan cannot be deleted. This prevents its ID from being reused
for a different item while the original loan is still active. Deletion is allowed once there are no active
loans for that ID; returned loan records do not block deletion.

Equipment that belongs to a bundle cannot be deleted during that session. This keeps each bundle's contents
and completion status consistent with the inventory. Bundles are cleared when the application restarts.

Invalid delete syntax produces an error followed by the delete command format.
An ID that does not exist produces an error without changing the saved records.

## Equipment availability

New equipment is serviceable and `Available`. Adding a loan changes its status to `Loaned`.
Use `return` to retain the loan record and record whether the returned item is serviceable.
An item returned with `serviceable/yes` becomes `Available`; an item returned with `serviceable/no`
becomes `Unserviceable` and cannot be borrowed or added to another bundle. This assessment survives restarting.
Removing an older returned loan does not change the assessment or affect a newer active loan.

A returned loan cannot be made active again. To borrow the same item again, create a new loan.

## Loaning out equipment: `loan`

Format: `loan id/[ITEM ID] b/[BORROWER ID] [issued/DD-MM-YY] due/[DD-MM-YY]`

Example: `loan id/001 b/002 issued/01-10-26 due/08-10-26`

Example with the issue date omitted: `loan id/001 b/002 due/28-10-26`

* `ITEM ID` is the three-digit identifier of existing equipment, from `001` to `999`. The item must be `Available`.
* `BORROWER ID` is the three-digit identifier of the borrower, from `000` to `999`.
* `issued/` is optional and defaults to today's date in the application's local time zone, at midnight.
  The date is read once for each command. A supplied issue date overrides this default.
* `due/` is required. Both dates use `DD-MM-YY`. The issue date must not be later than the due date.
  When omitting `issued/`, choose a due date on or after today.
* Keep the displayed field order. Empty, invalid, repeated or misplaced supplied dates are rejected;
  `issued/` without a value does not use the default.
* An item cannot have more than one active loan at the same time.

Each successful loan receives a unique three-digit loan ID, which is printed in the success message.
Loan IDs start at `001` and increase through `999`. Deleted IDs are never reused, including after a restart.
Once `999` has been assigned, further new loans are rejected.

Invalid loan commands leave both the loan records and equipment availability unchanged.

## Viewing loans: `list loans`

Format: `list loans`

This displays all recorded loans with their unique loan ID, item ID, borrower ID, issue date and due date.
The `LOAN ID` column comes before `ITEM ID`. The final `RETURNED` column is `true` for returned loans
and `false` for active loans.

## Viewing an item's loan history: `history`

Format: `history id/[ITEM ID]`

Example: `history id/001`

Use an existing Item ID from `001` to `999`. The history includes both active and returned loans for the
equipment currently registered under that ID. Each row shows the loan ID, borrower, issue date, due date and
return status. Rows are ordered by issue date from earliest to latest, then by loan ID for equal issue dates.
This command does not change equipment, loans, availability or saved records.

If the item has no recorded loans, the application prints `No loan history for item ID 001.`
If the item does not exist, the application reports the missing ID. Invalid commands show the required format.

Deleting equipment and adding another item under the same Item ID starts a separate history, including when
the replacement has the same name and type. The previous item's loans remain in `list loans`, but are excluded
from the replacement's history. This separation survives restarting and normal equipment updates and returns.

Older saved returned loans may lack enough information to prove which item they belonged to. Such loans remain
in `list loans` and are excluded from item-specific histories. If they use the queried Item ID, the history view
explains this and points to `list loans`. Existing active loans can be associated with the current item when loaded.

## Returning equipment: `return`

Format: `return loan/[LOAN ID] serviceable/[yes|no] damage/[DAMAGE|none] [remarks/[REMARKS]]`

Examples:

```text
return loan/001 serviceable/yes damage/none remarks/Checked on return
return loan/002 serviceable/no damage/SENSOR_DAMAGE remarks/Sensor needs repair
return loan/003 serviceable/yes damage/none
```

* Use the loan ID from `list loans`, from `001` to `999`. It must identify an active loan.
* `loan/`, `serviceable/` and `damage/` are required, once each and in the displayed order.
  The complete command must be on one line.
* `serviceable/yes` or `serviceable/no` is a mandatory condition assessment. These values are case-insensitive.
  The assessment determines whether the equipment may be borrowed again; damage alone does not decide this.
* `damage/` must use a value from the equipment type's damage table under **Adding equipment**.
  Values are case-insensitive. Use `none` when no damage is found.
* `remarks/` is optional and must be last when supplied. Omit it or supply empty text to clear previous remarks.
  Supplied text may contain up to 200 characters after trimming. All text after this prefix is treated as remarks,
  including prefix-like text.
* The assessment replaces the item's previous damage and remarks. Its name, numeric condition, type-specific
  attributes and existing bundle memberships are retained. The numeric condition can be changed separately with `update`.

A successful return keeps the loan record and its ID, sets `RETURNED` to `true`, and clears its active equipment link.
An already returned loan cannot be returned again or reactivated. Borrowing the same serviceable equipment later
creates a new loan with a new ID. Returning equipment does not consume a loan ID.

Equipment and loan files are saved before the live records change. If a save fails, the application reports the error
and attempts to restore the previous files. The live loan remains active, with its previous equipment assessment.
After fixing the file or directory problem, retry the same command. If restoration also fails, the error explicitly
asks you to check the saved files before retrying.

There is currently no repair command to make unserviceable equipment serviceable again.
Changing its numeric condition with `update` does not remove the unserviceable assessment.

## Deleting a loan: `delete loan`

Format: `delete loan/[LOAN ID]`

Example: `delete loan/001`

Use the loan ID shown by `list loans`, from `001` to `999`. This selects one record even when the same item
has been borrowed more than once. The value is a loan ID, not an equipment ID. Issue-date arguments are no longer
accepted. If no matching loan exists or the command is invalid, the application reports an error and keeps
the existing loan records unchanged.

The deletion is saved before the loan is removed or its equipment is released. If saving fails, the application
reports an error and keeps both the loan records and equipment availability unchanged. You can retry the command
after fixing the storage problem.

Deleting an active loan releases its equipment without changing its serviceability assessment.
Deleting returned loan history leaves any
newer active loan and the equipment's `Loaned` status unchanged.

## Filtering equipment: `filter`

Format: `filter [id/ITEM ID] [i/NAME] [type/TYPE] [cond/CONDITION] [status/STATUS]`

Examples:

* `filter type/camera cond/1-3 status/available`
* `filter i/canon`
* `filter id/001`

Supply at least one filter, in any order. Each prefix can appear only once. All supplied filters must match.
Names use case-insensitive partial matching and may contain spaces. Types use case-insensitive full matching
against `camera`, `lens`, `battery`, or `tripod`. `id/` uses an exact three-digit ID from `001` to `999`.

`cond/` accepts a value from `1` to `10` or an inclusive range such as `1-3`. Ranges cannot be reversed.
`status/` accepts `available` or `loaned` and checks the current active loans.
The `available` filter also excludes unserviceable equipment. Returned loans do not block availability.

Prefixes `id/`, `i/`, `type/`, `cond/`, and `status/` after whitespace are reserved field separators.
Text like `id/card` within a name is allowed. Empty or repeated fields are rejected. Filtering leaves equipment
unchanged and prints matches in inventory order, or `No equipment matches the filters.` when none match.
Matching rows include the equipment's current status, type-specific details, recorded damage and remarks, as in `list`.

## Updating equipment: `update`

Format: `update id/ITEM ID [i/NAME] [type/TYPE] [cond/CONDITION]`

Examples:

* `update id/001 cond/5`
* `update id/001 i/Film Camera type/camera`

Supply an existing ID and at least one of `i/`, `type/`, or `cond/`. Fields may appear in any order, once each.
The ID stays fixed and omitted details keep their current values. Names contain 1-100 characters and
conditions are whole numbers from 1 to 10. Condition ranges are not accepted for updates.

Equipment types are fixed by their subclasses. If `type/` is supplied, it must match the item's current type,
ignoring case. Changing a camera to a lens, for example, is rejected before saving or changing any details.
Updates preserve the same equipment object, its position in the list, its type-specific fields, damage and remarks,
and its availability, loan references and bundle membership. Type-specific fields, damage and remarks cannot be
changed with `update`.

Prefixes `id/`, `i/`, `type/`, `cond/`, and `status/` after whitespace are reserved separators; `status/`
cannot be updated manually. Text such as `USB id/card reader` within a name is allowed.
The proposed details are saved before the existing object is changed. Invalid commands or failed saves
leave the existing equipment details unchanged, and the application displays an error.

## Creating a bundle: `add-bundle`

Format: `add-bundle name/[NAME]`

Example: `add-bundle name/Travel Kit`

The name must contain 1 to 100 characters. A bundle starts empty and incomplete. The application assigns
three-digit bundle IDs in creation order, starting at `001`, with at most 999 bundles per session.
Bundle IDs are separate from equipment IDs.

Bundles are stored only for the current session. They are not saved when you enter `bye`, and restarting
the application clears all bundles and starts their IDs at `001` again. Equipment and loans remain saved.

## Adding equipment to a bundle: `add-to-bundle`

Format: `add-to-bundle id/[BUNDLE ID] [EQUIPMENT ID]...`

Example: `add-to-bundle id/001 001 002 005`

The bundle must exist. Supply one or more equipment IDs, separated by spaces. Each requested item gets
its own success or error message. Valid items are added even if another requested item is rejected.

* Equipment must exist in the inventory and be `Available` when added.
* An item cannot appear twice in the same bundle. It can appear in different bundles.
* A bundle can contain only one camera and at most one tripod. Extra batteries and lenses are allowed.
* A bundle is complete when it contains exactly one camera, at least one battery and at least one lens.
  A tripod is optional.

Returning a serviceable item or deleting its active loan makes it available for a later `add-to-bundle` command.
Unserviceable equipment cannot be added to a bundle. Existing bundle memberships are retained on return.
Updating a bundled item's name or condition also updates the item shown in the bundle.

## Viewing bundles: `list bundles`

Format: `list bundles`

This displays each bundle's ID, name, completion status and contained equipment. The `COMPLETE` column
checks the required equipment counts. Borrowing an item later does not remove it from its bundle.

## Saving equipment

Successful equipment additions, updates, deletions and return assessments are saved automatically in
`data/equipment.txt`, relative to the directory from which the application is run.
Successful loan additions, returns and deletions are saved in
`data/loans.txt`, and the next unused loan ID is saved in `data/loan-sequence.txt`. Records are loaded when the
application starts again. A final save is also attempted when `bye` is entered; if it fails, the application
reports the affected file and lets you retry.
The directory and saved files are created on the first save, including the final save performed by `bye`.
Each item's availability and associated loan ID are rebuilt from active loans at startup, so loaned items
remain `Loaned` after restarting the application.

The text file contains one equipment record per line, using the add command format. For example:

```text
add i/Canon EOS R10 id/001 type/camera cond/1
add i/Travel tripod id/003 type/tripod cond/2 thread/1/4 inch damage/NONE
add i/Canon EOS R10 id/004 type/camera cond/1 remarks/Handle with care
```

Saved equipment records use the four supported types and retain their type-specific values and damage.
Existing records of supported types without optional fields are also accepted, using `unspecified` and `NONE`.
Records created before remarks were supported may omit the `remarks/` field; they load with empty remarks.
Remarks are shown in the `list` equipment table and are restored when the application starts again.
Saved records may start with `serviceable/yes` or `serviceable/no`, followed by a tab and the add record.
This prefix is storage metadata, not an add-command field. Older records without the prefix are serviceable.
New records also start with `recordId/` and a permanent equipment identifier, followed by a tab before
the serviceability prefix. The application assigns this identifier; it is separate from the reusable Item ID.
Older equipment records are upgraded and saved with permanent identifiers when first loaded. If that save fails,
startup reports the error instead of using identifiers that could change on the next restart.

Loan records include the loan ID, item ID, borrower ID, issue date, due date and returned status. For example:

```text
loan loanId/001 id/001 b/002 issued/01-10-26 due/08-10-26 returned/false
```

Saved loans always contain explicit issue and due dates, including a default issue date chosen by a command.
Loading a saved record never substitutes today's date for a missing or malformed date.

New loan records also end with `equipment/` and the permanent identifier of the borrowed equipment.
Returns retain this identifier, so later reuse of the Item ID cannot mix histories. Older returned records without
it are kept without assigning them to the current item. Older active records receive the current item's identifier
and are saved before the application uses their associations.

Existing loan records without loan IDs are assigned IDs in file order on first load and saved in the new format.
These IDs remain stable on later restarts. The sequence file preserves the next unused ID even when loan records
are deleted. Equipment records retain their details and remarks; loan associations are restored from the loan records.

To transfer your records, copy `data/equipment.txt`, `data/loans.txt` and `data/loan-sequence.txt` to the same
location on the other computer. If a file cannot be read or contains invalid or duplicate records, the application
reports the problem and stops.

## Errors

* Empty or unknown commands show an error and direct you to `help`. You can then enter another command.
* Invalid add or delete commands show the correct syntax. Invalid fields, duplicate IDs and missing IDs are rejected.
* File errors show the reason and ask you to check the file or directory permissions.
* Saves are written to temporary files before replacing the saved records, preserving previous records if writing fails.
* Command errors, including unexpected failures, show an error and return to the `>` prompt for another command.
* The application stops if saved records cannot be loaded or console input is no longer available.

## Command Summary

| Action | Command |
| --- | --- |
| Show command formats, examples and defaults | `help` |
| Add equipment | `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]` with optional details, damage and remarks |
| Delete equipment | `delete item/[ITEM ID]` |
| Add loan | `loan id/[ITEM ID] b/[BORROWER ID] [issued/DD-MM-YY] due/[DD-MM-YY]` |
| Return equipment | `return loan/[LOAN ID] serviceable/[yes|no] damage/[DAMAGE|none] [remarks/[REMARKS]]` |
| Delete loan | `delete loan/[LOAN ID]` |
| List equipment | `list` |
| List loans | `list loans` |
| View item loan history | `history id/[ITEM ID]` |
| Filter equipment | `filter [id/ID] [i/NAME] [type/TYPE] [cond/1-3] [status/available]` |
| Update equipment | `update id/ID [i/NAME] [type/TYPE] [cond/CONDITION]` |
| Create bundle | `add-bundle name/[NAME]` |
| Add equipment to bundle | `add-to-bundle id/[BUNDLE ID] [EQUIPMENT ID]...` |
| List bundles | `list bundles` |
| Exit | `bye` |
