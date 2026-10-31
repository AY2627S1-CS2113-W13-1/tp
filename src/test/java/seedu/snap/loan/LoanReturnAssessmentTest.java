package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanReturnUnsuccessful;

/** Verifies per-loan assessments across reuse, rejected returns and failed persistence. */
class LoanReturnAssessmentTest {
    private static final String LOAN = "loan id/001 b/002 issued/01-10-26 due/20-10-26";
    private EquipmentHandler equipment;
    private LoanHandler loans;
    private Equipment item;
    private Loan first;
    private Path equipmentFile;
    private Path loanFile;
    private Path sequenceFile;

    @TempDir
    private Path directory;

    @BeforeEach
    void setUp() throws Exception {
        equipmentFile = directory.resolve("equipment.txt");
        loanFile = directory.resolve("loans.txt");
        sequenceFile = directory.resolve("loan-sequence.txt");
        equipment = new EquipmentHandler(equipmentFile);
        item = equipment.addEquipment("add i/Camera id/001 type/camera cond/2 remarks/At issue");
        loans = new LoanHandler(equipment, loanFile, sequenceFile);
        first = loans.addLoan(LOAN);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    void returnLoan_explicitRating_recordsAssessmentAndUpdatesSameEquipment(int rating) throws Exception {
        assertNull(first.getReturnAssessment());

        assertSame(first, loans.returnLoan("return loan/001 serviceable/no damage/SENSOR_DAMAGE cond/"
                + rating + " remarks/Repair needed"));

        assertSame(item, equipment.getEquipmentList().getFirst());
        assertEquals(rating, item.getCondition());
        assertFalse(item.isServiceable());
        assertFalse(item.isLoaned());
        assertEquals(2, first.getIssueConditionSnapshot().getCondition());
        assertEquals("At issue", first.getIssueConditionSnapshot().getRemarks());
        assertEquals(new LoanReturnAssessment(
                new EquipmentConditionSnapshot("camera", rating, "SENSOR_DAMAGE", "Repair needed"), false),
                first.getReturnAssessment());
        assertEquals("002", loans.getNextLoanId());
        reload();
        assertEquals(rating, equipment.getEquipmentList().getFirst().getCondition());
        assertEquals(first.getReturnAssessment(), loans.getLoanList().getFirst().getReturnAssessment());
    }

    @Test
    void returnLoan_omittedRating_capturesCurrentRatingRatherThanIssueRating() throws Exception {
        equipment.updateEquipment("update id/001 cond/6");
        loans.returnLoan("return loan/001 serviceable/yes damage/NONE");

        assertEquals(2, first.getIssueConditionSnapshot().getCondition());
        assertEquals(6, first.getReturnAssessment().condition().getCondition());
        assertEquals("", first.getReturnAssessment().condition().getRemarks());
    }

    @Test
    void returnLoan_laterUpdatesAndReturns_preserveEachAssessmentAcrossRestarts() throws Exception {
        loans.returnLoan("return loan/001 serviceable/yes damage/SHUTTER_FAULT cond/4 remarks/First return");
        LoanReturnAssessment firstReturn = first.getReturnAssessment();
        equipment.updateEquipment("update id/001 cond/6");
        Loan second = loans.addLoan(LOAN);
        loans.returnLoan("return loan/002 serviceable/no damage/SENSOR_DAMAGE cond/9 remarks/Second return");
        equipment.updateEquipment("update id/001 cond/10");

        reload();
        Loan restoredFirst = loans.getLoanList().get(0);
        Loan restoredSecond = loans.getLoanList().get(1);
        assertEquals(firstReturn, restoredFirst.getReturnAssessment());
        assertEquals(2, restoredFirst.getIssueConditionSnapshot().getCondition());
        assertEquals(6, restoredSecond.getIssueConditionSnapshot().getCondition());
        assertEquals(second.getReturnAssessment(), restoredSecond.getReturnAssessment());
        assertEquals(9, restoredSecond.getReturnAssessment().condition().getCondition());
        assertFalse(restoredSecond.getReturnAssessment().serviceable());
        assertEquals(10, equipment.getEquipmentList().getFirst().getCondition());
        assertThrows(IllegalStateException.class, () -> restoredFirst.completeReturn(firstReturn));
        assertThrows(IllegalStateException.class, () -> restoredFirst.setReturned(false));
    }

    @Test
    void returnLoan_deletedEquipmentAndReusedItemId_doNotChangeOldAssessment() throws Exception {
        loans.returnLoan("return loan/001 serviceable/yes damage/NONE cond/3 remarks/Original camera");
        LoanReturnAssessment snapshot = first.getReturnAssessment();
        equipment.deleteEquipment("delete item/001");
        equipment.addEquipment("add i/Replacement id/001 type/lens cond/8");
        loans.addLoan(LOAN);
        loans.returnLoan("return loan/002 serviceable/yes damage/NONE cond/7 remarks/New lens");

        reload();
        assertEquals(snapshot, loans.getLoanList().getFirst().getReturnAssessment());
        assertEquals("lens", loans.getLoanList().getLast().getReturnAssessment().condition().getEquipmentType());
        assertEquals(List.of(loans.getLoanList().getLast()), loans.getLoanHistory("history id/001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "11", "01", "-1", "1.5", "one", "１", "999999999999999999999999", "",
        "5 cond/6", "5 unknown/x"})
    void returnLoan_invalidRating_preservesFilesAndAllowsRetry(String rating) throws Exception {
        String equipmentBefore = Files.readString(equipmentFile);
        String loansBefore = Files.readString(loanFile);
        String sequenceBefore = Files.readString(sequenceFile);

        assertThrows(LoanReturnUnsuccessful.class, () -> loans.returnLoan(
                "return loan/001 serviceable/no damage/SENSOR_DAMAGE cond/" + rating + " remarks/Rejected"));

        assertNull(first.getReturnAssessment());
        assertFalse(first.isReturned());
        assertTrue(item.isLoaned());
        assertEquals(2, item.getCondition());
        assertEquals(equipmentBefore, Files.readString(equipmentFile));
        assertEquals(loansBefore, Files.readString(loanFile));
        assertEquals(sequenceBefore, Files.readString(sequenceFile));
        loans.returnLoan("return loan/001 serviceable/yes damage/NONE cond/3");
        assertEquals(3, first.getReturnAssessment().condition().getCondition());
    }

    @ParameterizedTest
    @ValueSource(strings = {"equipment", "loans", "sequence"})
    void returnLoan_failedSave_doesNotPublishAssessmentAndCanBeRetried(String blocked) throws Exception {
        String equipmentBefore = Files.readString(equipmentFile);
        String loansBefore = Files.readString(loanFile);
        String sequenceBefore = Files.readString(sequenceFile);
        Path target = switch (blocked) {
        case "equipment" -> equipmentFile;
        case "loans" -> loanFile;
        default -> sequenceFile;
        };
        String original = Files.readString(target);
        Files.delete(target);
        Files.createDirectory(target);
        Path sentinel = target.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        assertThrows(LoanReturnUnsuccessful.class, () -> loans.returnLoan(
                "return loan/001 serviceable/no damage/SENSOR_DAMAGE cond/10 remarks/Retry later"));

        assertNull(first.getReturnAssessment());
        assertFalse(first.isReturned());
        assertTrue(item.isLoaned());
        assertTrue(item.isServiceable());
        assertEquals(2, item.getCondition());
        assertEquals("At issue", item.getRemarks());
        assertEquals("keep", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(target);
        Files.writeString(target, original);
        assertEquals(equipmentBefore, Files.readString(equipmentFile));
        assertEquals(loansBefore, Files.readString(loanFile));
        assertEquals(sequenceBefore, Files.readString(sequenceFile));

        loans.returnLoan("return loan/001 serviceable/no damage/SENSOR_DAMAGE cond/10 remarks/Retry later");
        reload();
        assertNotNull(loans.getLoanList().getFirst().getReturnAssessment());
        assertEquals(10, loans.getLoanList().getFirst().getReturnAssessment().condition().getCondition());
    }

    @Test
    void assessment_missingCondition_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LoanReturnAssessment(null, true));
        assertThrows(IllegalStateException.class, () -> first.completeReturn(null));
        assertNull(first.getReturnAssessment());
        assertFalse(first.isReturned());
    }

    @Test
    void returnedCopy_detachedProposal_cannotReleaseEquipmentOrReassessReturnedHistory() throws Exception {
        LoanReturnAssessment assessment = LoanReturnAssessment.fromEquipment(item);
        Loan proposed = first.returnedCopy(assessment);

        assertTrue(proposed.isReturned());
        assertEquals(assessment, proposed.getReturnAssessment());
        assertFalse(first.isReturned());
        assertNull(first.getReturnAssessment());
        assertTrue(item.isLoaned());
        assertThrows(IllegalStateException.class, () -> first.returnedCopy(null));
        assertThrows(IllegalStateException.class, () -> proposed.returnedCopy(assessment));
        LoanReturnAssessment wrongType = new LoanReturnAssessment(
                new EquipmentConditionSnapshot("lens", 2, "NONE", ""), true);
        assertThrows(IllegalArgumentException.class, () -> first.completeReturn(wrongType));
        assertThrows(IllegalArgumentException.class, () -> first.returnedCopy(wrongType));
        assertFalse(first.isReturned());
        assertTrue(item.isLoaned());
    }

    @Test
    void returnLoan_reassignedEquipment_comparesReplacementIssueAndReturnSnapshots() throws Exception {
        Equipment replacement = equipment.addEquipment("add i/Lens id/002 type/lens cond/3");
        loans.updateLoan("update loan/001 id/002");

        loans.returnLoan("return loan/001 serviceable/yes damage/NONE cond/7 remarks/Assessed lens");

        assertEquals("lens", first.getIssueConditionSnapshot().getEquipmentType());
        assertEquals(3, first.getIssueConditionSnapshot().getCondition());
        assertEquals("lens", first.getReturnAssessment().condition().getEquipmentType());
        assertEquals(7, first.getReturnAssessment().condition().getCondition());
        assertEquals(7, replacement.getCondition());
        assertFalse(item.isLoaned());
        assertEquals(2, item.getCondition());
        reload();
        assertEquals(first.getReturnAssessment(), loans.getLoanList().getFirst().getReturnAssessment());
    }

    /** Loads both files in the same order as application startup. */
    private void reload() throws Exception {
        equipment = new EquipmentHandler(equipmentFile);
        equipment.loadFromStorage();
        loans = new LoanHandler(equipment, loanFile, sequenceFile);
        loans.loadFromStorage();
    }
}
