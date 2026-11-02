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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.equipment.EquipmentHandler;

/** Tests return-snapshot storage, compatibility and rejection of damaged history. */
class LoanReturnAssessmentStorageTest {
    private static final String RECORD = "loan loanId/001 id/001 b/002 issued/01-10-26 due/20-10-26 ";
    private static final String RETURN_FIELDS = "returnType/camera returnCondition/5 returnDamage/NONE "
            + "returnRemarks/Checked returnServiceable/yes";

    @TempDir
    private Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"camera", "lens", "battery", "tripod"})
    void saveAndLoad_allSubtypes_roundTripBothAssessmentsAndUnicode(String type) throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        inventory.addEquipment("add i/Test item id/001 type/" + type + " cond/1 remarks/At issue + 100% ✓");
        LoanHandler handler = new LoanHandler(inventory);
        Loan original = handler.addLoan("loan id/001 b/002 issued/01-10-26 due/20-10-26");
        String remarks = "Checked 中文 + % / cond/9 serviceable/no issueType/camera returnType/lens ✓";
        handler.returnLoan("return loan/001 serviceable/no damage/OTHER cond/10 remarks/" + remarks);
        Path file = directory.resolve("loans.txt");

        LoanStorage.save(file, handler.getLoanList());
        Loan restored = LoanStorage.load(file).getFirst();

        assertEquals(original.getIssueConditionSnapshot(), restored.getIssueConditionSnapshot());
        assertEquals(Boolean.TRUE, restored.getIssueServiceability());
        assertEquals(original.getReturnAssessment(), restored.getReturnAssessment());
        assertEquals(remarks, restored.getReturnAssessment().condition().getRemarks());
        assertFalse(restored.getReturnAssessment().serviceable());
        assertTrue(restored.isReturned());
        assertEquals(1, Files.readAllLines(file).size());
        assertTrue(Files.readString(file).contains("returnCondition/10"));
        assertTrue(Files.readString(file).contains("returnServiceable/no"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " returned/false", " returned/true",
        " returned/true issueType/camera issueCondition/2 issueDamage/NONE issueRemarks/Old"})
    void saveAndLoad_legacyRecords_doNotInventReturnAssessments(String optionalFields) throws Exception {
        Path file = directory.resolve("loans.txt");
        Files.writeString(file, RECORD.stripTrailing() + optionalFields + "\n");

        Loan legacy = LoanStorage.load(file).getFirst();
        assertNull(legacy.getReturnAssessment());
        LoanStorage.save(file, List.of(legacy));
        assertNull(LoanStorage.load(file).getFirst().getReturnAssessment());
        assertFalse(Files.readString(file).contains("returnType/"));
    }

    @Test
    void saveAndLoad_legacyIssueWithoutSnapshot_preservesNewReturnAssessment() throws Exception {
        Path file = directory.resolve("loans.txt");
        Files.writeString(file, RECORD + "returned/false\n");
        EquipmentHandler inventory = new EquipmentHandler();
        inventory.addEquipment("add i/Camera id/001 type/camera cond/3");
        LoanHandler handler = new LoanHandler(inventory, file);
        handler.loadFromStorage();
        handler.returnLoan("return loan/001 serviceable/yes damage/NONE cond/5");

        Loan restored = LoanStorage.load(file).getFirst();
        assertNull(restored.getIssueConditionSnapshot());
        assertEquals(5, restored.getReturnAssessment().condition().getCondition());
        assertTrue(restored.getReturnAssessment().serviceable());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "returnType/camera",
        "returnType/camera returnCondition/5 returnDamage/NONE returnRemarks/",
        "returnType/camera returnCondition/0 returnDamage/NONE returnRemarks/ returnServiceable/yes",
        "returnType/camera returnCondition/11 returnDamage/NONE returnRemarks/ returnServiceable/yes",
        "returnType/camera returnCondition/no returnDamage/NONE returnRemarks/ returnServiceable/yes",
        "returnType/camera returnCondition/5 returnDamage/UNKNOWN returnRemarks/ returnServiceable/yes",
        "returnType/camera returnCondition/5 returnDamage/SCRATCHED_GLASS returnRemarks/ returnServiceable/yes",
        "returnType/unknown returnCondition/5 returnDamage/NONE returnRemarks/ returnServiceable/yes",
        "returnType/camera returnCondition/5 returnDamage/NONE returnRemarks/%ZZ returnServiceable/yes",
        "returnType/camera returnCondition/5 returnDamage/NONE returnRemarks/%0A returnServiceable/yes",
        "returnType/camera returnCondition/5 returnDamage/NONE returnRemarks/ returnServiceable/true",
        "returnType/camera returnCondition/5 returnDamage/NONE returnRemarks/ returnServiceable/",
        "returnType/camera returnCondition/5 returnDamage/NONE returnServiceable/yes returnRemarks/",
        RETURN_FIELDS + " returnServiceable/no",
        RETURN_FIELDS + " " + RETURN_FIELDS})
    void load_malformedAssessment_reportsLineAndPreservesLiveRecords(String fields) throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        inventory.addEquipment("add i/Camera id/001 type/camera cond/3");
        Path file = directory.resolve("loans.txt");
        LoanHandler handler = new LoanHandler(inventory, file);
        Loan active = handler.addLoan("loan id/001 b/002 issued/01-10-26 due/20-10-26");
        Files.writeString(file, RECORD + "returned/true " + fields + "\n");
        String damagedFile = Files.readString(file);

        IOException error = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(error.getMessage().contains("line 1"));
        assertEquals(List.of(active), handler.getLoanList());
        assertFalse(active.isReturned());
        assertNull(active.getReturnAssessment());
        assertEquals("001", inventory.getEquipmentList().getFirst().getLoanId());
        assertEquals(damagedFile, Files.readString(file));
    }

    @Test
    void load_activeLoanWithReturnAssessment_isRejected() throws Exception {
        Path file = directory.resolve("loans.txt");
        Files.writeString(file, RECORD + "returned/false " + RETURN_FIELDS);

        IOException error = assertThrows(IOException.class, () -> LoanStorage.load(file));

        assertTrue(error.getMessage().contains("An active loan cannot contain a return assessment"));
    }

    @Test
    void load_issueAndReturnTypesDisagree_isRejected() throws Exception {
        Path file = directory.resolve("loans.txt");
        Files.writeString(file, RECORD + "returned/true "
                + "issueType/lens issueCondition/2 issueDamage/NONE issueRemarks/ " + RETURN_FIELDS);

        assertThrows(IOException.class, () -> LoanStorage.load(file));
    }

    @Test
    void returnLoan_fullLoanIdRange_keepsSeparateAssessmentsAndRoundTrips() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        inventory.addEquipment("add i/Camera id/001 type/camera cond/1");
        LoanHandler handler = new LoanHandler(inventory);
        int previous = 1;
        for (int number = 1; number <= 999; number++) {
            Loan loan = handler.addLoan("loan id/001 b/002 issued/01-10-26 due/20-10-26");
            int condition = number % 10 + 1;
            handler.returnLoan("return loan/" + loan.getLoanId()
                    + " serviceable/yes damage/NONE cond/" + condition + " remarks/Return " + number);
            assertEquals(previous, loan.getIssueConditionSnapshot().getCondition());
            previous = condition;
        }
        Path file = directory.resolve("loans.txt");
        LoanStorage.save(file, handler.getLoanList());
        List<Loan> restored = LoanStorage.load(file);

        assertEquals(999, restored.size());
        assertEquals("1000", handler.getNextLoanId());
        for (int index = 0; index < restored.size(); index++) {
            Loan loan = restored.get(index);
            assertEquals((index + 1) % 10 + 1, loan.getReturnAssessment().condition().getCondition());
            assertEquals("Return " + (index + 1), loan.getReturnAssessment().condition().getRemarks());
            assertEquals(Boolean.TRUE, loan.getIssueServiceability());
            assertEquals(handler.getLoanList().get(index).getIssueConditionSnapshot(),
                    loan.getIssueConditionSnapshot());
        }
    }
}
