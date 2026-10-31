package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.LoanViewingUnsuccessful;

// @@author Rohita-29

/** Tests viewing issue-time equipment conditions for loans. */
class LoanIssueConditionViewTest {
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
    void viewLoanAssessments_recordedSnapshot_printsDetails()
            throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(
                "add i/Test Camera id/001 type/camera cond/6 "
                        + "sensor/Full frame "
                        + "damage/SENSOR_DAMAGE "
                        + "remarks/Handle carefully");
        LoanHandler handler = new LoanHandler(equipment);
        handler.addLoan(
                "loan id/001 b/101 "
                        + "issued/01-10-26 due/20-10-26");
        output.reset();

        handler.viewLoanAssessments("view loan/001");

        String actual = output.toString(StandardCharsets.UTF_8).replaceAll(" +", " ");
        assertTrue(actual.contains("Loan 001 condition assessments (Item ID: 001)"));
        assertTrue(actual.contains("Status: Active"));
        assertTrue(actual.contains("Type | camera | Not returned"));
        assertTrue(actual.contains("Condition | 6 | Not returned"));
        assertTrue(actual.contains("Damage | SENSOR_DAMAGE | Not returned"));
        assertTrue(actual.contains("Issue remarks: Handle carefully"));

    }

    @Test
    void viewLoanAssessments_emptyRemarks_printsNone()
            throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(
                "add i/Test Camera id/001 "
                        + "type/camera cond/6");
        LoanHandler handler = new LoanHandler(equipment);
        handler.addLoan(
                "loan id/001 b/101 "
                        + "issued/01-10-26 due/20-10-26");
        output.reset();

        handler.viewLoanAssessments("view loan/001");

        assertTrue(
                output.toString(StandardCharsets.UTF_8)
                        .contains("Issue remarks: None"));
    }

    @Test
    void viewLoanAssessments_legacyLoan_printsNotRecorded()
            throws Exception {
        Path loanFile =
                temporaryDirectory.resolve("loans.txt");
        Files.writeString(
                loanFile,
                "loan loanId/001 id/001 b/101 "
                        + "issued/01-10-26 due/20-10-26 "
                        + "returned/false\n");

        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(
                "add i/Test Camera id/001 "
                        + "type/camera cond/6");
        LoanHandler handler =
                new LoanHandler(equipment, loanFile);
        handler.loadFromStorage();
        output.reset();
        handler.viewLoanAssessments("view loan/001");

        String actual = output.toString(StandardCharsets.UTF_8).replaceAll(" +", " ");
        assertTrue(actual.contains("Status: Active"));
        assertTrue(actual.contains("Condition | Not recorded | Not returned"));
        assertTrue(actual.contains("Issue remarks: Not recorded"));

    }

    @Test
    void viewLoanAssessments_invalidFormat_throwsException() {
        LoanHandler handler =
                new LoanHandler(new EquipmentHandler());

        LoanViewingUnsuccessful exception =
                assertThrows(
                        LoanViewingUnsuccessful.class,
                        () -> handler.viewLoanAssessments(
                                "view item/001"));

        assertEquals(
                "Invalid command format. Expected: "
                        + "view loan/<LOAN ID>.",
                exception.getMessage());
    }

    @Test
    void viewLoanAssessments_invalidLoanId_throwsException() {
        LoanHandler handler =
                new LoanHandler(new EquipmentHandler());

        LoanViewingUnsuccessful exception =
                assertThrows(
                        LoanViewingUnsuccessful.class,
                        () -> handler.viewLoanAssessments(
                                "view loan/01"));

        assertEquals(
                "Invalid loan ID \"01\". Loan ID must be "
                        + "exactly three digits from 001 to 999.",
                exception.getMessage());
    }

    @Test
    void viewLoanAssessments_unknownLoan_throwsException() {
        LoanHandler handler =
                new LoanHandler(new EquipmentHandler());

        LoanViewingUnsuccessful exception =
                assertThrows(
                        LoanViewingUnsuccessful.class,
                        () -> handler.viewLoanAssessments(
                                "view loan/999"));

        assertEquals(
                "No loan found for loan ID 999.",
                exception.getMessage());
    }
}

// @@author
