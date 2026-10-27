package seedu.snap.eqmmanager;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.equipment.Equipment;
import seedu.snap.bundle.Bundle;
import seedu.snap.bundle.BundleHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;
import seedu.snap.exceptions.BundleHistoryUnsuccessful;
import seedu.snap.exceptions.BackupUnsuccessful;
import seedu.snap.equipment.EquipmentFilter;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.EquipmentFilteringUnsuccessful;
import seedu.snap.exceptions.EquipmentStatusUnsuccessful;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;
import seedu.snap.exceptions.EquipmentViewingUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanBorrowerUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;
import seedu.snap.exceptions.LoanReturnUnsuccessful;
import seedu.snap.exceptions.LoanHistoryUnsuccessful;
import seedu.snap.exceptions.LoanViewingUnsuccessful;
import seedu.snap.exceptions.LoanUpdateUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;
import seedu.snap.exceptions.MaintenanceRecordUnsuccessful;
import seedu.snap.maintenance.MaintenanceHandler;
import seedu.snap.maintenance.MaintenanceRecord;
import seedu.snap.parser.Parser;
import seedu.snap.ui.Help;
import seedu.snap.ui.Welcome;

/** Coordinates command input and execution for the SNAP application. */
public class EqmManager {
    private static final Logger logger = Logger.getLogger(EqmManager.class.getName());
    private final Parser parser;
    private final LoanHandler loans;
    private final EquipmentHandler equipment;
    private final MaintenanceHandler maintenance;
    private final BundleHandler bundles;
    private final BackupHandler backups;
    private final Path dataDirectory;
    private final Clock clock;
    private String startupError;
    private boolean isRunning;

    /** Creates an equipment manager ready to accept commands. */
    public EqmManager() {
        this(Path.of("data"));
    }

    /**
     * Creates a manager using the supplied directory for persistent equipment, loan, and maintenance files.
     * A missing directory or file is treated as an empty data set.
     *
     * @param dataDirectory directory containing equipment.txt and loans.txt
     */
    public EqmManager(Path dataDirectory) {
        this(dataDirectory, Clock.systemDefaultZone());
    }

    /** Creates a manager with an explicit clock for optional issue-date defaults. */
    public EqmManager(Path dataDirectory, Clock clock) {
        if (dataDirectory == null) {
            throw new IllegalArgumentException("Data directory must not be null.");
        }
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        this.parser = new Parser();
        this.clock = clock;
        this.equipment = new EquipmentHandler(this.dataDirectory.resolve("equipment.txt"), clock);
        this.loans = new LoanHandler(equipment, this.dataDirectory.resolve("loans.txt"),
                this.dataDirectory.resolve("loan-sequence.txt"), clock);
        this.maintenance = new MaintenanceHandler(equipment, this.dataDirectory.resolve("maintenance.txt"), clock);
        this.bundles = new BundleHandler(equipment, this.dataDirectory.resolve("bundles.txt"));
        this.backups = new BackupHandler(this.dataDirectory, equipment, loans, bundles);
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
        try {
            bundles.loadFromStorage();
        } catch (IOException e) {
            loadErrors.add("bundles.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not load saved bundle data", e);
        }
        try {
            maintenance.loadFromStorage();
        } catch (IOException e) {
            loadErrors.add("maintenance.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not load saved maintenance data", e);
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
        if (normalizedInput.equals("restore") || normalizedInput.startsWith("restore ")) {
            restoreBackup(normalizedInput);
        } else if (startupError != null) {
            if (normalizedInput.equals("bye")) {
                isRunning = false;
            } else {
                System.out.println("Could not load saved data: " + startupError);
            }
        } else if (normalizedInput.matches("(?sU)help(?:\\s.*)?")) {
            Help.print(normalizedInput.substring("help".length()));
        } else if (normalizedInput.equals("bye")) {
            isRunning = !saveBeforeExit();
        } else if (normalizedInput.equals("backup") || normalizedInput.startsWith("backup ")) {
            createBackup(normalizedInput);
        } else if (normalizedInput.equals("delete loan")
                || normalizedInput.startsWith("delete loan/")
                || normalizedInput.startsWith("delete loan ")) {
            deleteLoan(normalizedInput);
        } else if (normalizedInput.equals("delete") || normalizedInput.startsWith("delete ")) {
            deleteEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?U)add(?:\\s.*)?")) {
            addEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?sU)loan(?:\\s.*)?")) {
            addLoan(normalizedInput);
        } else if (normalizedInput.matches("(?sU)return(?:\\s.*)?")) {
            returnEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?sU)maintain(?:\\s.*)?")) {
            recordMaintenance(normalizedInput);
        } else if (normalizedInput.startsWith("history bundle/")) {
            viewBundleLoanHistory(normalizedInput);
        } else if (normalizedInput.matches("(?sU)status-history(?:\\s.*)?")) {
            viewStatusHistory(normalizedInput);
        } else if (normalizedInput.matches("(?sU)mark(?:\\s.*)?")) {
            markEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?sU)history(?:\\s.*)?")) {
            viewLoanHistory(normalizedInput);
        } else if (normalizedInput.matches("(?U)view(?:\\s.*)?")) {
            viewLoanIssueCondition(normalizedInput);
        } else if (normalizedInput.matches("(?U)update\\s+loan(?:\\s.*|/.*)?")) {
            updateLoan(normalizedInput);
        } else if (normalizedInput.matches("(?sU)list\\s+(?:borrower|member)(?:/.*|\\s.*)?")) {
            viewActiveLoansForBorrower(normalizedInput);
        } else if (normalizedInput.matches("(?U)filter(?:\\s.*)?")) {
            filterEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?U)update(?:\\s.*)?")) {
            updateEquipment(normalizedInput);
        } else if (normalizedInput.matches("(?sU)show(?:\\s.*)?")) {
            showEquipment(normalizedInput);
        } else if (normalizedInput.equals("list overdue")) {
            viewOverdueLoans();
        } else if (normalizedInput.equals("list loans")) {
            loans.viewAllLoans();
        } else if (normalizedInput.equals("add-bundle") || normalizedInput.startsWith("add-bundle ")) {
            addBundle(normalizedInput);
        } else if (normalizedInput.equals("add-to-bundle") || normalizedInput.startsWith("add-to-bundle ")) {
            addItemsToBundle(normalizedInput);
        } else if (normalizedInput.matches("(?U)remove-from-bundle(?:\\s.*)?")) {
            removeItemsFromBundle(normalizedInput);
        } else if (normalizedInput.equals("list bundles")) {
            try {
                bundles.viewAllBundles(normalizedInput);
            } catch (BundleAdditionUnsuccessful e) {
                System.out.println(e.getMessage());
            }
        } else if (normalizedInput.equals("list") || normalizedInput.startsWith("list ")) {
            equipment.viewAllEquipment();
        } else {
            System.out.println("Command not recognised. Type help for available commands.");
        }
    }

    /** Displays complete equipment values without changing records or performing a save. */
    private void showEquipment(String input) {
        try {
            equipment.showEquipment(input);
        } catch (EquipmentViewingUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Runs the main input and execution loop until the user exits. */
    public void run() {
        if (startupError != null) {
            System.out.println("Could not load saved data: " + startupError);
        } else {
            Welcome.printLogo();
            System.out.println("Welcome to snAp. Loaded " + equipment.getEquipmentList().size()
                    + " equipment record(s) and " + loans.getLoanCount() + " loan(s).");
            System.out.println();
            Help.print();
        }

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
        try {
            bundles.saveToStorage();
        } catch (IOException e) {
            saveErrors.add("bundles.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not save bundle data before exit", e);
        }
        try {
            maintenance.saveToStorage();
        } catch (IOException e) {
            saveErrors.add("maintenance.txt: " + e.getMessage());
            logger.log(Level.SEVERE, "Could not save maintenance data before exit", e);
        }

        if (!saveErrors.isEmpty()) {
            System.out.println("Could not save data: " + String.join(System.lineSeparator(), saveErrors));
            return false;
        }
        logger.log(Level.FINE, "Saved application data before exit");
        return true;
    }

    // @@author Rohita-29

    /** Displays a loan's issue-time equipment condition. */
    private void viewLoanIssueCondition(String input) {
        try {
            loans.viewLoanIssueCondition(input);
        } catch (LoanViewingUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    // @@author

    /** Creates a backup and displays either its destination or the safe failure message. */
    private void createBackup(String input) {
        try {
            Path destination = backups.createBackup(input);
            System.out.println("Backup created successfully at " + destination + ".");
        } catch (BackupUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Restores a validated backup and reports either the restored source or the safe failure message. */
    private void restoreBackup(String input) {
        try {
            Path source = backups.restoreBackup(input);
            startupError = null;
            System.out.println("Backup restored successfully from " + source + ".");
        } catch (BackupUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
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

    /** Reports the outcome of removing equipment from a persisted bundle. */
    private void removeItemsFromBundle(String input) {
        try {
            for (String message : bundles.removeItemsFromBundle(input)) {
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

    /** Displays a persisted return result or its validation/save error. */
    private void returnEquipment(String input) {
        try {
            Loan returned = loans.returnLoan(input);
            System.out.println("Loan " + returned.getLoanId() + " for item ID " + returned.getItemId()
                    + " returned successfully.");
        } catch (LoanReturnUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Displays the outcome of recording a maintenance activity. */
    private void recordMaintenance(String input) {
        try {
            MaintenanceRecord record = maintenance.addMaintenanceRecord(input);
            assert record != null : "A successful maintenance command must return a record";
            System.out.println("Maintenance recorded successfully.");
        } catch (MaintenanceRecordUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Displays the selected equipment's borrowing history or its validation error. */
    private void viewLoanHistory(String input) {
        try {
            loans.viewLoanHistory(input);
        } catch (LoanHistoryUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Displays the selected bundle's active and returned loan history. */
    private void viewBundleLoanHistory(String input) {
        try {
            bundles.viewBundleLoanHistory(input, loans);
        } catch (BundleHistoryUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Displays the outcome of marking one item missing or lost. */
    private void markEquipment(String input) {
        try {
            Equipment marked = equipment.markStatus(input);
            System.out.println("Equipment with item ID " + marked.getItemId() + " marked "
                    + marked.getStatus().getStorageValue() + " successfully.");
        } catch (EquipmentStatusUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Displays one physical item's current state and chronological status changes. */
    private void viewStatusHistory(String input) {
        try {
            equipment.viewStatusHistory(input);
        } catch (EquipmentStatusUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Displays the active equipment currently held by one borrower or its validation error. */
    private void viewActiveLoansForBorrower(String input) {
        try {
            loans.viewActiveLoansForBorrower(input);
        } catch (LoanBorrowerUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    /** Displays matching equipment or the filter error. */
    private void filterEquipment(String input) {
        try {
            EquipmentFilter filter = EquipmentFilter.parse(input);
            List<Equipment> matches = filter.apply(equipment.getEquipmentList(), loans);
            equipment.viewEquipment(matches);
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

    /** Displays the outcome of a loan-detail update. */
    private void updateLoan(String input) {
        try {
            Loan updated = loans.updateLoan(input);
            System.out.println("Loan " + updated.getLoanId() + " updated successfully.");
        } catch (LoanUpdateUnsuccessful exception) {
            System.out.println(exception.getMessage());
        }
    }

    // @@author Rohita-29

    /** Displays loans that are overdue as of the current local date. */
    private void viewOverdueLoans() {
        LocalDate referenceDate = LocalDate.now(clock);
        loans.viewOverdueLoans(referenceDate);
    }

    // @@author
}
