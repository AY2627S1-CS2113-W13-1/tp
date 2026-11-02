package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises bundle-history command routing and user-visible output. */
class BundleLoanHistoryIntegrationTest {
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
    void historyBundleCommand_displaysActiveAndReturnedLoans() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/5 sensor/Full frame");
        manager.executeInput("eqm-add name/Battery e/E002 type/battery cond/5 capacity/2000 mAh");
        manager.executeInput("bundle-create name/Travel Kit");
        manager.executeInput("bundle-add-eqm b/B001 E001 E002");
        manager.executeInput("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        manager.executeInput("loan-add e/E002 u/U222 issued/03-10-26 due/04-10-26");
        manager.executeInput("loan-return l/L002 serviceable/yes damage/NONE");
        output.reset();

        manager.executeInput("bundle-history b/B001");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Loan history for bundle B001 (Travel Kit)"));
        assertTrue(printed.contains("U111"));
        assertTrue(printed.contains("U222"));
        assertTrue(printed.contains("STATE"));
        assertTrue(printed.indexOf("U111") < printed.indexOf("U222"));
    }

    @Test
    void historyBundleCommand_reportsUnknownBundle() {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("bundle-history b/B001");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("requested bundle does not exist"));
        assertFalse(printed.contains("Command not recognised"));
    }

    @Test
    void historyBundleCommand_survivesRestartWithBundleAndLoanPersistence() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/5 sensor/Full frame");
        manager.executeInput("eqm-add name/Battery e/E002 type/battery cond/5 capacity/2000 mAh");
        manager.executeInput("bundle-create name/Travel Kit");
        manager.executeInput("bundle-add-eqm b/B001 E001 E002");
        manager.executeInput("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        manager.executeInput("loan-add e/E002 u/U222 issued/03-10-26 due/04-10-26");
        manager.executeInput("loan-return l/L002 serviceable/yes damage/NONE");
        manager.executeInput("exit");
        output.reset();

        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("bundle-history name/Travel Kit");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Loan history for bundle B001 (Travel Kit)"));
        assertTrue(printed.contains("U111"));
        assertTrue(printed.contains("U222"));
        assertTrue(printed.indexOf("U111") < printed.indexOf("U222"));
    }
}
