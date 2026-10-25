package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// @@author Rohita-29

/** Tests CLI routing for issue-time loan condition viewing. */
class LoanIssueConditionCommandTest {
    private final ByteArrayOutputStream output =
            new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void redirectOutput() {
        originalOutput = System.out;
        System.setOut(
                new PrintStream(
                        output,
                        true,
                        StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void executeInput_existingLoan_printsIssueCondition() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        addEquipmentAndLoan(manager);
        output.reset();

        manager.executeInput("view loan/001");

        String actual =
                output.toString(StandardCharsets.UTF_8);
        assertTrue(
                actual.contains(
                        "Loan 001 issue-time condition:"));
        assertTrue(actual.contains("Item ID: 001"));
        assertTrue(actual.contains("Type: camera"));
        assertTrue(actual.contains("Condition: 6"));
        assertTrue(actual.contains("Damage: SENSOR_DAMAGE"));
        assertTrue(
                actual.contains(
                        "Remarks: Handle carefully"));
    }

    @Test
    void executeInput_afterRestart_printsSavedIssueCondition() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        addEquipmentAndLoan(manager);
        output.reset();

        EqmManager restarted =
                new EqmManager(temporaryDirectory);
        restarted.executeInput("view loan/001");

        String actual =
                output.toString(StandardCharsets.UTF_8);
        assertTrue(
                actual.contains(
                        "Loan 001 issue-time condition:"));
        assertTrue(actual.contains("Condition: 6"));
        assertTrue(actual.contains("Damage: SENSOR_DAMAGE"));
        assertTrue(
                actual.contains(
                        "Remarks: Handle carefully"));
    }

    @Test
    void executeInput_unknownLoan_printsError() {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("view loan/999");

        assertEquals(
                "No loan found for loan ID 999."
                        + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void executeInput_invalidViewCommand_printsFormatError() {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("view item/001");

        assertEquals(
                "Invalid command format. Expected: "
                        + "view loan/[LOAN ID]."
                        + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    /** Adds one camera and creates its first loan. */
    private void addEquipmentAndLoan(EqmManager manager) {
        manager.executeInput(
                "add i/Test Camera id/001 "
                        + "type/camera cond/6 "
                        + "sensor/Full frame "
                        + "damage/SENSOR_DAMAGE "
                        + "remarks/Handle carefully");
        manager.executeInput(
                "loan id/001 b/101 "
                        + "issued/01-10-26 due/20-10-26");
    }
}

// @@author
