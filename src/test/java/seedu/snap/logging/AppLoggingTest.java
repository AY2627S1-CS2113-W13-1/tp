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

/** Tests cleanup after failure without touching the application's log directory. */
class AppLoggingTest {
    @TempDir
    Path temporaryDirectory;

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
