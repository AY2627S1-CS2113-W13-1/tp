# snAp

snAp is a command-line application for photography-club equipment officers. It tracks equipment,
loans, returns, bundles and maintenance records.

The [User Guide Quick Start](docs/UserGuide.md#quick-start) explains how to run the v2.1 JAR.
The setup below is for building and testing from source. Use documentation from the same version as your JAR;
older releases may not support every command described in the guides.

## Setup

Install JDK 25 and check that `java -version` reports version 25. The first Gradle run needs internet
access to download dependencies.

In IntelliJ IDEA, open the repository as a Gradle project. Set both the project SDK and Gradle JVM to
JDK 25. Run `seedu.snap.Main` from `src/main/java/seedu/snap/Main.java`.

To run from a terminal in the project folder:

| Task | Windows PowerShell | macOS/Linux |
| --- | --- | --- |
| Start the application | `.\gradlew.bat run --console=plain --quiet` | `./gradlew run --console=plain --quiet` |
| Run tests and Checkstyle | `.\gradlew.bat clean check --no-daemon` | `./gradlew clean check --no-daemon` |
| Build the JAR | `.\gradlew.bat shadowJar` | `./gradlew shadowJar` |

The application opens with a command menu. Enter `help` to see it again, `help loan` for one command,
or `help all` for the complete reference. Enter `bye` to save and exit.

The packaged application is `build/libs/snAp.jar`. Run it with `java -jar build/libs/snAp.jar`.
Saved records are stored in `data` relative to the working folder. Use a separate working folder
when trying examples so that you do not change your normal records.

## Tests and checks

JUnit tests are in `src/test/java/seedu/snap`. The Gradle `check` task runs them and checks Java style
using `config/checkstyle/checkstyle.xml`. GitHub Actions runs the same task with Java 25 on Linux,
macOS and Windows for pushes and pull requests.

## Documentation

* [User Guide](docs/UserGuide.md): installation, commands, examples and saved data.
* [Developer Guide](docs/DeveloperGuide.md): design, implementation and manual tests.
* [Help audit](docs/help-audit.md): checked command coverage and remaining release checks for issue #104.
* [Documentation setup](docs/README.md): Jekyll layout, local preview and publishing.
* [About Us](docs/AboutUs.md): project team.
