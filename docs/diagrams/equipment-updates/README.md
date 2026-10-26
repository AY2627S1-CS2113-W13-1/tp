# Equipment update sequence

The [successful update sequence](equipment-update-sequence.puml) accompanies **Updating equipment details**
in the Developer Guide and [issue #81](https://github.com/AY2627S1-CS2113-W13-1/tp/issues/81).
It shows parsing, lookup, validation and saving before the original object changes.

The shared [application architecture](../AppArchitecture.puml) and
[equipment class diagram](../EquipmentClasses.puml) cover the common structure.
Validation rejection and save failures are explained in the DG text.

The sequence assumes configured storage and item `001` already loaded. It includes `../style.puml`
for the shared vibrant theme. Utility lifelines receive static calls; brief notes summarise validation
and proposal construction. The display self-message summarises console output.

From the repository root, regenerate the SVGs with Java 25 and your local PlantUML JAR:

```text
java -jar path/to/plantuml.jar -charset UTF-8 -failfast2 -tsvg -o ../../images docs/diagrams/equipment-updates/*.puml
```

The output path is relative to each source file. Inspect the exported images after changing the sources
and include both source and SVG in the change. See the [shared rendering notes](../README.md).
