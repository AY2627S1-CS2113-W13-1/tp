# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

### Updating equipment details

The update feature lets an equipment officer correct an item's name or condition without
deleting the item and adding it again. The Item ID identifies the record to update and stays fixed.
Only the supplied details change; omitted details keep their existing values. The implementation
described here is provided by
[PR #59](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/59).

The equipment type is fixed by its subclass. An optional `type/` field must match the current type,
ignoring case; a request to change to a different type is rejected. This keeps type-specific fields,
damage, availability, and existing loan references attached to the same object.

#### Classes involved

The feature uses the existing command handler, equipment model, and storage class. Command parsing,
validation, and saving finish before the existing equipment object is changed.

| Class | Responsibility in an update |
| --- | --- |
| `EqmManager` | Routes the command to `EquipmentHandler` and displays the result or error message. |
| `EquipmentFields` | Extracts named fields and rejects empty, repeated, or unsupported reserved fields. |
| `EquipmentHandler` | Finds the target, fills in omitted values, validates and saves the resulting details, then changes the existing object. |
| `Equipment` | Stores the item's identity and applies the validated name and condition together, preserving its subclass and other state. |
| `EquipmentStorage` | Writes the proposed inventory to the backing file when storage is configured. |

`EquipmentHandler` owns the inventory. `EquipmentFields` is a utility class with static parsing
methods; it does not store equipment. Keeping these responsibilities separate lets the handler
check the command against the inventory while the parser deals with field syntax.

<!-- Add class diagram here: EqmManager uses EquipmentHandler; EquipmentHandler holds Equipment
     objects and uses EquipmentFields and EquipmentStorage. Show only the update-related methods. -->

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
5. `saveUpdate(...)` creates a copy of the inventory list and replaces the target's entry in
   that copy with an object of the same equipment subclass. The existing creation method,
   `createValidatedEquipment(...)`, builds this proposed object using the new name and condition,
   the same Item ID and type, and the target's type-specific fields and damage from `getDetails()`.
   The handler saves this proposed inventory through `EquipmentStorage.save(...)` when a
   backing file is configured. The existing object and inventory remain unchanged at this stage.
6. After the save succeeds, or is skipped because no backing file is configured, the handler
   calls `Equipment.updateDetails(...)` on the existing object. The handler returns that object
   to `EqmManager`, which displays the success message.

After the command, the same object occupies the same position in the inventory. Its Item ID
remains `001`, its name is `Film Camera`, its type is `camera`, and its condition is `5`.

<!-- Add sequence diagram here for the example above. Include :EqmManager, :EquipmentHandler,
     the static EquipmentFields parser, and the target :Equipment. Show parsing, target lookup,
     validation, saveUpdate(...), EquipmentStorage.save(...), updateDetails(...), and the returned
     result. Note that a failed save leaves the existing object and inventory unchanged. -->

#### Validation and rejected commands

An update requires an Item ID from `001` to `999` and at least one of `i/`, `type/`, or `cond/`.
The name must contain 1 to 100 characters, the type must contain text, and the condition must
be a whole number from 1 to 10. Addition and updates use the same value validation method,
so an update cannot introduce details that an addition would reject.
An explicitly supplied type must match the target's type, ignoring case. A different type is
rejected before saving, even when the other supplied values are valid.

A missing target is rejected. The lookup also rejects multiple matches rather than changing
an arbitrary item if the inventory contains duplicate IDs. Empty and repeated fields are
rejected during parsing, before the handler attempts to modify an item.

For example, `update id/001 i/Film Camera cond/11` must leave both the original name and
condition unchanged. The handler first validates every proposed value, then saves the proposed
inventory before calling `updateDetails(...)`. A rejected command becomes an
`EquipmentUpdateUnsuccessful` exception, whose message is displayed by `EqmManager`.
The application can then accept another command.

If saving throws an `IOException`, `saveUpdate(...)` wraps it in an
`EquipmentUpdateUnsuccessful` exception with a message beginning `Could not save equipment update:`.
The handler does not call `updateDetails(...)`, so the original object's name, type, condition,
and Item ID remain unchanged. The inventory keeps the same objects in the same order.
`EqmManager` displays the error instead of a success message, and the application can accept
another command.

#### Design decisions

The Item ID is fixed because other records use it to identify the equipment. Changing details
should not change which item those records refer to. Updating the existing object also
preserves references held by active loans and the item's availability. Only the proposed list used
for saving contains a new object; the inventory retains the original object after a successful save.
Changing type is rejected because types are represented by different subclasses. Replacing the
inventory object with another subclass would require rules for converting its specific fields and
damage, and updating every existing loan reference. Keeping the current type avoids those changes.

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

`EquipmentUpdatePersistenceTest` checks that successful updates are saved and can be loaded again.
It also checks that a failed save leaves the original equipment details, object identity, and
inventory order unchanged.

### Overdue loan identification

#### Overview

The overdue-loan feature allows equipment managers to identify active loans whose due dates have passed. Users access the feature with the `list overdue` command. The command displays the item ID, borrower ID, and due date of every overdue loan. If there are no overdue loans, the application displays a clear message instead of an empty table.

A loan is considered overdue only when both of the following conditions are satisfied:

1. The loan has not been marked as returned.
2. Its due date is earlier than the current date.

The comparison is date-based because loan commands currently accept dates in `DD-MM-YY` format without a specific time. Therefore, a loan due today is not considered overdue. It becomes overdue on the following day if it has not been returned.

The feature performs a read-only operation. Listing overdue loans does not add, remove, return, or otherwise modify any loan record.

#### Component responsibilities

The implementation distributes responsibilities among the existing loan-management classes.

`EqmManager` recognises the exact `list overdue` command and delegates the request to `LoanHandler`. It obtains the current date once and supplies it as the reference date for the whole operation. Using one reference date ensures that every loan in the same command is evaluated consistently.

`LoanHandler` examines the stored loans and selects those that are overdue. It preserves their existing order so that the displayed results remain predictable. It is also responsible for formatting the output and displaying the no-results message.

`Loan` determines whether one loan is overdue. The planned `isOverdue(referenceDate)` method checks the loan's due date and returned status without changing either field. Keeping this rule in `Loan` places behaviour together with the data that it evaluates and prevents the same condition from being duplicated across command-handling classes.

Conceptually, the overdue condition is:

```text
not returned AND due date is before reference date
```

The reference date is passed into the method instead of being obtained inside `Loan`. This makes the result deterministic during automated testing. Tests can supply a fixed date and verify past, present, and future due dates without depending on the date on which the test suite is executed.

#### Command execution

When the user enters `list overdue`, `EqmManager` recognises the command and obtains the current local date. It then asks `LoanHandler` to display loans that are overdue on that date.

`LoanHandler` checks every loan using `Loan.isOverdue(referenceDate)`. Loans that have been returned are excluded even when their due dates are in the past. Active loans due today or in the future are also excluded. Matching loans are displayed without modifying the underlying loan list.

If no loans match, the user receives a message such as:

```text
No overdue loans found.
```

This is clearer than displaying only a table heading because it confirms that the command completed successfully.

#### Defensive behaviour

The reference date must not be `null`. A null reference date indicates a programming error rather than invalid user input because users do not provide this value directly. The overdue-checking method therefore rejects a null reference date immediately.

The feature relies on the existing invariant that every valid loan has a due date and returned status. Assertions can detect violations of this invariant during development.

The operation does not modify the loan collection. Invalid or unrecognised commands are handled by the existing command-routing behaviour and leave all stored records unchanged. Diagnostic logging records that an overdue-loan listing was requested and the number of matching loans without exposing internal diagnostic information to the user.

#### Design alternatives

One alternative was to store an `isOverdue` Boolean field in each `Loan`. This was rejected because overdue status changes as time passes. A stored value could become outdated unless every loan were updated regularly. Deriving the status from the due date and reference date ensures that it is always current.

Another alternative was to place the complete overdue condition inside `LoanHandler`. This would keep `Loan` as a data-only class, but it would separate loan-related behaviour from the object that owns the relevant state. Placing `isOverdue` in `Loan` also allows the rule to be tested independently.

A third alternative was to compare complete `LocalDateTime` values. This was rejected because users currently provide only a due date. Comparing times would cause a loan due today to become overdue immediately after midnight, which would be surprising to users. Comparing only the date components matches the command format more closely.

#### Testing strategy

The `Loan.isOverdue` behaviour will be tested using fixed reference dates. The tests will cover an active loan with a past due date, a loan due on the reference date, a future loan, and a returned loan with a past due date. A null reference date will also be tested.

`LoanHandler` tests will use collections containing mixtures of overdue, returned, current, and future loans. They will verify that only the correct loans are displayed, that their order is preserved, and that the underlying collection remains unchanged. An empty list and a list containing no overdue loans will verify the no-results message.

An integration test through `EqmManager` will verify that `list overdue` is routed correctly and that similar invalid commands do not crash the application or modify saved data.

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
