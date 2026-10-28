---
layout: default
title: Yi An Li
---

# Yi An Li - Project Portfolio Page

## Overview

snAp is a command-line application for photography clubs to manage equipment, bundles, loans and persistent
records.

My work focused on the application flow, loan lifecycle, defensive validation, equipment subtypes and bundle
management.

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

### Documentation

- **About Us:** Updated team links and the project documentation landing links. ([PR #1](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/1))
- **User Guide:** Updated loan command documentation for stable loan IDs and unambiguous deletion. ([PR #76](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76))

### Resolved issues

- Closed [Issue #10](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/10) for adding loan records,
  [Issue #16](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/16) for the overall program flow,
  [Issue #19](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/19) for the redundant equipment insertion
  helper, [Issue #32](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/32) for misleading success output on
  invalid commands and [Issue #66](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/66) for equipment loan-state
  tracking.
- Implemented the issue-backed fixes for unique loan targeting and loan-addition invariants through [PR #76](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76)
  and [PR #28](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/28), respectively.

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
