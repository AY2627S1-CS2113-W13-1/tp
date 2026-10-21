# User Guide

## Introduction

snAp tracks individual pieces of equipment and their loans. Records are retained between application sessions.

## Quick Start

1. Install Java 25.
2. From the project directory, run `./gradlew run` on macOS/Linux or `.\gradlew.bat run` on Windows.
3. Enter one command per line. Type `bye` to exit.

## Adding equipment: `add`

Format: `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]`

Example: `add i/Canon EOS R10 id/001 type/camera cond/1`

* `ITEM` is a non-empty name of up to 100 characters.
* `ITEM ID` uses exactly three digits from `001` to `999` and must not already exist.
* `TYPE` is non-empty text, such as `camera`, `lighting`, `lens`, `accessories` or `audio`.
* `CONDITION` is a whole number from `1` to `10`. We recommend `1` for pristine and `10` for non-functional.
* Enter the fields in the order shown above. Names and types can contain spaces.
* Equipment with the same name can be added separately using different IDs.
* Names can contain text such as `USB id/card reader` or `fluid/head tripod`.
  The actual `id/` field is the last one before `type/`. Earlier whitespace-prefixed `id/` text in the name must
  have a non-empty value starting with a non-whitespace character other than a digit, `+`, or `-`.
  Whitespace-prefixed numeric ID-like text such as `id/001` is reserved and cannot be part of the name.
* Prefixes `i/`, `type/`, and `cond/` at the start or after whitespace are reserved command delimiters.
  They must occur once each, in the documented order; fields must be separated by whitespace.
  There is no quoting or escaping syntax. Prefix-like text inside words remains valid.

## Deleting equipment: `delete`

Format: `delete item/[ITEM ID]`

Example: `delete item/001`

The item is deleted immediately and a success message is printed. The deleted ID can then be used for another item.

Equipment with an active (unreturned) loan cannot be deleted. This prevents its ID from being reused
for a different item while the original loan is still active. Deletion is allowed once there are no active
loans for that ID; returned loan records do not block deletion.

Invalid delete syntax produces an error followed by the delete command format.
An ID that does not exist produces an error without changing the saved records.

## Loaning out equipment: `loan`

Format: `loan id/[ITEM ID] b/[BORROWER ID] issued/[DD-MM-YY] due/[DD-MM-YY]`

Example: `loan id/001 b/002 issued/01-10-26 due/08-10-26`

* `ITEM ID` is the three-digit identifier of the equipment being loaned.
* `BORROWER ID` is the three-digit identifier of the borrower.
* `issued` and `due` use the `DD-MM-YY` format. The issue date must not be later than the due date.
* An item cannot have more than one active loan at the same time.

## Viewing loans: `list loans`

Format: `list loans`

This displays all recorded loans with their unique loan ID, item ID, borrower ID, issue date, and due date.

## Deleting a loan: `delete-loan`

Format: `delete-loan id/[LOAN ID]`

Example: `delete-loan id/001`

Every loan receives a three-digit ID from `001` through `999`. Deletion accepts only that loan ID, so
equipment IDs and dates cannot accidentally select a different loan. If no matching loan exists, the application
reports an error and keeps the existing loan records unchanged.

## Equipment availability

The equipment list shows `Available` or `Loaned` for each item. A loan can only be added for equipment
that exists and is available. Rejected loan commands leave the item's status unchanged.

Equipment with an active loan cannot be deleted. Returning or deleting the active loan makes the item
available again; removing an older returned loan does not affect a newer active loan.

## Saving equipment

Successful equipment additions and deletions are saved automatically in `data/equipment.txt`, relative to the
directory from which the application is run. Successful loan additions and deletions are saved in
`data/loans.txt`, and the next unused loan ID is saved in `data/loan-sequence.txt`. Records are loaded when the
application starts again. A final save of both files is also attempted
when `bye` is entered; if it fails, the application reports the affected file and lets you retry.
The directory and both files are created on the first save, including the final save performed by `bye`.

The text file contains one equipment record per line, using the add command format. For example:

```text
add i/Canon EOS R10 id/001 type/camera cond/1 loanId/none
```

To transfer your records, copy `data/equipment.txt` to the same location on the other computer.
Copy `data/loans.txt` and `data/loan-sequence.txt` as well when transferring loan records. Loan records include the
loan ID, item ID, borrower ID, issue date, due date, and returned status. Older files without loan IDs are migrated
in file order, and deleted loan IDs are never reused. Legacy equipment `loaned/true|false` fields are accepted and
reconciled against the active loan records. If a file cannot be read or contains invalid or duplicate records, the
application reports the problem and stops.

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
| Add equipment | `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]` |
| Delete equipment | `delete item/[ITEM ID]` |
| Add loan | `loan id/[ITEM ID] b/[BORROWER ID] issued/[DD-MM-YY] due/[DD-MM-YY]` |
| Delete loan | `delete-loan id/[LOAN ID]` |
| List equipment | `list` |
| List loans | `list loans` |
| Exit | `bye` |
