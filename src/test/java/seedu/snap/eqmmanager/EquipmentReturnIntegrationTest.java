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

import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Camera.CameraDamage;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Exercises return commands through the application, including persistence and command recovery. */
class EquipmentReturnIntegrationTest {
    // @@author yarnieee
    private static final String LOAN_COMMAND = "loan-add e/E001 u/U002 issued/01-10-26 due/02-10-26";
    // @@author
    private static final String RETURN_COMMAND =
            // @@author yarnieee
            "loan-return l/L001 serviceable/yes damage/NONE remarks/Checked by staff";
    // @@author
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        manager = new EqmManager(temporaryDirectory);
        // @@author yarnieee
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/4 sensor/Full frame remarks/Before loan");
        // @@author
        manager.executeInput(LOAN_COMMAND);
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void returnCommand_serviceableReturn_preservesHistoryAndAllowsNextLoanAfterRestart() throws Exception {
        manager.executeInput(RETURN_COMMAND);

        // @@author yarnieee
        assertTrue(printed().contains("[OK] Loan L001 returned."));
        // @@author
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt")).contains("returned/true"));
        output.reset();
        // @@author yarnieee
        manager.executeInput("eqm-filter e/E001 status/available");
        // @@author
        assertTrue(printed().contains("Camera"));

        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();
        restarted.executeInput(LOAN_COMMAND);
        // @@author yarnieee
        assertTrue(printed().contains("[OK] Loan L002 issued."));
        // @@author
        List<String> records = Files.readAllLines(temporaryDirectory.resolve("loans.txt"));
        assertEquals(2, records.size());
        // @@author yarnieee
        assertTrue(records.getFirst().contains("loanId/L001") && records.getFirst().contains("returned/true"));
        assertTrue(records.getLast().contains("loanId/L002") && records.getLast().contains("returned/false"));
        assertEquals("nextLoanId/L003", Files.readString(temporaryDirectory.resolve("loan-sequence.txt")).trim());
        // @@author
    }

    @Test
    void returnCommand_unserviceableReturn_staysUnavailableAcrossRestartAndEquipmentUpdate() throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/no damage/SENSOR_DAMAGE remarks/Sensor needs repair");
        // @@author
        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();

        // @@author yarnieee
        restarted.executeInput("eqm-filter e/E001 status/available");
        // @@author
        assertTrue(printed().contains("No equipment matches the filters."));
        output.reset();
        restarted.executeInput(LOAN_COMMAND);
        assertFalse(printed().contains("[OK]"));
        // @@author yarnieee
        restarted.executeInput("eqm-update e/E001 name/Workshop Camera cond/5");
        restarted.executeInput("exit");
        // @@author

        EquipmentHandler inventory = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        inventory.loadFromStorage();
        Camera camera = (Camera) inventory.getEquipmentList().getFirst();
        assertFalse(camera.isServiceable());
        assertFalse(camera.isLoaned());
        assertEquals("Workshop Camera", camera.getItemName());
        assertEquals(5, camera.getCondition());
        assertEquals("Full frame", camera.getSensorFormat());
        assertEquals(CameraDamage.SENSOR_DAMAGE, camera.getDamage());
        assertEquals("Sensor needs repair", camera.getRemarks());
        LoanHandler loans = new LoanHandler(inventory, temporaryDirectory.resolve("loans.txt"));
        loans.loadFromStorage();
        assertEquals(1, loans.getLoanCount());
        assertTrue(loans.getLoanList().getFirst().isReturned());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
    }

    @Test
    void returnCommand_invalidAssessmentAndUnknownLoan_allowValidCommandInSameSession() throws Exception {
        String equipmentBefore = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        String loansBefore = Files.readString(temporaryDirectory.resolve("loans.txt"));

        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/maybe damage/NONE remarks/Rejected");
        manager.executeInput("loan-return l/L999 serviceable/yes damage/NONE remarks/Unknown");
        manager.executeInput("loan-return l/L001 serviceable/yes");

        assertFalse(printed().contains("[OK] Loan L001 returned."));
        // @@author
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertEquals(loansBefore, Files.readString(temporaryDirectory.resolve("loans.txt")));
        output.reset();
        manager.executeInput(RETURN_COMMAND);
        // @@author yarnieee
        assertTrue(printed().contains("[OK] Loan L001 returned."));
        // @@author
        output.reset();
        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/no damage/SHUTTER_FAULT remarks/Second attempt");
        assertFalse(printed().contains("[OK] Loan L001 returned."));
        // @@author
        EquipmentHandler inventory = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        inventory.loadFromStorage();
        assertTrue(inventory.getEquipmentList().getFirst().isServiceable());
        assertEquals("Checked by staff", inventory.getEquipmentList().getFirst().getRemarks());
    }

    @Test
    void returnCommand_failedLoanSave_keepsLoanActiveUntilStorageIsFixed() throws Exception {
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        String originalLoans = Files.readString(loanFile);
        String originalEquipment = Files.readString(equipmentFile);
        String originalSequence = Files.readString(sequenceFile);
        Files.delete(loanFile);
        Files.createDirectory(loanFile);
        Path sentinel = loanFile.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        manager.executeInput(RETURN_COMMAND);

        assertTrue(printed().contains("Could not save"));
        // @@author yarnieee
        assertFalse(printed().contains("[OK] Loan L001 returned."));
        // @@author
        assertEquals(originalEquipment, Files.readString(equipmentFile));
        assertEquals(originalSequence, Files.readString(sequenceFile));
        assertEquals("keep", Files.readString(sentinel));
        output.reset();
        // @@author yarnieee
        manager.executeInput("eqm-filter e/E001 status/loaned");
        // @@author
        assertTrue(printed().contains("Camera"));
        Files.delete(sentinel);
        Files.delete(loanFile);
        Files.writeString(loanFile, originalLoans);
        output.reset();

        manager.executeInput(RETURN_COMMAND);

        // @@author yarnieee
        assertTrue(printed().contains("[OK] Loan L001 returned."));
        // @@author
        EquipmentHandler inventory = new EquipmentHandler(equipmentFile);
        inventory.loadFromStorage();
        LoanHandler loans = new LoanHandler(inventory, loanFile);
        loans.loadFromStorage();
        Loan returned = loans.getLoanList().getFirst();
        assertTrue(returned.isReturned());
        assertFalse(inventory.getEquipmentList().getFirst().isLoaned());
        // @@author yarnieee
        assertEquals("L002", loans.getNextLoanId());
        // @@author
    }

    /** Returns the captured application output without depending on table column spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
