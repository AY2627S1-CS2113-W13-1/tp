---
layout: default
title: Amos Loh
---

# Amos Loh - Project Portfolio Page

## Overview

snAp is a command-line application for photography clubs to manage equipment and borrowing records. It tracks
equipment details, availability, loans and returns.

## Summary of Contributions

**Code contributed:** [Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=holsoma&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00&filteredFileName&tabOpen=true&tabType=zoom&zA=holsoma&zR=AY2627S1-CS2113-W13-1%2Ftp%5Bmaster%5D&zACS=318&zS=2026-09-18T00%3A00%3A00&zFS=holsoma&zU=2026-10-25T23%3A59%3A59&zMG=false&zFTF=commit&zFGS=groupByRepos&zFR=false)

### v1.0

- **Equipment management:** Contributed the initial equipment creation and deletion prototype, including input
  validation, error handling and tests. Worked with the team's evolving implementation through
  [PR #17](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/17).
- **Team documentation:** Updated the About Us page, member information, documentation images and individual
  portfolio templates.

### v2.0

- **Equipment deletion safeguards:** Added checks and tests for invalid, missing and ambiguous IDs, repeated
  deletion, ID reuse and recovery after rejected commands. Added assertions and diagnostic logging.
  ([#42](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/42))
- **Equipment attributes and damage:** Extended the equipment subclasses with type-specific attributes and
  damage values while integrating the team's subclass implementation. Added validation, persistence and
  regression tests. ([#86](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/86))
- **Assessed equipment returns:** Implemented returns that retain loan history and record serviceability,
  damage and remarks. Proposed records are saved before live objects change; tests cover save failures and
  restrictions on unserviceable equipment. ([#92](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/92))
- **Separate item histories:** Implemented item-specific loan history using permanent equipment identities.
  This prevents a replacement item from inheriting another item's history when an Item ID is reused, including
  after restarting. ([#93](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/93))
- **Optional fields and defaults:** Added optional loan issue dates and return remarks while retaining strict
  validation of supplied values and saved records. Tests cover local dates, midnight boundaries, defaults and
  failed saves. ([#94](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/94))
- **Command help:** Implemented help covering all supported commands, examples, optional fields, defaults and
  restrictions. Tests verify that help leaves records, identifiers and saved files unchanged.
  ([#95](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/95))

### Documentation

- **User Guide:** Documented equipment deletion, returns, item history, optional fields, defaults and help,
  including examples and error behaviour.
- **Developer Guide:** Explained responsibilities, execution flows, design decisions, alternatives and testing
  for returns, history, optional fields and help. Added five sequence diagrams and one class diagram, with
  editable PlantUML sources.
- **Project website:** Created and refined the documentation landing page, including responsive navigation,
  keyboard access and links to the guides.
  ([#42](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/42),
  [#43](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/43))

### Team contributions and reviews

- Resolved integration conflicts while preserving teammates' contributions, equipment references, availability
  rules and save-failure safeguards. Examples include bundles, remarks and persistent loan IDs.
  ([#89](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/89),
  [#90](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/90),
  [#91](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/91))
- Identified a remarks-parsing compatibility bug that could prevent existing equipment records from loading.
  ([Review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/74#discussion_r4360852969))
- Identified inconsistent borrower-ID validation that could terminate the application when assertions were
  enabled. ([Review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76#discussion_r4361337732))
