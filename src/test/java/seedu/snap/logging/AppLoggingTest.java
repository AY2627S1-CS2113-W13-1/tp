package seedu.snap.logging;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests shared logger inheritance and cleanup without touching the application's log directory. */
class AppLoggingTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void start_childLoggers_shareFileWithoutDuplicateRecords() throws Exception {
        Logger equipment = Logger.getLogger("seedu.snap.equipment.EquipmentHandler");
        Logger loans = Logger.getLogger("seedu.snap.loan.LoanHandler");
        Logger futureFeature = Logger.getLogger("seedu.snap.reports.ReportHandler");
        try (AppLogging logging = AppLogging.start(temporaryDirectory)) {
            equipment.finer("equipment inheritance probe");
            loans.warning("loan inheritance probe");
            futureFeature.fine("future feature inheritance probe");
        }
        String logs = Files.readString(temporaryDirectory.resolve("snap.log.0"));
        for (String message : new String[]{"equipment inheritance probe", "loan inheritance probe",
            "future feature inheritance probe"}) {
            assertEquals(1L, logs.lines().filter(line -> line.contains(message)).count());
        }
    }

    @Test
    void close_applicationFails_restoresSettingsAndReleasesFileLock() throws Exception {
        Logger parent = Logger.getLogger("seedu.snap");
        Level originalLevel = parent.getLevel();
        boolean originalUseParentHandlers = parent.getUseParentHandlers();
        Handler[] originalHandlers = parent.getHandlers();

        assertThrows(AssertionError.class, () -> {
            try (AppLogging logging = AppLogging.start(temporaryDirectory)) {
                parent.fine("record before failure");
                throw new AssertionError("simulated application failure");
            }
        });

        assertEquals(originalLevel, parent.getLevel());
        assertEquals(originalUseParentHandlers, parent.getUseParentHandlers());
        assertArrayEquals(originalHandlers, parent.getHandlers());
        assertTrue(Files.readString(temporaryDirectory.resolve("snap.log.0")).contains("record before failure"));
        try (var files = Files.list(temporaryDirectory)) {
            assertEquals(0L, files.filter(path -> path.toString().endsWith(".lck")).count());
        }
    }
}
