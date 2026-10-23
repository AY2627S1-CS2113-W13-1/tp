package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.exceptions.LoanDeletionUnsuccessful;

/** Tests loan storage validation and failure-safe file replacement. */
class LoanStorageTest {
    private static final String LOAN = "loan id/001 b/002 issued/01-10-26 due/02-10-26";
    @TempDir
    private Path temporaryDirectory;

    @Test
    void load_malformedRecord_reportsTheLineNumber() throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, "loan id/001 b/002 issued/01-10-26 due/02-10-26\nnot a loan record\n");

        IOException exception = assertThrows(IOException.class, () -> LoanStorage.load(file));

        assertTrue(exception.getMessage().contains("line 2"));
    }

    @Test
    void load_invalidReturnedStatus_rejectsTheSavedFile() throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, "loan id/001 b/002 issued/01-10-26 due/02-10-26 returned/maybe\n");

        IOException exception = assertThrows(IOException.class, () -> LoanStorage.load(file));

        assertTrue(exception.getMessage().contains("returned/true or returned/false"));
    }

    @Test
    void load_returnedHistoryForDeletedAndReusedIds_attachesOnlyTheActiveLoan() throws Exception {
        EquipmentHandler equipment = inventoryWithItems("001");
        Equipment replacement = equipment.getEquipmentList().getFirst();
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file,
                "loan id/001 b/002 issued/01-09-26 due/02-09-26 returned/true\n"
                + "loan id/999 b/003 issued/01-09-26 due/02-09-26 returned/true\n" + LOAN + "\n");
        LoanHandler handler = new LoanHandler(equipment, file);

        handler.loadFromStorage();

        assertEquals(3, handler.getLoanCount());
        assertTrue(replacement.isLoaned());
        Loan history = handler.getLoanList().getFirst();
        assertTrue(history.isReturned());
        history.setReturned(true);
        assertThrows(IllegalStateException.class, () -> history.setReturned(false));
        handler.deleteLoan("delete loan/001");
        assertTrue(replacement.isLoaned());
        assertEquals(2, handler.getLoanCount());
        assertTrue(Files.readString(file).contains("id/999"));
        assertTrue(Files.readString(file).contains(
                "loan loanId/003 id/001 b/002 issued/01-10-26 due/02-10-26 returned/false"));
    }

    @Test
    void load_invalidActiveRecords_preservesCurrentLoansAndAvailability() throws Exception {
        EquipmentHandler equipment = inventoryWithItems("001", "002");
        Equipment camera = equipment.getEquipmentList().get(0);
        Equipment spare = equipment.getEquipmentList().get(1);
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, file);
        Loan original = handler.addLoan(LOAN);
        String other = LOAN.replace("id/001", "id/002");
        String[] invalidRecords = {
            other + "\n" + LOAN.replace("id/001", "id/999") + "\n",
            other + "\n" + other + "\n"
        };

        for (String records : invalidRecords) {
            Files.writeString(file, records);
            assertThrows(IOException.class, handler::loadFromStorage);
            assertEquals(List.of(original), handler.getLoanList());
            assertEquals(1, handler.getLoanCount());
            assertTrue(camera.isLoaned());
            assertFalse(spare.isLoaned());
        }

        original.setReturned(true);
        assertFalse(camera.isLoaned());
    }

    @Test
    void load_ambiguousEquipmentId_preservesCurrentLoansAndAvailability() throws Exception {
        EquipmentHandler equipment = inventoryWithItems("001", "002");
        Equipment duplicate = new Camera("Duplicate camera", "002", 5);
        Field inventoryField = EquipmentHandler.class.getDeclaredField("inventory");
        inventoryField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Equipment> inventory = (ArrayList<Equipment>) inventoryField.get(equipment);
        inventory.add(duplicate);
        Path file = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, file);
        Loan original = handler.addLoan(LOAN);
        Files.writeString(file, LOAN.replace("id/001", "id/002") + "\n");

        IOException error = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(error.getMessage().contains("not unique"));
        assertEquals(List.of(original), handler.getLoanList());
        assertTrue(inventory.getFirst().isLoaned());
        assertFalse(inventory.get(1).isLoaned());
        assertFalse(duplicate.isLoaned());
    }

    @Test
    void load_replacesEquipmentAndLoans_detachesOldReferences() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        Equipment previousItem = equipment.addEquipment("add i/Camera id/001 type/camera cond/5");
        LoanHandler handler = new LoanHandler(equipment, temporaryDirectory.resolve("loans.txt"));
        Loan previousLoan = handler.addLoan(LOAN);

        equipment.loadFromStorage();
        handler.loadFromStorage();

        Equipment currentItem = equipment.getEquipmentList().getFirst();
        Loan currentLoan = handler.getLoanList().getFirst();
        assertNotSame(previousItem, currentItem);
        assertNotSame(previousLoan, currentLoan);
        assertFalse(previousItem.isLoaned());
        assertTrue(currentItem.isLoaned());
        previousLoan.setReturned(true);
        assertThrows(IllegalStateException.class, () -> previousLoan.setReturned(false));
        assertTrue(currentItem.isLoaned());
        assertSame(currentLoan, handler.getLoanList().getFirst());
        currentLoan.setReturned(true);
        assertFalse(currentItem.isLoaned());
    }

    @Test
    void addLoan_failedSave_leavesEquipmentAvailableAndLoansUnchanged() throws Exception {
        EquipmentHandler equipment = inventoryWithItems("001");
        Path destination = temporaryDirectory.resolve("loans.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        LoanHandler handler = new LoanHandler(equipment, destination);

        assertThrows(LoanAdditionUnsuccessful.class, () -> handler.addLoan(LOAN));

        assertFalse(equipment.getEquipmentList().getFirst().isLoaned());
        assertTrue(handler.getLoanList().isEmpty());
        assertEquals(0, handler.getLoanCount());
        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
    }

    @Test
    void deleteLoan_failedSave_keepsEquipmentLoanedAndOriginalLoanAttached() throws Exception {
        EquipmentHandler equipment = inventoryWithItems("001");
        Equipment camera = equipment.getEquipmentList().getFirst();
        Path destination = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, destination);
        Loan original = handler.addLoan(LOAN);
        Files.delete(destination);
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");

        LoanDeletionUnsuccessful error = assertThrows(LoanDeletionUnsuccessful.class,
                () -> handler.deleteLoan("delete loan/001"));
        assertTrue(error.getMessage().startsWith("Could not save loan deletion:"));

        assertTrue(camera.isLoaned());
        assertEquals(List.of(original), handler.getLoanList());
        assertEquals(1, handler.getLoanCount());
        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
        original.setReturned(true);
        assertFalse(camera.isLoaned());
    }

    @Test
    void deleteLoan_returnedHistorySaveFailure_preservesNewerActiveLoanAndAllowsRetry() throws Exception {
        EquipmentHandler equipment = inventoryWithItems("001");
        Equipment camera = equipment.getEquipmentList().getFirst();
        Path destination = temporaryDirectory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(equipment, destination);
        Loan history = handler.addLoan(LOAN);
        history.setReturned(true);
        Loan active = handler.addLoan("loan id/001 b/003 issued/03-10-26 due/04-10-26");
        Files.delete(destination);
        Files.createDirectory(destination);
        Path sentinel = destination.resolve("keep.txt");
        Files.writeString(sentinel, "keep this record");

        LoanDeletionUnsuccessful error = assertThrows(LoanDeletionUnsuccessful.class,
                () -> handler.deleteLoan("delete loan/001"));

        assertTrue(error.getMessage().startsWith("Could not save loan deletion:"));
        assertEquals(List.of(history, active), handler.getLoanList());
        assertEquals(2, handler.getLoanCount());
        assertTrue(camera.isLoaned());
        assertEquals("keep this record", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(destination);

        assertSame(history, handler.deleteLoan("delete loan/001"));
        assertEquals(List.of(active), handler.getLoanList());
        assertTrue(camera.isLoaned());
        List<Loan> saved = LoanStorage.load(destination);
        assertEquals(1, saved.size());
        assertEquals(active.getIssuedDate(), saved.getFirst().getIssuedDate());
        assertFalse(saved.getFirst().isReturned());
        active.setReturned(true);
        assertFalse(camera.isLoaned());
    }

    private EquipmentHandler inventoryWithItems(String... ids) throws EquipmentAdditionUnsuccessful {
        EquipmentHandler equipment = new EquipmentHandler();
        for (String id : ids) {
            equipment.addEquipment("add i/Camera " + id + " id/" + id + " type/camera cond/5");
        }
        return equipment;
    }

    @Test
    void save_failedReplacement_preservesDestinationAndCleansTemporaryFile() throws IOException {
        Path destination = temporaryDirectory.resolve("loans.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        Loan loan = new Loan("001", "001", "002", LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 2, 0, 0));

        assertThrows(IOException.class, () -> LoanStorage.save(destination, List.of(loan)));

        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("loans-")));
        }
    }
}
