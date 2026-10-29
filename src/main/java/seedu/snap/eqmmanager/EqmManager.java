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
import seedu.snap.ui.BundleView;
import seedu.snap.ui.ConsoleOutput;
import seedu.snap.ui.LoanView;

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
        this.equipment = new EquipmentHandler(this.dataDirectory.resolve("equipment.txt"));
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
                ConsoleOutput.error("Could not load saved data: " + startupError);
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
                ConsoleOutput.error(e.getMessage());
            }
        } else if (normalizedInput.equals("list")) {
            equipment.viewAllEquipment();
        } else {
            ConsoleOutput.error("Command not recognised. Type help for available commands.");
        }
    }

    /** Displays complete equipment values without changing records or performing a save. */
    private void showEquipment(String input) {
        try {
            equipment.showEquipment(input);
        } catch (EquipmentViewingUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Runs the main input and execution loop until the user exits. */
    public void run() {
        if (startupError != null) {
            ConsoleOutput.error("Could not load saved data: " + startupError);
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

        ConsoleOutput.success("Goodbye.");
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
            ConsoleOutput.error("Could not save data: " + String.join(System.lineSeparator(), saveErrors));
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
            ConsoleOutput.error(exception.getMessage());
        }
    }

    // @@author

    /** Creates a backup and displays either its destination or the safe failure message. */
    private void createBackup(String input) {
        try {
            Path destination = backups.createBackup(input);
            ConsoleOutput.success("Backup created successfully at "
                    + ConsoleOutput.inline(destination.toString()) + ".");
        } catch (BackupUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Restores a validated backup and reports either the restored source or the safe failure message. */
    private void restoreBackup(String input) {
        try {
            Path source = backups.restoreBackup(input);
            startupError = null;
            ConsoleOutput.success("Backup restored successfully from "
                    + ConsoleOutput.inline(source.toString()) + ".");
        } catch (BackupUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Displays the outcome of a loan deletion. */
    private void deleteLoan(String input) {
        // @@author Rohita-29
        try {
            Loan deleted = loans.deleteLoan(input);
            ConsoleOutput.success("Loan " + deleted.getLoanId() + " deleted.");
            reportLoanItem(deleted);
        } catch (LoanDeletionUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
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
            ConsoleOutput.success("Equipment " + target.getItemId() + " \""
                    + ConsoleOutput.inline(target.getItemName()) + "\" deleted.");
        } catch (EquipmentDeletionUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
        // @@author
    }

    /** Creates a bundle and reports its assigned identifier. */
    private void addBundle(String input) {
        try {
            Bundle bundle = bundles.addBundle(input);
            ConsoleOutput.success("Bundle " + bundle.getBundleId() + " \""
                    + ConsoleOutput.inline(bundle.getBundleName()) + "\" created.");
            System.out.println("  Composition: " + BundleView.composition(bundle));
            BundleView.persistenceNote();
        } catch (BundleAdditionUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
    }

    /** Reports each equipment addition to a bundle independently. */
    private void addItemsToBundle(String input) {
        try {
            reportBundleResults(bundles.addItemsToBundleWithResults(input), "added");
        } catch (BundleAdditionUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
    }

    /** Reports the outcome of removing equipment from a persisted bundle. */
    private void removeItemsFromBundle(String input) {
        try {
            reportBundleResults(bundles.removeItemsFromBundleWithResults(input), "removed");
        } catch (BundleAdditionUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
    }

    /** Displays the outcome of equipment addition. */
    private void addEquipment(String input) {
        try {
            Equipment added = equipment.addEquipment(input);
            reportEquipment(added, "added");
        } catch (EquipmentAdditionUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
    }

    /** Displays the outcome of loan addition. */
    private void addLoan(String input) {
        // @@author Yi An
        try {
            Loan added = loans.addLoan(input);
            reportLoan(added, "issued");
        } catch (LoanAdditionUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
        // @@author
    }

    /** Displays a persisted return result or its validation/save error. */
    private void returnEquipment(String input) {
        try {
            Loan returned = loans.returnLoan(input);
            ConsoleOutput.success("Loan " + returned.getLoanId() + " returned.");
            reportLoanItem(returned);
        } catch (LoanReturnUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Displays the outcome of recording a maintenance activity. */
    private void recordMaintenance(String input) {
        try {
            MaintenanceRecord record = maintenance.addMaintenanceRecord(input);
            assert record != null : "A successful maintenance command must return a record";
            ConsoleOutput.success("Maintenance recorded successfully.");
            equipment.getEquipmentList().stream()
                    .filter(item -> item.getRecordId().equals(record.getEquipmentRecordId())).findFirst()
                    .ifPresent(item -> System.out.println("  Item: " + item.getItemId() + " \""
                            + ConsoleOutput.inline(item.getItemName()) + "\""));
            System.out.println("  Date: " + LoanView.date(record.getMaintenanceDate().atStartOfDay()));
            System.out.println("  Work: " + ConsoleOutput.inline(record.getWork()));
            System.out.println("  Outcome: " + ConsoleOutput.inline(record.getOutcome()));
        } catch (MaintenanceRecordUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Displays the selected equipment's borrowing history or its validation error. */
    private void viewLoanHistory(String input) {
        try {
            loans.viewLoanHistory(input);
        } catch (LoanHistoryUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Displays the selected bundle's active and returned loan history. */
    private void viewBundleLoanHistory(String input) {
        try {
            bundles.viewBundleLoanHistory(input, loans);
        } catch (BundleHistoryUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Displays the active equipment currently held by one borrower or its validation error. */
    private void viewActiveLoansForBorrower(String input) {
        try {
            loans.viewActiveLoansForBorrower(input);
        } catch (LoanBorrowerUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Displays matching equipment or the filter error. */
    private void filterEquipment(String input) {
        try {
            EquipmentFilter filter = EquipmentFilter.parse(input);
            List<Equipment> matches = filter.apply(equipment.getEquipmentList(), loans);
            equipment.viewEquipment(matches);
        } catch (EquipmentFilteringUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
    }

    /** Displays an update result while leaving validation and persistence to the handler. */
    private void updateEquipment(String input) {
        try {
            Equipment updated = equipment.updateEquipment(input);
            reportEquipment(updated, "updated");
        } catch (EquipmentUpdateUnsuccessful e) {
            ConsoleOutput.error(e.getMessage());
        }
    }

    /** Displays the outcome of a loan-detail update. */
    private void updateLoan(String input) {
        try {
            Loan updated = loans.updateLoan(input);
            reportLoan(updated, "updated");
        } catch (LoanUpdateUnsuccessful exception) {
            ConsoleOutput.error(exception.getMessage());
        }
    }

    /** Includes the current values only after the equipment handler has saved its successful change. */
    private static void reportEquipment(Equipment item, String action) {
        ConsoleOutput.success("Equipment " + item.getItemId() + " " + action + ".");
        System.out.println("  Name: \"" + ConsoleOutput.inline(item.getItemName()) + "\"");
        System.out.println("  Type: " + item.getType() + " | Condition: " + item.getCondition()
                + " | Status: " + availability(item));
    }

    /** Shows the assigned or updated loan details without requiring a separate listing request. */
    private void reportLoan(Loan loan, String action) {
        ConsoleOutput.success("Loan " + loan.getLoanId() + " " + action + ".");
        reportLoanItem(loan);
        System.out.println("  Borrower: " + loan.getBorrowerId() + " | Issued: "
                + LoanView.date(loan.getIssuedDate()) + " | Due: " + LoanView.date(loan.getDueDate()));
    }

    /** Avoids attaching a replacement item's name to old history when visible IDs have been reused. */
    private void reportLoanItem(Loan loan) {
        Equipment item = equipment.getEquipmentById(loan.getItemId());
        if (item != null && item.getRecordId().equals(loan.getEquipmentRecordId())) {
            System.out.println("  Item: " + item.getItemId() + " \""
                    + ConsoleOutput.inline(item.getItemName()) + "\"");
            System.out.println("  Status: " + availability(item));
        } else {
            System.out.println("  Item: " + loan.getItemId() + " (original equipment is not in the inventory)");
        }
    }

    /** Labels every membership result using the handler's outcome rather than interpreting its message text. */
    private void reportBundleResults(List<BundleHandler.ItemResult> results, String action) {
        for (BundleHandler.ItemResult result : results) {
            if (result.successful()) {
                ConsoleOutput.success(result.message() + " Name: \""
                        + ConsoleOutput.inline(result.itemName()) + "\".");
            } else {
                ConsoleOutput.error(result.message());
            }
        }
        long successes = results.stream().filter(BundleHandler.ItemResult::successful).count();
        System.out.println(successes + (successes == 1 ? " membership " : " memberships ") + action
                + "; " + (results.size() - successes) + " failed.");
        if (!results.isEmpty()) {
            String bundleId = results.getFirst().bundleId();
            bundles.getBundleList().stream().filter(bundle -> bundle.getBundleId().equals(bundleId)).findFirst()
                    .ifPresent(bundle -> System.out.println("  Composition: " + BundleView.composition(bundle)));
        }
    }

    /** Reads the existing loan and return-assessment flags without changing availability rules. */
    private static String availability(Equipment item) {
        return item.isLoaned() ? "Loaned" : item.isServiceable() ? "Available" : "Unserviceable";
    }

    // @@author Rohita-29

    /** Displays loans that are overdue as of the current local date. */
    private void viewOverdueLoans() {
        LocalDate referenceDate = LocalDate.now(clock);
        loans.viewOverdueLoans(referenceDate);
    }

    // @@author
}
