package seedu.snap;

import seedu.snap.eqmmanager.EqmManager;

/** Starts the SNAP application. */
public class Main {
    /** Starts the application and hands control to the equipment manager. */
    public static void main(String[] args) {
        EqmManager eqmManager = new EqmManager();
        eqmManager.run();
    }
}
