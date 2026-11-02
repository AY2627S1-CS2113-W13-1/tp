---
layout: default
title: Yi An Li
---

# Yi An Li - Project Portfolio Page

## Overview

snAp is a Java 25 command-line application for photography clubs to manage equipment and borrowing records.
It tracks equipment details, availability, loans, returns, reusable bundles and maintenance records, with data
retained across sessions.

My main contributions are the application flow, loan creation and identifiers, equipment subclasses, bundle
creation and command consistency. I focused on clear command syntax, defensive validation and preserving
existing records when an operation fails.

## Summary of Contributions

**Code contributed:** [Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=yarnieee&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00)

### Enhancements implemented

- **Application flow and command routing:** Replaced the starter Duke flow with `Main`, `EqmManager` and
  `Parser` so commands are parsed and dispatched to the relevant handlers. Added integration coverage and
  corrected misleading success output for unrecognised commands.
  ([PR #13](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/13),
  [PR #36](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/36))
- **Loan creation and invariants:** Implemented loan creation with borrower, item and date validation,
  failure reporting and command integration. Added assertions, logging and tests for successful insertion
  and keeping the loan counter consistent with the backing list.
  ([PR #18](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/18),
  [PR #28](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/28))
- **Equipment subclasses:** Introduced `Camera`, `Lens`, `Battery` and `Tripod` subclasses and updated equipment
  creation to select the concrete type. Added tests for subclass construction and listing, providing the model
  used by later subtype-specific attributes and compatibility checks.
  ([PR #65](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/65))
- **Equipment bundles:** Implemented the bundle model, handler, creation, item validation, duplicate prevention,
  completion tracking and listing. Added command integration and tests for valid and rejected additions and
  bundle composition.
  ([PR #71](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/71))
- **Persistent loan identifiers:** Added unique loan IDs so repeated loans for the same equipment can be
  targeted unambiguously. Updated creation, deletion, storage, legacy-record loading and the persisted
  next-loan counter, with tests for ID preservation across restarts and failed saves.
  ([PR #76](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76))
- **Typed identifiers:** Added the shared `Identifier` utility to distinguish equipment (`E001`), loans (`L001`),
  bundles (`B001`) and borrowers (`U001`). Normalised lowercase input to canonical uppercase IDs and migrated
  legacy numeric saved records while rejecting collisions after normalisation. Updated validation, storage,
  command help, guides and tests across the affected features.
  ([PR #170](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/170), merged)
- **Consistent commands and aliases:** Standardised equipment, loan and bundle commands, including `eqm-add`,
  `loan-add` and `bundle-create`, with consistent field prefixes. Centralised short aliases in `CommandAliases`
  so routing and help use the same mapping; for example, `ae` invokes `eqm-add`. Updated documentation,
  examples and regression tests alongside the command handlers.
  ([PR #171](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/171), merged)

### Testing and quality

- Added focused unit and integration tests for loan creation, invalid commands, equipment subclasses, bundle
  validation, storage and loan-ID persistence, including failed saves and restart behaviour.
- Added regression coverage for typed-ID validation, case normalisation, legacy migration and collisions, and
  for canonical command names, aliases and command-help consistency in PRs #170 and #171.
- Used assertions and diagnostic logging to detect inconsistent loan counts, invalid internal state and failed
  operations while retaining the intended command-line behaviour.
- Prepared a broader JaCoCo coverage audit and regression tests for malformed storage, equipment ownership,
  backup/restore and invalid input. The standalone
  [PR #169](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/169) was closed without merging.
  ([Issue #167](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/167))

### Documentation

- **User Guide and command help:** Documented persistent loan IDs and unambiguous deletion, then updated syntax,
  examples and identifier rules for typed IDs and standardised commands. Clarified equipment-addition and update
  success output, persistence-file creation and transferring bundle data with equipment and loan files.
  ([PR #76](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76),
  [PR #151](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/151),
  [PR #170](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/170),
  [PR #171](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/171))
- **Bundle compatibility design:** Added a Developer Guide explanation, sequence diagram, rendered SVG and
  diagram-index entry. Documented per-item validation, rejection of known incompatibilities, permitted unknown
  comparisons, partial success and saving bundle membership on exit.
  ([PR #181](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/181), merged)
- **Loan update and reassignment design:** Added a Developer Guide subsection, sequence diagram and rendered SVG
  covering ownership validation, metadata-only updates and equipment reassignment. Explained preserving or
  recapturing issue-condition snapshots, saving proposals before changing live objects and retaining original
  state after failed saves. These UML contributions document the existing bundle-compatibility and loan-update
  implementations.
  ([PR #182](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/182), merged)
- **Team documentation:** Updated the About Us page and team documentation links.
  ([PR #1](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/1))

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
- **Resolved feature and documentation tasks:** Addressed loan creation, application routing, loan invariants,
  invalid-command output and unique loan targeting through the enhancement PRs above. Completed command-format
  standardisation and typed identifiers for
  [#149](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/149) and
  [#150](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/150), and the bundle compatibility and loan update UML
  tasks for [#176](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/176) and
  [#177](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/177).
