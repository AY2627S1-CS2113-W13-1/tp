---
layout: default
title: User Guide
---

# User Guide

## Introduction

snAp tracks individual pieces of equipment, their loans and equipment bundles. Equipment, loan and bundle
records are retained between application sessions.

## Quick Start

1. Install Java 25 and ensure that `java -version` reports version 25.
2. From the project directory, run the command for your shell.

   **macOS/Linux:**

   ```sh
   ./gradlew run --console=plain --quiet
   ```

   **Windows PowerShell:**

   ```powershell
   .\gradlew.bat run --console=plain --quiet
   ```

   These options remove Gradle's progress display and task logs. Application prompts, command results and
   errors remain visible.
3. Enter one command per line. Type `bye` to exit.

The welcome screen displays the snAp logo. It uses green on supported
interactive terminals and plain text when output is redirected. Gradle pipes the application's output, so you
can request colour in PowerShell with `$env:SNAP_COLOUR = 'always'` before running the application. On
macOS/Linux, use `SNAP_COLOUR=always ./gradlew run`. Set `SNAP_COLOUR=never` to disable colour, or remove the
variable to restore automatic detection. A non-empty `NO_COLOR` variable or `TERM=dumb` also disables colour.

The logo uses `#` by default, preserving every space and line break in the original artwork.
Use `$env:SNAP_LOGO = 'ascii'` in PowerShell to force `#`, or `$env:SNAP_LOGO = 'unicode'` to request `█`.
On macOS/Linux, use `SNAP_LOGO=ascii ./gradlew run` or `SNAP_LOGO=unicode ./gradlew run`.
Even with `SNAP_LOGO=unicode`, an output encoding that cannot represent `█` falls back to `#`.
The optional `SNAP_LOGO=auto` mode uses blocks only on an attached terminal with a suitable output encoding;
redirected output, including Gradle runs, and `TERM=dumb` terminals use `#`. Font support cannot be detected.
Remove `SNAP_LOGO` to restore the ASCII default. Colour works with either version.

After the logo and loaded-record counts, snAp displays a boxed command menu automatically. The menu groups
commands by Equipment, Loans, Bundles and General, and shows how to request more help.
The `>>>` input prompt is separated from the previous output and the next response by horizontal lines
and blank lines.

## Getting help: `help`

| Format | What it displays | Example |
| --- | --- | --- |
| `help` | The same boxed command menu shown at startup | `help` |
| `help <command>` | The format, example, fields, defaults and restrictions for one command | `help loan` |
| `help all` | The complete command reference | `help all` |

Replace `<command>` with one of these exact lowercase topics:

* Equipment: `add`, `list`, `delete item`, `filter`, `update`.
* Loans: `loan`, `return`, `delete loan`, `list loans`, `view loan`, `list overdue`, `history`.
* Bundles: `add-bundle`, `add-to-bundle`, `remove-from-bundle`, `list bundles`, `history bundle`.
* General: `help`, `bye`.

Include the whole topic for commands with more than one word, for example `help list overdue` or
`help delete loan`. Extra whitespace around or between words is accepted. The group names are menu labels;
they are not help topics. An unknown topic, such as `help loans`, displays an error followed by the compact
menu so that you can choose a supported topic.

Help uses `<VALUE>` for a value you must replace and `[field/<VALUE>]` for a field you can omit.
Command details have separate headings, formats, examples and notes, with blank lines between them.
Each command heading has a full-width divider above and below it. In `help all`, two blank lines separate
each command from the previous section.
Help text wraps at 100 columns. A long displayed format or example can span several rows; enter each command
on one line. Unrecognised-command errors point to `help`.

Every help request, including an unknown topic, leaves equipment, loans, bundles, availability, identifiers and
saved records unchanged. You can then enter another command normally. Help does not save data or end the session.

## Optional fields and defaults

Square brackets around a field mean that the whole field can be omitted. Defaults apply only when a field is
absent. Supplying an empty or invalid value produces an error, except that empty remarks are valid unless damage is `OTHER`.

| Command | Optional field | When omitted |
| --- | --- | --- |
| `add` | Type-specific detail (`sensor/`, `zoom/`, `capacity/` or `thread/`) | `unspecified` |
| `add` | `damage/` | `NONE` |
| `add` | `remarks/` | Empty text; rejected if damage is `OTHER` |
| `loan` | `issued/` | Today's date in the application's local time zone, at midnight |
| `return` | `remarks/` | Empty text, replacing previous remarks; rejected if damage is `OTHER` |
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
* `REMARKS` is free-form text of up to 200 characters. It must remain on one line and is trimmed at both ends.
* Put `remarks/` last. Include non-empty remarks when using `damage/OTHER`.
* Enter the fields in the order shown above. Names can contain spaces.
* Equipment with the same name can be added separately using different IDs.

After `cond/`, you can add the fields for the equipment type, followed by
`damage/[DAMAGE]`:

| Type | Optional fields, in command order | Damage values |
| --- | --- | --- |
| `camera` | `sensor/[SENSOR FORMAT]`, `mount/[LENS MOUNT]`, `thread/[TRIPOD THREAD]`, `battery/[BATTERY TYPE]` | `NONE`, `SENSOR_DAMAGE`, `SHUTTER_FAULT`, `WATER_DAMAGE`, `BODY_CRACK`, `MISSING_CAP`, `OTHER` |
| `lens` | `zoom/[ZOOM RANGE]`, `mount/[LENS MOUNT]`, `sensor/[SENSOR FORMAT]` | `NONE`, `SCRATCHED_GLASS`, `AUTOFOCUS_FAULT`, `WATER_DAMAGE`, `CRACKED_GLASS`, `MISSING_CAP`, `OTHER` |
| `battery` | `capacity/[CAPACITY]`, `battery/[BATTERY TYPE]` | `NONE`, `SWELLING`, `CONTACT_DAMAGE`, `WATER_DAMAGE`, `LEAKING`, `OTHER` |
| `tripod` | `thread/[TRIPOD THREAD]` | `NONE`, `BENT_LEG`, `HEAD_FAULT`, `WATER_DAMAGE`, `MISSING_PART`, `OTHER` |

* Each type-specific value is free text containing 1 to 100 characters
  on one line.
* Type-specific fields must appear in the order shown in the table.
  `damage/` follows those fields when supplied.
* Optional fields may be omitted independently, except `remarks/` with `damage/OTHER`.
  Omitted compatibility values default to `unspecified`, and damage defaults to `NONE`.
* Damage values are case-insensitive and stored in uppercase. Use a
  damage value from the selected type's row. For `OTHER`, remarks must be non-empty after trimming.
* Fields belonging to another equipment type, invalid damage values,
  repeated fields and fields in the wrong order are rejected.
* Compatibility comparisons ignore letter case.
* Compatibility comparison is currently provided as an internal API for
  follow-up features such as bundle construction. This version does not
  provide a CLI command that displays compatibility results.
* The internal comparison returns `UNKNOWN` when a required attribute was
  omitted. A known conflicting attribute takes precedence and returns
  `INCOMPATIBLE`.
* Compatibility attributes are saved, restored and preserved when the
  equipment name or condition is updated.

Examples:

```text
add i/Canon EOS R10 id/001 type/camera cond/1 sensor/APS-C mount/Canon RF thread/1/4"-20 battery/Li-ion damage/NONE
add i/Standard Zoom id/002 type/lens cond/2 zoom/24-70 mm mount/Canon RF sensor/APS-C damage/NONE
add i/Spare Battery id/003 type/battery cond/3 capacity/2000 mAh battery/Li-ion damage/NONE
add i/Travel Tripod id/004 type/tripod cond/2 thread/1/4"-20 damage/NONE
add i/Backup Camera id/005 type/camera cond/5 damage/OTHER remarks/Damaged control dial
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

Equipment that belongs to a bundle cannot be deleted. Use `remove-from-bundle` to remove the item from every
bundle that contains it before deleting the equipment. This keeps each persisted bundle's contents and completion
status consistent with the inventory.

Invalid delete syntax produces an error followed by the delete command format.
An ID that does not exist produces an error without changing the saved records.

## Equipment availability

New equipment is serviceable and `Available`. Adding a loan changes its status to `Loaned`.
Use `return` to retain the loan record and record whether the returned item is serviceable.
An item returned with `serviceable/yes` becomes `Available`; an item returned with `serviceable/no`
becomes `Unserviceable` and cannot be borrowed or added to another bundle. This assessment survives restarting.
Removing an older returned loan does not change the assessment or affect a newer active loan.

A returned loan cannot be made active again. To borrow the same item again, create a new loan.

An equipment officer can mark an item `Missing` or `Lost` without deleting its record:

```text
mark id/001 status/missing
mark id/001 status/lost
```

An item with an active loan cannot be marked missing or lost, because doing so would leave an inconsistent
loan relationship. Marked items remain in the inventory and are not eligible for new loans or bundles.
Their status and status history remain tied to the permanent physical-item identity.

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

## Viewing the issue-time equipment condition: `view loan`

Format: `view loan/[LOAN ID]`

Example: `view loan/001`

Use an existing loan ID from `001` to `999`. This displays the equipment type, numeric condition, damage and remarks captured when that loan was issued. The snapshot describes the equipment at issue time, not its current condition.

Later equipment updates and return assessments do not change the saved snapshot. Returned loans retain their snapshots. Older loan records created before this feature report that the issue condition was not recorded.

This is a read-only command. It does not change equipment, loans, availability or saved records. An invalid command or unknown loan ID produces an error without modifying any data.

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

## Viewing an item's status history: `status-history`

Format: `status-history id/[ITEM ID]`

Example: `status-history id/001`

This displays the current physical-item state and each recorded `Missing` or `Lost` transition in chronological order.
Status history is tied to the equipment record's permanent identity, not only its reusable Item ID. Deleting an item
and adding a replacement under the same Item ID therefore starts an empty status history for the replacement.
Items without recorded status changes show `No status changes recorded.` The command is read-only and does not change
equipment, loans, histories or saved files.

## Viewing overdue loans: `list overdue`

Format: `list overdue`

This displays every active loan whose due date is earlier than the current local date. The results show the
item ID, borrower ID and due date in the same order as the stored loans.

Returned loans are excluded. A loan due today is not overdue, regardless of its time, and becomes overdue only
on the following date if it remains active. If no loans are overdue, the application displays
`No overdue loans found.`

This is a read-only command. It does not modify loan records, equipment availability or saved files.

## Returning equipment: `return`

Format: `return loan/[LOAN ID] serviceable/[yes|no] damage/[DAMAGE|none] [remarks/[REMARKS]]`

Examples:

```text
return loan/001 serviceable/yes damage/none remarks/Checked on return
return loan/002 serviceable/no damage/SENSOR_DAMAGE remarks/Sensor needs repair
return loan/003 serviceable/yes damage/none
return loan/004 serviceable/no damage/OTHER remarks/Mounting plate damaged
```

* Use the loan ID from `list loans`, from `001` to `999`. It must identify an active loan.
* `loan/`, `serviceable/` and `damage/` are required, once each and in the displayed order.
  The complete command must be on one line.
* `serviceable/yes` or `serviceable/no` is a mandatory condition assessment. These values are case-insensitive.
  The assessment determines whether the equipment may be borrowed again; damage alone does not decide this.
* `damage/` must use a value from the equipment type's damage table under **Adding equipment**.
  Values are case-insensitive. Use `none` when no damage is found.
* `remarks/` must be last. For `damage/OTHER`, provide non-empty text after trimming.
  For other damage values, omit it or supply empty text to clear previous remarks. Remarks may contain up to 200
  characters after trimming. All text after `remarks/`, including prefix-like text, is treated as remarks.
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

## Updating loan details: `update loan`

Format: `update loan/[LOAN ID] [id/[ITEM ID]] [b/[BORROWER ID]] [issued/DD-MM-YY] [due/DD-MM-YY]`

Example:

```text
update loan/001 b/007 due/15-10-26
update loan/001 id/002 b/007 issued/03-10-26 due/15-10-26
```

Use the unique loan ID shown by `list loans`. Supply at least one field to update, in any order, once each.
The loan ID remains unchanged, and omitted fields retain their current values. The supported fields are:

* `id/` changes the equipment assigned to the active loan. The replacement must exist, be serviceable and have
  no active loan. Its equipment record identity is retained so availability remains correct after a restart.
* `b/` changes the three-digit borrower ID.
* `issued/` changes the issue date.
* `due/` changes the due date.

The effective issue date must not be later than the effective due date. Unknown loan IDs, malformed IDs or dates,
duplicate fields, unavailable equipment and returned loans are rejected. This project stores borrower IDs rather than
borrower profiles, so borrower validation is limited to the documented three-digit identifier format.

The complete proposed loan list is saved before the live loan and equipment associations change. If validation or
saving fails, the original loan, equipment availability and saved file remain unchanged. Successful edits remain after
restarting the application.

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
three-digit bundle IDs in creation order, starting at `001`, with at most 999 bundles in the saved collection.
Bundle IDs are separate from equipment IDs.

Bundles and their equipment memberships are saved when you enter `bye` and restored when the application
restarts. Bundle IDs continue from the highest restored ID. Equipment and loans remain saved separately.

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

## Removing equipment from a bundle: `remove-from-bundle`

Format: `remove-from-bundle id/[BUNDLE ID] [EQUIPMENT ID]...`

Example: `remove-from-bundle id/001 001`

The bundle must exist. Each requested equipment ID is removed independently, and an equipment item can be
deleted only after it has been removed from every bundle that contains it. Successful membership removals are
saved immediately; a save failure restores the previous membership.

## Viewing bundles: `list bundles`

Format: `list bundles`

This displays each bundle's ID, name, completion status and contained equipment. The `COMPLETE` column
checks the required equipment counts. Borrowing an item later does not remove it from its bundle.

## Viewing a bundle's loan history: `history bundle`

Format: `history bundle/id/[BUNDLE ID]` or `history bundle/name/[NAME]`

Examples: `history bundle/id/001` and `history bundle/name/Travel Kit`

This displays all active and returned loans for every physical equipment item in the selected bundle.
Results are ordered by issue date from earliest to latest, then by loan ID. An empty history and an unknown
bundle are reported clearly. Bundle memberships are matched using each equipment record's permanent identity,
so a reused visible Item ID cannot inherit the wrong historical membership.

## Saving application data

Successful equipment additions, updates, deletions and return assessments are saved automatically in
`data/equipment.txt`, relative to the directory from which the application is run.
Successful loan additions, updates, returns and deletions are saved in
`data/loans.txt`, the next unused loan ID is saved in `data/loan-sequence.txt`, and bundle memberships are saved
in `data/bundles.txt`. Records are loaded when the application starts again. A final save is also attempted when
`bye` is entered; if it fails, the application reports the affected file and lets you retry.
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
Records with `damage/OTHER` require non-empty remarks; invalid saved records are reported when loaded.
Remarks are shown in the `list` equipment table and are restored when the application starts again.
Saved records may start with `serviceable/yes` or `serviceable/no`, followed by status metadata and a tab before
the add record.
This prefix is storage metadata, not an add-command field. Older records without the prefix are serviceable.
New records also start with `recordId/` and a permanent equipment identifier, followed by a tab before
the serviceability prefix. The application assigns this identifier; it is separate from the reusable Item ID.
New records include `status/present`, `status/missing` or `status/lost` metadata and timestamped
`statusHistory/<STATUS>@<INSTANT>` entries when status changes have been recorded. Older records without status
metadata load as `present` with an empty status history and are upgraded safely.
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

To transfer your records, copy `data/equipment.txt`, `data/loans.txt`, `data/loan-sequence.txt` and
`data/bundles.txt` to the same location on the other computer. If a file cannot be read or contains invalid or
duplicate records, the application reports the problem and stops.

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
| Show the compact command menu | `help` |
| Show one command's format, example and defaults | `help <command>` |
| Show the complete command reference | `help all` |
| Add equipment | `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]` with optional details, damage and remarks |
| Delete equipment | `delete item/[ITEM ID]` |
| Add loan | `loan id/[ITEM ID] b/[BORROWER ID] [issued/DD-MM-YY] due/[DD-MM-YY]` |
| Return equipment | `return loan/[LOAN ID] serviceable/[yes|no] damage/[DAMAGE|none] [remarks/[REMARKS]]` |
| Delete loan | `delete loan/[LOAN ID]` |
| Update loan | `update loan/[LOAN ID] [id/[ITEM ID]] [b/[BORROWER ID]] [issued/DD-MM-YY] [due/DD-MM-YY]` |
| List equipment | `list` |
| List loans | `list loans` |
| View issue-time equipment condition | `view loan/[LOAN ID]` |
| View item loan history | `history id/[ITEM ID]` |
| View bundle loan history | `history bundle/id/[BUNDLE ID]` or `history bundle/name/[NAME]` |
| Mark equipment missing or lost | `mark id/[ITEM ID] status/[missing\|lost]` |
| View equipment status history | `status-history id/[ITEM ID]` |
| List overdue loans | `list overdue` |
| Filter equipment | `filter [id/ID] [i/NAME] [type/TYPE] [cond/1-3] [status/available]` |
| Update equipment | `update id/ID [i/NAME] [type/TYPE] [cond/CONDITION]` |
| Create bundle | `add-bundle name/[NAME]` |
| Add equipment to bundle | `add-to-bundle id/[BUNDLE ID] [EQUIPMENT ID]...` |
| Remove equipment from bundle | `remove-from-bundle id/[BUNDLE ID] [EQUIPMENT ID]...` |
| List bundles | `list bundles` |
| Exit | `bye` |
