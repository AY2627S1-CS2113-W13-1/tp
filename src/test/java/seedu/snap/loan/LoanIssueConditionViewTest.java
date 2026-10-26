package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
    void viewLoanIssueCondition_recordedSnapshot_printsDetails()
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

        handler.viewLoanIssueCondition("view loan/001");

        String expected =
                "Loan 001 issue-time condition:"
                        + System.lineSeparator()
                        + "Item ID: 001"
                        + System.lineSeparator()
                        + "Type: camera"
                        + System.lineSeparator()
                        + "Condition: 6"
                        + System.lineSeparator()
                        + "Damage: SENSOR_DAMAGE"
                        + System.lineSeparator()
                        + "Remarks: Handle carefully"
                        + System.lineSeparator();
        assertEquals(
                expected,
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewLoanIssueCondition_emptyRemarks_printsNone()
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

        handler.viewLoanIssueCondition("view loan/001");

        assertTrue(
                output.toString(StandardCharsets.UTF_8)
                        .contains("Remarks: None"));
    }

    @Test
    void viewLoanIssueCondition_legacyLoan_printsNotRecorded()
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
        Logger loanLogger =
                Logger.getLogger(LoanHandler.class.getName());
        Level previousLevel = loanLogger.getLevel();
        ArrayList<LogRecord> records = new ArrayList<>();
        Handler recordingHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        loanLogger.setLevel(Level.INFO);
        loanLogger.addHandler(recordingHandler);

        try {
            handler.viewLoanIssueCondition("view loan/001");
        } finally {
            loanLogger.removeHandler(recordingHandler);
            loanLogger.setLevel(previousLevel);
        }

        assertEquals(
                "Loan 001 issue-time condition: Not recorded"
                        + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertTrue(
                records.stream().anyMatch(
                        record ->
                                record.getLevel() == Level.INFO
                                        && record.getMessage()
                                                .contains("not recorded")
                                        && "001".equals(
                                                record.getParameters()[0])));
    }

    @Test
    void viewLoanIssueCondition_invalidFormat_throwsException() {
        LoanHandler handler =
                new LoanHandler(new EquipmentHandler());

        LoanViewingUnsuccessful exception =
                assertThrows(
                        LoanViewingUnsuccessful.class,
                        () -> handler.viewLoanIssueCondition(
                                "view item/001"));

        assertEquals(
                "Invalid command format. Expected: "
                        + "view loan/[LOAN ID].",
                exception.getMessage());
    }

    @Test
    void viewLoanIssueCondition_invalidLoanId_throwsException() {
        LoanHandler handler =
                new LoanHandler(new EquipmentHandler());

        LoanViewingUnsuccessful exception =
                assertThrows(
                        LoanViewingUnsuccessful.class,
                        () -> handler.viewLoanIssueCondition(
                                "view loan/01"));

        assertEquals(
                "Invalid loan ID \"01\". Loan ID must be "
                        + "exactly three digits from 001 to 999.",
                exception.getMessage());
    }

    @Test
    void viewLoanIssueCondition_unknownLoan_throwsException() {
        LoanHandler handler =
                new LoanHandler(new EquipmentHandler());

        LoanViewingUnsuccessful exception =
                assertThrows(
                        LoanViewingUnsuccessful.class,
                        () -> handler.viewLoanIssueCondition(
                                "view loan/999"));

        assertEquals(
                "No loan found for loan ID 999.",
                exception.getMessage());
    }
}

// @@author
