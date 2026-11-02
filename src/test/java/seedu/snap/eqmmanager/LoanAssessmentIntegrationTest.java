package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the existing return and view commands with permanent per-loan condition records. */
class LoanAssessmentIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;
    private Path data;

    @TempDir
    private Path directory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        data = directory.resolve("data");
        manager = new EqmManager(data);
        manager.executeInput("add i/Camera id/001 type/camera cond/2 remarks/Before issue");
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/20-10-26");
        output.reset();
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void view_returnedLoan_comparesBothAssessmentsAfterRestartWithoutChangingFiles() throws Exception {
        manager.executeInput("return loan/001 serviceable/no damage/SENSOR_DAMAGE cond/8 remarks/Needs repair");
        manager.executeInput("update id/001 cond/5");
        manager = new EqmManager(data);
        List<Path> files = List.of(data.resolve("equipment.txt"), data.resolve("loans.txt"),
                data.resolve("loan-sequence.txt"));
        List<String> before = files.stream().map(this::readFile).toList();
        output.reset();

        manager.executeInput("view loan/001");

        String printed = printed();
        assertTrue(printed.contains("Status: Returned"));
        assertTrue(printed.contains("Type | camera | camera"));
        assertTrue(printed.contains("Condition | 2 | 8"));
        assertTrue(printed.contains("Damage | NONE | SENSOR_DAMAGE"));
        assertTrue(printed.contains("Serviceable | Yes | No"));
        assertTrue(printed.contains("Issue remarks: Before issue"));
        assertTrue(printed.contains("Return remarks: Needs repair"));
        assertEquals(before, files.stream().map(this::readFile).toList());
    }

    @Test
    void view_activeAndLegacyReturnedLoans_distinguishesMissingAssessmentFromPendingReturn() throws Exception {
        manager.executeInput("view loan/001");
        assertTrue(printed().contains("Status: Active"));
        assertTrue(printed().contains("Condition | 2 | Not returned"));
        Files.writeString(data.resolve("loans.txt"), "loan loanId/001 id/001 b/002 "
                + "issued/01-10-26 due/20-10-26 returned/true\n");
        manager = new EqmManager(data);
        output.reset();

        manager.executeInput("view loan/001");

        assertTrue(printed().contains("Status: Returned"));
        assertTrue(printed().contains("Condition | Not recorded | Not recorded"));
        assertFalse(printed().contains("Not returned"));
    }

    @Test
    void view_olderIssueSnapshot_doesNotInferServiceabilityFromCurrentEquipment() throws Exception {
        Path loansFile = data.resolve("loans.txt");
        Files.writeString(loansFile, Files.readString(loansFile).replace(" issueServiceable/yes", ""));
        manager = new EqmManager(data);
        output.reset();
        manager.executeInput("view loan/001");
        assertTrue(printed().contains("Condition | 2 | Not returned"));
        assertTrue(printed().contains("Serviceable | Not recorded | Not returned"));

        manager.executeInput("return loan/001 serviceable/yes damage/NONE cond/4");
        manager = new EqmManager(data);
        output.reset();
        manager.executeInput("view loan/001");

        assertTrue(printed().contains("Serviceable | Not recorded | Yes"));
        assertTrue(printed().contains("Condition | 2 | 4"));
    }

    @Test
    void return_invalidRatingAndRepeatedReturn_preserveOriginalAssessmentAndRecover() throws Exception {
        String before = readFile(data.resolve("loans.txt"));
        manager.executeInput("return loan/001 serviceable/yes damage/NONE cond/11 remarks/Rejected");
        assertTrue(printed().contains("Return condition must be a whole number from 1 to 10"));
        assertEquals(before, readFile(data.resolve("loans.txt")));
        manager.executeInput("return loan/001 serviceable/yes damage/NONE cond/4 remarks/Recorded");
        manager.executeInput("loan id/001 b/003 issued/21-10-26 due/22-10-26");
        String saved = readFile(data.resolve("loans.txt"));
        output.reset();

        manager.executeInput("return loan/001 serviceable/no damage/SENSOR_DAMAGE cond/10 remarks/Rejected");

        assertTrue(printed().contains("already been returned"));
        assertEquals(saved, readFile(data.resolve("loans.txt")));
        manager.executeInput("return loan/002 serviceable/no damage/SENSOR_DAMAGE cond/8 remarks/Later assessment");
        output.reset();
        manager.executeInput("view loan/001");
        assertTrue(printed().contains("Condition | 2 | 4"));
        assertTrue(printed().contains("Return remarks: Recorded"));
        assertFalse(printed().contains("Later assessment"));
    }

    @Test
    void backupAndRestore_preservePerLoanAssessmentsAndNextId() throws Exception {
        manager.executeInput("return loan/001 serviceable/yes damage/NONE cond/4 remarks/First return");
        Path backup = directory.resolve("backup");
        manager.executeInput("backup path/" + backup);
        assertTrue(printed().contains("Backup created successfully"));
        manager.executeInput("loan id/001 b/003 issued/21-10-26 due/22-10-26");
        manager.executeInput("return loan/002 serviceable/no damage/SENSOR_DAMAGE cond/8 remarks/Later return");
        output.reset();

        manager.executeInput("restore path/" + backup);

        assertTrue(printed().contains("Backup restored successfully"));
        manager = new EqmManager(data);
        output.reset();
        manager.executeInput("view loan/001");
        assertTrue(printed().contains("Condition | 2 | 4"));
        assertTrue(printed().contains("Return remarks: First return"));
        manager.executeInput("view loan/002");
        assertTrue(printed().contains("No loan found for loan ID 002"));
        assertEquals("nextLoanId/002", readFile(data.resolve("loan-sequence.txt")).strip());
    }

    @Test
    void help_explainsOptionalRatingAndHistoricalComparison() {
        manager.executeInput("help return");
        manager.executeInput("help view loan");

        String helpText = printed().replaceAll("\\s+", " ");
        assertTrue(helpText.contains("[cond/<N>]"));
        assertTrue(helpText.contains("If omitted, the current numeric condition is retained"));
        assertTrue(helpText.contains("Missing legacy assessments show Not recorded"));
    }

    /** Normalises column padding while retaining rows and literal assessment values. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8).replaceAll(" +", " ");
    }

    /** Allows snapshots of the small persisted files to be compared before and after read-only commands. */
    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
