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

import seedu.snap.bundle.BundleHandler;
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
    private static final String RESTORE_COMMAND_PREFIX = "restore path/";
    private static final String BACKUP_VERSION = "backupVersion/1";
    private static final Pattern NO_LINE_BREAKS = Pattern.compile("(?s).*\\R.*");
    private static final Logger logger = Logger.getLogger(BackupHandler.class.getName());

    private final Path dataDirectory;
    private final EquipmentHandler equipment;
    private final LoanHandler loans;
    private final BundleHandler bundles;

    /** Creates a backup handler for the live data directory and current loan sequence state. */
    BackupHandler(Path dataDirectory, EquipmentHandler equipment, LoanHandler loans) {
        this(dataDirectory, equipment, loans, null);
    }

    /** Creates a backup handler that also clears session-only bundles after a successful restore. */
    BackupHandler(Path dataDirectory, EquipmentHandler equipment, LoanHandler loans, BundleHandler bundles) {
        if (dataDirectory == null || equipment == null || loans == null) {
            throw new IllegalArgumentException("Backup handler requires data and application handlers.");
        }
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        this.equipment = equipment;
        this.loans = loans;
        this.bundles = bundles;
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

    /**
     * Validates a backup completely before replacing the live records, then reloads the application state.
     *
     * @param input command in the form {@code restore path/[DIRECTORY]}
     * @return the normalized backup directory
     * @throws BackupUnsuccessful if the backup is invalid or replacement cannot be completed safely
     */
    Path restoreBackup(String input) throws BackupUnsuccessful {
        Path source = parsePath(input, RESTORE_COMMAND_PREFIX, "restore");
        validateRestoreSource(source);
        try {
            BackupSnapshot snapshot = readBackupSnapshot(source);
            validateSnapshot(snapshot);
            replaceLiveData(snapshot);
            logger.log(Level.INFO, "Restored application data from backup at {0}.", source);
            return source;
        } catch (IOException | RuntimeException exception) {
            logger.log(Level.WARNING, "Restore failed for " + source + ".", exception);
            throw new BackupUnsuccessful("Could not restore backup from " + source + ": "
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

    /** Writes only the three live data files into a validation or restore staging directory. */
    private static void writeLiveFiles(Path directory, BackupSnapshot snapshot) throws IOException {
        Files.write(directory.resolve(EQUIPMENT_FILE_NAME), snapshot.equipment());
        Files.write(directory.resolve(LOAN_FILE_NAME), snapshot.loans());
        Files.write(directory.resolve(SEQUENCE_FILE_NAME), snapshot.sequence());
    }

    /** Reads the required backup files and verifies that the manifest matches every byte. */
    private static BackupSnapshot readBackupSnapshot(Path source) throws IOException {
        byte[] equipment = readRequiredFile(source.resolve(EQUIPMENT_FILE_NAME));
        byte[] loans = readRequiredFile(source.resolve(LOAN_FILE_NAME));
        byte[] sequence = readRequiredFile(source.resolve(SEQUENCE_FILE_NAME));
        Path manifestFile = source.resolve(MANIFEST_FILE_NAME);
        if (!Files.isRegularFile(manifestFile)) {
            throw new IOException("Backup manifest is missing or is not a regular file.");
        }
        String manifest = Files.readString(manifestFile, StandardCharsets.UTF_8);
        BackupSnapshot snapshot = new BackupSnapshot(equipment, loans, sequence);
        snapshot.validateManifest(manifest);
        return snapshot;
    }

    /** Requires a regular backup data file and reports missing files as an invalid backup. */
    private static byte[] readRequiredFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Backup file is missing or is not a regular file: " + file.getFileName());
        }
        return Files.readAllBytes(file);
    }

    /** Validates the three records in an isolated directory before touching live files. */
    private static void validateSnapshot(BackupSnapshot snapshot) throws IOException {
        Path temporaryDirectory = Files.createTempDirectory("snap-validate-");
        try {
            writeLiveFiles(temporaryDirectory, snapshot);
            EquipmentHandler checkedEquipment = new EquipmentHandler(
                    temporaryDirectory.resolve(EQUIPMENT_FILE_NAME));
            checkedEquipment.loadFromStorage();
            LoanHandler checkedLoans = new LoanHandler(checkedEquipment,
                    temporaryDirectory.resolve(LOAN_FILE_NAME),
                    temporaryDirectory.resolve(SEQUENCE_FILE_NAME));
            checkedLoans.loadFromStorage();
            assert checkedLoans.getLoanCount() >= 0 : "Validated loan count must not be negative";
        } finally {
            try {
                deleteDirectory(temporaryDirectory);
            } catch (IOException cleanupFailure) {
                logger.log(Level.WARNING, "Could not remove temporary backup validation data.", cleanupFailure);
            }
        }
    }

    /** Replaces all live files transactionally and reloads handlers only after every replacement succeeds. */
    private void replaceLiveData(BackupSnapshot snapshot) throws IOException {
        if (Files.exists(dataDirectory) && !Files.isDirectory(dataDirectory)) {
            throw new IOException("Live data path is not a directory: " + dataDirectory);
        }
        boolean dataDirectoryExisted = Files.exists(dataDirectory);
        List<LiveFileSnapshot> originals = captureLiveFiles();
        Files.createDirectories(dataDirectory);
        Path temporaryDirectory = Files.createTempDirectory(dataDirectory.getParent(), "snap-restore-");
        try {
            writeLiveFiles(temporaryDirectory, snapshot);
            moveReplacing(temporaryDirectory.resolve(EQUIPMENT_FILE_NAME),
                    dataDirectory.resolve(EQUIPMENT_FILE_NAME));
            moveReplacing(temporaryDirectory.resolve(LOAN_FILE_NAME), dataDirectory.resolve(LOAN_FILE_NAME));
            moveReplacing(temporaryDirectory.resolve(SEQUENCE_FILE_NAME),
                    dataDirectory.resolve(SEQUENCE_FILE_NAME));
            reloadLiveState();
            if (bundles != null) {
                bundles.clearBundles();
            }
        } catch (IOException | RuntimeException failure) {
            rollbackLiveData(originals, dataDirectoryExisted, failure);
            if (failure instanceof IOException ioException) {
                throw ioException;
            }
            throw failure;
        } finally {
            try {
                deleteDirectory(temporaryDirectory);
            } catch (IOException cleanupFailure) {
                logger.log(Level.WARNING, "Could not remove temporary restore data.", cleanupFailure);
            }
        }
    }

    /** Reloads both handlers using the same order as application startup. */
    private void reloadLiveState() throws IOException {
        equipment.loadFromStorage();
        loans.loadFromStorage();
        assert loans.getLoanCount() == loans.getLoanList().size() : "Reloaded loan count must match its records";
    }

    /** Captures current live files before any replacement is attempted. */
    private List<LiveFileSnapshot> captureLiveFiles() throws IOException {
        return List.of(
                LiveFileSnapshot.capture(dataDirectory.resolve(EQUIPMENT_FILE_NAME)),
                LiveFileSnapshot.capture(dataDirectory.resolve(LOAN_FILE_NAME)),
                LiveFileSnapshot.capture(dataDirectory.resolve(SEQUENCE_FILE_NAME)));
    }

    /** Replaces one staged file; the outer transaction restores all originals if any move fails. */
    private static void moveReplacing(Path staged, Path target) throws IOException {
        Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Restores both bytes and handler state after a failed live replacement. */
    private void rollbackLiveData(List<LiveFileSnapshot> originals, boolean dataDirectoryExisted,
            Exception failure) {
        try {
            for (LiveFileSnapshot original : originals) {
                original.restore();
            }
            reloadLiveState();
            if (!dataDirectoryExisted && Files.isDirectory(dataDirectory)) {
                try (Stream<Path> paths = Files.list(dataDirectory)) {
                    if (paths.findAny().isEmpty()) {
                        Files.deleteIfExists(dataDirectory);
                    }
                }
            }
        } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
            logger.log(Level.SEVERE, "Could not fully roll back a failed restore.", rollbackFailure);
        }
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

    /** Validates that a restore source is a separate existing directory. */
    private void validateRestoreSource(Path source) throws BackupUnsuccessful {
        if (source.equals(dataDirectory) || source.startsWith(dataDirectory)
                || dataDirectory.startsWith(source)) {
            throw new BackupUnsuccessful("Restore source must be outside the live data directory.");
        }
        if (!Files.isDirectory(source)) {
            throw new BackupUnsuccessful("Restore source is not an existing directory: " + source);
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
        try {
            deleteDirectory(directory);
        } catch (IOException exception) {
            original.addSuppressed(exception);
        }
    }

    /** Removes a temporary directory tree from its deepest path upwards. */
    private static void deleteDirectory(Path directory) throws IOException {
        IOException failure = null;
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
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
            throw new BackupUnsuccessful("Path cannot be empty.");
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

        void validateManifest(String manifest) throws IOException {
            String normalized = manifest.replace("\r\n", "\n").replace('\r', '\n');
            if (normalized.endsWith("\n")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            String expected = String.join("\n", List.of(
                    BACKUP_VERSION,
                    "equipmentSha256/" + checksum(equipment),
                    "loansSha256/" + checksum(loans),
                    "loanSequenceSha256/" + checksum(sequence)));
            if (!expected.equals(normalized)) {
                throw new IOException("Backup manifest does not match the backup files.");
            }
        }

        private static String checksum(byte[] bytes) {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("SHA-256 must be available.", exception);
            }
        }
    }

    /** Snapshot of one live file used to roll back a failed multi-file restore. */
    private record LiveFileSnapshot(Path path, boolean existed, byte[] contents) {
        static LiveFileSnapshot capture(Path path) throws IOException {
            if (Files.notExists(path)) {
                return new LiveFileSnapshot(path, false, new byte[0]);
            }
            if (!Files.isRegularFile(path)) {
                throw new IOException("Live data path is not a regular file: " + path.getFileName());
            }
            return new LiveFileSnapshot(path, true, Files.readAllBytes(path));
        }

        void restore() throws IOException {
            if (existed) {
                Files.write(path, contents);
            } else if (Files.exists(path)) {
                if (!Files.isRegularFile(path)) {
                    throw new IOException("Cannot remove non-regular live data path: " + path.getFileName());
                }
                Files.delete(path);
            }
        }
    }
}
