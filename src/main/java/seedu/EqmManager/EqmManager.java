package seedu.EqmManager;

import seedu.EqmManager.Parser.Parser;
import seedu.EqmManager.Loan.LoanHandler;


public class EqmManager {
    private static Parser parser;
    private static LoanHandler loans;
    private static boolean isRunning;


    /** Creates a fella using the supplied personality-specific constants.
     * @param constants messages and keywords used by this fella
     */
    protected EqmManager() {
        this.parser = new Parser();
        this.loans = new LoanHandler();
        this.isRunning = true;
    }

    /**
     * Main entry-point for the application.
     */
    public static void main(String[] args) {
        System.out.println("Greeting message.");

        // main process
        String input;
        while (isRunning) {
            input = parser.getInput();
            parser.matchInput(input);
        }

        System.out.println("Goodbye message.");
    }
}
