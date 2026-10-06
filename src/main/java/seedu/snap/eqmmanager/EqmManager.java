package seedu.snap.eqmmanager;

import seedu.snap.parser.Parser;

public class EqmManager {
    private Parser parser;
    private boolean isRunning;

    public EqmManager() {
        this.parser = new Parser();
        this.isRunning = true;
    }

    public void executeInput(String input) {
        if (input.equals("bye")) {
            System.out.println("Farewell message.");
            isRunning = false;
        } else {
            System.out.println("Success message.");
        }
    }

    /**
     * Main entry-point for the application.
     */
    public void run() {
        System.out.println("Greeting message.");

        // main process
        String input;
        while (isRunning) {
            input = parser.getInput();
            executeInput(input);
        }

        System.out.println("Goodbye message.");
    }
}
