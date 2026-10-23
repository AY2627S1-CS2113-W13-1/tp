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
            "loan id/001 b/002 issued/01-10-26 due/02-10-26";

    private static final String SECOND_LOAN =
            "loan id/003 b/004 issued/03-10-26 due/04-10-26";

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EquipmentHandler equipment;
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

    @Test
    public void addLoan_validLoan_keepsCountConsistent() throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);

        loanHandler.addLoan(FIRST_LOAN);

        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(1, loanHandler.getLoanList().size());
        assertEquals("001", loanHandler.getLoanList().getFirst().getLoanId());
        assertEquals("001", equipment.getEquipmentList().getFirst().getLoanId());
        assertTrue(equipment.getEquipmentList().getFirst().isLoaned());
    }

    @Test
    void addLoan_unknownEquipment_preservesInventoryAvailability() {
        LoanHandler loanHandler = new LoanHandler(equipment);

        assertThrows(LoanAdditionUnsuccessful.class,
                () -> loanHandler.addLoan("loan id/999 b/002 issued/01-10-26 due/02-10-26"));

        assertEquals(0, loanHandler.getLoanCount());
        assertTrue(loanHandler.getLoanList().isEmpty());
        for (Equipment item : equipment.getEquipmentList()) {
            assertFalse(item.isLoaned());
        }
    }

    @Test
    void addLoan_zeroItemId_rejectsBeforeConstructionAndKeepsNextId() {
        LoanHandler loanHandler = new LoanHandler(equipment);

        assertThrows(LoanAdditionUnsuccessful.class,
                () -> loanHandler.addLoan("loan id/000 b/002 issued/01-10-26 due/02-10-26"));

        assertEquals("001", loanHandler.getNextLoanId());
        assertTrue(loanHandler.getLoanList().isEmpty());
        assertFalse(equipment.getEquipmentList().getFirst().isLoaned());
    }

    @Test
    void addLoan_zeroBorrowerId_acceptsAndAssignsLoanId() throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);

        Loan loan = loanHandler.addLoan("loan id/001 b/000 issued/01-10-26 due/02-10-26");

        assertEquals("000", loan.getBorrowerId());
        assertEquals("001", loan.getLoanId());
        assertEquals("001", equipment.getEquipmentList().getFirst().getLoanId());
        assertEquals("002", loanHandler.getNextLoanId());
    }

    @Test
    public void persistence_addDeleteAndReload_preservesLoansAcrossHandlers() throws Exception {
        Path file = temporaryDirectory.resolve("data/loans.txt");
        LoanHandler firstHandler = new LoanHandler(equipment, file);
        firstHandler.addLoan(FIRST_LOAN);

        EquipmentHandler secondInventory = createInventory();
        LoanHandler secondHandler = new LoanHandler(secondInventory, file);
        secondHandler.loadFromStorage();
        assertEquals(1, secondHandler.getLoanCount());
        assertEquals("001", secondHandler.getLoanList().getFirst().getItemId());
        assertEquals("002", secondHandler.getLoanList().getFirst().getBorrowerId());
        assertEquals("001", secondHandler.getLoanList().getFirst().getLoanId());
        assertEquals("001", secondInventory.getEquipmentList().getFirst().getLoanId());
        assertTrue(secondInventory.getEquipmentList().getFirst().isLoaned());

        secondHandler.deleteLoan("delete loan/001");
        LoanHandler thirdHandler = new LoanHandler(createInventory(), file);
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
                + "loan id/001 b/002 issued/01-10-26 due/02-10-26" + System.lineSeparator());

        List<Loan> loaded = LoanStorage.load(file);

        assertEquals(1, loaded.size());
        assertEquals("001", loaded.getFirst().getItemId());
    }

    @Test
    void deleteEquipment_activeLoan_blocksDeletionAndIdReuse() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/1");
        manager.executeInput("add i/Tripod id/002 type/tripod cond/5");
        manager.executeInput(FIRST_LOAN);
        output.reset();

        manager.executeInput("delete item/001");
        manager.executeInput("add i/Replacement id/001 type/battery cond/3");
        manager.executeInput("delete item/002");

        assertEquals("Cannot delete item ID 001 while it has an active loan." + System.lineSeparator()
                + "Item ID 001 already exists." + System.lineSeparator()
                + "Equipment with item ID 002 deleted successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                .contains("loan loanId/001 id/001 b/002 issued/01-10-26 due/02-10-26 returned/false"));
    }

    @Test
    void deleteEquipment_returnedLoan_allowsDeletionAndIdReuse() throws Exception {
        writeLoanRecords("loan id/001 b/002 issued/01-10-26 due/02-10-26 returned/true");
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/1");
        output.reset();

        manager.executeInput("delete item/001");
        manager.executeInput("add i/Replacement id/001 type/battery cond/3");

        assertEquals("Equipment with item ID 001 deleted successfully." + System.lineSeparator()
                + "Equipment added successfully." + System.lineSeparator(), output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                .contains("loan loanId/001 id/001 b/002 issued/01-10-26 due/02-10-26 returned/true"));
    }

    @Test
    void deleteEquipment_olderReturnedLoanAndNewActiveLoan_stillBlocksDeletion()
            throws Exception {
        writeLoanRecords("loan id/001 b/002 issued/01-10-26 due/02-10-26 returned/true",
                "loan id/001 b/003 issued/03-10-26 due/04-10-26 returned/false");
        Files.writeString(temporaryDirectory.resolve("equipment.txt"),
                "add i/Camera id/001 type/camera cond/1\n");
        EqmManager manager = new EqmManager(temporaryDirectory);
        output.reset();

        manager.executeInput("delete item/001");

        assertEquals("Cannot delete item ID 001 while it has an active loan." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertEquals(2, Files.readAllLines(temporaryDirectory.resolve("loans.txt")).size());
    }

    @Test
    void deleteEquipment_loanDeleted_allowsDeletionAndIdReuse() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/1");
        manager.executeInput(FIRST_LOAN);
        output.reset();

        manager.executeInput("delete loan/001");
        manager.executeInput("delete item/001");
        manager.executeInput("add i/Replacement id/001 type/battery cond/3");

        assertEquals("Loan 001 for item ID 001 deleted successfully." + System.lineSeparator()
                + "Equipment with item ID 001 deleted successfully." + System.lineSeparator()
                + "Equipment added successfully." + System.lineSeparator(), output.toString(StandardCharsets.UTF_8));
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
        inventory.addEquipment("add i/Camera id/001 type/camera cond/1");
        inventory.addEquipment("add i/Tripod id/003 type/tripod cond/2");
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
                loanHandler.deleteLoan("delete loan/001");

        assertEquals("001", deletedLoan.getItemId());
        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(
                "003",
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
                        "delete loan/999"));

        assertEquals(
                "No loan found for loan ID 999.",
                exception.getMessage());
        assertEquals(1, loanHandler.getLoanCount());
        assertEquals(
                "001",
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
                        "delete loan/01"));

        assertEquals(
                "Invalid loan ID \"01\". Loan ID must be exactly three digits from 001 to 999.",
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
                () -> loanHandler.deleteLoan("delete loan"));

        assertEquals(
                "Invalid command format. Expected: delete loan/[LOAN ID].",
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
                        "delete loan item/001"));

        assertEquals(1, loanHandler.getLoanCount());
    }

    @Test
    public void deleteLoan_sameItemHistory_usesUniqueLoanIds()
            throws LoanAdditionUnsuccessful, LoanDeletionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        Loan history = loanHandler.addLoan(FIRST_LOAN);
        history.setReturned(true);
        Loan active = loanHandler.addLoan("loan id/001 b/005 issued/05-10-26 due/06-10-26");
        assertEquals("001", history.getLoanId());
        assertEquals("002", active.getLoanId());

        assertSame(history, loanHandler.deleteLoan("delete loan/001"));
        assertEquals(List.of(active), loanHandler.getLoanList());
        assertTrue(equipment.getEquipmentList().getFirst().isLoaned());

        Loan deletedLoan = loanHandler.deleteLoan("delete loan/002");
        assertSame(active, deletedLoan);
        assertEquals("005", deletedLoan.getBorrowerId());
        assertEquals(0, loanHandler.getLoanCount());
        assertFalse(equipment.getEquipmentList().getFirst().isLoaned());
    }

    @Test
    void deleteLoan_invalidCommands_preserveSavedRecordsAndAvailability() throws Exception {
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, file);
        Loan original = handler.addLoan(FIRST_LOAN);
        String saved = Files.readString(file);
        for (String command : new String[]{null, "", "delete", "delete loan", "delete loan/",
            "delete loan/000", "delete loan/01", "delete loan/999", "delete loan/001 extra",
            "delete-loan id/001", "delete loan/001 issued/31-02-26",
            "delete loan/001 issued/03-10-26", "delete loan/001 issued/01-10-26 extra"}) {
            assertThrows(LoanDeletionUnsuccessful.class, () -> handler.deleteLoan(command), command);
            assertEquals(List.of(original), handler.getLoanList());
            assertEquals(1, handler.getLoanCount());
            assertTrue(equipment.getEquipmentList().getFirst().isLoaned());
            assertEquals(saved, Files.readString(file));
        }
    }

    @Test
    void viewAllLoans_noLoans_printsHeaderOnly() {
        new LoanHandler(equipment).viewAllLoans();

        String expected = String.format(
                "%-4s %-12s | %-12s | %-12s | %-16s | %-16s%n",
                "No.", "LOAN ID", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE");
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewAllLoans_existingLoans_printsHeaderAndRows()
            throws LoanAdditionUnsuccessful {
        LoanHandler loanHandler = new LoanHandler(equipment);
        loanHandler.addLoan(FIRST_LOAN);
        loanHandler.addLoan(SECOND_LOAN);

        loanHandler.viewAllLoans();

        String expected = String.format(
                "%-4s %-12s | %-12s | %-12s | %-16s | %-16s%n",
                "No.", "LOAN ID", "ITEM ID", "BORROWER ID", "ISSUE DATE", "DUE DATE");
        expected += String.format(
                "%-4s %-12s | %-12s | %-12s | %-16s | %-16s%n",
                "1)", "001", "001", "002", "2026-10-01T00:00", "2026-10-02T00:00");
        expected += String.format(
                "%-4s %-12s | %-12s | %-12s | %-16s | %-16s%n",
                "2)", "002", "003", "004", "2026-10-03T00:00", "2026-10-04T00:00");

        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }
}
