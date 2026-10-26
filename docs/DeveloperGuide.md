---
layout: default
title: Developer Guide
---

# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

The following sections explain the architecture and implementation of equipment management, loans,
returns, history, optional fields, help and shared logging.

Diagrams omit minor helpers and repetitive calls; `...` omits parameters. Named and unnamed object lifelines
use `item:Equipment` and `:Equipment` notation. Class and utility lifelines receive static calls.
Action labels such as `display success` summarise local processing rather than additional Java methods.
Select an SVG to view it at full size; editable PlantUML sources are linked below each figure.

### Application architecture

The architecture diagram groups related classes into feature areas. Arrows show dependencies;
the feature sequence diagrams illustrate execution order for individual commands.

[![Application architecture: command coordination, shared equipment inventory and local file storage](images/AppArchitecture.svg)](images/AppArchitecture.svg)

[PlantUML source](diagrams/AppArchitecture.puml)

| Part | Main classes | Responsibility |
| --- | --- | --- |
| Startup and logging | `Main`, `AppLogging` | Starts the command loop and shared rotating logs; closes logging when the application ends. |
| Coordination | `EqmManager` | Creates the handlers, routes commands and reports outcomes. |
| Input | `Parser` | Reads one command line from the console. Feature parsers interpret its fields. |
| Help | `Help` | Prints the command reference. |
| Equipment | `EquipmentHandler`, `EquipmentFilter`, `Equipment` and its subclasses | Manages the inventory, filtering, updates and subtype details. |
| Loans | `LoanHandler`, `Loan` | Manages borrowing, returns, history and overdue queries. |
| Bundles | `BundleHandler`, `Bundle` | Groups equipment for the current session. |
| File storage | `EquipmentStorage`, `LoanStorage`, `LoanReturnStorage` | Loads and saves records, including coordinated return saves. |

Both `LoanHandler` and `BundleHandler` use the same `EquipmentHandler` created by `EqmManager`.
They therefore work with the current equipment objects and availability. For example, updating an item's
name changes the item seen by an existing loan or bundle without replacing its equipment reference.

The file-storage box groups the existing utilities. Default paths are `data/equipment.txt`,
`data/loans.txt` and `data/loan-sequence.txt`. The [logging section](#shared-logging-and-equipment-assertions)
explains the shared file handler and cleanup.

Build and run with Java 25. Gradle starts `seedu.snap.Main`; `shadowJar` produces
`build/libs/snAp.jar`. The `run` and `test` tasks enable assertions. For the packaged JAR,
`java -ea -jar build/libs/snAp.jar` enables the same development checks; normal command validation
uses exceptions and remains active without `-ea`.

### Equipment compatibility attributes

#### Purpose and representation

Different equipment subtypes require different information when deciding whether they can be used together.
The model therefore stores compatibility information on the subtype that owns it instead of adding unrelated
fields to the base `Equipment` class.

| Equipment subtype | Compatibility attributes |
| --- | --- |
| `Camera` | Lens mount, tripod thread and battery type |
| `Lens` | Lens mount and supported sensor format |
| `Battery` | Battery type |
| `Tripod` | Tripod thread |

The existing subtype detail remains separate from these attributes. For example, a camera retains its sensor
format while also storing its mount, tripod thread and battery type. Every compatibility value is normalised
through `CompatibilityText`. Leading and trailing Unicode whitespace is removed, values must contain between
1 and 100 characters, and omitted values use the shared `unspecified` representation.

`EquipmentDetailField` defines the supported command markers. `EquipmentDetailParser` scans the optional fields,
rejects repeated, reordered or inapplicable markers, and returns the values supplied by the user.
The subtype-specific parsers then construct `Camera`, `Lens`, `Battery` or `Tripod` objects.
`EquipmentDetailFormatter` generates the same ordered representation for listing and persistence.

#### Compatibility comparison

`EquipmentCompatibility.compare(first, second)` provides an internal API for bundle and other follow-up
features. This version does not expose a separate command that prints compatibility results.
Comparison works in either argument order and follows these relationships:

1. A camera and lens compare their lens mounts and sensor formats.
2. A camera and battery compare their battery types.
3. A camera and tripod compare their tripod threads.
4. Other equipment pairs return `NOT_APPLICABLE`.

Known matching values produce `COMPATIBLE`. A known mismatch produces `INCOMPATIBLE`.
If a required value is `unspecified`, the result is `UNKNOWN`. For camera-and-lens comparison, a known mismatch
takes precedence over an unknown value; the pair cannot be compatible when one known attribute conflicts.
Comparisons ignore letter case because compatibility identifiers may be entered with different capitalisation.

#### Preservation and persistence

Compatibility attributes form part of each subtype's normal equipment details. `EquipmentStorage` therefore
saves them using the same command-style record used for the other subtype fields. Loading reconstructs the
correct subclass and restores every supplied attribute. Older records remain valid because omitted compatibility
fields default to `unspecified`.

Equipment name and condition updates recreate a proposed record from `getDetails()`, so compatibility values
survive validation, saving and restart. Return assessments also copy every compatibility attribute into the
detached proposal before saving. Applying an assessment changes damage, remarks and serviceability without
discarding the equipment information needed by later bundle checks.

#### Design choices and verification

Storing the fields directly on their relevant subclasses keeps invalid combinations unrepresentable; for
example, a standalone battery does not acquire lens-mount data. Using a shared parser and formatter avoids
different ordering or validation rules between subtypes. A three-state applicable result distinguishes missing
information from a confirmed mismatch, while `NOT_APPLICABLE` distinguishes unrelated equipment types.

`EquipmentCompatibilityTest` covers matching, conflicting, unknown and unrelated pairs, reversed arguments,
precedence rules and comparisons after persistence reload. Subtype parser and integration tests cover optional,
repeated, reordered and inapplicable fields, Unicode whitespace, listing, updating and legacy saved records.
`EquipmentReturnAssessmentTest` verifies that compatibility attributes survive assessment preparation,
application, saving and restart.

### Returning equipment with an assessment

#### Purpose and responsibilities

A return must end the active loan while retaining the original borrowing record.
It must also record whether the item can be used again. For example,
`return loan/001 serviceable/no damage/SENSOR_DAMAGE remarks/Needs repair` retains loan `001` as returned
and records an unserviceable camera. Deleting that loan instead would lose its borrowing history.

| Class | Responsibility in a return |
| --- | --- |
| `EqmManager` | Routes the command and displays success or `LoanReturnUnsuccessful`. |
| `LoanReturn` | Parses the ordered assessment fields into a record without changing equipment. |
| `LoanHandler` | Checks the loan and its ownership, prepares proposals, requests saving, then changes the original objects. |
| `EquipmentHandler` | Validates subtype-specific damage and prepares and saves the proposed equipment assessment. |
| `LoanReturnStorage` | Captures the configured files, saves both proposals and attempts restoration after a write failure. |
| `Equipment` and `Loan` | Apply the saved assessment and permanent returned state while preserving their identities. |

#### Successful return

The sequence below assumes an active loan `001` and an assessment valid for its equipment subtype.
Parsing and repeated inventory checks are summarised in the first note to keep attention on the order of saving
and changing records.

[![Return sequence: prepare detached proposals, save them, then change the original equipment and loan.](images/ReturnSequence.svg)](images/ReturnSequence.svg)

*Figure 1. A successful return changes the original records only after saving succeeds.*
[PlantUML source](diagrams/ReturnSequence.puml)

1. `LoanReturn.parse(...)` requires a Loan ID from `001` to `999`, an explicit serviceability decision and
   a damage value. Final `remarks/` is optional and defaults to empty text. Supplied remarks must be a single
   line of at most 200 characters after trimming; text after that final prefix remains literal.
2. `LoanHandler.findLoanForReturn(...)` requires exactly one active loan. The handler finds the current equipment
   and calls `Loan.ownsEquipment(...)`, checking the object reference, active loan ID and permanent equipment
   record ID. It also rejects another active loan for the same Item ID. These checks prevent a stale association
   from assessing or releasing replacement equipment.
3. `EquipmentHandler.prepareReturnAssessment(...)` uses the equipment subtype's damage enum to create a detached
   proposal. It preserves the name, Item ID, permanent record ID, numeric condition and subtype attributes.
   `Loan.returnedCopy()` creates a detached returned loan with the same loan ID, equipment identity, borrower
   and dates. The proposals do not change either live object.
4. `LoanReturnStorage.save(...)` saves a proposed inventory containing the assessment and a proposed loan list
   containing the returned copy. The next loan number is saved without being incremented.
5. After saving succeeds, `Equipment.applyReturnAssessment(...)` changes the original item and
   `Loan.setReturned(true)` changes the original loan. The latter detaches its active equipment reference and
   clears the item's loan ID and loaned flag. The loan remains in the list; its count and next ID are unchanged.

The original equipment object is retained because bundles and active loan associations use object references.
Replacing the inventory entry alone could leave a bundle pointing to old details. Returning a loan is permanent:
it cannot be returned twice or reactivated. Later borrowing creates a new loan with a new ID.

Serviceability is separate from the loaned flag. An unserviceable returned item has no active loan, but new loans,
new bundle additions and `status/available` filtering reject it. Listing displays `Unserviceable`.
Normal name and numeric-condition updates retain the assessment. There is no repair command in this feature.
Storage prefixes each add record with `recordId/UUID`, then `serviceable/yes|no`, separated by tabs.
This keeps metadata separate from free-form names, subtype attributes and remarks. Legacy equipment without an
assessment defaults to serviceable.

#### Save failures and recovery

The next diagram expands the storage step in Figure 1. It shows a loan or sequence save failing after the
equipment proposal was written. An equipment-save failure enters the same restoration loop. Only configured
files are captured. A snapshot records regular-file bytes, absence or a pre-existing non-regular path.

[![Failed return save: capture configured files, attempt writes, restore every snapshot, then report the failure.](images/ReturnSaveFailure.svg)](images/ReturnSaveFailure.svg)

*Figure 2. A reported write failure triggers restoration before the return is rejected.*
[PlantUML source](diagrams/ReturnSaveFailure.puml)

All snapshots are captured before any return writes begin. If capture itself fails, no proposed records are
written. On a write failure, `LoanReturnStorage` attempts each restoration independently, even if an earlier
restoration also fails. Restoration errors are attached to the original exception. `LoanHandler` reports them
in `LoanReturnUnsuccessful` and leaves the live equipment, active loan and counters unchanged. The user must
check saved files if restoration also failed; the application does not report a successful return.

Snapshots handle reported I/O failures, but do not provide recovery after abrupt process termination or power
loss between file replacements. That would need a persistent transaction journal or transactional database.

#### Design choices and alternatives

| Decision | Reason and alternative considered |
| --- | --- |
| Retain the loan and mark it returned | Deleting the loan would discard the borrowing record needed by history. |
| Prepare detached proposals and save before changing records | Changing live objects first would need reversal after a failed save. In particular, a returned loan cannot be reactivated. |
| Apply the saved result to the original objects | Replacing objects would require updating every bundle and loan reference. |
| Record serviceability separately | Deriving it from `isLoaned` or numeric condition would treat some returned damaged items as available without an explicit assessment. |
| Snapshot files for a multi-file return | Independent writes could leave equipment and loans inconsistent after a reported failure. A journal would also cover crash recovery, but adds a separate recovery mechanism. |

#### Verification

`LoanReturnTest`, `EquipmentReturnAssessmentTest` and `EquipmentReturnIntegrationTest` cover valid assessments
for each subtype, required fields, permanent returned state, stale ownership, unchanged bundle references and
serviceability restrictions. Persistence tests reload returned records and verify the assessment. Failure tests
block equipment, loan and sequence saves, check that records and previous files are retained where restoration
is possible, and retry successfully after the storage problem is removed.

### Viewing the history of a physical equipment item

#### Permanent identity

`history id/001` displays active and returned loans for the physical item currently registered as `001`.
The three-digit Item ID can be reused after deletion. Matching only that value would mix the old and replacement
items' histories, even if the replacement has the same name and type.

Each new `Equipment` receives a UUID stored as the `recordId` string. A loan created from that equipment copies
it into `equipmentRecordId`. These stored values survive restarts. The class diagram shows the distinction
between that permanent identity and an active object reference; unrelated fields, subclasses and storage classes
are omitted.

[![History class diagram: handlers manage records; loans store the permanent equipment identity and an optional active reference.](images/EquipmentIdentity.svg)](images/EquipmentIdentity.svg)

*Figure 3. History uses stored identity values, while an active loan also refers to an equipment object.*
[PlantUML source](diagrams/EquipmentIdentity.puml)

`LoanHandler` selects a record only when **both** its Item ID and permanent equipment ID match the current item.
A replacement receives a fresh UUID even if all user-visible details are identical. Return and update proposals
preserve the original UUID. A returned loan keeps its identity string after its active object reference is cleared.

Equipment storage writes `recordId/UUID` before serviceability and the add record, with tab delimiters.
Loan storage appends `equipment/UUID` after its returned flag. Both loaders validate UUID syntax;
`EquipmentStorage` also rejects duplicate equipment record IDs before publishing the inventory. Multiple loans
may intentionally share an equipment identity. An active loan whose explicit identity does not match the current
item is rejected.

#### Query sequence

The following sequence assumes Item ID `001` exists uniquely. Invalid syntax, a missing item or an ambiguous
Item ID produces `LoanHistoryUnsuccessful` before selection.

[![History query sequence: find the current item, match both identifiers, sort a separate list and display it without saving.](images/HistorySequence.svg)](images/HistorySequence.svg)

*Figure 4. A history query sorts a selected copy and leaves the managed records unchanged.*
[PlantUML source](diagrams/HistorySequence.puml)

`getLoanHistory(...)` sorts the selected copy by issue date and then numeric loan ID, returning `List.copyOf(...)`.
`viewLoanHistory(...)` uses the shared loan table or prints a clear empty-history message. Sorting the main loan
list instead would change the order used by later `list loans` requests. The query performs no storage operations.

#### Loading older records

Older equipment files lack permanent IDs. The parser assigns UUIDs to proposed records, then validates the
complete file. The migration must be saved successfully before the loaded inventory replaces the current objects.
Later loads do not rewrite files that already contain identities. If equipment migration succeeds but loan
migration fails, a retry reuses the saved equipment IDs rather than generating another set.

An active legacy loan can be associated with the current item because equipment cannot be deleted while it has
an active loan. The loader binds its proposed record to the current UUID and saves the migration before changing
the live loan list, counters, equipment flags or associations.

Returned legacy loans cannot be associated safely because their Item IDs may already have been reused.
They retain a null identity across saves and loads and remain visible in `list loans`. Item-specific history
excludes them and prints an omission note when relevant. This preserves the evidence available in the old data
without guessing which physical item was borrowed.

#### Design choices and alternatives

| Decision | Reason and alternative considered |
| --- | --- |
| Store a UUID separately from the Item ID | Item IDs are reusable. Combining an ID with a name or type still fails for an identical replacement. |
| Persist identity values in equipment and loans | Java object references change after restart and cannot link saved historical records on their own. |
| Leave uncertain returned legacy loans unattributed | Assigning them to today's item would create false borrowing history. They remain accessible in the full loan list. |
| Return an unmodifiable sorted copy | Sorting or exposing the managed list would allow a display request or caller to reorder the application's records. |

#### Verification

`EquipmentLoanHistoryTest` and `EquipmentLoanHistoryIntegrationTest` exercise same-name and different-type
replacement items, Item ID reuse across restarts, preserved identities through updates and returns, chronological
ordering, empty histories and read-only queries. Storage tests also reject malformed or duplicate identities and
check that failed migrations do not publish partial in-memory state. A retry must retain any identities already
saved by a successful equipment migration.

### Optional command fields and explicit defaults

#### Rules and responsibilities

Optional fields shorten commands while preserving validation. Defaults apply only when the whole field is absent;
an invalid supplied value is rejected so that a typing mistake does not silently change its meaning.

| Command or field | Behaviour when omitted |
| --- | --- |
| Equipment subtype detail | `unspecified` |
| Equipment `damage/` | `NONE` |
| Equipment `remarks/` | Empty text |
| Loan `issued/` | Today's date in the application's local time zone, at midnight |
| Return `remarks/` | Empty text, replacing previous remarks after a successful save |
| An update field | Keep its current value |
| A filter criterion | Impose no restriction for that criterion |

Equipment addition supplies its defaults through `EquipmentHandler`. Optional details and damage use the same
Unicode whitespace trimming as common fields; a supplied whitespace-only detail is rejected.
`LoanReturn.parse(...)` supplies omitted final remarks as an empty string through the existing assessment and
save-before-change path. An explicitly empty remark has the same meaning. Loan ID, serviceability and damage
remain mandatory, so the application cannot assume that a return assessment was performed.

#### Default issue-date sequence

For this example, assume the application's local date is 24 October 2026 and the equipment is available.
`loan id/001 b/002 due/31-10-26` stores 24 October 2026 at midnight as its issue date.
The diagram omits model construction and ordinary availability checks to focus on when the default is chosen.

[![Optional issue-date sequence: validate required input, read the local date once, save explicit dates and then publish the loan.](images/LoanDateDefault.svg)](images/LoanDateDefault.svg)

*Figure 5. The optional date is resolved during command parsing and stored as an ordinary explicit date.*
[PlantUML source](diagrams/LoanDateDefault.puml)

`LoanHandler.parseLoanCommand(...)` accepts four or five arguments and delegates to `parseLoanArguments(...)`.
For the four-argument form, the shared validator checks the IDs and required due date, then reads
`LocalDate.now(clock)` once and calls `atStartOfDay()`. It rejects an issue date after the due date before saving
or consuming a loan ID. The five-argument form strictly parses the supplied issue date without reading the clock.

The `Clock` is supplied through constructors, with existing constructors using the system's local time zone.
Tests can supply a fixed clock and check date boundaries without changing the computer's date. Reading once
also prevents one command from obtaining inconsistent defaults if the clock crosses midnight.

`parseLoanRecord(...)` is a separate entry point requiring both dates explicitly, while sharing value validation
with commands. Restart loading never replaces a missing or invalid historical date with today's date.

#### Design choices and alternatives

Keeping `due/` mandatory leaves the intended loan duration to the user. Inventing a default duration would require
an additional rule that has not been specified. Defaulting empty or invalid `issued/` values was also rejected
because it would hide input errors. A supplied `Clock` is preferable to direct system-date calls because local
dates and midnight boundaries can be tested consistently.

Using the permissive command parser for storage would make the meaning of an incomplete saved record depend on
when it was loaded. The separate strict entry point prevents that. Similarly, omitted return remarks clear the
previous assessment's notes; retaining them could present an old damage note as part of the new assessment.

#### Verification

`EquipmentOptionalFieldsTest` checks every subset of optional equipment fields for all four subtypes.
`LoanOptionalFieldsTest` checks fixed local dates, explicit dates, time zones, one clock read at midnight, invalid
supplied values and strict stored records. `OptionalFieldsIntegrationTest` checks persistence, omitted and empty
return remarks, required assessments, and failed saves followed by successful retries.

### Showing command help without changing records

#### Implementation and sequence

`help` displays all 16 supported command formats and examples, required and optional fields, defaults and
restrictions. The welcome message and unknown-command error direct users to it. `EqmManager.executeInput(...)`
recognises the exact command after trimming surrounding whitespace; `help add` follows the unknown-command path.
The existing startup-error check remains first: the input loop stops if saved data cannot be loaded.

[![Help sequence: EqmManager calls Help.print, which prints the fixed reference without using record or storage handlers.](images/HelpSequence.svg)](images/HelpSequence.svg)

*Figure 6. Help prints a reference and returns control to the command loop.*
[PlantUML source](diagrams/HelpSequence.puml)

`Help` is a utility class containing a fixed reference built once as `TEXT`. It uses immutable equipment limits
and damage enums when constructing the text, so those values come from the model. `Help.print()` only calls
`System.out.print(TEXT)`. It has no equipment, loan or bundle handler references and performs no storage operations.
Consequently, requesting help does not consume identifiers, save files, alter records or end the session.

#### Design choices and alternatives

A fixed reference is sufficient for one no-argument help command. Per-command help would need additional command
syntax, while generating descriptions by reflection would need metadata that the handlers do not currently
provide. Both add work beyond the present requirement. Reading constants and enum values reduces duplicated
limits, but command syntax and explanatory prose still need to be updated when supported behaviour changes.

Printing independently of handlers also means help does not require a writeable data directory after successful
startup. Calling save methods from a general command-completion path would make this read-only request depend
on storage and could change files unnecessarily.

#### Verification

`HelpTest` checks formats and examples for every command, defaults, limits, subtype damage values and output
line endings. `HelpIntegrationTest` compares record contents, object references, permanent IDs, assessments,
counts, next identifiers, bundle completion, file bytes and modification times before and after repeated requests.
It also checks missing and blocked storage paths, surrounding Unicode whitespace, invalid help topics, continued
commands and the interactive help-then-exit loop. Invalid saved data still causes the existing startup abort.

### Equipment management

Equipment addition, filtering and updates use the same inventory. The shared model also tracks
availability so borrowing and queries agree about which equipment can be used.

#### Shared equipment model

`EquipmentHandler` owns the ordered inventory. Loans and bundles share its equipment objects, so they see
the same details and availability. `getEquipmentList()` provides an unmodifiable view for queries and display;
additions must go through validation and duplicate checks.

[![Shared equipment classes: inventory ownership, filtering, field parsing, storage and a representative subtype](images/EquipmentClasses.svg)](images/EquipmentClasses.svg)

[PlantUML source](diagrams/EquipmentClasses.puml)

`Equipment` holds common identity, name, condition and availability state. `Camera` represents one
concrete subtype; `Lens`, `Battery` and `Tripod` have the same inheritance relationship.
`EquipmentFields` parses filter and update fields, while `EquipmentStorage` handles file access.
The add command has its own ordered parser in `EquipmentHandler`.

Solid arrows show stored references; dashed arrows show dependencies. `+`, `-` and `~` mean public,
private and package-private; underlined operations are static and italic names are abstract.
Minor fields, getters and the loan/bundle classes are omitted.

##### Availability and loan ownership

An active `Loan` refers to the original equipment object. The equipment records its loan ID and loaned
flag together through `setLoanId(...)`. Constructing a proposed loan does not reserve the item:
`LoanHandler.addLoan(...)` checks for an existing active loan, finds one available, serviceable item
and saves before `Loan.attachEquipment(...)` publishes the association.

[![Equipment availability: saved borrowing and return operations change the shared item's status](images/EquipmentAvailability.svg)](images/EquipmentAvailability.svg)

[PlantUML source](diagrams/EquipmentAvailability.puml)

The state diagram shows successful commands; loading and equipment deletion are omitted. With no active
loan, serviceability decides between `Available` and `Unserviceable`. The integrated
[return assessment](#returning-equipment-with-an-assessment) sets serviceability separately from numeric
condition and damage. A name or condition update does not change it.

`Loan.detachEquipment()` clears the item's loan ID only if that loan still owns it, then drops its
reference. This stops an older loan from releasing an item borrowed under a newer loan. Returns and loan
deletions save before detaching. Equipment deletion is blocked by the CLI's active-loan check and the
handler's loaned-flag check; the CLI also checks bundle membership.

Equipment loads before loans at startup. The loan loader validates active associations, clears old
runtime links and reattaches accepted active loans. Rebuilding links avoids persisting a second loaned
flag that could disagree with the loan records. `EquipmentAvailabilityTest` covers double borrowing,
failed reservations, ownership guards, deletion and reborrowing; `MainTest` checks restart and loan-deletion
behaviour. Return integration tests cover serviceability and save failures.

#### Adding equipment and rejecting duplicate Item IDs

##### Command parsing

`EquipmentHandler.addEquipment(...)` parses and validates the command, checks for duplicates, then saves
and appends the item. `EqmManager` displays the outcome.

Fields follow the order name, Item ID, type, condition, optional subtype details and final remarks.
The parser recognises prefixes at field boundaries, including after Unicode whitespace, rather than
splitting on every space. This preserves names such as `USB id/card reader` and `fluid/head tripod`.
The last `id/` before `type/` identifies the Item ID; earlier fragments in the name cannot start with
whitespace, a digit, `+` or `-`. Numeric ID-like fragments stay reserved to detect repeated fields.

Subtype fields are parsed after `cond/`. The first `remarks/` starts the final literal field, so text such
as `Check id/001 adapter` is not mistaken for another ID. Subtype parsers handle their own attributes and
damage values, as described under compatibility attributes and optional fields.

##### Validation, uniqueness and publication

Before construction, common validation checks a name of 1 to 100 characters, an Item ID from `001` to
`999`, a nonblank type and an integer condition written as `1` to `10`. Blank checks include Unicode
whitespace and nonbreaking spaces. Only supported subtypes are constructed. Updates reuse these checks.

IDs are compared with existing items using exact equality. A second `id/001` is rejected even if its name
or type differs; names may be shared when IDs differ. A deleted item's ID can be reused, but the replacement
gets a new permanent record identity to keep physical-item histories separate.

Parsing finishes before duplicate detection, so malformed input is reported first. The proposed inventory
is saved before the item is appended to the live list. Rejection or a failed save leaves that list unchanged.

##### Design choices and verification

An ordered boundary parser supports spaces and prefix-like name text without a quoting language.
Duplicate IDs are rejected because replacing an existing entry could detach loan and bundle references.

[![Equipment addition: reject an existing Item ID or save before appending a new item](images/EquipmentAdditionSequence.svg)](images/EquipmentAdditionSequence.svg)

[PlantUML source](diagrams/EquipmentAdditionSequence.puml)

The sequence assumes configured storage. Parsing includes validation and subtype construction.
Any validation or save failure exits before insertion.

`EquipmentAdditionTest` covers valid boundaries, all ten condition values, malformed and repeated fields,
Unicode whitespace, reserved ID syntax, literal remarks and recovery after rejection. `EquipmentHandlerTest`
checks duplicate IDs, same-name items, ID reuse and the unmodifiable inventory view.
`EquipmentAdditionPersistenceTest` checks rejected blank values against saved records and successful retry
after a blocked save. Rejection checks inspect existing items as well as the inventory size.

#### Filtering equipment without changing records

##### Parsing and matching

`EqmManager` parses and applies an `EquipmentFilter`, then asks the equipment handler to display the
matches. An empty result produces a no-matches message.
`EquipmentFields.parse(...)` accepts fields in any order while rejecting empty, repeated or unsupported
reserved fields. At least one field is required. Name fragments containing permitted `id/card`-like text
use the same field-boundary rules as equipment updates.

| Criterion | Matching rule |
| --- | --- |
| `id/` | Exact three-digit Item ID from `001` to `999` |
| `i/` | Case-insensitive substring of the equipment name |
| `type/` | Case-insensitive equality with the equipment's full type |
| `cond/` | One value or an inclusive ascending range within `1` to `10` |
| `status/` | `available` or `loaned`, ignoring case |

`filter type/camera cond/1-3 status/available` requires all three criteria to match. Omitted criteria impose
no restriction; this is AND matching, with no OR expression syntax. Name matching uses `Locale.ROOT`, so
results do not depend on the computer's default locale. Type text is compared directly with each item's
canonical type; an unmatched nonempty type produces no matches rather than a separate unsupported-type error.

A single condition sets both bounds to the same value. A range such as `1-3` includes both endpoints;
`3-1`, `01`, decimals and values outside the scale are rejected before inventory traversal. Omitting condition
uses the complete `1` to `10` range.

##### Availability and result preservation

For each item, `EquipmentFilter.apply(...)` asks `LoanHandler.hasActiveLoanForItem(...)` whether an unreturned
loan exists. Returned history does not make an item loaned. An item with no active loan is considered
available only if it is serviceable. The filter currently accepts no `status/unserviceable` query, although
unserviceable items remain visible when status is omitted.

`apply(...)` returns an unmodifiable list of original items in inventory order. Membership is a snapshot;
the referenced equipment can still change later. Filtering and display do not alter managed records,
save files or consume identifiers.

[![Equipment filtering: parse criteria, inspect current loans and display original matching items without saving](images/EquipmentFilterSequence.svg)](images/EquipmentFilterSequence.svg)

[PlantUML source](diagrams/EquipmentFilterSequence.puml)

The class lifeline receives the static `parse(...)` call; `filter:EquipmentFilter` is its returned query.
`apply(...)` checks each item's criteria using current active-loan information.

##### Design choices

AND matching makes combined queries predictable: adding a criterion can only keep or reduce the matches.
A separate query object allows parsing and matching to be tested independently. Scanning loans for each
item trades performance for simpler availability checks within the supported three-digit ID range.
An additional availability index would need updating on every loan, return and deletion.

##### Verification

`EquipmentFilterTest` covers each criterion, combinations, range boundaries, case matching, invalid input,
output order and read-only results. `EquipmentFilterStressTest` checks all 120 orders of five fields,
Unicode separators, large inputs, seeded malformed commands and generated queries across 999 items against
independently calculated expectations. `EquipmentFilterIntegrationTest` checks subtype details, original
object references, saved records, returned history and current availability together.

#### Updating equipment details

Updates correct an item's name or condition without deleting and recreating it. The Item ID and
equipment subtype stay fixed; omitted fields keep their current values. An optional `type/` must
match the existing type, ignoring case. A matching type alone is accepted, even when no value changes.

##### Parsing and validation

`EquipmentHandler.updateEquipment(...)` uses the same field parser as filtering. Fields may appear in any
order; empty, repeated or unsupported reserved fields are rejected. An ID from `001` to `999`, exactly one
matching item and at least one of `i/`, `type/` or `cond/` are required.

Omitted values come from the target. Common validation is shared with addition, including the name and
condition limits. A different type is rejected because changing subclasses would require conversion rules
for their attributes and damage. All values are checked before saving or assignment.

##### Save before changing the original object

For an existing camera `001`, `update id/001 i/Film Camera cond/5` follows this sequence:

[![Equipment update: validate and save the proposed inventory before changing the existing camera](images/equipment-update-sequence.svg)](images/equipment-update-sequence.svg)

[PlantUML source](diagrams/equipment-updates/equipment-update-sequence.puml)

1. Parse fields and find the original target.
2. Fill omitted values and validate the complete proposal.
3. Save a copied inventory containing a same-subtype proposal, preserving the Item ID, subtype details,
   damage, remarks, permanent record identity and serviceability.
4. Apply the name and condition to the original target with `updateDetails(...)`, then report success.

The sequence assumes configured storage; in-memory handlers skip the file write. The live target stays in
its original position, so loans and bundles keep valid references and see the corrected details.

##### Rejection and design choices

For `update id/001 i/Film Camera cond/11`, invalid condition prevents both requested changes.
A missing or ambiguous ID, invalid field or changed type is also rejected before saving.
The handler reports `EquipmentUpdateUnsuccessful`; common validation errors from
`EquipmentAdditionUnsuccessful` are wrapped with their original messages.

A save `IOException` is reported with `Could not save equipment update:`. Because `updateDetails(...)`
has not run, the original object and inventory order remain unchanged. After a handled rejection, the
command loop accepts another command. This ordering avoids having to undo partly assigned values.
Keeping omitted fields makes small corrections possible without resubmitting the whole item.

##### Verification

`EquipmentUpdateTest` checks partial and combined changes, boundaries, malformed commands, missing or
ambiguous IDs, original object identity and recovery after rejection. `EquipmentUpdatePersistenceTest`
checks restart, file preservation and retry after failed saves. `EquipmentUpdateIntegrationTest` covers
active loan references and subtype details. `EquipmentUpdateStressTest` checks all valid Item IDs,
Unicode input and repeated valid/rejected operations against expected state.

### Shared logging and equipment assertions

#### Configuration and cleanup

`Main` starts one `AppLogging` session before constructing `EqmManager`. Try-with-resources closes it
after `run()` finishes or throws. Feature classes obtain loggers by full class name, such as
`Logger.getLogger(EquipmentHandler.class.getName())`; their names inherit the shared `seedu.snap`
configuration.

The package logger and its `FileHandler` accept `FINER` and higher levels. `SimpleFormatter` writes
under the working directory's `logs/` folder, with two rotating files, normally `snap.log.0` and
`snap.log.1`, at approximately 1,000,000 bytes each. Append mode retains existing records across runs;
rotation replaces older records. Parent-handler propagation is disabled during the session to keep
diagnostics out of normal command output.

[![Logging lifecycle: configure shared output, run the application and close the session](images/LoggingLifecycle.svg)](images/LoggingLifecycle.svg)

[PlantUML source](diagrams/LoggingLifecycle.puml)

An `IOException` during directory or handler setup prints a warning and returns a session without a
file handler, allowing commands to continue. This handles logging I/O failures rather than all startup
exceptions. Closing a configured session removes and closes its handler, flushes output, releases its
lock and restores the package logger's previous level and parent-handler setting. The design assumes
one session at a time; feature loggers need no separate file or cleanup code.

#### Diagnostics and invariants

Equipment additions and updates log accepted/rejected outcomes at `FINE` and save failures at `WARNING`.
Other features may use `INFO` for state changes. Parameterized messages such as
`Added equipment with item ID {0}` insert values through the logging API.

Exceptions reject invalid user input whether assertions are enabled or disabled. Equipment construction
assertions check internal name, ID, condition and remarks; addition assertions check a non-null candidate,
unchanged size after rejection and exactly one appended item after success. Updates assert unchanged
size and Item ID. State changes happen outside assertions so disabling them cannot skip application work.

#### Verification and limits

`EquipmentHandlerTest` checks addition log records. `MainTest` runs separate JVMs with assertions enabled
and disabled, tests blocked log setup and checks equipment/loan diagnostics sharing one output without
duplicate command records. `AppLoggingTest` checks cleanup after an application exception, restored logger
settings, flushed output and lock release. The suite does not force rotation or explicitly test appending
to an existing log after restart; these are configured `FileHandler` behaviours.

### Overdue loan identification

#### Overview

The overdue-loan feature allows equipment managers to identify active loans whose due dates have passed. Users access the feature with the `list overdue` command. The command displays the item ID, borrower ID, and due date of every overdue loan. If there are no overdue loans, the application displays a clear message instead of an empty table.

A loan is overdue only when both conditions are satisfied:

1. The loan has not been returned.
2. Its due date is earlier than the current local date.

The comparison is date-based because loan commands accept dates in `DD-MM-YY` format without a specific time. A loan due today is therefore not overdue, even if its stored due time is earlier than the current time. It becomes overdue on the following day if it has not been returned.

Listing overdue loans is read-only. It does not add, remove, return, or otherwise modify any loan record.

#### Component responsibilities

`EqmManager` recognises the exact `list overdue` command. It obtains the current date once from its configured `Clock` and delegates the request to `LoanHandler`. Reading the date once ensures that every loan in the same command is evaluated consistently and allows tests to use a fixed date.

`LoanHandler.getOverdueLoans(referenceDate)` examines the stored loans in their existing order. It selects loans for which `Loan.isOverdue(referenceDate)` returns `true` and returns an unmodifiable result list. `LoanHandler.viewOverdueLoans(referenceDate)` formats the matching loans and displays a clear message when none are found.

`Loan.isOverdue(referenceDate)` owns the overdue-status rule because the loan contains the required due date and returned status. The condition is:

```text
not returned AND due date is before reference date
```

The reference date is supplied to the method instead of being obtained directly from the system clock. This keeps the model behaviour deterministic and independently testable.

#### Command execution

When the user enters `list overdue`, `EqmManager` obtains the current local date using its configured `Clock` and passes that date to `LoanHandler`.

`LoanHandler` evaluates every stored loan using `Loan.isOverdue(referenceDate)`. Returned loans are excluded even when their due dates are in the past. Active loans due today or in the future are also excluded. Matching loans retain their existing order and are displayed using the following columns:

```text
No.  ITEM ID      | BORROWER ID  | DUE DATE
```

If no loans match, the application displays:

```text
No overdue loans found.
```

The handler records the number of matching loans through diagnostic logging, including the valid zero-result case.

#### Defensive behaviour

The reference date must not be `null`. A null reference date represents a programming error because users do not provide it directly. `Loan.isOverdue(...)` and `LoanHandler.getOverdueLoans(...)` therefore reject it with a `NullPointerException`.

`LoanHandler` also asserts that its stored loan count matches the collection size and that the collection contains no null loan entries. These assertions detect corrupted internal state with clear development-time messages.

The returned overdue-loan list is unmodifiable. Callers therefore cannot use the result to alter or reorder the managed loan collection.

#### Design alternatives

One alternative was to store an `isOverdue` Boolean field in each `Loan`. This was rejected because overdue status changes as time passes. A stored value could become outdated unless every loan were updated regularly. Deriving the value from the due date and reference date keeps it current.

Another alternative was to place the complete overdue condition inside `LoanHandler`. This was rejected because it would separate loan-related behaviour from the object containing the relevant state. Keeping `isOverdue(...)` in `Loan` also allows the rule to be tested independently.

A third alternative was to compare complete `LocalDateTime` values. This was rejected because users provide due dates without specific times. Date-time comparison could mark a loan due today as overdue during that same day. Comparing only dates matches the user-facing command format.

#### Verification

`LoanTest` covers active loans with past, current, and future due dates; loans due at different times on the reference date; returned loans; and null reference dates.

`LoanHandlerTest` covers mixed loan collections, preserved ordering, unmodifiable results, null reference dates,
empty results, formatted output and unchanged managed records. The overdue-listing code logs the number of
matches, but these tests do not explicitly assert its diagnostic log records or inject null loan entries.

`EqmManagerTest` verifies command routing using a fixed `Clock`. It also confirms that `list overdue` does not modify equipment availability, loan records, or persisted files.

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
