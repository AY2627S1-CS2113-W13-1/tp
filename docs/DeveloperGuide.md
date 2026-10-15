# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

{Describe the design and implementation of the product. Use UML diagrams and short code snippets where applicable.}

### Equipment assertions and diagnostics

User input is checked with validation exceptions even when Java assertions are disabled.
Assertions check programming assumptions in equipment creation and addition: equipment fields are valid
before construction, rejected additions do not change inventory size, and successful additions append one item.
Never put a mutation inside an assertion, because Java skips the whole assertion when assertions are disabled.

The Gradle `run` task enables assertions. For an IDE Application configuration, add `-ea` to VM options.
For the packaged application, run `java -ea -jar build/libs/duke.jar` after building with `shadowJar`.
Gradle tests run with assertions enabled and exercise invalid constructor arguments and boundary values.

Application diagnostics are written to `logs/snap.log.0` and `logs/snap.log.1`, relative to the
working directory. The files rotate at approximately one megabyte each and are excluded from Git.
`AppLogging` owns configuration and cleanup; `Main` uses try-with-resources to close it automatically.
`FINE` records equipment addition successes and rejection reasons.
Expected input errors are diagnostics rather than warnings.
Equipment addition does not log raw commands or equipment names.

All classes under `seedu.snap` share the file configuration through the `seedu.snap` parent logger.
In each feature class, declare `private static final Logger logger = Logger.getLogger(MyClass.class.getName());`
using that class's own name, then add meaningful logging calls in the feature's PR.
Leave logger levels, handlers, and parent propagation at their defaults in feature classes;
`AppLogging` configures these centrally. Start it only once in `Main`.
Existing loan diagnostics also inherit the shared file handler; adding or changing feature messages
remains the responsibility of each feature's author.

If the log directory or file cannot be created, the application prints a warning and continues.
The file handler is closed on normal exit and unexpected failure. Integration tests verify logging
with assertions enabled and disabled, console output, and startup when the log directory is blocked.


## Product scope
### Target user profile

{Describe the target user profile}

### Value proposition

{Describe the value proposition: what problem does it solve?}

## User Stories

|Version| As a ... | I want to ... | So that I can ...|
|--------|----------|---------------|------------------|
|v1.0|new user|see usage instructions|refer to them when I forget how to use the application|
|v2.0|user|find a to-do item by name|locate a to-do without having to go through the entire list|

## Non-Functional Requirements

{Give non-functional requirements}

## Glossary

* *glossary item* - Definition

## Instructions for manual testing

{Give instructions on how to do a manual product testing e.g., how to load sample data to be used for testing}
