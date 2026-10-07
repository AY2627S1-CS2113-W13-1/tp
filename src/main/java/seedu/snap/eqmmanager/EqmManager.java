package seedu.snap.eqmmanager;

import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.loan.LoanHandler;
import seedu.snap.parser.Parser;

/** Coordinates command input and execution for the SNAP application. */
public class EqmManager {
    private Parser parser;
    private LoanHandler loans;
    private boolean isRunning;

    /** Creates an equipment manager ready to accept commands. */
    public EqmManager() {
        this.parser = new Parser();
        this.loans = new LoanHandler();
        this.isRunning = true;
    }

    /**
     * Executes one command entered by the user.
     *
     * @param input command to execute
     */
    public void executeInput(String input) {
        if (input.equals("bye")) {
            isRunning = false;
        } else if (input.equals("loan") || input.startsWith("loan ")) {
            try {
                loans.addLoan(input);
                System.out.println("Loan added successfully.");
            } catch (LoanAdditionUnsuccessful e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Success message.");
        }
    }

    /** Runs the main input and execution loop until the user exits. */
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
