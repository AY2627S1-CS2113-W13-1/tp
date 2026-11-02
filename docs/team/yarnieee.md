---
layout: default
title: Yi An Li
---

# Yi An Li - Project Portfolio Page

## Overview

snAp is a command-line application for photography clubs to manage equipment, bundles, loans and persistent
records.

My work focused on the application flow, loan lifecycle, defensive validation, equipment subtypes and bundle
management. Recent contributions also cover command consistency, typed identifiers, regression testing and
UML documentation.

### Summary of Contributions

**Code contributed:** [Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=yarnieee&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00)

### v1.0

- **Program flow and command routing:** Replaced the starter Duke flow with `Main`, `EqmManager` and `Parser` so
  user commands are parsed and dispatched to the application manager. Added integration coverage and resolved
  the overall program-flow issue. ([PR #13](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/13),
  [Issue #16](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/16))
- **Loan creation:** Implemented loan records with item, borrower and date validation, loan-state handling,
  failure reporting and command integration. ([PR #18](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/18),
  [Issue #10](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/10))
- **Loan invariants:** Added assertions, diagnostic logging and focused tests to ensure created loans are stored
  successfully and the loan counter stays synchronized with the backing list. ([PR #28](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/28),
  [Issue #29](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/29))
- **Unrecognized command handling:** Corrected the success output for invalid commands and added regression
  tests. ([PR #36](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/36),
  [Issue #32](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/32))

### v2.0

- **Team documentation:** Updated the About Us page and documentation links for the team site. ([PR #1](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/1))
- **Equipment subtype model:** Added `Camera`, `Lens`, `Battery` and `Tripod` subclasses, extended equipment
  handling to recognise the concrete types and added tests for subclass creation and listing. ([PR #65](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/65),
  [Issues #45](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/45),
  [#49](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/49))
- **Equipment bundles:** Implemented bundle creation, item validation, duplicate prevention, completion tracking
  and bundle listing. Added bundle model, handler, command integration and focused tests. ([PR #71](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/71),
  [Issue #51](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/51))
- **Stable loan identifiers:** Added persistent loan IDs and updated loan creation, deletion, storage, reload,
  documentation and tests so repeated loans for one item can be distinguished safely. ([PR #76](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76),
  [Issue #25](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/25))

### Testing and quality

- Added focused unit and integration tests for loan creation, invalid commands, equipment subclasses, bundle
  operations, storage and loan-ID persistence.
- Used assertions and warning-level diagnostics to detect inconsistent loan state and failed operations without
  changing the intended command-line behaviour.
- **Coverage audit:** Added JaCoCo reporting and regression tests across bundles, backup/restore, equipment,
  loans, maintenance and command handling. Cases include malformed storage, legacy loan identifiers, equipment
  ownership and invalid input. [PR #169](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/169)
  was closed without merging; this work is not recorded here as a released enhancement.
  ([Issue #167](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/167))

### Recent enhancements under review

The following PR statuses reflect the repository at the time of this update.

- **Command consistency (open):** Standardised equipment, loan and bundle commands around entity-prefixed
  names, with short aliases maintained centrally in `CommandAliases`. Updated routing, field prefixes, help,
  guides and tests together so the documented syntax follows the command handlers.
  ([PR #171](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/171),
  [Issue #149](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/149))
- **Typed identifiers (draft):** Added distinct equipment, loan, borrower and bundle ID prefixes, canonical
  uppercase normalisation and support for loading legacy numeric identifiers. Updated persistence, validation,
  help, guides and regression tests, including rejection of identifiers that collide after normalisation.
  ([PR #170](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/170),
  [Issue #150](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/150))

### Documentation

- **About Us:** Updated team links and the project documentation landing links. ([PR #1](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/1))
- **User Guide:** Updated loan command documentation for stable loan IDs and unambiguous deletion. ([PR #76](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76))
- **User Guide clarifications (merged):** Documented successful equipment addition and update output,
  persistence-file creation and transferring bundle data together with equipment and loan files.
  ([PR #151](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/151))
- **Bundle compatibility UML (merged):** Added a sequence diagram, rendered SVG, Developer Guide explanation
  and diagram-index entry. Explained per-item validation, compatibility rejection, permitted unknown results,
  partial success and saving bundle membership on exit.
  ([PR #181](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/181),
  [Issue #176](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/176))
- **Loan update UML (merged):** Added a dedicated Developer Guide subsection, sequence diagram and SVG for
  metadata updates and equipment reassignment. Documented ownership validation, preserving or recapturing
  issue-condition snapshots, saving before live mutation and retaining original state after save failures.
  ([PR #182](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/182),
  [Issue #177](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/177))

### Resolved issues

- Closed [Issue #10](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/10) for adding loan records,
  [Issue #16](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/16) for the overall program flow,
  [Issue #19](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/19) for the redundant equipment insertion
  helper, [Issue #32](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/32) for misleading success output on
  invalid commands and [Issue #66](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/66) for equipment loan-state
  tracking.
- Implemented the issue-backed fixes for unique loan targeting and loan-addition invariants through [PR #76](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76)
  and [PR #28](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/28), respectively.
- Resolved [Issue #176](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/176) with the bundle compatibility
  sequence diagram in merged [PR #181](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/181), and
  [Issue #177](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/177) with the loan update and equipment
  reassignment documentation and UML diagram in merged
  [PR #182](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/182).

### Team contributions and reviews

- **Code reviews:** Reviewed teammate work in [PR #12](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/12),
  [#14](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/14),
  [#15](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/15),
  [#17](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/17),
  [#22](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/22),
  [#23](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/23) and
  [#69](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/69). Feedback covered integration with the current
  codebase, Javadocs, input validation, deletion semantics, code quality and persistence; changes were requested
  where necessary and approvals were given after follow-up updates.
- Also left implementation review comments on [PR #65](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/65)
  to record the subtype damage-enum and issue-coverage details.
- **Documentation review:** Reviewed and approved the command-help and documentation audit in
  [PR #144](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/144).
- **Issue planning:** Defined the scope and acceptance criteria for the coverage audit, command-format
  standardisation and typed identifiers in
  [#167](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/167),
  [#149](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/149) and
  [#150](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/150). Also opened UML documentation tasks for
  backup restore, bundle compatibility and loan reassignment in
  [#175](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/175),
  [#176](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/176) and
  [#177](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/177).
