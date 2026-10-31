package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.equipment.EquipmentHandler;

/** Verifies captured issue serviceability and unknown values in older loan records. */
class LoanIssueServiceabilityTest {
    private static final String RECORD = "loan loanId/001 id/001 b/002 issued/01-10-26 due/20-10-26 ";
    private static final String ISSUE = "issueType/camera issueCondition/2 issueDamage/NONE issueRemarks/";
    private EquipmentHandler inventory;
    private Path file;

    @TempDir
    private Path directory;

    @BeforeEach
    void setUp() throws Exception {
        inventory = new EquipmentHandler();
        inventory.addEquipment("add i/Camera id/001 type/camera cond/2");
        file = directory.resolve("loans.txt");
    }

    @Test
    void newLoan_returnAndLaterEquipmentChange_preserveIssueServiceability() throws Exception {
        LoanHandler handler = new LoanHandler(inventory, file);
        Loan loan = handler.addLoan("loan id/001 b/002 issued/01-10-26 due/20-10-26");
        assertEquals(Boolean.TRUE, loan.getIssueServiceability());
        assertEquals(Boolean.TRUE, loan.returnedCopy().getIssueServiceability());
        handler.returnLoan("return loan/001 serviceable/no damage/SENSOR_DAMAGE cond/8 remarks/Repair needed");
        inventory.updateEquipment("update id/001 cond/4");

        Loan restored = LoanStorage.load(file).getFirst();
        assertEquals(Boolean.TRUE, loan.getIssueServiceability());
        assertEquals(Boolean.TRUE, restored.getIssueServiceability());
        assertFalse(restored.getReturnAssessment().serviceable());
        assertFalse(inventory.getEquipmentList().getFirst().isServiceable());
        assertTrue(Files.readString(file).contains("issueServiceable/yes"));
    }

    @Test
    void legacyLoan_borrowerAndDateEditThenReturn_keepIssueServiceabilityUnknown() throws Exception {
        Files.writeString(file, RECORD + "returned/false " + ISSUE);
        LoanHandler handler = new LoanHandler(inventory, file);
        handler.loadFromStorage();
        Loan legacy = handler.getLoanList().getFirst();

        assertNull(legacy.getIssueServiceability());
        handler.updateLoan("update loan/001 b/003 due/21-10-26");
        assertNull(legacy.getIssueServiceability());
        assertFalse(Files.readString(file).contains("issueServiceable/"));
        assertNull(legacy.returnedCopy().getIssueServiceability());
        handler.returnLoan("return loan/001 serviceable/yes damage/NONE cond/3");

        assertNull(LoanStorage.load(file).getFirst().getIssueServiceability());
        assertTrue(legacy.getReturnAssessment().serviceable());
        assertFalse(Files.readString(file).contains("issueServiceable/"));
    }

    @Test
    void updateLoan_sameEquipmentPreservesValue_reassignmentCapturesReplacement() throws Exception {
        Files.writeString(file, RECORD + "returned/false " + ISSUE);
        LoanHandler handler = new LoanHandler(inventory, file);
        handler.loadFromStorage();
        Loan legacy = handler.getLoanList().getFirst();
        inventory.addEquipment("add i/Lens id/002 type/lens cond/5");

        handler.updateLoan("update loan/001 id/001");
        assertNull(legacy.getIssueServiceability());
        handler.updateLoan("update loan/001 id/002");
        assertEquals(Boolean.TRUE, legacy.getIssueServiceability());
        assertEquals("lens", legacy.getIssueConditionSnapshot().getEquipmentType());
        handler.updateLoan("update loan/001 b/003");
        assertEquals(Boolean.TRUE, legacy.getIssueServiceability());
        assertEquals(Boolean.TRUE, LoanStorage.load(file).getFirst().getIssueServiceability());
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "no"})
    void saveAndLoad_explicitHistoricalValue_roundTripsWithoutUsingCurrentEquipment(String value) throws Exception {
        Files.writeString(file, RECORD + "returned/true " + ISSUE + " issueServiceable/" + value);
        Loan loaded = LoanStorage.load(file).getFirst();
        Boolean expected = value.equals("yes");

        assertEquals(expected, loaded.getIssueServiceability());
        LoanStorage.save(file, List.of(loaded));
        assertEquals(expected, LoanStorage.load(file).getFirst().getIssueServiceability());
        assertTrue(Files.readString(file).contains("issueServiceable/" + value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "true", "false", "YES", "maybe", "yes issueServiceable/no"})
    void load_invalidIssueServiceability_rejectsRecordWithoutReplacingLiveLoans(String value) throws Exception {
        LoanHandler handler = new LoanHandler(inventory, file);
        Loan active = handler.addLoan("loan id/001 b/002 issued/01-10-26 due/20-10-26");
        Files.writeString(file, RECORD + "returned/false " + ISSUE + " issueServiceable/" + value);
        String damaged = Files.readString(file);

        IOException error = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(error.getMessage().contains("line 1"));
        assertEquals(List.of(active), handler.getLoanList());
        assertEquals(Boolean.TRUE, active.getIssueServiceability());
        assertEquals("001", inventory.getEquipmentList().getFirst().getLoanId());
        assertEquals(damaged, Files.readString(file));
    }

    @Test
    void load_serviceabilityWithoutIssueSnapshot_rejectsIncompleteAssessment() throws Exception {
        Files.writeString(file, RECORD + "returned/true issueServiceable/yes");

        assertThrows(IOException.class, () -> LoanStorage.load(file));
    }
}
