package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

/** Exercises the borrower-specific loan view through the application command interface. */
class BorrowerLoanViewIntegrationTest {
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
        manager.executeInput("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        manager.executeInput("add i/Lens id/002 type/lens cond/5 zoom/24-70mm");
        manager.executeInput("add i/Tripod id/003 type/tripod cond/3");
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void listLoansByBorrower_multipleLoans_showsOnlyActiveMatchingEquipment() {
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("loan id/002 b/111 issued/03-10-26 due/04-10-26");
        manager.executeInput("loan id/003 b/222 issued/05-10-26 due/06-10-26");
        output.reset();

        manager.executeInput("list loans b/111");

        String printed = printed();
        assertTrue(printed.contains("Active loans for borrower ID 111"));
        assertTrue(printed.contains("001"));
        assertTrue(printed.contains("002"));
        assertFalse(printed.contains("003"));
        assertFalse(printed.contains("222"));
    }

    @Test
    void listLoansByBorrower_returnedLoan_isExcluded() {
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        manager.executeInput("loan id/002 b/111 issued/03-10-26 due/04-10-26");
        output.reset();

        manager.executeInput("list loans b/111");

        String printed = printed();
        assertTrue(printed.contains("002"));
        assertFalse(printed.contains("001"));
        assertFalse(printed.contains("true"));
    }

    @Test
    void listLoansByBorrower_unknownOrEmptyBorrower_showsClearMessage() {
        manager.executeInput("list loans b/999");
        String unknown = printed();
        output.reset();

        manager.executeInput("list loans b/111");
        String empty = printed();

        assertTrue(unknown.contains("No active loans found for borrower ID 999."));
        assertTrue(empty.contains("No active loans found for borrower ID 111."));
        assertFalse(unknown.contains("Command not recognised"));
        assertFalse(empty.contains("Command not recognised"));
    }

    @Test
    void listLoansByBorrower_invalidCommand_reportsFormatError() {
        manager.executeInput("list loans borrower/11");

        assertTrue(printed().contains("Invalid format"));
        assertFalse(printed().contains("Command not recognised"));
    }

    @Test
    void listLoansByBorrower_afterRestart_readsPersistedActiveLoanWithoutModifyingFiles() throws Exception {
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        Path loansFile = temporaryDirectory.resolve("loans.txt");
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        String loansBefore = Files.readString(loansFile);
        String sequenceBefore = Files.readString(sequenceFile);
        output.reset();
        EqmManager restarted = new EqmManager(temporaryDirectory);

        restarted.executeInput("list loans member/111");

        assertTrue(printed().contains("001"));
        assertEquals(loansBefore, Files.readString(loansFile));
        assertEquals(sequenceBefore, Files.readString(sequenceFile));
        assertNotEquals("", loansBefore);
    }

    /** Returns captured application output without requiring exact table spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
