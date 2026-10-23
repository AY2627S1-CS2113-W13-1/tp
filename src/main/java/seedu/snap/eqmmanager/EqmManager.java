package seedu.snap.eqmmanager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger logger = Logger.getLogger(EqmManager.class.getName());
    private final Parser parser;
    private final LoanHandler loans;
    private final EquipmentHandler equipment;
    private final String startupError;
    private boolean isRunning;

    /** Creates an equipment manager ready to accept commands. */
    public EqmManager() {
        this(Path.of("data"));
    }

    /**
     * Creates a manager using the supplied directory for persistent equipment and loan files.
     * A missing directory or file is treated as an empty data set.
     *
     * @param dataDirectory directory containing equipment.txt and loans.txt
     */
    public EqmManager(Path dataDirectory) {
        if (dataDirectory == null) {
            throw new IllegalArgumentException("Data directory must not be null.");
        }
        this.parser = new Parser();
        this.loans = new LoanHandler(dataDirectory.resolve("loans.txt"));
        this.equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        List<String> loadErrors = new ArrayList<>();
        try {
            equipment.loadFromStorage();
        } catch (IOException e) {
            loadErrors.add("equipment.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not load saved equipment data", e);
        }
        try {
            loans.loadFromStorage();
        } catch (IOException e) {
            loadErrors.add("loans.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not load saved loan data", e);
        }
        this.startupError = loadErrors.isEmpty() ? null : String.join(System.lineSeparator(), loadErrors);
        this.isRunning = true;
    }

    /**
     * Executes one command entered by the user.
     *
     * @param input command to execute
     */
    public void executeInput(String input) {
        if (startupError != null) {
            System.out.println("Could not load saved data: " + startupError);
            return;
        }
        if (input.equals("bye")) {
            isRunning = !saveBeforeExit();
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
        if (startupError != null) {
            System.out.println("Could not load saved data: " + startupError);
            return;
        }

        System.out.println("Welcome to snAp. Loaded " + equipment.getEquipmentList().size()
                + " equipment record(s) and " + loans.getLoanCount() + " loan(s).");

        // main process
        String input;
        while (isRunning) {
            input = parser.getInput();
            executeInput(input);
        }

        System.out.println("Goodbye.");
    }

    /**
     * Performs a final save for both data files before ending the session.
     *
     * @return true when both files were saved successfully, or false when the user should retry
     */
    private boolean saveBeforeExit() {
        List<String> saveErrors = new ArrayList<>();
        try {
            equipment.saveToStorage();
        } catch (IOException e) {
            saveErrors.add("equipment.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not save equipment data before exit", e);
        }
        try {
            loans.saveToStorage();
        } catch (IOException e) {
            saveErrors.add("loans.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not save loan data before exit", e);
        }

        if (!saveErrors.isEmpty()) {
            System.out.println("Could not save data: " + String.join(System.lineSeparator(), saveErrors));
            return false;
        }
        logger.log(Level.FINE, "Saved application data before exit");
        return true;
    }
}
