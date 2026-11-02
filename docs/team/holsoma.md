---
layout: default
title: Amos Loh
---

# Amos Loh - Project Portfolio Page

## Overview

snAp is a Java 25 command-line application for photography clubs to manage equipment, loans, returns, bundles
and maintenance records. My work focused on equipment returns and history, readable command output,
documentation, and preserving saved records when commands or storage operations fail.

## Summary of Contributions

**Code contributed:** [Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=holsoma&sort=groupTitle&sortWithin=title&groupSelect=groupByRepos&breakdown=true)

The contributions below were checked against the repository's pull requests and commit history on
3 November 2026. Shared work is identified separately from features I implemented.

### Enhancements implemented

- **Equipment creation and deletion:** Contributed the initial prototype with validation, text-file storage,
  error handling and tests. Later strengthened deletion with checks for invalid, missing and ambiguous IDs,
  repeated deletion, ID reuse and recovery after errors, plus assertions and diagnostic logging.
  ([#17](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/17),
  [#42](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/42))
- **Equipment attributes and damage:** Extended Yi An's equipment subclasses with type-specific attributes and
  damage enums. Integrated validation, saving and reloading, and regression tests while preserving existing
  duplicate-ID and active-loan protections. ([#86](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/86))
- **Equipment availability integration:** Integrated the team's availability changes with subclasses and saved
  loans. Preserved availability across restarts, saved loan changes before releasing equipment, and prevented
  returned or stale loans from changing newer loans. Added failure and restart tests. The code from
  [#87](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/87) was integrated through
  [commit f454107](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/f4541074947055d0ab673625764ed08e27fe34af).
- **Assessed equipment returns:** Implemented returns that retain loan history and record serviceability,
  damage and remarks. Saved proposed equipment and loan records before changing live objects, with rollback
  after reported save failures. Prevented unserviceable equipment from being loaned or added to bundles.
  ([#92](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/92))
- **Separate item histories:** Implemented item-specific loan history using permanent equipment identities,
  so reusing an Item ID does not transfer the previous item's history to its replacement. Added migration of
  older records, restart checks and failed-migration tests; unattributable older returns remain outside
  item-specific histories. ([#93](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/93))
- **Optional fields and defaults:** Defaulted omitted loan issue dates to the local date and omitted return
  remarks to empty text. Kept supplied values and saved records strictly validated. Tested time zones,
  midnight boundaries, equipment defaults, failed saves and retries.
  ([#94](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/94))
- **Command help and startup display:** Implemented command formats, examples, defaults and restrictions,
  then added a compact startup menu, command-specific help and `help all`. Shared command descriptions across
  views, wrapped long text, and added an ASCII welcome logo with optional colour and Unicode output.
  ([#95](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/95),
  [#114](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/114))
- **Compact equipment lists and full details:** Added shared 96-column inventory and filter tables, result
  counts and a full equipment-detail view. Kept complete values available when list names are shortened,
  handled common Unicode characters and escaped control characters and table separators. Tested long text,
  invalid queries, restarts and unchanged records after display commands.
  ([#137](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/137))
- **Loan and bundle output:** Standardised loan columns, dates, states and counts across lists and histories.
  Displayed missing bundle equipment types, per-item batch results and success/failure totals. Included
  affected IDs, names and resulting availability in command responses, with readable error and success labels.
  ([#140](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/140))

### Reliability and testing

- **Maintenance recovery safeguard:** Fixed a recovery path that could overwrite damaged maintenance data
  with an empty collection. Retained the maintenance loading error and rejected backup restoration before
  changing live files. Added regression tests for malformed and unreadable data, repair and restart, and
  recovery from other supported loading errors.
  ([#158](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/158))
- **End-of-input handling:** Replaced an uncaught exception with a final save attempt when console input ends.
  Preserved startup-error protections, reported failed saves without another prompt, and retained retries
  while input remains available. Tested empty input, a final command without a newline, bundle persistence,
  save failures and damaged startup data.
  ([#162](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/162))
- **Help audit:** Checked command routing, validation, help and both guides against the merged implementation.
  Added tests executing all 29 help examples present at that audit, and checked that repeated help requests
  preserve records, identifiers, saved bytes and modification times. Recorded the audit and remaining release
  checks in [the help audit](../help-audit.md).
  ([#144](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/144))
- Ran Java 25 Gradle tests and Checkstyle during feature integration, with packaged-JAR checks for terminal
  output, recovery, persistence and release instructions. Added targeted regression tests for invalid commands,
  storage failures and restart behaviour.

### Documentation

- **User Guide:** Documented equipment deletion, returns, item history, defaults, help, display changes and
  recovery behaviour alongside the corresponding features, including command examples and error conditions.
- **Developer Guide:** Explained responsibilities, execution flows, alternatives and testing for returns,
  history, defaults and help. Added five sequence diagrams and one identity class diagram, their editable
  PlantUML sources and regeneration instructions
  ([commit 18439c4](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/18439c4ba88171888eac75d5c96c499d65b8a8e3)).
  Added numbered figure captions and separate source links in
  [#144](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/144).
- **Help sequence correction:** Corrected the output-stream instance, visible-output note and method-return
  arrows in [#165](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/165). That PR was closed after the changes
  were incorporated with attribution in [#168](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/168).
- **Project website:** Created and refined the documentation landing page, then shared its layout across the
  guides and team pages. Added section navigation, keyboard access, narrow-screen contents controls and
  full-size diagram links. Checked desktop and mobile layouts, including navigation without JavaScript.
  ([#42](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/42),
  [#43](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/43),
  [#113](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/113))
- **Manual testing and release instructions:** Added reproducible equipment-detail and bundle-compatibility
  test sequences with expected results and restart checks. Updated the README and guides for the v2.1 JAR,
  Java 25, writable working folders and saved-data locations, and verified the documented launch sequence.
  ([#163](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/163),
  [#161](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/161))
- **Team pages:** Updated member information, images and portfolio templates, then aligned portfolio filenames
  with GitHub usernames and repaired their links.
  ([#4](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/4),
  [#17](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/17),
  [commit e2fe966](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/e2fe96665c1fd76a63ec2e8085a6f1f88ce40f3f))

### Team contributions and reviews

- Integrated teammates' bundles, remarks and persistent loan IDs while retaining their original commits.
  Preserved shared equipment references, availability rules, validation, older saved records and save-failure
  safeguards. Also resolved conflicts in equipment filtering, updates and later help/output changes.
  ([#89](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/89),
  [#90](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/90),
  [#91](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/91),
  [commit 03a9ed2](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/03a9ed2a37c2f6f22afcd44a3f00f7a331265169),
  [commit 175acae](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/175acae7c74c7590b5210810de68c1a5c457f36f))
- Reviewed subclass requirements and tests, equipment-update preservation and returned-loan safeguards.
  ([#65 review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/65#discussion_r4334065113),
  [#59 review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/59#discussion_r4360210435),
  [#73 review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/73#discussion_r4335065505))
- Identified remarks parsing that prevented existing equipment records from loading, and inconsistent
  borrower-ID validation that could terminate the application with assertions enabled.
  ([#74 review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/74#discussion_r4360852969),
  [#76 review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76#discussion_r4361337732))
- Reproduced a bundle-removal save-order bug that left saved bundles referring to deleted equipment and
  blocked the next startup. Requested saving membership removal before permitting deletion, with a restart
  regression test. ([#115 review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/115#discussion_r4379633281))
- Reproduced loan updates that lost saved issue-condition snapshots, and reassignment that attributed the
  original item's condition to its replacement. Provided concrete reproduction steps and requested tests
  covering the saved data after restarting.
  ([#116 snapshot review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/116#discussion_r4386666562),
  [#116 reassignment review](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/116#discussion_r4388113525))

### Superseded work

- Submitted validation and regression tests for malformed list commands in
  [#164](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/164). It was closed without merging after the team's
  command standardisation replaced the ambiguous list routes. This is recorded as submitted work, not a
  delivered feature.
