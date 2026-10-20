package seedu.snap.eqmmanager;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.equipment.Equipment;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;
import seedu.snap.loan.LoanHandler;
import seedu.snap.parser.Parser;
import seedu.snap.loan.Loan;

/** Coordinates command input and execution for the SNAP application. */
public class EqmManager {
    private final Parser parser;
    private final LoanHandler loans;
    private final EquipmentHandler equipment;
    private boolean isRunning;

    /** Creates an equipment manager ready to accept commands. */
    public EqmManager() {
        this.parser = new Parser();
        this.equipment = new EquipmentHandler();
        this.loans = new LoanHandler(equipment);
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
        } else if (input.equals("delete-loan") || input.startsWith("delete-loan ")){
            try{
                Loan deletedLoan = loans.deleteLoan(input);
                System.out.println("Loan for item ID " + deletedLoan.getItemId() + " deleted successfully.");
            } catch (LoanDeletionUnsuccessful e){
                System.out.println(e.getMessage());
            }
        } else if (input.equals("delete") || input.startsWith("delete ")) {
            try {
                Equipment target = equipment.getEquipmentForDeletion(input);
                if (loans.hasActiveLoanForItem(target.getItemId())) {
                    throw new EquipmentDeletionUnsuccessful("Cannot delete item ID " + target.getItemId()
                            + " while it has an active loan.");
                }
                Equipment deletedEquipment = equipment.deleteEquipment(input);
                System.out.println("Equipment with item ID " + deletedEquipment.getItemId()
                        + " deleted successfully.");
            } catch (EquipmentDeletionUnsuccessful e) {
                System.out.println(e.getMessage());
            }
        } else if (input.equals("add") || input.startsWith("add ")) {
            try {
                equipment.addEquipment(input);
                System.out.println("Equipment added successfully.");
            } catch (EquipmentAdditionUnsuccessful e) {
                System.out.println(e.getMessage());
            }
        } else if (input.equals("loan") || input.startsWith("loan ")) {
            try {
                loans.addLoan(input);
                System.out.println("Loan added successfully.");
            } catch (LoanAdditionUnsuccessful e) {
                System.out.println(e.getMessage());
            }
        } else if (input.equals("list loans")) {
            loans.viewAllLoans();
        } else if (input.equals("list") || input.startsWith("list ")) {
            equipment.viewAllEquipment();
        } else {
            System.out.println("Command not recognised. Please try again.");
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
