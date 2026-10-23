# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

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
