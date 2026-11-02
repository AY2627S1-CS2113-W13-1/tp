---
layout: default
title: Kaushal Rajesh - Project Portfolio
---

# Kaushal Rajesh - Project Portfolio Page

## Overview

snAp is a Java 25 command-line application for photography clubs to manage equipment, loans, returns and reusable
bundles across sessions. I contributed equipment management, immutable loan-condition records and shared logging,
with an emphasis on validation, safe persistence and referential integrity.

## Summary of Contributions

**Code contributed:** [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=rktheg&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other)

### Enhancements implemented

- **Equipment addition and validation:** Implemented field validation for duplicate Item IDs, malformed prefixes,
  conditions and blank values while preserving names such as `USB e/card reader`; added boundary and recovery tests.
  ([#14](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/14),
  [#39](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/39))
- **Equipment availability tracking:** Connected loans to physical equipment, preventing double borrowing and deletion
  while loaned; ownership checks stop old loans from releasing items reserved by newer loans.
  ([integration #87](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/87),
  [implementation](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/79b3d62))
- **Multi-criteria equipment filtering:** Implemented non-mutating combined filters for Item ID, partial name, type,
  condition range and availability; stress-tested all 120 field orders and inventories of 999 items.
  ([#58](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/58))
- **Safe equipment updates:** Added validated in-place updates that preserve subtype details and loan/bundle associations.
  Proposed state is saved before mutation, so failed saves leave live state unchanged.
  ([#59](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/59))
- **Shared logging and assertions:** Implemented rotating file logging, safe error recovery, equipment invariants and
  tests across assertion modes.
  ([#30](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/30))
- **Immutable loan return assessments:** Stored independent issue and return snapshots with each loan, including
  serviceability, with backward-compatible codecs and tests for ID reuse and failed saves.
  ([#166](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/166))

### Contributions to the User Guide

- Documented equipment addition, availability, [filtering](../UserGuide.md#filtering-equipment-eqm-filter) and
  [updates](../UserGuide.md#updating-equipment-eqm-update), including executable examples and constraints.
- Added guidance for [loan returns](../UserGuide.md#returning-equipment-loan-return) and
  [condition comparison](../UserGuide.md#comparing-a-loans-condition-assessments-loan-view).
- Contributed error-recovery guidance, worked examples and data-transfer instructions.
  ([#131](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/131),
  [#161](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/161))

### Contributions to the Developer Guide

- Documented [Equipment Management](../DeveloperGuide.md#equipment-management),
  [per-loan condition preservation](../DeveloperGuide.md#preserving-condition-assessments-for-each-loan) and shared
  logging, including validation, persistence rationale and verification.
- **UML diagrams:** Contributed the architecture and shared class diagrams; equipment addition, filtering, availability,
  update and return sequences; and snapshot object diagram. Established shared PlantUML styling and notation conventions.
  ([#80](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/80),
  [#82](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/82),
  [#88](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/88),
  [#152](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/152),
  [#166](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/166),
  [#168](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/168))
- Maintained core DG sections: scope, user stories, NFRs, glossary and manual testing paths.

### Contributions to team-based tasks

- **Project management:** Created milestone user stories and tracking issues
  ([#38](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/38),
  [#44](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/44)–[#56](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/56));
  and coordinated team deliverables.
- **Build and packaging:** Configured Gradle build packaging for `snAp.jar` and eliminated dead code.
  ([#23](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/23),
  [#35](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/35))

### Review/mentoring contributions

- [#170](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/170): Found a fatal storage crash, parser flaw,
  Checkstyle violations and broken assertion tests, and supplied concrete fixes.
- [#171](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/171): Prevented a rebase regression across four merged features,
  reported 19 failing tests and corrected command naming inconsistencies.
- Reviewed validation, ID-reuse tests and UML accuracy in [#127](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/127),
  [#172](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/172) and
  [#174](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/174), plus availability and null-safety changes across team PRs.

### Contributions beyond the project team

- Reviewed Developer Guide architecture diagrams and component abstractions for team W10-3 (NUS BudgetBites)
  ([#10](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/10)).
