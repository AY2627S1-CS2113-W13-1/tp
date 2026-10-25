package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        manager.executeInput("add i/Camera id/001 type/camera cond/4");
        manager.executeInput("add i/Tripod id/002 type/tripod cond/5");
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/10-10-26");
        output.reset();

        manager.executeInput("update loan/001 id/002 b/007 due/15-10-26");

        assertEquals("Loan 001 updated successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
        assertTrue(Files.readString(temporaryDirectory.resolve("loans.txt"))
                .contains("loanId/001 id/002 b/007 issued/01-10-26 due/15-10-26"));

        output.reset();
        manager.executeInput("list loans");
        assertTrue(output.toString(StandardCharsets.UTF_8).contains(
                "001          | 002          | 007"));
    }

    @Test
    void updateLoanCommand_invalidInput_printsLoanErrorAndRecovers() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/4");
        manager.executeInput("loan id/001 b/002 issued/01-10-26 due/10-10-26");
        output.reset();

        manager.executeInput("update loan/001 due/30-09-26");
        manager.executeInput("list loans");

        String actual = output.toString(StandardCharsets.UTF_8);
        assertTrue(actual.contains("Issued date must not be later than the due date."));
        assertTrue(actual.contains("001          | 001          | 002"));
    }
}
