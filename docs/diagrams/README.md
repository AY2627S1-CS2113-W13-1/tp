---
layout: default
title: Developer Guide diagrams
permalink: /diagrams/index.html
---

# Developer Guide diagrams

The Developer Guide embeds SVG files from `docs/images/` and links to their editable PlantUML sources here.
These diagrams use the shared `style.puml`. Keep each diagram beside the design explanation that it supports.

[AppArchitecture.puml](AppArchitecture.puml) gives the application overview: startup, console input,
command coordination, the four feature areas and local storage. Related classes are grouped so the
shared equipment inventory is visible without showing every helper.

The equipment-management designs also include:

| Source | Design illustrated |
| --- | --- |
| [EquipmentClasses.puml](EquipmentClasses.puml) | Shared inventory, equipment state, filtering and helper dependencies. |
| [EquipmentWorkflow.puml](EquipmentWorkflow.puml) | User Guide example showing how a condition update changes filter results. |
| [EquipmentAdditionSequence.puml](EquipmentAdditionSequence.puml) | Duplicate rejection and saving before insertion. |
| [EquipmentAvailability.puml](EquipmentAvailability.puml) | Availability and loan ownership supporting borrowing and filters. |
| [EquipmentFilterSequence.puml](EquipmentFilterSequence.puml) | Current-loan checks and read-only filter results. |
| [BundleCompatibilitySequence.puml](BundleCompatibilitySequence.puml) | Per-item compatibility checks and partial-success bundle additions. |
| [equipment-update-sequence.puml](equipment-updates/equipment-update-sequence.puml) | Saving a proposal before changing the original equipment object. |
| [LoggingLifecycle.puml](LoggingLifecycle.puml) | Logging setup, I/O fallback and session cleanup. |
| [LoanAdditionSequence.puml](LoanAdditionSequence.puml) | Saving a valid new loan before reserving its equipment. |
| [ReturnSequence.puml](ReturnSequence.puml) | Applying a saved return assessment to the original objects. |
| [ReturnSaveFailure.puml](ReturnSaveFailure.puml) | Restoring captured files after a return save fails. |
| [BackupRestoreSequence.puml](BackupRestoreSequence.puml) | Staged restore validation and rollback of live files. |
| [LoanAssessmentObject.puml](LoanAssessmentObject.puml) | One returned loan's immutable issue and return snapshots. |

The overdue-loan design includes:

| Source | Design illustrated |
| --- | --- |
| [OverdueLoanClasses.puml](OverdueLoanClasses.puml) | Relationships between command routing, selection and the loan-owned overdue rule. |
| [OverdueLoanSequence.puml](OverdueLoanSequence.puml) | One-date evaluation and read-only display flow for `loan-list-overdue`. |

The maintenance and backup designs include:

| Source | Design illustrated |
| --- | --- |
| [MaintenanceRecordSequence.puml](MaintenanceRecordSequence.puml) | Command routing, validation, permanent identity resolution and save-before-append success and failure paths. |
| [MaintenanceBackupClasses.puml](MaintenanceBackupClasses.puml) | Maintenance record ownership, shared handlers, storage dependencies and backup/restore boundaries. |

The current images were rendered with [PlantUML](https://plantuml.com/download) 1.2026.9 and Java 25.
From the repository root, replace `path/to/plantuml.jar` with your local JAR path and run, for example:

```text
java -jar path/to/plantuml.jar -charset UTF-8 -failfast2 -tsvg -o ../images docs/diagrams/ReturnSequence.puml
```

Repeat for other changed diagram sources. PlantUML resolves the output directory relative to each source file,
so `../images` writes to `docs/images/`.
Class, state and architecture diagrams use the bundled Smetana layout engine.
Sequence and activity diagrams do not need an external layout tool.
After editing a source, regenerate its SVG, check the rendered labels and arrows, and include both files in the change.
Keep details outside the diagram's purpose in the accompanying text.
