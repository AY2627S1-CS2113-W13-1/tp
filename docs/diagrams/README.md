# Developer Guide diagrams

The Developer Guide embeds SVG files from `docs/images/` and links to their editable PlantUML sources here.
These diagrams use the shared `style.puml`. Keep each diagram beside the design explanation that it supports.

The current images were rendered with [PlantUML](https://plantuml.com/download) 1.2026.9 and Java 25.
From the repository root, replace `path/to/plantuml.jar` with your local JAR path and run, for example:

```text
java -jar path/to/plantuml.jar -charset UTF-8 -failfast2 -tsvg -o ../images docs/diagrams/ReturnSequence.puml
```

Repeat for other changed diagram sources. PlantUML resolves the output directory relative to each source file,
so `../images` writes to `docs/images/`.
The class diagram uses the bundled Smetana layout engine. Sequence diagrams do not need an external layout tool.
After editing a source, regenerate its SVG, check the rendered labels and arrows, and include both files in the change.
Keep details outside the diagram's purpose in the accompanying text.
