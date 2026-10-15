package seedu.snap;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.logging.AppLogging;

/** Starts the SNAP application. */
public class Main {
    /** Starts the application and hands control to the equipment manager. */
    public static void main(String[] args) {
        try (AppLogging logging = AppLogging.start()) {
            EqmManager eqmManager = new EqmManager();
            eqmManager.run();
        }
    }
}
