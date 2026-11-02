package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Tests loan IDs, availability and deletion of individual loan records. */
public class LoanHandlerTest {
    private static final String FIRST_LOAN =
            // @@author yarnieee
            "loan-add e/E001 u/U002 issued/01-10-26 due/02-10-26";
    // @@author

    private static final String SECOND_LOAN =
            // @@author yarnieee
            "loan-add e/E003 u/U004 issued/03-10-26 due/04-10-26";
    // @@author

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    // @@author yarnieee
    private EquipmentHandler equipment;
    // @@author
    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    public void redirectOutput() throws EquipmentAdditionUnsuccessful {
        equipment = createInventory();
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void restoreOutput() {
        System.setOut(originalOutput);
    }

    // @@author yarnieee
    @Test
    public void addLoan_validLoan_keepsCountConsistent() throws LoanAdditionUnsuccessful {
        // @@author
        LoanHandler loanHandler = new LoanHandler(equipment);
        // @@author yarnieee

        loanHandler.addLoan(FIRST_LOAN);
        // @@author

        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(1, loanHandler.getLoanList().size());
        // @@author yarnieee
        assertEquals("L001", loanHandler.getLoanList().getFirst().getLoanId());
        assertEquals("L001", equipment.getEquipmentList().getFirst().getLoanId());
        // @@author
        assertTrue(equipment.getEquipmentList().getFirst().isLoaned());
    }

    // @@author yarnieee
    @Test
    void addLoan_lowercaseIds_normalisesStoredValues() throws LoanAdditionUnsuccessful {
        // @@author
        LoanHandler loanHandler = new LoanHandler(equipment);
        // @@author yarnieee

        Loan loan = loanHandler.addLoan("loan-add e/e001 u/u002 issued/01-10-26 due/02-10-26");

        assertEquals("L001", loan.getLoanId());
        assertEquals("E001", loan.getItemId());
        assertEquals("U002", loan.getBorrowerId());
    }

    // @@author
    @Test
    void addLoan_unknownEquipment_preservesInventoryAvailability() {
        LoanHandler loanHandler = new LoanHandler(equipment);

        assertThrows(LoanAdditionUnsuccessful.class,
                // @@author yarnieee
                () -> loanHandler.addLoan("loan-add e/E999 u/U002 issued/01-10-26 due/02-10-26"));
        // @@author

        assertEquals(0, loanHandler.getLoanCount());
        assertTrue(loanHandler.getLoanList().isEmpty());
        for (Equipment item : equipment.getEquipmentList()) {
            assertFalse(item.isLoaned());
        }
        // @@author yarnieee
    }

    // @@author
    @Test
    void addLoan_zeroItemId_rejectsBeforeConstructionAndKeepsNextId() {
        LoanHandler loanHandler = new LoanHandler(equipment);

        assertThrows(LoanAdditionUnsuccessful.class,
                // @@author yarnieee
                () -> loanHandler.addLoan("loan-add e/E000 u/U002 issued/01-10-26 due/02-10-26"));

        assertEquals("L001", loanHandler.getNextLoanId());
        // @@author
        assertTrue(loanHandler.getLoanList().isEmpty());
        assertFalse(equipment.getEquipmentList().getFirst().isLoaned());
    }

    // @@author yarnieee
    @Test
    void addLoan_rejectsBareAndWrongTypeIdentifiers() {
        // @@author
        LoanHandler loanHandler = new LoanHandler(equipment);
        // @@author yarnieee
        String[] commands = {
            "loan-add e/001 u/U002 issued/01-10-26 due/02-10-26",
            "loan-add e/B001 u/U002 issued/01-10-26 due/02-10-26",
            "loan-add e/E001 u/001 issued/01-10-26 due/02-10-26",
            "loan-add e/E001 u/L001 issued/01-10-26 due/02-10-26"
        };

        for (String command : commands) {
            assertThrows(LoanAdditionUnsuccessful.class, () -> loanHandler.addLoan(command), command);
        }
        // @@author
        assertTrue(loanHandler.getLoanList().isEmpty());
        // @@author yarnieee
        assertEquals("L001", loanHandler.getNextLoanId());
    }

    // @@author
    @Test
    void addLoan_zeroBorrowerId_acceptsAndAssignsLoanId() throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);

        // @@author yarnieee
        Loan loan = loanHandler.addLoan("loan-add e/E001 u/U000 issued/01-10-26 due/02-10-26");

        assertEquals("U000", loan.getBorrowerId());
        assertEquals("L001", loan.getLoanId());
        assertEquals("L001", equipment.getEquipmentList().getFirst().getLoanId());
        assertEquals("L002", loanHandler.getNextLoanId());
    }

    // @@author
    @Test
    public void persistence_addDeleteAndReload_preservesLoansAcrossHandlers() throws Exception {
        Path file = temporaryDirectory.resolve("data/loans.txt");
        Path equipmentFile = temporaryDirectory.resolve("data/equipment.txt");
        EquipmentHandler firstInventory = new EquipmentHandler(equipmentFile);
        // @@author yarnieee
        firstInventory.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        firstInventory.addEquipment("eqm-add name/Tripod e/E003 type/tripod cond/2");
        // @@author
        LoanHandler firstHandler = new LoanHandler(firstInventory, file);
        firstHandler.addLoan(FIRST_LOAN);

        // A restart reloads the same physical items instead of constructing replacements with reused IDs.
        EquipmentHandler secondInventory = new EquipmentHandler(equipmentFile);
        secondInventory.loadFromStorage();
        LoanHandler secondHandler = new LoanHandler(secondInventory, file);
        secondHandler.loadFromStorage();
        assertEquals(1, secondHandler.getLoanCount());
        // @@author yarnieee
        assertEquals("E001", secondHandler.getLoanList().getFirst().getItemId());
        assertEquals("U002", secondHandler.getLoanList().getFirst().getBorrowerId());
        assertEquals("L001", secondHandler.getLoanList().getFirst().getLoanId());
        assertEquals("L001", secondInventory.getEquipmentList().getFirst().getLoanId());
        // @@author
        assertTrue(secondInventory.getEquipmentList().getFirst().isLoaned());
        assertEquals(firstInventory.getEquipmentList().getFirst().getRecordId(),
                secondHandler.getLoanList().getFirst().getEquipmentRecordId());

        // @@author yarnieee
        secondHandler.deleteLoan("loan-delete l/L001");
        // @@author
        EquipmentHandler thirdInventory = new EquipmentHandler(equipmentFile);
        thirdInventory.loadFromStorage();
        LoanHandler thirdHandler = new LoanHandler(thirdInventory, file);
        thirdHandler.loadFromStorage();
        assertEquals(0, thirdHandler.getLoanCount());
    }

    @Test
    public void saveToStorage_persistsInMemoryLoanStatus() throws Exception {
        Path file = temporaryDirectory.resolve("data/loans.txt");
        LoanHandler loanHandler = new LoanHandler(equipment, file);
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.getLoanList().getFirst().setReturned(true);

        loanHandler.saveToStorage();

        LoanHandler reloadedHandler = new LoanHandler(createInventory(), file);
        reloadedHandler.loadFromStorage();
        assertTrue(reloadedHandler.getLoanList().getFirst().isReturned());
    }

    @Test
    public void storage_blankLines_areIgnoredDuringLoad() throws Exception {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, System.lineSeparator()
                + "  " + System.lineSeparator()
                // @@author yarnieee
                + "loan id/E001 b/U002 issued/01-10-26 due/02-10-26" + System.lineSeparator());
        // @@author

        List<Loan> loaded = LoanStorage.load(file);

        assertEquals(1, loaded.size());
        // @@author yarnieee
        assertEquals("E001", loaded.getFirst().getItemId());
        // @@author
    }

    @Test
    void deleteEquipment_activeLoan_blocksDeletionAndIdReuse() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        // @@author yarnieee
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
        manager.executeInput("eqm-add name/Tripod e/E002 type/tripod cond/5");
        // @@author
        manager.executeInput(FIRST_LOAN);
        output.reset();

        // @@author yarnieee
        manager.executeInput("eqm-delete e/E001");
        manager.executeInput("eqm-add name/Replacement e/E001 type/battery cond/3");
        manager.executeInput("eqm-delete e/E002");

        assertEquals("[ERROR] Cannot delete item ID E001 while it has an active loan." + System.lineSeparator()
                + "[ERROR] Item ID E001 already exists." + System.lineSeparator()
                + "[OK] Equipment E002 \"Tripod\" deleted." + System.lineSeparator(),
                // @@author
                output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                // @@author yarnieee
                .contains("loan loanId/L001 id/E001 b/U002 issued/01-10-26 due/02-10-26 returned/false"));
        // @@author
    }

    @Test
    void deleteEquipment_returnedLoan_allowsDeletionAndIdReuse() throws Exception {
        // @@author yarnieee
        writeLoanRecords("loan id/E001 b/U002 issued/01-10-26 due/02-10-26 returned/true");
        // @@author
        EqmManager manager = new EqmManager(temporaryDirectory);
        // @@author yarnieee
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author
        output.reset();

        // @@author yarnieee
        manager.executeInput("eqm-delete e/E001");
        manager.executeInput("eqm-add name/Replacement e/E001 type/battery cond/3");

        assertEquals("[OK] Equipment E001 \"Camera\" deleted." + System.lineSeparator()
                + "[OK] Equipment E001 added." + System.lineSeparator()
                // @@author
                + "  Name: \"Replacement\"" + System.lineSeparator()
                + "  Type: battery | Condition: 3 | Status: Available" + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                // @@author yarnieee
                .contains("loan loanId/L001 id/E001 b/U002 issued/01-10-26 due/02-10-26 returned/true"));
        // @@author
    }

    @Test
    void deleteEquipment_olderReturnedLoanAndNewActiveLoan_stillBlocksDeletion()
            throws Exception {
        // @@author yarnieee
        writeLoanRecords("loan id/E001 b/U002 issued/01-10-26 due/02-10-26 returned/true",
                "loan id/E001 b/U003 issued/03-10-26 due/04-10-26 returned/false");
        // @@author
        Files.writeString(temporaryDirectory.resolve("equipment.txt"),
                // @@author yarnieee
                "add i/Camera id/E001 type/camera cond/1\n");
        // @@author
        EqmManager manager = new EqmManager(temporaryDirectory);
        output.reset();

        // @@author yarnieee
        manager.executeInput("eqm-delete e/E001");

        assertEquals("[ERROR] Cannot delete item ID E001 while it has an active loan." + System.lineSeparator(),
                // @@author
                output.toString(StandardCharsets.UTF_8));
        assertEquals(2, Files.readAllLines(temporaryDirectory.resolve("loans.txt")).size());
    }

    @Test
    void deleteEquipment_loanDeleted_allowsDeletionAndIdReuse() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        // @@author yarnieee
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author
        manager.executeInput(FIRST_LOAN);
        output.reset();

        // @@author yarnieee
        manager.executeInput("loan-delete l/L001");
        manager.executeInput("eqm-delete e/E001");
        manager.executeInput("eqm-add name/Replacement e/E001 type/battery cond/3");

        assertEquals("[OK] Loan L001 deleted." + System.lineSeparator()
                + "  Item: E001 \"Camera\"" + System.lineSeparator()
                // @@author
                + "  Status: Available" + System.lineSeparator()
                // @@author yarnieee
                + "[OK] Equipment E001 \"Camera\" deleted." + System.lineSeparator()
                + "[OK] Equipment E001 added." + System.lineSeparator()
                // @@author
                + "  Name: \"Replacement\"" + System.lineSeparator()
                + "  Type: battery | Condition: 3 | Status: Available" + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt")).isBlank());
    }

    /** Writes loan records in the same format used by the persistence layer. */
    private void writeLoanRecords(String... records) throws Exception {
        Files.writeString(temporaryDirectory.resolve("loans.txt"),
                String.join(System.lineSeparator(), records) + System.lineSeparator());
    }

    /** Creates the equipment required by the loan fixtures without sharing availability between tests. */
    private EquipmentHandler createInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler inventory = new EquipmentHandler();
        // @@author yarnieee
        inventory.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        inventory.addEquipment("eqm-add name/Tripod e/E003 type/tripod cond/2");
        // @@author
        return inventory;
    }

    @Test
    public void deleteLoan_existingLoanId_deletesCorrectLoan()
            throws LoanAdditionUnsuccessful,
            LoanDeletionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.addLoan(SECOND_LOAN);

        Loan deletedLoan =
                // @@author yarnieee
                loanHandler.deleteLoan("loan-delete l/L001");

        assertEquals("E001", deletedLoan.getItemId());
        // @@author
        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(
                // @@author yarnieee
                "E003",
                // @@author
                loanHandler.getLoanList().get(0).getItemId());
    }

    @Test
    public void deleteLoan_unknownLoanId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        // @@author yarnieee
                        "loan-delete l/L999"));
        // @@author

        assertEquals(
                // @@author yarnieee
                "No loan found for loan ID L999.",
                // @@author
                exception.getMessage());
        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(
                // @@author yarnieee
                "E001",
                // @@author
                loanHandler.getLoanList().get(0).getItemId());
    }

    @Test
    public void deleteLoan_invalidLoanId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        // @@author yarnieee
                        "loan-delete l/01"));
        // @@author

        assertEquals(
                // @@author yarnieee
                "Invalid loan ID \"01\". Loan ID must be exactly L001 to L999.",
                // @@author
                exception.getMessage());
        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    public void deleteLoan_missingLoanId_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);

        LoanDeletionUnsuccessful exception = assertThrows(
                LoanDeletionUnsuccessful.class,
                // @@author yarnieee
                () -> loanHandler.deleteLoan("loan-delete"));
                // @@author

        assertEquals(
                // @@author yarnieee
                "Invalid command format. Expected: loan-delete l/[LOAN ID].",
                // @@author
                exception.getMessage());
        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    public void deleteLoan_invalidPrefix_keepsLoansUnchanged()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);

        assertThrows(
                LoanDeletionUnsuccessful.class,
                () -> loanHandler.deleteLoan(
                        // @@author yarnieee
                        "eqm-delete e/001"));
        // @@author

        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    public void deleteLoan_sameItemHistory_usesUniqueLoanIds()
            throws LoanAdditionUnsuccessful, LoanDeletionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        Loan history = loanHandler.addLoan(FIRST_LOAN);
        history.setReturned(true);
        // @@author yarnieee
        Loan active = loanHandler.addLoan("loan-add e/E001 u/U005 issued/05-10-26 due/06-10-26");
        assertEquals("L001", history.getLoanId());
        assertEquals("L002", active.getLoanId());

        assertSame(history, loanHandler.deleteLoan("loan-delete l/L001"));
        // @@author
        assertEquals(List.of(active), loanHandler.getLoanList());
        assertTrue(equipment.getEquipmentList().getFirst().isLoaned());

        // @@author yarnieee
        Loan deletedLoan = loanHandler.deleteLoan("loan-delete l/L002");
        // @@author
        assertSame(active, deletedLoan);
        // @@author yarnieee
        assertEquals("U005", deletedLoan.getBorrowerId());
        // @@author
        assertEquals(0, loanHandler.getLoanCount());
        assertFalse(equipment.getEquipmentList().getFirst().isLoaned());
    }

    @Test
    void deleteLoan_invalidCommands_preserveSavedRecordsAndAvailability() throws Exception {
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, file);
        Loan original = handler.addLoan(FIRST_LOAN);
        String saved = Files.readString(file);
        // @@author yarnieee
        for (String command : new String[]{null, "", "delete", "loan-delete", "loan-delete l/",
            "loan-delete l/L000", "loan-delete l/01", "loan-delete l/L999", "loan-delete l/L001 extra",
            "delete-loan id/E001", "loan-delete l/L001 issued/31-02-26",
            "loan-delete l/L001 issued/03-10-26", "loan-delete l/L001 issued/01-10-26 extra"}) {
            // @@author
            assertThrows(LoanDeletionUnsuccessful.class, () -> handler.deleteLoan(command), command);
            assertEquals(List.of(original), handler.getLoanList());
            assertEquals(1, handler.getLoanCount());
            assertTrue(equipment.getEquipmentList().getFirst().isLoaned());
            assertEquals(saved, Files.readString(file));
        }
    }

    // @@author Rohita-29

    @Test
    void getOverdueLoans_mixedLoans_returnsOnlyActivePastDueLoans() throws Exception {
        LoanHandler loanHandler = new LoanHandler(equipment);
        Loan returnedLoan = loanHandler.addLoan(FIRST_LOAN);
        returnedLoan.setReturned(true);
        Loan overdueLoan = loanHandler.addLoan(SECOND_LOAN);

        equipment.addEquipment("eqm-add name/Battery e/E005 type/battery cond/3");
        Loan futureLoan = loanHandler.addLoan(
                "loan-add e/E005 u/U006 issued/05-10-26 due/06-10-26");

        List<Loan> originalLoans = List.copyOf(loanHandler.getLoanList());
        List<Loan> overdueLoans = loanHandler.getOverdueLoans(
                LocalDate.of(2026, 10, 5));

        assertEquals(List.of(overdueLoan), overdueLoans);
        assertEquals(originalLoans, loanHandler.getLoanList());
        assertTrue(returnedLoan.isReturned());
        assertFalse(futureLoan.isOverdue(LocalDate.of(2026, 10, 5)));
    }

    @Test
    void getOverdueLoans_multipleMatches_preservesStoredOrder() throws Exception {
        LoanHandler loanHandler = new LoanHandler(equipment);
        Loan firstLoan = loanHandler.addLoan(FIRST_LOAN);
        Loan secondLoan = loanHandler.addLoan(SECOND_LOAN);

        List<Loan> overdueLoans = loanHandler.getOverdueLoans(
                LocalDate.of(2026, 10, 5));

        assertEquals(List.of(firstLoan, secondLoan), overdueLoans);
    }

    @Test
    void getOverdueLoans_nullReferenceDate_throwsNullPointerException() {
        LoanHandler loanHandler = new LoanHandler(equipment);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> loanHandler.getOverdueLoans(null));

        assertEquals("Reference date must not be null", exception.getMessage());
    }

    @Test
    void getOverdueLoans_returnedList_cannotModifyHandlerLoans() throws Exception {
        LoanHandler loanHandler = new LoanHandler(equipment);
        Loan overdueLoan = loanHandler.addLoan(FIRST_LOAN);
        List<Loan> overdueLoans = loanHandler.getOverdueLoans(
                LocalDate.of(2026, 10, 3));

        assertThrows(UnsupportedOperationException.class, overdueLoans::clear);
        assertEquals(List.of(overdueLoan), loanHandler.getLoanList());
        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    void viewOverdueLoans_noMatches_printsClearMessage() throws Exception {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);
        output.reset();

        loanHandler.viewOverdueLoans(LocalDate.of(2026, 10, 1));

        assertEquals(
                "Overdue loans" + System.lineSeparator() + "-------------" + System.lineSeparator()
                        + "No overdue loans found." + System.lineSeparator() + "0 loans." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewOverdueLoans_matchingLoan_printsRequiredColumns() throws Exception {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);
        output.reset();

        loanHandler.viewOverdueLoans(LocalDate.of(2026, 10, 3));

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("LOAN ID"));
        assertTrue(actual.contains("BORROWER ID"));
        assertTrue(actual.contains("| L001    | E001    | U002        | 01-10-26 | 02-10-26 | Overdue  |"));
        assertTrue(actual.endsWith("1 loan." + System.lineSeparator()));
    }

    // @@author

    @Test
    void viewAllLoans_noLoans_printsGuidanceAndZeroCount() {
        new LoanHandler(equipment).viewAllLoans();

        String actual = output.toString(StandardCharsets.UTF_8);
        // @@author yarnieee
        assertTrue(actual.contains("No loans recorded. Type help loan-list for the command format."));
        // @@author
        assertTrue(actual.endsWith("0 loans." + System.lineSeparator()));
    }

    @Test
    void viewAllLoans_existingLoans_printsHeaderAndRows()
            throws LoanAdditionUnsuccessful {
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T00:00:00Z"), ZoneOffset.UTC);
        LoanHandler loanHandler = new LoanHandler(equipment, null, null, clock);
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.addLoan(SECOND_LOAN);

        loanHandler.viewAllLoans();

        String actual = output.toString(StandardCharsets.UTF_8);
        // @@author yarnieee
        String first = "| L001    | E001    | U002        | 01-10-26 | 02-10-26 | Overdue  |";
        String second = "| L002    | E003    | U004        | 03-10-26 | 04-10-26 | Overdue  |";
        // @@author
        assertTrue(actual.contains(first));
        assertTrue(actual.contains(second));
        assertTrue(actual.indexOf(first) < actual.indexOf(second));
        assertTrue(actual.endsWith("2 loans." + System.lineSeparator()));
    }
}
