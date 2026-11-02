package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.equipment.Tripod;
import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;

/** Checks equipment availability through successful, rejected, returned, and deleted loans. */
class EquipmentAvailabilityTest {
    private static final String LOAN = "loan-add e/E001 u/U002 issued/01-10-26 due/02-10-26";
    private EquipmentHandler equipment;
    private Equipment camera;
    private LoanHandler loans;
    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() throws EquipmentAdditionUnsuccessful {
        equipment = new EquipmentHandler();
        camera = equipment.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        loans = new LoanHandler(equipment);
    }

    @Test
    void newlyAddedEquipment_isAvailable() {
        assertFalse(camera.isLoaned());
    }

    @Test
    void loanConstructor_nullOrUnavailableEquipment_rejectsWithoutChangingAvailability() {
        LocalDateTime issued = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime due = LocalDateTime.of(2026, 10, 2, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> new Loan((Equipment) null, "U002", issued, due));
        assertFalse(camera.isLoaned());

        camera.setLoaned(true);
        assertThrows(IllegalArgumentException.class, () -> new Loan(camera, "U002", issued, due));
        assertTrue(camera.isLoaned());
        assertTrue(loans.getLoanList().isEmpty());
    }

    @Test
    void loanConstructor_unactivatedCandidate_cannotReleaseLaterActiveLoan() throws LoanAdditionUnsuccessful {
        Loan candidate = new Loan(camera, "U002", LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 2, 0, 0));
        assertFalse(camera.isLoaned());
        Loan active = loans.addLoan(LOAN);

        candidate.setReturned(true);

        assertTrue(camera.isLoaned());
        assertSame(active, loans.getLoanList().getFirst());
    }

    @Test
    void addLoan_existingItem_marksItLoaned() throws LoanAdditionUnsuccessful {
        assertFalse(camera.isLoaned());
        loans.addLoan(LOAN);
        assertTrue(camera.isLoaned());
        assertEquals(1, loans.getLoanCount());
    }

    @Test
    void addLoan_missingItem_rejectsWithoutChangingState() {
        LoanAdditionUnsuccessful error = assertThrows(LoanAdditionUnsuccessful.class,
                () -> loans.addLoan(LOAN.replace("e/E001", "e/E999")));
        assertEquals("Item ID E999 does not exist.", error.getMessage());
        assertFalse(camera.isLoaned());
        assertTrue(loans.getLoanList().isEmpty());
    }

    @Test
    void addLoan_invalidDates_doesNotReserveEquipment() throws LoanAdditionUnsuccessful {
        for (String command : new String[]{LOAN.replace("02-10-26", "00-10-26"),
            LOAN.replace("02-10-26", "01-09-26"), LOAN + " extra"}) {
            assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(command));
            assertFalse(camera.isLoaned());
            assertEquals(0, loans.getLoanCount());
        }
        loans.addLoan(LOAN);
        assertTrue(camera.isLoaned());
    }

    @Test
    void addLoan_alreadyLoaned_preservesOriginalLoan() throws LoanAdditionUnsuccessful {
        Loan original = loans.addLoan(LOAN);
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(LOAN));
        assertEquals(List.of(original), loans.getLoanList());
        assertTrue(camera.isLoaned());
    }

    @Test
    void addLoan_ambiguousEquipmentId_rejectsWithoutReservingEitherItem()
            throws Exception {
        Equipment duplicate = new Tripod("Tripod", "E001", 2);
        Field field = EquipmentHandler.class.getDeclaredField("inventory");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Equipment> inventory = (ArrayList<Equipment>) field.get(equipment);
        inventory.add(duplicate);
        assertThrows(LoanAdditionUnsuccessful.class, () -> loans.addLoan(LOAN));
        assertFalse(camera.isLoaned());
        assertFalse(duplicate.isLoaned());
        assertTrue(loans.getLoanList().isEmpty());
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
    void setReturned_returnedLoanCannotReactivateEvenWhenEquipmentIsAvailable() throws LoanAdditionUnsuccessful {
        Loan returned = loans.addLoan(LOAN);
        returned.setReturned(true);

        assertThrows(IllegalStateException.class, () -> returned.setReturned(false));

        assertTrue(returned.isReturned());
        assertFalse(camera.isLoaned());
        assertFalse(loans.hasActiveLoanForItem("E001"));
    }

    @Test
    void deleteLoan_activeLoan_releasesEquipment() throws Exception {
        loans.addLoan(LOAN);
        loans.deleteLoan("loan-delete l/L001");
        assertFalse(camera.isLoaned());
        assertEquals(0, loans.getLoanCount());
        loans.addLoan(LOAN);
        assertTrue(camera.isLoaned());
    }

    @Test
    void deleteLoan_oldReference_cannotChangeNewLoansAvailability() throws Exception {
        Loan deleted = loans.addLoan(LOAN);
        loans.deleteLoan("loan-delete l/L001");
        Loan current = loans.addLoan(LOAN);
        deleted.setReturned(true);
        assertTrue(camera.isLoaned());
        assertSame(current, loans.getLoanList().getFirst());
        assertThrows(IllegalStateException.class, () -> deleted.setReturned(false));
        assertTrue(camera.isLoaned());
    }

    @Test
    void deleteLoan_oldReturnedLoan_doesNotReleaseNewActiveLoan() throws Exception {
        loans.addLoan(LOAN).setReturned(true);
        loans.addLoan(LOAN.replace("01-10-26", "02-10-26"));
        loans.deleteLoan("loan-delete l/L001");
        assertTrue(camera.isLoaned());
        assertEquals(1, loans.getLoanCount());
    }

    @Test
    void deleteLoan_returnedHistoryAfterEquipmentIdReuse_doesNotReleaseReplacement() throws Exception {
        Loan returned = loans.addLoan(LOAN);
        returned.setReturned(true);
        equipment.deleteEquipment("eqm-delete e/E001");
        Equipment replacement = equipment.addEquipment("eqm-add name/Replacement tripod e/E001 type/tripod cond/4");
        Loan active = loans.addLoan(LOAN.replace("01-10-26", "02-10-26"));

        assertSame(returned, loans.deleteLoan("loan-delete l/L001"));
        returned.setReturned(true);
        assertThrows(IllegalStateException.class, () -> returned.setReturned(false));

        assertTrue(replacement.isLoaned());
        assertFalse(camera.isLoaned());
        assertEquals(List.of(active), loans.getLoanList());
    }

    @Test
    void deleteLoan_rejectedDeletion_preservesAvailability() throws Exception {
        Loan original = loans.addLoan(LOAN);
        assertThrows(seedu.snap.exceptions.LoanDeletionUnsuccessful.class,
                () -> loans.deleteLoan("loan-delete l/L999"));
        assertTrue(camera.isLoaned());
        assertSame(original, loans.getLoanList().getFirst());
    }

    @Test
    void deleteEquipment_activeLoan_blocksDeletionUntilReturned() throws Exception {
        Loan loan = loans.addLoan(LOAN);
        assertThrows(EquipmentDeletionUnsuccessful.class, () -> equipment.deleteEquipment("eqm-delete e/E001"));
        assertEquals(List.of(camera), equipment.getEquipmentList());
        loan.setReturned(true);
        assertSame(camera, equipment.deleteEquipment("eqm-delete e/E001"));
    }

    @Test
    void executeInput_missingThenExistingEquipment_recoversAndShowsStatus() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDirectory);
            manager.executeInput(LOAN);
            manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
            manager.executeInput(LOAN);
            manager.executeInput("eqm-list");
            manager.executeInput("eqm-delete e/E001");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Item ID E001 does not exist."));
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("[OK] Loan L001 issued."));
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Loaned"));
            assertTrue(output.toString(StandardCharsets.UTF_8)
                    .contains("Cannot delete item ID E001 while it has an active loan."));
        } finally {
            System.setOut(original);
        }
    }
}
