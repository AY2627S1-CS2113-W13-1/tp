package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;

/** Checks equipment availability through successful, rejected, returned, and deleted loans. */
class EquipmentAvailabilityTest {
    private static final String LOAN = "loan id/001 b/002 issued/01-10-26 due/02-10-26";
    private EquipmentHandler equipment;
    private Equipment camera;
    private LoanHandler loans;

    @BeforeEach
    void setUp() throws EquipmentAdditionUnsuccessful {
        LoanHandler.loans.clear();
        LoanHandler.numLoans = 0;
        equipment = new EquipmentHandler();
        camera = equipment.addEquipment("add i/Camera id/001 type/camera cond/1");
        loans = new LoanHandler(equipment);
    }

    @AfterEach
    void clearLoans() {
        LoanHandler.loans.clear();
        LoanHandler.numLoans = 0;
    }

    @Test
    void addLoan_existingItem_marksItLoaned() throws LoanAdditionUnsuccessful {
        assertFalse(camera.isLoaned());
        loans.addLoan(LOAN);
        assertTrue(camera.isLoaned());
        assertEquals(1, LoanHandler.numLoans);
    }

    @Test
    void addLoan_missingItem_rejectsWithoutChangingState() {
        LoanAdditionUnsuccessful error = assertThrows(LoanAdditionUnsuccessful.class,
                () -> loans.addLoan(LOAN.replace("id/001", "id/999")));
        assertEquals("Item ID 999 does not exist.", error.getMessage());
        assertFalse(camera.isLoaned());
        assertTrue(LoanHandler.loans.isEmpty());
    }

    @Test
    void addLoan_invalidDates_doesNotReserveEquipment() throws LoanAdditionUnsuccessful {
        for (String command : new String[]{LOAN.replace("02-10-26", "00-10-26"),
            LOAN.replace("02-10-26", "01-09-26"), LOAN + " extra"}) {
            assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(command));
            assertFalse(camera.isLoaned());
            assertEquals(0, LoanHandler.numLoans);
        }
        loans.addLoan(LOAN);
        assertTrue(camera.isLoaned());
    }

    @Test
    void addLoan_alreadyLoaned_preservesOriginalLoan() throws LoanAdditionUnsuccessful {
        Loan original = loans.addLoan(LOAN);
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(LOAN));
        assertEquals(List.of(original), LoanHandler.loans);
        assertTrue(camera.isLoaned());
    }

    @Test
    void addLoan_ambiguousEquipmentId_rejectsWithoutReservingEitherItem()
            throws EquipmentAdditionUnsuccessful {
        Equipment duplicate = equipment.addEquipment("add i/Tripod id/001 type/accessory cond/2");
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(LOAN));
        assertFalse(camera.isLoaned());
        assertFalse(duplicate.isLoaned());
        assertTrue(LoanHandler.loans.isEmpty());
    }

    @Test
    void setReturned_returnAndRelend_keepsAvailabilityCorrect() throws LoanAdditionUnsuccessful {
        Loan first = loans.addLoan(LOAN);
        first.setReturned(true);
        assertFalse(camera.isLoaned());
        Loan second = loans.addLoan(LOAN.replace("01-10-26", "02-10-26"));
        assertTrue(camera.isLoaned());
        first.setReturned(true);
        assertTrue(camera.isLoaned());
        assertThrows(IllegalStateException.class, () -> first.setReturned(false));
        assertTrue(first.isReturned());
        second.setReturned(true);
        assertFalse(camera.isLoaned());
    }

    @Test
    void deleteLoan_activeLoan_releasesEquipment() throws Exception {
        loans.addLoan(LOAN);
        loans.deleteLoan("delete-loan id/001");
        assertFalse(camera.isLoaned());
        assertEquals(0, LoanHandler.numLoans);
        loans.addLoan(LOAN);
        assertTrue(camera.isLoaned());
    }

    @Test
    void deleteLoan_oldReturnedLoan_doesNotReleaseNewActiveLoan() throws Exception {
        loans.addLoan(LOAN).setReturned(true);
        loans.addLoan(LOAN.replace("01-10-26", "02-10-26"));
        loans.deleteLoan("delete-loan id/001 issued/01-10-26");
        assertTrue(camera.isLoaned());
        assertEquals(1, LoanHandler.numLoans);
    }

    @Test
    void deleteLoan_rejectedDeletion_preservesAvailability() throws Exception {
        Loan original = loans.addLoan(LOAN);
        assertThrows(seedu.snap.exceptions.LoanDeletionUnsuccessful.class,
                () -> loans.deleteLoan("delete-loan id/999"));
        assertTrue(camera.isLoaned());
        assertSame(original, LoanHandler.loans.getFirst());
    }

    @Test
    void deleteEquipment_activeLoan_blocksDeletionUntilReturned() throws Exception {
        Loan loan = loans.addLoan(LOAN);
        assertThrows(EquipmentDeletionUnsuccessful.class, () -> equipment.deleteEquipment("delete item/001"));
        assertEquals(List.of(camera), equipment.getEquipmentList());
        loan.setReturned(true);
        assertSame(camera, equipment.deleteEquipment("delete item/001"));
    }

    @Test
    void executeInput_missingThenExistingEquipment_recoversAndShowsStatus() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager();
            manager.executeInput(LOAN);
            manager.executeInput("add i/Camera id/001 type/camera cond/1");
            manager.executeInput(LOAN);
            manager.executeInput("list");
            manager.executeInput("delete item/001");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Item ID 001 does not exist."));
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Loan added successfully."));
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Loaned"));
            assertTrue(output.toString(StandardCharsets.UTF_8)
                    .contains("Cannot delete item ID 001 while it has an active loan."));
        } finally {
            System.setOut(original);
        }
    }
}
