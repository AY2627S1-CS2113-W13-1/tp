package seedu.snap.eqmmanager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BackupUnsuccessful;
import seedu.snap.loan.LoanHandler;

/** Creates consistent, non-destructive backups of the application's three persisted data files. */
final class BackupHandler {
    static final String EQUIPMENT_FILE_NAME = "equipment.txt";
    static final String LOAN_FILE_NAME = "loans.txt";
    static final String SEQUENCE_FILE_NAME = "loan-sequence.txt";
    static final String MANIFEST_FILE_NAME = "backup-manifest.txt";
    private static final String BACKUP_COMMAND_PREFIX = "backup path/";
    private static final String BACKUP_VERSION = "backupVersion/1";
    private static final Pattern NO_LINE_BREAKS = Pattern.compile("(?s).*\\R.*");
    private static final Logger logger = Logger.getLogger(BackupHandler.class.getName());

    private final Path dataDirectory;
    private final LoanHandler loans;

    /** Creates a backup handler for the live data directory and current loan sequence state. */
    BackupHandler(Path dataDirectory, EquipmentHandler equipment, LoanHandler loans) {
        if (dataDirectory == null || equipment == null || loans == null) {
            throw new IllegalArgumentException("Backup handler requires data and application handlers.");
        }
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        this.loans = loans;
    }

    /**
     * Creates a new backup directory from the live files without changing those files.
     *
     * @param input command in the form {@code backup path/[DIRECTORY]}
     * @return the normalized backup directory
     * @throws BackupUnsuccessful if the command, destination or live data is invalid
     */
    Path createBackup(String input) throws BackupUnsuccessful {
        Path destination = parsePath(input, BACKUP_COMMAND_PREFIX, "backup");
        validateBackupDestination(destination);
        try {
            BackupSnapshot snapshot = captureLiveSnapshot();
            Path parent = destination.getParent();
            Files.createDirectories(parent);
            Path temporaryDirectory = Files.createTempDirectory(parent, "snap-backup-");
            try {
                writeSnapshot(temporaryDirectory, snapshot);
                moveNewDirectory(temporaryDirectory, destination);
                logger.log(Level.INFO, "Created application backup at {0}.", destination);
            } catch (IOException exception) {
                deleteDirectory(temporaryDirectory, exception);
                throw exception;
            }
            return destination;
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Backup creation failed for " + destination + ".", exception);
            throw new BackupUnsuccessful("Could not create backup at " + destination + ": "
                    + exception.getMessage(), exception);
        }
    }

    /** Captures all live records before any backup directory is written. */
    private BackupSnapshot captureLiveSnapshot() throws IOException {
        byte[] equipment = readLiveFile(dataDirectory.resolve(EQUIPMENT_FILE_NAME), new byte[0]);
        byte[] loan = readLiveFile(dataDirectory.resolve(LOAN_FILE_NAME), new byte[0]);
        byte[] sequence = readLiveFile(dataDirectory.resolve(SEQUENCE_FILE_NAME),
                ("nextLoanId/" + loans.getNextLoanId() + System.lineSeparator())
                        .getBytes(StandardCharsets.UTF_8));
        BackupSnapshot snapshot = new BackupSnapshot(equipment, loan, sequence);
        assert snapshot.hasAllFiles() : "A backup snapshot must contain all persistent files";
        return snapshot;
    }

    /** Reads an existing regular file or supplies the valid representation of an empty data file. */
    private static byte[] readLiveFile(Path file, byte[] emptyFile) throws IOException {
        if (Files.notExists(file)) {
            return emptyFile.clone();
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("Live data path is not a regular file: " + file.getFileName());
        }
        return Files.readAllBytes(file);
    }

    /** Writes all data and the checksummed manifest to a temporary directory. */
    private static void writeSnapshot(Path temporaryDirectory, BackupSnapshot snapshot) throws IOException {
        Files.write(temporaryDirectory.resolve(EQUIPMENT_FILE_NAME), snapshot.equipment());
        Files.write(temporaryDirectory.resolve(LOAN_FILE_NAME), snapshot.loans());
        Files.write(temporaryDirectory.resolve(SEQUENCE_FILE_NAME), snapshot.sequence());
        Files.writeString(temporaryDirectory.resolve(MANIFEST_FILE_NAME), snapshot.manifest(),
                StandardCharsets.UTF_8);
        assert Files.isRegularFile(temporaryDirectory.resolve(MANIFEST_FILE_NAME))
                : "A backup must contain a manifest";
    }

    /** Validates a new destination before the first backup byte is written. */
    private void validateBackupDestination(Path destination) throws BackupUnsuccessful {
        if (destination.equals(dataDirectory) || destination.startsWith(dataDirectory)
                || dataDirectory.startsWith(destination)) {
            throw new BackupUnsuccessful("Backup destination must be outside the live data directory.");
        }
        if (destination.getFileName() == null) {
            throw new BackupUnsuccessful("Backup destination must be a directory path.");
        }
        if (Files.exists(destination)) {
            throw new BackupUnsuccessful("Backup destination already exists and will not be overwritten: "
                    + destination);
        }
        Path parent = destination.getParent();
        if (parent == null) {
            throw new BackupUnsuccessful("Backup destination must have a writable parent directory.");
        }
        if (Files.exists(parent) && !Files.isDirectory(parent)) {
            throw new BackupUnsuccessful("Backup destination parent is not a directory: " + parent);
        }
    }

    /** Moves a completed directory without replacing a destination that appeared concurrently. */
    private static void moveNewDirectory(Path temporaryDirectory, Path destination) throws IOException {
        try {
            Files.move(temporaryDirectory, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryDirectory, destination);
        }
    }

    /** Removes only a temporary backup directory after a failed creation. */
    private static void deleteDirectory(Path directory, IOException original) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    original.addSuppressed(exception);
                }
            });
        } catch (IOException exception) {
            original.addSuppressed(exception);
        }
    }

    /** Parses a command prefix and supports paths containing spaces after the prefix. */
    private static Path parsePath(String input, String prefix, String command) throws BackupUnsuccessful {
        if (input == null || NO_LINE_BREAKS.matcher(input).matches()) {
            throw new BackupUnsuccessful("Invalid format. Use: " + command + " path/[DIRECTORY].");
        }
        String trimmed = input.trim();
        if (!trimmed.startsWith(prefix)) {
            throw new BackupUnsuccessful("Invalid format. Use: " + command + " path/[DIRECTORY].");
        }
        String value = trimmed.substring(prefix.length()).trim();
        if (value.isBlank()) {
            throw new BackupUnsuccessful("Backup path cannot be empty.");
        }
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new BackupUnsuccessful("Backup path is invalid: " + exception.getMessage(), exception);
        }
    }

    /** Immutable bytes captured from the three live persistence files. */
    private record BackupSnapshot(byte[] equipment, byte[] loans, byte[] sequence) {
        boolean hasAllFiles() {
            return equipment != null && loans != null && sequence != null;
        }

        String manifest() {
            return String.join(System.lineSeparator(), List.of(
                    BACKUP_VERSION,
                    "equipmentSha256/" + checksum(equipment),
                    "loansSha256/" + checksum(loans),
                    "loanSequenceSha256/" + checksum(sequence))) + System.lineSeparator();
        }

        private static String checksum(byte[] bytes) {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 must be available.", exception);
            }
        }
    }
}
