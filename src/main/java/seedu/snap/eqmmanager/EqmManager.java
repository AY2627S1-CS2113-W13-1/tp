package seedu.snap.eqmmanager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;
import seedu.snap.bundle.Bundle;
import seedu.snap.bundle.BundleHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;
import seedu.snap.equipment.EquipmentFilter;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.EquipmentFilteringUnsuccessful;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;
import seedu.snap.parser.Parser;

/** Coordinates command input and execution for the SNAP application. */
public class EqmManager {
    private static final Logger logger = Logger.getLogger(EqmManager.class.getName());
    private final Parser parser;
    private final LoanHandler loans;
    private final EquipmentHandler equipment;
    private final BundleHandler bundles;
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
        this.equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        this.loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"));
        this.bundles = new BundleHandler(equipment);
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
        String normalizedInput = input == null ? "" : input.replaceAll("(?U)^\\s+|\\s+$", "");
        if (startupError != null) {
            System.out.println("Could not load saved data: " + startupError);
        } else if (normalizedInput.equals("bye")) {
            isRunning = !saveBeforeExit();
        } else if (normalizedInput.equals("delete loan")
                || normalizedInput.startsWith("delete loan/")
                || normalizedInput.startsWith("delete loan ")) {
            deleteLoan(normalizedInput);
        } else if (normalizedInput.equals("delete") || normalizedInput.startsWith("delete ")) {
            deleteEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?U)add(?:\\s.*)?")) {
            addEquipment(normalizedInput);
        } else if (normalizedInput.equals("loan") || normalizedInput.startsWith("loan ")) {
            addLoan(normalizedInput);
        } else if (normalizedInput.matches("(?U)filter(?:\\s.*)?")) {
            filterEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?U)update(?:\\s.*)?")) {
            updateEquipment(normalizedInput);
        } else if (normalizedInput.equals("list loans")) {
            loans.viewAllLoans();
        } else if (normalizedInput.equals("add-bundle") || normalizedInput.startsWith("add-bundle ")) {
            addBundle(normalizedInput);
        } else if (normalizedInput.equals("add-to-bundle") || normalizedInput.startsWith("add-to-bundle ")) {
            addItemsToBundle(normalizedInput);
        } else if (normalizedInput.equals("list bundles")) {
            try {
                bundles.viewAllBundles(normalizedInput);
            } catch (BundleAdditionUnsuccessful e) {
                System.out.println(e.getMessage());
            }
        } else if (normalizedInput.equals("list") || normalizedInput.startsWith("list ")) {
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

    /** Displays the outcome of a loan deletion. */
    private void deleteLoan(String input) {
        // @@author Rohita-29
        try {
            Loan deleted = loans.deleteLoan(input);
            System.out.println("Loan " + deleted.getLoanId() + " for item ID " + deleted.getItemId()
                    + " deleted successfully.");
        } catch (LoanDeletionUnsuccessful e) {
            System.out.println(e.getMessage());
        }
        // @@author
    }

    /** Prevents deletion of borrowed equipment before asking the inventory to remove it. */
    private void deleteEquipment(String input) {
        try {
            Equipment target = equipment.getEquipmentForDeletion(input);
            if (bundles.containsEquipmentId(target.getItemId())) {
                throw new EquipmentDeletionUnsuccessful("Cannot delete item ID " + target.getItemId()
                        + " while it belongs to a bundle.");
            }
            if (loans.hasActiveLoanForItem(target.getItemId())) {
                throw new EquipmentDeletionUnsuccessful("Cannot delete item ID " + target.getItemId()
                        + " while it has an active loan.");
            }
            // @@author Yi An
            equipment.deleteEquipment(input);
            System.out.println("Equipment with item ID " + target.getItemId() + " deleted successfully.");
        } catch (EquipmentDeletionUnsuccessful e) {
            System.out.println(e.getMessage());
        }
        // @@author
    }

    /** Creates a bundle and reports its assigned identifier. */
    private void addBundle(String input) {
        try {
            Bundle bundle = bundles.addBundle(input);
            System.out.println("Bundle " + bundle.getBundleId() + " added successfully.");
        } catch (BundleAdditionUnsuccessful e) {
            System.out.println(e.getMessage());
        }
    }

    /** Reports each equipment addition to a bundle independently. */
    private void addItemsToBundle(String input) {
        try {
            for (String message : bundles.addItemsToBundle(input)) {
                System.out.println(message);
            }
        } catch (BundleAdditionUnsuccessful e) {
            System.out.println(e.getMessage());
        }
    }

    /** Displays the outcome of equipment addition. */
    private void addEquipment(String input) {
        try {
            equipment.addEquipment(input);
            System.out.println("Equipment added successfully.");
        } catch (EquipmentAdditionUnsuccessful e) {
            System.out.println(e.getMessage());
        }
    }

    /** Displays the outcome of loan addition. */
    private void addLoan(String input) {
        // @@author Yi An
        try {
            Loan added = loans.addLoan(input);
            System.out.println("Loan " + added.getLoanId() + " added successfully.");
        } catch (LoanAdditionUnsuccessful e) {
            System.out.println(e.getMessage());
        }
        // @@author
    }

    /** Displays matching equipment or the filter error. */
    private void filterEquipment(String input) {
        try {
            EquipmentFilter filter = EquipmentFilter.parse(input);
            List<Equipment> matches = filter.apply(equipment.getEquipmentList(), loans);
            if (matches.isEmpty()) {
                System.out.println("No equipment matches the filters.");
            } else {
                equipment.viewEquipment(matches);
            }
        } catch (EquipmentFilteringUnsuccessful e) {
            System.out.println(e.getMessage());
        }
    }

    /** Displays an update result while leaving validation and persistence to the handler. */
    private void updateEquipment(String input) {
        try {
            Equipment updated = equipment.updateEquipment(input);
            System.out.println("Equipment with item ID " + updated.getItemId() + " updated successfully.");
        } catch (EquipmentUpdateUnsuccessful e) {
            System.out.println(e.getMessage());
        }
    }
}
