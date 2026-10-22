# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

### Updating equipment details

The update feature lets an equipment officer correct an item's name, type, or condition without
deleting the item and adding it again. The Item ID identifies the record to update and stays fixed.
Only the supplied details change; omitted details keep their existing values. The implementation
described here is on the equipment update feature branch in
[PR #59](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/59), pending merge into `master`.

#### Classes involved

The feature uses the existing command handler and equipment model. Command parsing and validation
finish before the stored item is changed.

| Class | Responsibility in an update |
| --- | --- |
| `EqmManager` | Routes the command to `EquipmentHandler` and displays the result or error message. |
| `EquipmentFields` | Extracts named fields and rejects empty, repeated, or unsupported reserved fields. |
| `EquipmentHandler` | Finds the target, fills in omitted values, validates the resulting details, and requests the change. |
| `Equipment` | Stores the item's identity and applies the validated name, type, and condition together. |

`EquipmentHandler` owns the inventory. `EquipmentFields` is a utility class with static parsing
methods; it does not store equipment. Keeping these responsibilities separate lets the handler
check the command against the inventory while the parser deals with field syntax.

<!-- Add class diagram here: EqmManager uses EquipmentHandler; EquipmentHandler holds Equipment
     objects and uses EquipmentFields. Show only the update-related methods. -->

#### Update flow

Suppose Item ID `001` belongs to an equipment object named `Camera`, with type `camera` and
condition `1`. The command `update id/001 i/Film Camera cond/5` changes its name and condition
while keeping its type.

1. `EqmManager.executeInput(...)` recognises the update command and calls
   `EquipmentHandler.updateEquipment(...)`.
2. The handler calls `EquipmentFields.parse(...)` to obtain a map of field names and values.
   Fields may appear in any order. Each field may occur only once, so a later value cannot
   silently replace an earlier value.
3. The handler checks that an Item ID and at least one editable field were supplied.
   `findEquipmentForUpdate(...)` then searches the inventory for exactly one matching item.
4. The handler prepares the complete set of replacement details. In this example, it takes
   `Film Camera` and `5` from the command and keeps `camera` from the target object.
   `validateEquipmentDetails(...)` checks these values before any assignment occurs.
5. The handler calls `Equipment.updateDetails(...)` on the existing object and returns that
   object to `EqmManager`, which displays the success message.

After the command, the same object occupies the same position in the inventory. Its Item ID
remains `001`, its name is `Film Camera`, its type is `camera`, and its condition is `5`.

<!-- Add sequence diagram here for the example above. Include :EqmManager, :EquipmentHandler,
     the static EquipmentFields parser, and the target :Equipment. Show parsing, target lookup,
     validation, updateDetails(...), and the returned result. Keep error handling in a note. -->

#### Validation and rejected commands

An update requires an Item ID from `001` to `999` and at least one of `i/`, `type/`, or `cond/`.
The name must contain 1 to 100 characters, the type must contain text, and the condition must
be a whole number from 1 to 10. Addition and updates use the same value validation method,
so an update cannot introduce details that an addition would reject.

A missing target is rejected. The lookup also rejects multiple matches rather than changing
an arbitrary item if the inventory contains duplicate IDs. Empty and repeated fields are
rejected during parsing, before the handler attempts to modify an item.

For example, `update id/001 i/Film Camera cond/11` must leave both the original name and
condition unchanged. The handler first validates every proposed value, then calls
`updateDetails(...)` only if all checks pass. A rejected command becomes an
`EquipmentUpdateUnsuccessful` exception, whose message is displayed by `EqmManager`.
The application can then accept another command.

#### Design decisions

The Item ID is fixed because other records use it to identify the equipment. Changing details
should not change which item those records refer to. Updating the existing object also
preserves references held by other parts of the application. An alternative would be to create
a replacement object, but that would require care to update existing references and preserve
any additional state stored on the original item.

Keeping omitted fields makes small corrections convenient. Requiring every field again would
make the command longer and give users more opportunities to overwrite correct details.
At least one editable field is still required, so a command containing only an ID is rejected.

User input is checked with exceptions, which work whether Java assertions are enabled or
disabled. Assertions inside the model and handler check programming assumptions, including
valid internal values, an unchanged Item ID, and an unchanged inventory size. Diagnostic
logging records accepted and rejected updates at `FINE` level.

#### Testing the feature

`EquipmentUpdateTest` covers partial updates, multiple supplied fields, valid boundaries,
malformed commands, and missing or ambiguous IDs. Rejection tests check that every original
field and the inventory order remain unchanged, including when one supplied value is valid
and another is invalid. Successful updates are checked for object identity as well as values.
Command-level tests also check that a valid update succeeds after a rejected command.


## Product scope
### Target user profile

{Describe the target user profile}

### Value proposition

{Describe the value proposition: what problem does it solve?}

## User Stories

|Version| As a ... | I want to ... | So that I can ...|
|--------|----------|---------------|------------------|
|v1.0|new user|see usage instructions|refer to them when I forget how to use the application|
|v2.0|user|find a to-do item by name|locate a to-do without having to go through the entire list|

## Non-Functional Requirements

{Give non-functional requirements}

## Glossary

* *glossary item* - Definition

## Instructions for manual testing

{Give instructions on how to do a manual product testing e.g., how to load sample data to be used for testing}
