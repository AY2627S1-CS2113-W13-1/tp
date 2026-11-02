package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests command routing and user-facing output for editing loan details. */
class LoanUpdateIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void redirectOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void updateLoanCommand_routesToLoanHandlerAndPersistsChangedDetails() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/4");
        manager.executeInput("eqm-add name/Tripod e/E002 type/tripod cond/5");
        manager.executeInput("loan-add e/E001 u/U002 issued/01-10-26 due/10-10-26");
        output.reset();

        manager.executeInput("loan-update l/L001 e/E002 u/U007 due/15-10-26");

        String feedback = output.toString(StandardCharsets.UTF_8);
        assertTrue(feedback.contains("[OK] Loan L001 updated."));
        assertTrue(feedback.contains("Item: E002 \"Tripod\""));
        assertTrue(feedback.contains("Borrower: U007 | Issued: 01-10-26 | Due: 15-10-26"));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                .contains("loanId/L001 id/E002 b/U007 issued/01-10-26 due/15-10-26"));

        output.reset();
        manager.executeInput("loan-list");
        assertTrue(output.toString(StandardCharsets.UTF_8).contains(
                "| L001    | E002    | U007        |"));
    }

    @Test
    void updateLoanCommand_invalidInput_printsLoanErrorAndRecovers() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/4");
        manager.executeInput("loan-add e/E001 u/U002 issued/01-10-26 due/10-10-26");
        output.reset();

        manager.executeInput("loan-update l/L001 due/30-09-26");
        manager.executeInput("loan-list");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("Issued date must not be later than the due date."));
        assertTrue(actual.contains("| L001    | E001    | U002        |"));
    }

    @Test
    void updateLoanCommand_acceptsRepeatedSpacesAndTabsBetweenCommandWords() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/4");
        manager.executeInput("loan-add e/E001 u/U002 issued/01-10-26 due/10-10-26");
        output.reset();

        manager.executeInput("loan-update l/L001 u/U007");
        manager.executeInput("loan-update l/L001 u/U008");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertEquals(2, actual.lines().filter(line -> line.equals("[OK] Loan L001 updated.")).count());
        assertTrue(actual.contains("Borrower: U007 | Issued: 01-10-26 | Due: 10-10-26"));
        assertTrue(actual.contains("Borrower: U008 | Issued: 01-10-26 | Due: 10-10-26"));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                .contains("loanId/L001 id/E001 b/U008 issued/01-10-26 due/10-10-26"));
    }

    @Test
    void updateLoanCommand_restartedManagerLoadsEditedLoan() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/4");
        manager.executeInput("loan-add e/E001 u/U002 issued/01-10-26 due/10-10-26");
        manager.executeInput("loan-update l/L001 u/U007 due/15-10-26");

        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();
        restarted.executeInput("loan-list");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("| L001    | E001    | U007        |"));
        assertTrue(actual.contains("15-10-26"));
    }

    @Test
    void updateLoanCommand_borrowerOnlyEditPreservesIssueConditionAfterRestart() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Test Camera e/E001 type/camera cond/6 sensor/Full frame "
                + "damage/SENSOR_DAMAGE remarks/Handle carefully");
        manager.executeInput("loan-add e/E001 u/U101 issued/01-10-26 due/20-10-26");
        manager.executeInput("loan-update l/L001 u/U007");

        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();
        restarted.executeInput("loan-show l/L001");

        String actual = output.toString(StandardCharsets.UTF_8).replaceAll(" +", " ");
        assertTrue(actual.contains("Condition: 6"));
        assertTrue(actual.contains("Damage: SENSOR_DAMAGE"));
        assertTrue(actual.contains("Remarks: Handle carefully"));
    }

    @Test
    void updateLoanCommand_equipmentChangeUsesReplacementConditionAfterRestart() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Test Camera e/E001 type/camera cond/6 sensor/Full frame "
                + "damage/SENSOR_DAMAGE remarks/Handle carefully");
        manager.executeInput("eqm-add name/Test Tripod e/E002 type/tripod cond/3 thread/3/8");
        manager.executeInput("loan-add e/E001 u/U101 issued/01-10-26 due/20-10-26");
        manager.executeInput("loan-update l/L001 e/E002");

        output.reset();
        manager.executeInput("loan-show l/L001");
        assertReplacementConditionIsDisplayed();

        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();
        restarted.executeInput("loan-show l/L001");

        assertReplacementConditionIsDisplayed();
    }

    /** Checks that a reassigned loan displays only the replacement equipment's condition. */
    private void assertReplacementConditionIsDisplayed() {
        String actual = output.toString(StandardCharsets.UTF_8).replaceAll(" +", " ");
        assertTrue(actual.contains("Item ID: E002"));
        assertTrue(actual.contains("Type: tripod"));
        assertTrue(actual.contains("Condition: 3"));
        assertTrue(actual.contains("Damage: NONE"));
        assertFalse(actual.contains("Type: camera"));
        assertFalse(actual.contains("Damage: SENSOR_DAMAGE"));
        assertFalse(actual.contains("Issue remarks: Handle carefully"));
    }
}
