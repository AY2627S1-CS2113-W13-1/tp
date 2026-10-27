---
layout: default
title: Rohita Reddy
---

# Rohita Reddy - Project Portfolio Page

## Overview

snAp is a command-line application for photography clubs to manage equipment and borrowing records. It tracks
equipment details, availability, loans, returns and reusable equipment bundles.

My work focused on preserving accurate loan history, validating equipment compatibility and making equipment
and loan information easier to retrieve safely.

## Summary of Contributions

**Code contributed:** [Code Dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=Rohita-29&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00)

### Enhancements implemented

- **Loan deletion:** Implemented and refined loan deletion with input validation, ambiguous-record handling,
  save-before-release behaviour and safeguards for returned history and newer active loans.
  ([PR #15](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/15),
  [PR #79](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/79))
- **Overdue-loan identification:** Added deterministic overdue detection based on the current local date,
  excluding returned loans and loans due today. Added ordered, read-only filtering, command output and model,
  handler and integration tests.
  ([PR #96](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/96))
- **Equipment compatibility attributes:** Added subtype-specific compatibility information for cameras, lenses,
  batteries and tripods. Implemented shared parsing, normalization, persistence and comparison results while
  preserving legacy saved records and constructors.
  ([PR #100](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/100))
- **Issue-time equipment snapshots:** Added immutable per-loan snapshots containing equipment type, numeric
  condition, damage and remarks. Snapshots remain unchanged after equipment updates and returns, survive
  restarts and display `Not recorded` for legacy loans instead of inventing historical information.
  ([PR #101](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/101))

- **Expanded equipment damage:** Expanded subtype-specific damage enums, added `OTHER` with mandatory meaningful
  remarks, preserved saved-data compatibility and added positive, negative, boundary, command and persistence
  tests.
  ([PR #103](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/103))

### Current v2.0 work

- **Bundle compatibility validation:** Added checks that reject known camera-lens, camera-battery and
  camera-tripod mismatches while allowing unknown or unrelated comparisons. Multi-item commands retain
  per-item results and do not mutate a bundle for rejected items.
  ([PR #136](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/136))

### Documentation

- **User Guide:** Documented loan deletion, overdue-loan listing, compatibility attributes, issue-time
  snapshots, expanded damage assessments and bundle compatibility rules with command formats, examples,
  defaults and error behaviour.
- **Developer Guide:** Wrote the overdue-loan design and detailed the responsibilities, persistence,
  backward-compatibility decisions and verification strategy for compatibility attributes and immutable
  issue-time condition snapshots.
  ([PR #85](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/85),
  [PR #100](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/100),
  [PR #101](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/101))

### Team contributions and reviews

- Reviewed teammate changes involving loan invariants, logging, packaged JAR naming, equipment validation,
  equipment deletion and the project website.
  ([PR #28](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/28),
  [PR #30](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/30),
  [PR #35](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/35),
  [PR #36](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/36),
  [PR #39](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/39),
  [PR #42](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/42))
- Strengthened reviewed and authored changes with focused regression tests, cross-platform Gradle checks,
  legacy-record coverage and documentation that matches executable behaviour.
