---
layout: default
title: Amos Loh
---

# Amos Loh - Project Portfolio Page

## Overview

snAp is a Java 25 command-line application for photography clubs to manage equipment, loans, returns,
bundles and maintenance. My work focused on returns, item history, command output and reliable storage.

## Summary of Contributions

**Code contributed:** [Individual Code Dashboard](https://NUS-CS2113-AY2627-S1.github.io/tp-dashboard/?search=holsoma&breakdown=true).
**Evidence:** [All authored PRs](https://github.com/AY2627S1-CS2113-W13-1/tp/pulls?q=is%3Apr+author%3Aholsoma).

### Enhancements implemented

- **Equipment:** Built the initial creation and deletion prototype, then strengthened deletion validation, tests,
  assertions and logging. Extended Yi An's subclasses with type-specific attributes and damage enums.
  ([#17](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/17),
  [#42](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/42),
  [#86](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/86))
- **Returns and history:** Implemented assessed returns with recovery after failed saves, permanent item
  identities preventing history transfer when IDs are reused, and optional issue dates and return remarks.
  Tested migration, restarts, midnight boundaries and failed writes.
  ([#92](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/92),
  [#93](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/93),
  [#94](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/94))
- **Help and output:** Added command-specific help, startup menus, compact equipment tables, full details and
  consistent loan and bundle results. Tested Unicode, long text, 29 help examples and unchanged saved records.
  ([#95](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/95),
  [#114](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/114),
  [#137](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/137),
  [#140](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/140),
  [#144](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/144))
- **Recovery:** Prevented backup restoration from overwriting damaged maintenance data and saved pending
  records when console input ends. Added failure and restart tests and packaged-JAR checks using Java 25.
  ([#158](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/158),
  [#162](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/162))

### Documentation

- **User Guide:** Wrote [deletion](../UserGuide.md#deleting-equipment-eqm-delete),
  [returns](../UserGuide.md#returning-equipment-loan-return),
  [item history](../UserGuide.md#viewing-an-items-loan-history-eqm-loan-history),
  [defaults](../UserGuide.md#optional-fields-and-defaults), [help](../UserGuide.md#getting-help-help) and
  [equipment details](../UserGuide.md#viewing-full-equipment-details-eqm-show), including examples and errors.
  Added [JAR setup](../UserGuide.md#quick-start).
  ([#161](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/161))
- **Developer Guide:** Explained returns, history, defaults and help, including failure paths and design
  alternatives. Added [manual tests](../DeveloperGuide.md#full-equipment-details-in-a-separate-folder)
  ([#163](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/163)) and six diagrams with editable sources:
  [return sequence](../DeveloperGuide.md#successful-return),
  [save-failure sequence](../DeveloperGuide.md#save-failures-and-recovery),
  [equipment identity classes](../DeveloperGuide.md#permanent-identity),
  [history query](../DeveloperGuide.md#query-sequence),
  [issue-date default](../DeveloperGuide.md#default-issue-date-sequence) and
  [help sequence](../DeveloperGuide.md#implementation-and-sequence).
  My help-notation correction in [#165](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/165) was incorporated
  through [#168](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/168).
- **Website and team pages:** Created the landing page and shared documentation layout, with responsive
  navigation and keyboard access. Updated member information and portfolio links.
  ([#4](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/4),
  [#42](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/42),
  [#43](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/43),
  [#113](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/113))

### Team tasks and reviews

- **Integration:** Resolved conflicts in availability, bundles, remarks and persistent loan IDs, preserving
  teammates' commits, shared equipment references and save-failure safeguards.
  ([#87 integration](https://github.com/AY2627S1-CS2113-W13-1/tp/commit/f4541074947055d0ab673625764ed08e27fe34af),
  [#89](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/89),
  [#90](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/90),
  [#91](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/91))
- **Reviews:** Identified [saved-record parsing failures](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/74#discussion_r4360852969),
  [assertion-triggered crashes](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/76#discussion_r4361337732),
  [unsafe bundle-removal save ordering](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/115#discussion_r4379633281)
  and [lost loan snapshots](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/116#discussion_r4386666562),
  providing reproduction steps and suggested fixes.
- **Beyond the team:** [Reviewed EduReserve's Developer Guide](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/13#pullrequestreview-5678361644),
  flagging inconsistent command syntax, undocumented duplicate-booking validation and incorrect UML object
  creation and activation notation.

Submitted list-validation work in [#164](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/164) was superseded
by the team's command standardisation and was not merged.
