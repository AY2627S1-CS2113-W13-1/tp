package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;

/** Exercises direct loan-model validation and ownership transitions. */
class LoanModelEdgesTest {
    private static final LocalDateTime ISSUE = LocalDateTime.of(2026, 10, 1, 10, 0);
    private static final LocalDateTime DUE = LocalDateTime.of(2026, 10, 2, 10, 0);

    @Test
    void constructor_rejectsInvalidStoredValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Loan("001", "001", "002", ISSUE, DUE, "not-a-uuid"));
        assertThrows(AssertionError.class, () -> new Loan("000", "001", "002", ISSUE, DUE));
        assertThrows(AssertionError.class, () -> new Loan("001", (String) null, "002", ISSUE, DUE));
        assertThrows(AssertionError.class, () -> new Loan("001", "000", "002", ISSUE, DUE));
        assertThrows(AssertionError.class, () -> new Loan("001", "001", "2", ISSUE, DUE));
        assertThrows(AssertionError.class, () -> new Loan("001", "001", "002", null, DUE));
        assertThrows(AssertionError.class, () -> new Loan("001", "001", "002", DUE, ISSUE));
    }

    @Test
    void setters_validateBorrowerAndDateOrder() {
        Loan loan = new Loan("001", "001", "002", ISSUE, DUE);

        loan.setBorrowerId("003");
        loan.setIssuedDate(ISSUE.minusDays(1));
        loan.setDueDate(DUE.plusDays(1));
        assertEquals("003", loan.getBorrowerId());

        assertThrows(AssertionError.class, () -> loan.setBorrowerId(null));
        assertThrows(AssertionError.class, () -> loan.setBorrowerId("12"));
        assertThrows(AssertionError.class, () -> loan.setIssuedDate(null));
        assertThrows(AssertionError.class, () -> loan.setIssuedDate(DUE.plusDays(2)));
        assertThrows(AssertionError.class, () -> loan.setDueDate(null));
        assertThrows(AssertionError.class, () -> loan.setDueDate(ISSUE.minusDays(2)));
    }

    @Test
    void bindEquipmentRecordId_acceptsMatchingActiveLegacyLoanOnly() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment camera = inventory.addEquipment("add i/Camera id/001 type/camera cond/5");
        Loan legacy = new Loan("001", "001", "002", ISSUE, DUE);

        legacy.bindEquipmentRecordId(camera);

        assertEquals(camera.getRecordId(), legacy.getEquipmentRecordId());
        assertThrows(IllegalArgumentException.class, () -> legacy.bindEquipmentRecordId(null));
        assertThrows(IllegalArgumentException.class,
                () -> legacy.bindEquipmentRecordId(new Camera("Other", "001", 5)));
        assertThrows(IllegalArgumentException.class,
                () -> legacy.bindEquipmentRecordId(new Camera("Wrong ID", "002", 5)));

        Loan returned = new Loan("002", "001", "002", ISSUE, DUE);
        returned.setReturned(true);
        assertThrows(IllegalArgumentException.class, () -> returned.bindEquipmentRecordId(camera));
    }

    @Test
    void attachAndDetachEquipment_enforceOwnership() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment camera = inventory.addEquipment("add i/Camera id/001 type/camera cond/5");
        Loan loan = new Loan("001", "001", "002", ISSUE, DUE, camera.getRecordId());

        loan.attachEquipment(camera);
        assertTrue(camera.isLoaned());
        loan.detachEquipment();
        assertFalse(camera.isLoaned());
        loan.attachEquipment(camera);
        loan.setReturned(true);

        assertThrows(AssertionError.class, () -> loan.attachEquipment(camera));
        assertThrows(AssertionError.class, () -> new Loan(null, "001", "002", ISSUE, DUE,
                camera.getRecordId()).attachEquipment(camera));

        Loan wrongItem = new Loan("002", "002", "002", ISSUE, DUE, camera.getRecordId());
        assertThrows(AssertionError.class, () -> wrongItem.attachEquipment(camera));
    }

    @Test
    void updatedCopy_rejectsMissingReplacementAndUnavailableReplacement() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment camera = inventory.addEquipment("add i/Camera id/001 type/camera cond/5");
        Equipment tripod = inventory.addEquipment("add i/Tripod id/002 type/tripod cond/5");
        Loan loan = new Loan("001", camera, "002", ISSUE, DUE);

        assertThrows(AssertionError.class,
                () -> loan.updatedCopy("001", camera.getRecordId(), "002", ISSUE, DUE, null));
        tripod.setLoaned(true);
        Loan updated = loan.updatedCopy("002", tripod.getRecordId(), "003", ISSUE, DUE, tripod);
        assertEquals("002", updated.getItemId());
        assertEquals("003", updated.getBorrowerId());
    }

    @Test
    void applyUpdate_rejectsMissingOrLoanedReplacement() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment camera = inventory.addEquipment("add i/Camera id/001 type/camera cond/5");
        Equipment tripod = inventory.addEquipment("add i/Tripod id/002 type/tripod cond/5");
        Loan loan = new Loan("001", camera, "002", ISSUE, DUE);
        loan.attachEquipment(camera);

        assertThrows(AssertionError.class, () -> loan.applyUpdate(null, tripod));
        assertThrows(AssertionError.class, () -> loan.applyUpdate(
                new Loan("001", "002", "002", ISSUE, DUE, tripod.getRecordId()), null));
        Loan mismatchedItem = new Loan("001", "002", "002", ISSUE, DUE, tripod.getRecordId());
        assertThrows(AssertionError.class, () -> loan.applyUpdate(mismatchedItem, camera));
        tripod.setLoaned(true);
        assertThrows(AssertionError.class, () -> loan.applyUpdate(mismatchedItem, tripod));
    }

    @Test
    void candidateLoan_rejectsUnserviceableEquipment() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Camera camera = (Camera) inventory.addEquipment("add i/Camera id/001 type/camera cond/5");
        camera.applyReturnAssessment(inventory.prepareReturnAssessment(camera, false,
                "SENSOR_DAMAGE", "Repair"));

        assertThrows(IllegalArgumentException.class, () -> new Loan(camera, "002", ISSUE, DUE));
    }

    @Test
    void recordIdValidation_acceptsCanonicalUuidOnly() {
        String valid = "e21c9254-ab91-4624-82eb-9b4423e5a795";

        assertTrue(Loan.isValidEquipmentRecordId(valid));
        assertFalse(Loan.isValidEquipmentRecordId(null));
        assertFalse(Loan.isValidEquipmentRecordId("not-a-uuid"));
        assertFalse(Loan.isValidEquipmentRecordId(valid.toUpperCase()));
    }
}
