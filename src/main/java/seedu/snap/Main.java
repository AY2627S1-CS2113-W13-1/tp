package seedu.snap;

import java.io.Console;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.logging.AppLogging;

/** Starts the SNAP application. */
public class Main {
    /** Starts the application and hands control to the equipment manager. */
    public static void main(String[] args) {
        configureStandardOutput();
        AppLogging logging = AppLogging.start();
        try (logging) {
            EqmManager eqmManager = new EqmManager();
            eqmManager.run();
        }
    }

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
}
