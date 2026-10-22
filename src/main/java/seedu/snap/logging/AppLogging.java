package seedu.snap.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Owns the shared rotating log files for one application run.
 * Class loggers named with their full seedu.snap package inherit this configuration.
 * Use with try-with-resources to release the file handler even when the application fails.
 */
public final class AppLogging implements AutoCloseable {
    private static final Logger logger = Logger.getLogger("seedu.snap");

    private static final int LOG_FILE_LIMIT_BYTES = 1_000_000;
    private static final int LOG_FILE_COUNT = 2;

    /** The handler owned by this session, or null when file logging could not be started. */
    private final FileHandler fileHandler;
    /** Logger settings restored on close so later runs in the same JVM do not inherit stale configuration. */
    private final Level previousLevel;
    private final boolean wereParentHandlersEnabled;

    /** Creates a session responsible for closing the supplied handler. */
    private AppLogging(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
        previousLevel = logger.getLevel();
        wereParentHandlersEnabled = logger.getUseParentHandlers();
    }

    /**
     * Enables diagnostics through FINER in two rotating files of approximately one megabyte each.
     * Existing logs are retained across runs. If setup fails, warns and allows the application to continue.
     *
     * @return a logging session that must be closed, including when file logging is unavailable
     */
    public static AppLogging start() {
        return start(Path.of("logs"));
    }

    /** Starts logging in a supplied directory so tests can isolate files from application data. */
    static AppLogging start(Path directory) {
        try {
            Files.createDirectories(directory);
            FileHandler handler = new FileHandler(directory.resolve("snap.log").toString(),
                    LOG_FILE_LIMIT_BYTES, LOG_FILE_COUNT, true);
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.FINER);
            AppLogging logging = new AppLogging(handler);
            logger.addHandler(handler);
            logger.setLevel(Level.FINER);
            logger.setUseParentHandlers(false);
            return logging;
        } catch (IOException e) {
            System.err.println("Warning: Application logging is unavailable: " + e.getMessage());
            return new AppLogging(null);
        }
    }

    /** Removes and closes this session's handler, flushing records and releasing the file lock. */
    @Override
    public void close() {
        if (fileHandler != null) {
            logger.removeHandler(fileHandler);
            fileHandler.close();
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(wereParentHandlersEnabled);
        }
    }
}
