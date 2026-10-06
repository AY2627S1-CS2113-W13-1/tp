package seedu.EqmManager;

import seedu.EqmManager.EqmManager.EqmManager;

public class Main {
    private static EqmManager eqmManager;

    public static void main(String[] args) {
        eqmManager = new EqmManager();

        eqmManager.run();
    }
}
