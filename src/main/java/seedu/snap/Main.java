// @@author yarnieee
package seedu.snap;
// @@author

import java.io.Console;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

// @@author yarnieee
import seedu.snap.eqmmanager.EqmManager;
// @@author
import seedu.snap.logging.AppLogging;

// @@author yarnieee
/** Starts the SNAP application. */
public class Main {
    /** Starts the application and hands control to the equipment manager. */
    public static void main(String[] args) {
// @@author
        configureStandardOutput();
        AppLogging logging = AppLogging.start();
        try (logging) {
            EqmManager eqmManager = new EqmManager();
            eqmManager.run();
        }
    // @@author yarnieee
    }
    // @@author

    /**
     * Uses UTF-8 for piped output, including Gradle runs, so Windows does not replace Unicode with question marks.
     * Attached terminals retain their own encoding, which may differ from UTF-8 on older Windows consoles.
     */
    private static void configureStandardOutput() {
        Console console = System.console();
        if (console == null || !console.isTerminal()) {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        }
    }
// @@author yarnieee
}
// @@author
