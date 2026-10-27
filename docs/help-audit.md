---
layout: default
title: Help audit
---

# Help audit

This records the draft audit for [issue #104](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/104),
checked against `master` at `bfd0e3a` on 28 October 2026. It is not the final v2.0 release sign-off.

## Included changes

The audit covers the combined command routing and handlers, including these merged PRs:

| Area | Merged PRs |
| --- | --- |
| Overdue loans, equipment fields, issue conditions and damage choices | [#96](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/96), [#100](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/100), [#101](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/101), [#103](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/103) |
| Help menu and topics, compact equipment lists and item details | [#114](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/114), [#137](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/137) |
| Bundle history, loan updates and borrower views | [#115](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/115), [#116](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/116), [#119](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/119) |
| Backup and restore, maintenance and bundle compatibility | [#118](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/118), [#127](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/127), [#136](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/136) |

The existing documentation layout from [#113](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/113)
is retained. The [User Guide](UserGuide.md) describes the executable development build.

## Command coverage

Start from the branches in `EqmManager.executeInput`, then follow each handler's parser and validation.
The help menu alone cannot establish that every executable command has documentation.

| Area | Commands and help topics checked |
| --- | --- |
| Equipment | `add`, `list`, `show`, `delete item`, `filter`, `update`, `maintain` |
| Loans | `loan`, `return`, `delete loan`, `update loan`, `list loans`, `list borrower`, `view loan`, `list overdue`, `history` |
| Bundles | `add-bundle`, `add-to-bundle`, `remove-from-bundle`, `list bundles`, `history bundle` |
| General | `backup`, `restore`, `help`, `bye` |
| Alternate forms | `list member`, both bundle-history selectors, `help`, `help <COMMAND>`, `help all` and unknown topics |

For each command, compare field spelling and order, required and optional fields, defaults, accepted
values, conditional restrictions and examples. Damage choices are still generated from the four
equipment subtype enums. Examples require suitable existing records; they are not one continuous session.

The automated checks cover:

* Help content, menu completeness, the borrower alias, wrapping and subtype-specific damage lists.
* Execution of every displayed help example through `EqmManager` with isolated records and a fixed clock.
* Repeated help requests against populated, empty and blocked storage, comparing equipment, loans,
  bundles, maintenance, identifiers, file contents and modification times.

Run `.\gradlew.bat clean check --no-daemon` with Java 25 on Windows, or
`./gradlew clean check --no-daemon` on macOS/Linux. Review the PR's CI results as well.

## Remaining release checks

* [PR #140](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/140), for issue #112, is still open at this
  audit's baseline. Recheck loan and bundle output, feedback and prompt wording after it merges.
* Confirm the remaining v2.0 feature set. Unmerged proposals, including issues #64 and #98 and the
  maintenance, location, missing-equipment and summary drafts, are excluded from this audit.
* After the selected changes merge, repeat the command comparison, executable examples, Java 25 checks
  and CI review against the final `master` before closing #104.
