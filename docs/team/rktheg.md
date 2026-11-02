---
layout: default
title: Kaushal Rajesh - Project Portfolio
---

# Kaushal Rajesh - Project Portfolio Page

## Overview

snAp is a Java 25 command-line application for photography clubs to manage equipment and borrowing records.
It tracks equipment details, availability, loans, returns and reusable bundles, with records retained across sessions.

My main contributions are equipment addition and validation, availability tracking, filtering, equipment updates,
immutable loan return condition assessments and shared diagnostic logging. I focused on defensive validation,
safe persistent-state updates and maintaining referential integrity across borrowing and equipment lifecycles.

## Summary of Contributions

**Code contributed:** [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=rktheg&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other)

### Enhancements implemented

- **Equipment addition and validation:** Implemented equipment addition with strict field validation, rejecting duplicate
  Item IDs, missing/repeated prefixes, invalid conditions and blank inputs. Preserved legitimate names containing reserved
  tokens (e.g. `USB id/card reader`). Added regression tests for boundaries, inventory preservation and rejection recovery.
  ([#14](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/14),
  [#39](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/39))
- **Equipment availability tracking:** Connected loan lifecycles to physical equipment, preventing double borrowing and
  blocking deletion of loaned items. Added ownership checks ensuring old loans cannot release items reserved by newer loans.
  ([#57](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/57),
  [commit](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/f4541074947055d0ab673625764ed08e27fe34af))
- **Multi-criteria equipment filtering:** Implemented combined filters across Item ID, partial name, type, condition range
  (inclusive) and availability. Preserves inventory order and returns original references without mutating state.
  Stress-tested across all 120 field orders and queries on 999 items.
  ([#58](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/58))
- **Safe equipment updates:** Enabled field updates without deleting and recreating equipment. Validates all inputs and
  persists the proposed state to disk before mutating live memory, preserving subtype details, availability and loan/bundle
  associations with rollback on failure. ([#59](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/59))
- **Shared logging and assertions:** Implemented `AppLogging` with rotating file handlers and safe error recovery. Added
  core equipment invariants and tests verifying diagnostics across assertion modes.
  ([#30](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/30))
- **Immutable loan return assessments:** Extended loans with independent issue and return condition snapshots (including
  serviceability) stored with the loan record, decoupled from mutable equipment. Added backward-compatible codecs,
  legacy-record tolerance, and regression tests for Item ID reuse and failed-save rollbacks.
  ([#166](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/166))

### Contributions to the User Guide

- Documented commands, options, examples and constraints for equipment addition, availability,
  [filtering](../UserGuide.md#filtering-equipment-eqm-filter) and [updates](../UserGuide.md#updating-equipment-eqm-update).
- Added guidance for [loan returns](../UserGuide.md#returning-equipment-loan-return) and
  [condition comparison](../UserGuide.md#comparing-a-loans-condition-assessments-loan-view).
- Contributed error-recovery guidance, worked examples and data-transfer instructions.
  ([#131](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/131),
  [#161](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/161))

### Contributions to the Developer Guide

- Documented [Equipment Management](../DeveloperGuide.md#equipment-management),
  [per-loan condition preservation](../DeveloperGuide.md#preserving-condition-assessments-for-each-loan), and shared logging.
  Explained defensive validation, safe persistence patterns, design alternatives and verification paths.
- **UML Diagrams contributed/updated:** Architecture diagram, shared Class diagram, Equipment Addition, Filtering,
  Availability, Update sequences, Return condition sequence, and Snapshot object diagram. Established shared PlantUML styling
  (`style.puml`) and UML notation conventions.
  ([#80](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/80),
  [#82](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/82),
  [#88](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/88),
  [#152](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/152),
  [#166](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/166),
  [#168](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/168))
- Maintained core DG sections: scope, user stories, NFRs, glossary and manual testing paths.

### Contributions to team-based tasks

- **Project management & milestones:** Created and structured milestone user stories and tracking issues
  ([#38](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/38),
  [#44](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/44)–[#56](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/56));
  monitored deadlines and coordinated team deliverables.
- **Build and packaging:** Configured Gradle build packaging for `snAp.jar` and eliminated dead code.
  ([#23](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/23),
  [#35](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/35))

### Review/mentoring contributions

- **PRs reviewed with deep technical feedback and bug catches:**
  - [#170](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/170): Caught fatal `IllegalStateException` crash in `EquipmentStorage.java:96` and supplied exact fix; detected parser delimiter logic flaws in `isNameIdText`; flagged Checkstyle naming violations and broken assertion tests.
  - [#171](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/171): Prevented branch rebase regression that would revert PRs #158, #162, #166 and #172; flagged 19 failing tests and resolved command naming inconsistencies.
  - [#127](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/127): Advised on separating validation vs I/O exceptions, physical ID reuse tests and documentation.
  - [#172](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/172) & [#174](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/174): Reviewed UML sequence/class diagrams, corrected DG text, and resolved merge conflicts.
  - Reviewed availability invariants, null safety and test coverage on [#18](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/18), [#31](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/31), [#41](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/41), [#73](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/73), [#119](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/119), [#158](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/158).

### Contributions beyond the project team

- Reviewed Developer Guide architecture diagrams and component abstractions for team W10-3 (NUS BudgetBites)
  ([#10](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/10)).
