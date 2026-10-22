package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Checks availability restoration and unchanged state when persistence operations fail. */
class EquipmentAvailabilityPersistenceTest {
    private static final String COMMAND = "loan id/001 b/002 issued/01-10-26 due/02-10-26";
    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadFromStorage_activeAndReturnedLoans_restoreAvailabilityAndPreventReactivation() throws Exception {
        EquipmentHandler inventory = inventory();
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(inventory, file);
        handler.addLoan(COMMAND).setReturned(true);
        handler.addLoan(COMMAND.replace("01-10-26", "02-10-26"));
        handler.saveToStorage();
        EquipmentHandler restoredInventory = inventory();
        LoanHandler restored = new LoanHandler(restoredInventory, file);
        restored.loadFromStorage();
        Equipment item = restoredInventory.getEquipmentList().getFirst();
        assertTrue(item.isLoaned());
        assertEquals(2, restored.getLoanCount());
        Loan returned = restored.getLoanList().getFirst();
        assertThrows(IllegalStateException.class, () -> returned.setReturned(false));
        restored.deleteLoan("delete-loan id/001 issued/01-10-26");
        assertTrue(item.isLoaned());
        restored.loadFromStorage();
        assertTrue(item.isLoaned());
        restored.deleteLoan("delete-loan id/001");
        assertFalse(item.isLoaned());
    }

    @Test
    void addLoan_failedSave_leavesEquipmentAvailableAndAllowsRecovery() throws Exception {
        EquipmentHandler inventory = inventory();
        Path file = blockedFile();
        LoanHandler handler = new LoanHandler(inventory, file);
        assertThrows(LoanAdditionUnsuccessful.class, () -> handler.addLoan(COMMAND));
        assertFalse(inventory.getEquipmentList().getFirst().isLoaned());
        assertTrue(handler.getLoanList().isEmpty());
        Files.delete(file.resolve("keep.txt"));
        Files.delete(file);
        handler.addLoan(COMMAND);
        assertTrue(inventory.getEquipmentList().getFirst().isLoaned());
    }

    @Test
    void deleteLoan_failedSave_preservesLoanAndEquipmentReservation() throws Exception {
        EquipmentHandler inventory = inventory();
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(inventory, file);
        Loan original = handler.addLoan(COMMAND);
        Files.delete(file);
        blockedFile();
        assertThrows(LoanDeletionUnsuccessful.class, () -> handler.deleteLoan("delete-loan id/001"));
        assertEquals(List.of(original), handler.getLoanList());
        assertTrue(inventory.getEquipmentList().getFirst().isLoaned());
        assertEquals("keep", Files.readString(file.resolve("keep.txt")));
    }

    @Test
    void loadFromStorage_missingActiveEquipment_preservesExistingRecordsAndFlags() throws Exception {
        EquipmentHandler inventory = inventory();
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(inventory, file);
        Loan original = handler.addLoan(COMMAND);
        Files.writeString(file, COMMAND.replace("id/001", "id/999"));
        assertThrows(IOException.class, handler::loadFromStorage);
        assertSame(original, handler.getLoanList().getFirst());
        assertTrue(inventory.getEquipmentList().getFirst().isLoaned());
        assertEquals(1, handler.getLoanCount());
    }

    @Test
    void loadFromStorage_returnedHistoryWithoutEquipment_remainsHistorical() throws Exception {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, COMMAND + " returned/true");
        LoanHandler handler = new LoanHandler(new EquipmentHandler(), file);
        handler.loadFromStorage();
        assertTrue(handler.getLoanList().getFirst().isReturned());
        assertThrows(IllegalStateException.class, () -> handler.getLoanList().getFirst().setReturned(false));
    }

    @Test
    void loadFromStorage_duplicateActiveRecords_preservesCurrentReservation() throws Exception {
        EquipmentHandler inventory = inventory();
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(inventory, file);
        Loan original = handler.addLoan(COMMAND);
        Files.writeString(file, COMMAND + System.lineSeparator() + COMMAND);
        assertThrows(IOException.class, handler::loadFromStorage);
        assertEquals(List.of(original), handler.getLoanList());
        assertTrue(inventory.getEquipmentList().getFirst().isLoaned());
    }

    @Test
    void loadFromStorage_equipmentReservedElsewhere_doesNotOverwriteItsStatus() throws Exception {
        EquipmentHandler inventory = inventory();
        new LoanHandler(inventory).addLoan(COMMAND);
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, COMMAND);
        LoanHandler handler = new LoanHandler(inventory, file);
        assertThrows(IOException.class, handler::loadFromStorage);
        assertTrue(handler.getLoanList().isEmpty());
        assertTrue(inventory.getEquipmentList().getFirst().isLoaned());
    }

    @Test
    void loadFromStorage_inMemoryHandler_keepsCurrentRecordsAndFlags() throws Exception {
        EquipmentHandler inventory = inventory();
        LoanHandler handler = new LoanHandler(inventory);
        Loan original = handler.addLoan(COMMAND);
        handler.loadFromStorage();
        assertEquals(List.of(original), handler.getLoanList());
        assertTrue(inventory.getEquipmentList().getFirst().isLoaned());
    }

    /** Creates an isolated inventory containing the equipment used by these loan records. */
    private EquipmentHandler inventory() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        inventory.addEquipment("add i/Camera id/001 type/camera cond/1");
        return inventory;
    }

    /** Makes replacement of the loan file fail without relying on operating-system permission settings. */
    private Path blockedFile() throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.createDirectory(file);
        Files.writeString(file.resolve("keep.txt"), "keep");
        return file;
    }
}
