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

/** Tests borrower-loan queries through command routing and user-facing output. */
class LoanBorrowerIntegrationTest {
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
    void listBorrower_validCommand_displaysOnlyActiveLoansForBorrower() {
        EqmManager manager = createManagerWithLoans();
        output.reset();

        manager.executeInput("list borrower/002");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Equipment currently held by borrower ID 002:"));
        assertTrue(printed.contains("002          | 002          | 002"));
        assertFalse(printed.contains("001          | 001          | 002"));
        assertFalse(printed.contains("003          | 003          | 003"));
    }

    @Test
    void listMember_invalidAndUnknownCommands_reportErrorsAndKeepSessionUsable() {
        EqmManager manager = createManagerWithLoans();
        output.reset();

        manager.executeInput("list member/999");
        manager.executeInput("list borrower/abc");
        manager.executeInput("list borrower/002");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("No member found for borrower ID 999."));
        assertTrue(printed.contains("Invalid format. Use: list borrower/[BORROWER ID], from 000 to 999."));
        assertTrue(printed.contains("Equipment currently held by borrower ID 002:"));
    }

    @Test
    void listBorrower_knownBorrowerWithNoActiveLoans_reportsEmptyResult() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/4");
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/10-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        output.reset();

        manager.executeInput("list borrower/002");

        assertTrue(output.toString(StandardCharsets.UTF_8)
                .contains("No active loans found for borrower ID 002."));
    }

    @Test
    void listBorrower_afterRestartReadsPersistedActiveLoansWithoutWriting() throws Exception {
        createManagerWithLoans();
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        String savedEquipment = Files.readString(equipmentFile);
        String savedLoans = Files.readString(loanFile);
        String savedSequence = Files.readString(sequenceFile);
        EqmManager restarted = new EqmManager(temporaryDirectory);
        output.reset();

        restarted.executeInput("list borrower/002");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Equipment currently held by borrower ID 002:"));
        assertTrue(printed.contains("002          | 002          | 002"));
        assertFalse(printed.contains("001          | 001          | 002"));
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
    }

    /** Creates active, returned and unrelated loans for the routing scenarios. */
    private EqmManager createManagerWithLoans() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/4");
        manager.executeInput("add i/Tripod id/002 type/tripod cond/5");
        manager.executeInput("add i/Lens id/003 type/lens cond/5");
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/10-10-26");
        manager.executeInput("loan id/002 b/002 issued/03-10-26 due/12-10-26");
        manager.executeInput("loan id/003 b/003 issued/03-10-26 due/12-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        return manager;
    }
}
