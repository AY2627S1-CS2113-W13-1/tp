package seedu.snap;

import seedu.snap.eqmmanager.EqmManager;

public class Main {
    private static EqmManager eqmManager;

    public static void main(String[] args) {
        eqmManager = new EqmManager();

        eqmManager.run();
    }
}
