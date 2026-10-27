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
        manager.executeInput("add i/Camera id/001 type/camera cond/5 sensor/Full frame");
        manager.executeInput("add i/Battery id/002 type/battery cond/5 capacity/2000 mAh");
        manager.executeInput("add-bundle name/Travel Kit");
        manager.executeInput("add-to-bundle id/001 001 002");
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("loan id/002 b/222 issued/03-10-26 due/04-10-26");
        manager.executeInput("return loan/002 serviceable/yes damage/NONE");
        output.reset();

        manager.executeInput("history bundle/id/001");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Loan history for bundle 001 (Travel Kit):"));
        assertTrue(printed.contains("111"));
        assertTrue(printed.contains("222"));
        assertTrue(printed.contains("RETURNED"));
        assertTrue(printed.indexOf("111") < printed.indexOf("222"));
    }

    @Test
    void historyBundleCommand_reportsUnknownBundle() {
        EqmManager manager = new EqmManager(temporaryDirectory);

        manager.executeInput("history bundle/id/001");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("requested bundle does not exist"));
        assertFalse(printed.contains("Command not recognised"));
    }

    @Test
    void historyBundleCommand_survivesRestartWithBundleAndLoanPersistence() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/5 sensor/Full frame");
        manager.executeInput("add i/Battery id/002 type/battery cond/5 capacity/2000 mAh");
        manager.executeInput("add-bundle name/Travel Kit");
        manager.executeInput("add-to-bundle id/001 001 002");
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("loan id/002 b/222 issued/03-10-26 due/04-10-26");
        manager.executeInput("return loan/002 serviceable/yes damage/NONE");
        manager.executeInput("bye");
        output.reset();

        EqmManager restarted = new EqmManager(temporaryDirectory);
        restarted.executeInput("history bundle/name/Travel Kit");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Loan history for bundle 001 (Travel Kit):"));
        assertTrue(printed.contains("111"));
        assertTrue(printed.contains("222"));
        assertTrue(printed.indexOf("111") < printed.indexOf("222"));
    }
}
