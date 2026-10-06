# User Guide

## Introduction

snAp tracks individual pieces of equipment. You can add equipment and delete records by their unique identifiers.

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

## Deleting equipment: `delete`

Format: `delete item/[ITEM ID]`

Example: `delete item/001`

The application asks for confirmation. Enter `yes` to delete the item; any other response cancels the deletion.
It prints a confirmation message after the deletion is saved. The deleted ID can then be used for another item.

Invalid delete syntax produces an error followed by the delete command format.
An ID that does not exist produces an error without changing the saved records.

## Saving equipment

Successful additions and deletions are saved automatically in `data/equipment.txt`, relative to the directory
from which the application is run. Records are loaded when the application starts again.
The directory and file are created on the first successful addition.

The text file contains one equipment record per line, using the add command format. For example:

```text
add i/Canon EOS R10 id/001 type/camera cond/1
```

To transfer your records, copy `data/equipment.txt` to the same location on the other computer.
If the file cannot be read or contains invalid or duplicate records, the application reports the problem and stops.

## Errors

* Empty or unknown commands show an error and the available commands. You can then enter another command.
* Invalid add or delete commands show the correct syntax. Invalid fields, duplicate IDs and missing IDs are rejected.
* File errors show the reason and ask you to check the file or directory permissions.
* Saves are written to a temporary file before replacing `equipment.txt`, preserving previous records if writing fails.
* Command errors, including unexpected failures, show an error and return to the `>` prompt for another command.
* The application stops if saved records cannot be loaded or console input is no longer available.

## Command Summary

| Action | Command |
| --- | --- |
| Add equipment | `add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]` |
| Delete equipment | `delete item/[ITEM ID]` |
| Exit | `bye` |
