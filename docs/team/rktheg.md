---
layout: default
title: Kaushal Rajesh - Project Portfolio
---

# Kaushal Rajesh - Project Portfolio Page

## Overview

snAp is a Java 25 command-line application for photography clubs to manage equipment and borrowing records.
It tracks equipment details, availability, loans, returns and reusable bundles, with records retained across sessions.

My main contributions are equipment addition and validation, availability tracking, filtering, equipment updates
and shared diagnostic logging. I focused on rejecting invalid commands without changing existing records and
keeping equipment references consistent when an item is borrowed or updated.

## Summary of Contributions

**Code contributed:** [tP Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=rktheg&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other)

### Enhancements implemented

- **Equipment addition and validation:** Implemented adding equipment, then strengthened parsing and rejected
  duplicate Item IDs, missing or repeated prefixes, invalid conditions and Unicode-only blank input. Preserved
  legitimate names such as `USB id/card reader`. Added regression tests for boundaries, inventory preservation,
  ID reuse and recovery after rejection.
  ([#14](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/14),
  [#39](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/39))
- **Equipment availability:** Connected loans to existing equipment, prevented double borrowing and blocked
  deletion of borrowed items. Added ownership checks so an old loan cannot release equipment reserved by a
  newer loan. The original work is in [#57](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/57);
  it reached master through the later
  [equipment integration](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/f4541074947055d0ab673625764ed08e27fe34af).
- **Equipment filtering:** Implemented combined filters for Item ID, partial name, type, condition and
  availability. Condition ranges include both endpoints. Matching preserves inventory order and returns
  original equipment references without changing records. Stress tests cover all 120 field orders and
  generated queries across 999 items. ([#58](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/58))
- **Equipment updates:** Allowed corrections without deleting and recreating equipment. The Item ID stays
  fixed, all values are validated and the proposed inventory is saved before the original object changes. Updates preserve its
  subtype, details, availability and loan/bundle references. Tests cover failed saves, restarts, rejected type
  changes and repeated operations. ([#59](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/59))
- **Shared logging and assertions:** Implemented `AppLogging` so feature loggers share rotating files and
  cleanup. Handled logging setup I/O failures with a warning so commands can continue. Added equipment
  invariants and tests for shared diagnostics, cleanup and validation with assertions enabled and disabled.
  ([#30](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/30))

### Documentation

- **User Guide:** Contributed the equipment-addition restrictions and examples, equipment availability,
  [filtering](../UserGuide.md#filtering-equipment-filter) and
  [updates](../UserGuide.md#updating-equipment-update), including condition ranges, rejected commands and
  preservation of existing records. Evidence is in the feature PRs above.
- **Developer Guide:** Documented [equipment management](../DeveloperGuide.md#equipment-management), covering
  the shared inventory, addition, availability, filtering and updates, plus logging and assertions. Explained
  validation, save failures, design choices and testing. Added the application architecture, shared class
  diagram, command sequences, availability states and logging lifecycle, with editable PlantUML sources
  and SVG images. Shared the vibrant theme with explicit UML visibility symbols.
  ([#80](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/80),
  [#88](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/88),
  [#82](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/82), all merged)
- **Guide review fixes:** Added a worked example and recovery guidance to the UG, and updated diagrams to
  current code. Completed the DG acknowledgements, scope, user stories, glossary and a verified manual test
  path, and refined seven NFRs.
  ([#131](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/131), awaiting review)

### Team tasks and reviews

- **Issue tracking:** Created and maintained the
  [addition-validation](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/38),
  [filtering](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/47) and
  [equipment-update](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/48) issues, including scope,
  assignees, priorities and labels.
- **Deadline coordination:** Kept the team on track with project deadlines by reminding members about
  upcoming deliverables and following up on progress.
- Renamed the packaged application JAR to `snAp.jar` and removed a redundant equipment insertion helper.
  ([#35](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/35),
  [#23](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/23))
- Reviewed loan addition and suggested invalid-date and ID tests
  ([review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/18#pullrequestreview-5477271046)).
  Reviewed listing assertions/logging and suggested logging tests
  ([review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/41#pullrequestreview-5564131119)).
