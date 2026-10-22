# Equipment update diagrams

Diagram work for [issue #81](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/81).
The explanation is in the text-only DG [PR #80](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/80).

The diagrams are still being prepared. Keep the editable source and exported image together
in this folder, then add the images beside the relevant explanation in `docs/DeveloperGuide.md`.

## Class diagram

Show the update-related responsibilities and relationships between `EqmManager`,
`EquipmentHandler`, `EquipmentFields`, `Equipment` and `EquipmentStorage`.
Leave out unrelated commands and minor helpers.

## Sequence diagram

Use `update id/001 i/Film Camera cond/5` as the example, with item `001` already present.
Show command routing, field parsing, target lookup, validation, saving the proposed details,
and updating the existing equipment object before displaying success.

Mention that omitted fields retain their current values. A short note can explain that
invalid input or a failed save leaves the original item unchanged.

Use the current implementation in
[PR #59](https://github.com/AY2627S1-CS2113-W13-1/tp/pull/59) when checking the method calls.
