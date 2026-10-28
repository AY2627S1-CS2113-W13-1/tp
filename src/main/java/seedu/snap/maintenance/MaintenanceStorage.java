package seedu.snap.maintenance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads and saves maintenance records in a readable, tab-separated UTF-8 file. */
final class MaintenanceStorage {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Logger logger = Logger.getLogger(MaintenanceStorage.class.getName());
    private static final String RECORD_MARKER = "maintenance";

    private MaintenanceStorage() {
        // Utility class; do not instantiate.
    }

    /**
     * Loads one maintenance record from each non-blank line in the supplied file.
     * A missing file represents an empty maintenance history.
     *
     * @param file file containing saved maintenance records
     * @return parsed maintenance records
     * @throws IOException if the file cannot be read or contains an invalid record
     */
    static List<MaintenanceRecord> load(Path file) throws IOException {
        List<MaintenanceRecord> loaded = new ArrayList<>();
        if (Files.notExists(file)) {
            return loaded;
        }

        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            try {
                loaded.add(parseStorageLine(line));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid maintenance record on line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }
        logger.log(Level.INFO, "Loaded {0} maintenance record(s) from {1}",
                new Object[]{loaded.size(), file});
        return loaded;
    }

    /**
     * Saves records through a temporary file before replacing the destination.
     * This preserves the previous file if writing the new contents fails.
     *
     * @param file destination file
     * @param records maintenance records to save
     * @throws IOException if the destination cannot be written
     */
    static void save(Path file, List<MaintenanceRecord> records) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "maintenance-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (MaintenanceRecord record : records) {
                assert record != null : "Maintenance records being saved must not be null";
                lines.add(toStorageLine(record));
            }
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            logger.log(Level.FINE, "Saved {0} maintenance record(s) to {1}",
                    new Object[]{records.size(), file});
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** Parses a tab-separated record while preserving spaces inside free-text fields. */
    private static MaintenanceRecord parseStorageLine(String line) {
        String[] fields = line.strip().split("\\t", -1);
        if (fields.length != 5 || !RECORD_MARKER.equals(fields[0])) {
            throw new IllegalArgumentException(
                    "Expected maintenance, equipment/<UUID>, date/DD-MM-YY, work/<TEXT>, and outcome/<TEXT>.");
        }
        String equipmentRecordId = parseField(fields[1], "equipment/");
        String dateText = parseField(fields[2], "date/");
        String work = parseField(fields[3], "work/");
        String outcome = parseField(fields[4], "outcome/");
        try {
            LocalDate date = LocalDate.parse(dateText, DATE_FORMATTER);
            return new MaintenanceRecord(equipmentRecordId, date, work, outcome);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Maintenance date must use DD-MM-YY.", exception);
        }
    }

    /** Returns the value after a required storage field prefix. */
    private static String parseField(String field, String prefix) {
        if (!field.startsWith(prefix)) {
            throw new IllegalArgumentException("Expected " + prefix + " value.");
        }
        String value = field.substring(prefix.length());
        if (value.isBlank()) {
            throw new IllegalArgumentException("Field " + prefix + " cannot be empty.");
        }
        return value;
    }

    /** Converts a maintenance record into a readable, reloadable storage line. */
    private static String toStorageLine(MaintenanceRecord record) {
        return RECORD_MARKER
                + "\tequipment/" + record.getEquipmentRecordId()
                + "\tdate/" + DATE_FORMATTER.format(record.getMaintenanceDate())
                + "\twork/" + record.getWork()
                + "\toutcome/" + record.getOutcome();
    }
}
