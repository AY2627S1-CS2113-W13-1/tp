# User Guide

## Introduction

snAp tracks individual pieces of equipment and their loans. Records are retained between application sessions.

## Quick Start

1. Install Java 25.
2. From the project directory, run `./gradlew run` on macOS/Linux or `.\gradlew.bat run` on Windows.
3. Enter one command per line. Type `bye` to exit.

## Adding equipment: `add`

Base format: `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]`

Example: `add i/Canon EOS R10 id/001 type/camera cond/1`

* `ITEM` is a non-empty name of up to 100 characters.
* `ITEM ID` uses exactly three digits from `001` to `999` and must not already exist.
* `TYPE` is `camera`, `lens`, `battery` or `tripod`. Type names are case-insensitive and stored in lowercase.
* `CONDITION` is a whole number from `1` to `10`. We recommend `1` for pristine and `10` for non-functional.
* Enter the fields in the order shown above. Names can contain spaces.
* Equipment with the same name can be added separately using different IDs.

After `cond/`, you can add the field for the equipment type, followed by `damage/[DAMAGE]`:

| Type | Optional field | Damage values |
| --- | --- | --- |
| `camera` | `sensor/[SENSOR FORMAT]` | `NONE`, `SENSOR_DAMAGE`, `SHUTTER_FAULT` |
| `lens` | `zoom/[ZOOM RANGE]` | `NONE`, `SCRATCHED_GLASS`, `AUTOFOCUS_FAULT` |
| `battery` | `capacity/[CAPACITY]` | `NONE`, `SWELLING`, `CONTACT_DAMAGE` |
| `tripod` | `thread/[THREAD]` | `NONE`, `BENT_LEG`, `HEAD_FAULT` |

* Each type-specific value is free text of 1 to 100 characters on one line, such as `APS-C`, `24-70 mm`,
  `1800 mAh` or `1/4 inch`.
* Either optional field can be omitted. The type-specific value defaults to `unspecified`, and damage defaults to `NONE`.
* Damage values are case-insensitive and stored in uppercase. Use a value from the row for the chosen type.
* A field for another type, an invalid damage value, a repeated field or fields in the wrong order are rejected.

Examples:

```text
add i/Canon EOS R10 id/001 type/camera cond/1 sensor/APS-C damage/NONE
add i/Spare battery id/002 type/battery cond/7 damage/swelling
add i/Travel tripod id/003 type/tripod cond/2 thread/1/4 inch
```

* Names can contain text such as `USB id/card reader` or `fluid/head tripod`.
  The actual `id/` field is the last one before `type/`. Earlier whitespace-prefixed `id/` text in the name must
  have a non-empty value starting with a non-whitespace character other than a digit, `+`, or `-`.
  Whitespace-prefixed numeric ID-like text such as `id/001` is reserved and cannot be part of the name.
* Prefixes `i/`, `type/` and `cond/` at the start or after whitespace are reserved command delimiters.
  Prefixes `sensor/`, `zoom/`, `capacity/`, `thread/` and `damage/` are delimiters only after `cond/`.
  Names can contain these optional prefixes, such as `Camera sensor/cleaning kit`.
  Required fields must occur once each; optional fields may occur at most once. Fields must follow the
  documented order and be separated by whitespace.
  There is no quoting or escaping syntax. Prefix-like text inside words remains valid.

## Viewing equipment: `list`

Format: `list`

This displays all equipment with its name, ID, type and condition. The `STATUS` column, before `DETAILS`, shows
`Available` or `Loaned`. The `DETAILS` column shows the type-specific value and damage, including the defaults
when optional fields were omitted.

## Deleting equipment: `delete`

Format: `delete item/[ITEM ID]`

Example: `delete item/001`

The item is deleted immediately and a success message is printed. The deleted ID can then be used for another item.

Equipment with an active (unreturned) loan cannot be deleted. This prevents its ID from being reused
for a different item while the original loan is still active. Deletion is allowed once there are no active
loans for that ID; returned loan records do not block deletion.

Invalid delete syntax produces an error followed by the delete command format.
An ID that does not exist produces an error without changing the saved records.

## Equipment availability

New equipment is `Available`. Adding a loan changes its status to `Loaned`. Marking an active loan as returned
or deleting it makes the item available again. Removing an older returned loan does not affect a newer active loan.

There is no command to mark a loan as returned yet. The current command-line option is `delete-loan`, which
also removes the loan record. A return workflow that retains loan history is planned in
[issue #78](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/78).

A returned loan cannot be made active again. To borrow the same item again, create a new loan.

## Loaning out equipment: `loan`

Format: `loan id/[ITEM ID] b/[BORROWER ID] issued/[DD-MM-YY] due/[DD-MM-YY]`

Example: `loan id/001 b/002 issued/01-10-26 due/08-10-26`

* `ITEM ID` is the three-digit identifier of existing equipment. The item must be `Available`.
* `BORROWER ID` is the three-digit identifier of the borrower.
* `issued` and `due` use the `DD-MM-YY` format. The issue date must not be later than the due date.
* An item cannot have more than one active loan at the same time.

Invalid loan commands leave both the loan records and equipment availability unchanged.

## Viewing loans: `list loans`

Format: `list loans`

This displays all recorded loans with their item ID, borrower ID, issue date, and due date.

## Deleting a loan: `delete-loan`

Format: `delete-loan id/[ITEM ID] [issued/DD-MM-YY]`

Example: `delete-loan id/001`

Use the optional issue date when more than one loan exists for the same item ID:

```text
delete-loan id/001 issued/01-10-26
```

If no matching loan exists, or if the command does not identify a unique loan, the application reports an error and
keeps the existing loan records unchanged.

Deleting an active loan makes its equipment `Available`. Deleting returned loan history leaves any newer active
loan and the equipment's `Loaned` status unchanged.

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
`status/` accepts `available` or `loaned` and checks the current active loans. Returned loans do not block availability.

Prefixes `id/`, `i/`, `type/`, `cond/`, and `status/` after whitespace are reserved field separators.
Text like `id/card` within a name is allowed. Empty or repeated fields are rejected. Filtering leaves equipment
unchanged and prints matches in inventory order, or `No equipment matches the filters.` when none match.
Matching rows include the equipment's current status, type-specific details, and recorded damage, as in `list`.

## Saving equipment

Successful equipment additions and deletions are saved automatically in `data/equipment.txt`, relative to the
directory from which the application is run. Successful loan additions and deletions are saved in
`data/loans.txt`. Records are loaded when the application starts again. A final save of both files is also attempted
when `bye` is entered; if it fails, the application reports the affected file and lets you retry.
The directory and both files are created on the first save, including the final save performed by `bye`.
Equipment availability is rebuilt from the active loans loaded at startup, so loaned items remain `Loaned`
after restarting the application.

The text file contains one equipment record per line, using the add command format. For example:

```text
add i/Canon EOS R10 id/001 type/camera cond/1
add i/Travel tripod id/003 type/tripod cond/2 thread/1/4 inch damage/NONE
```

Saved equipment records use the four supported types and retain their type-specific values and damage.
Existing records of supported types without optional fields are also accepted, using `unspecified` and `NONE`.

To transfer your records, copy `data/equipment.txt` to the same location on the other computer.
Copy `data/loans.txt` as well when transferring loan records. Loan records include the item ID, borrower ID,
issue date, due date, and returned status. If either file cannot be read or contains invalid or duplicate records,
the application reports the problem and stops.

## Errors

* Empty or unknown commands show an error and the available commands. You can then enter another command.
* Invalid add or delete commands show the correct syntax. Invalid fields, duplicate IDs and missing IDs are rejected.
* File errors show the reason and ask you to check the file or directory permissions.
* Saves are written to temporary files before replacing the saved records, preserving previous records if writing fails.
* Command errors, including unexpected failures, show an error and return to the `>` prompt for another command.
* The application stops if saved records cannot be loaded or console input is no longer available.

## Command Summary

| Action | Command |
| --- | --- |
| Add equipment | `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]`, with optional type-specific and damage fields as described above |
| Delete equipment | `delete item/[ITEM ID]` |
| Add loan | `loan id/[ITEM ID] b/[BORROWER ID] issued/[DD-MM-YY] due/[DD-MM-YY]` |
| Delete loan | `delete-loan id/[ITEM ID] [issued/DD-MM-YY]` |
| List equipment | `list` |
| List loans | `list loans` |
| Filter equipment | `filter [id/ID] [i/NAME] [type/TYPE] [cond/1-3] [status/available]` |
| Exit | `bye` |
