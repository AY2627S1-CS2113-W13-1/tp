package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests borrower-loan queries through command routing and user-facing output. */
class LoanBorrowerIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);
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
        assertEquals(List.of("002"), displayedLoanIds(printed));
        assertTrue(printed.contains("03-10-26"));
        assertTrue(printed.contains("12-10-26"));
        assertTrue(printed.contains("Overdue"));
        assertTrue(printed.contains("1 loan."));
        assertFalse(printed.contains("Returned"));
        assertFalse(printed.contains("T00:00"));
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
    void listBorrower_missingId_reportsErrorInsteadOfListingAllEquipment() {
        EqmManager manager = createManagerWithLoans();
        output.reset();

        manager.executeInput("list borrower");
        manager.executeInput("list member");

        String printed = output.toString(StandardCharsets.UTF_8);
        String error = "[ERROR] Invalid format. Use: list borrower/[BORROWER ID], from 000 to 999.";
        assertEquals(2, printed.lines().filter(error::equals).count());
        assertFalse(printed.contains("NAME"));
    }

    @Test
    void listBorrower_knownBorrowerWithNoActiveLoans_reportsEmptyResult() {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/4");
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/10-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        output.reset();

        manager.executeInput("list borrower/002");

        assertTrue(output.toString(StandardCharsets.UTF_8)
                .contains("No active loans found for borrower ID 002."));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("0 loans."));
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
        EqmManager restarted = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        output.reset();

        restarted.executeInput("list borrower/002");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Equipment currently held by borrower ID 002:"));
        assertEquals(List.of("002"), displayedLoanIds(printed));
        assertTrue(printed.contains("1 loan."));
        assertEquals(savedEquipment, Files.readString(equipmentFile));
        assertEquals(savedLoans, Files.readString(loanFile));
        assertEquals(savedSequence, Files.readString(sequenceFile));
    }

    /** Creates active, returned and unrelated loans for the routing scenarios. */
    private EqmManager createManagerWithLoans() {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/4");
        manager.executeInput("add i/Tripod id/002 type/tripod cond/5");
        manager.executeInput("add i/Lens id/003 type/lens cond/5");
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/10-10-26");
        manager.executeInput("loan id/002 b/002 issued/03-10-26 due/12-10-26");
        manager.executeInput("loan id/003 b/003 issued/03-10-26 due/12-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE remarks/");
        return manager;
    }

    /** Reads loan IDs from data rows without depending on the amount of column padding. */
    private List<String> displayedLoanIds(String printed) {
        return printed.lines().filter(line -> line.matches("\\| \\d{3} .*"))
                .map(line -> line.split("\\|", -1)[1].strip()).toList();
    }
}
