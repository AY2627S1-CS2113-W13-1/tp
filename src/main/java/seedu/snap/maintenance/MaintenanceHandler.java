package seedu.snap.maintenance;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.MaintenanceRecordUnsuccessful;

/** Handles validated maintenance-record commands and their persistent records. */
public class MaintenanceHandler {
    private static final Logger logger = Logger.getLogger(MaintenanceHandler.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern COMMAND = Pattern.compile(
            "(?sU)\\Amaintain\\s+id/([^\\s]+)\\s+date/([^\\s]+)\\s+work/(.*?)\\s+outcome/(.+)\\z");
    private static final String COMMAND_FORMAT = "Invalid format. Use: maintain id/[ITEM ID] "
            + "date/[DD-MM-YY] work/[DESCRIPTION] outcome/[OUTCOME].";

    /** Equipment inventory used to resolve the physical record identity for each command. */
    private final EquipmentHandler equipment;
    /** In-memory maintenance records in their insertion order. */
    private final ArrayList<MaintenanceRecord> records = new ArrayList<>();
    /** File used for persistence, or null for an in-memory handler. */
    private final Path storageFile;
    /** Clock used to validate that completed maintenance is not dated in the future. */
    private final Clock clock;

    /** Creates an in-memory maintenance handler for isolated tests or callers without persistence. */
    public MaintenanceHandler(EquipmentHandler equipment) {
        this(equipment, null, Clock.systemDefaultZone());
    }

    /** Creates a maintenance handler backed by the supplied text file. */
    public MaintenanceHandler(EquipmentHandler equipment, Path storageFile) {
        this(equipment, storageFile, Clock.systemDefaultZone());
    }

    /** Creates a maintenance handler with an explicit clock for deterministic date validation. */
    public MaintenanceHandler(EquipmentHandler equipment, Path storageFile, Clock clock) {
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment handler must not be null.");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock must not be null.");
        }
        this.equipment = equipment;
        this.storageFile = storageFile;
        this.clock = clock;
    }

    /**
     * Replaces the current records with records loaded from the backing file.
     * A missing file is treated as an empty maintenance history.
     *
     * @throws IOException if saved maintenance data cannot be read or is invalid
     */
    public void loadFromStorage() throws IOException {
        if (storageFile == null) {
            return;
        }
        List<MaintenanceRecord> loaded = MaintenanceStorage.load(storageFile);
        for (MaintenanceRecord record : loaded) {
            if (isFutureDate(record.getMaintenanceDate())) {
                throw new IOException("Maintenance date cannot be in the future.");
            }
        }
        records.clear();
        records.addAll(loaded);
        assert records.size() == loaded.size() : "Loaded maintenance record count must be preserved";
        logger.log(Level.INFO, "Loaded {0} maintenance record(s) from {1}",
                new Object[]{records.size(), storageFile});
    }

    /** Returns an unmodifiable view of all maintenance records in insertion order. */
    public List<MaintenanceRecord> getMaintenanceRecords() {
        return Collections.unmodifiableList(records);
    }

    /** Returns the backing file, or null when this handler is in-memory only. */
    public Path getStorageFile() {
        return storageFile;
    }

    /**
     * Adds and persists one maintenance record after validating the target equipment and all fields.
     *
     * @param input command in the form {@code maintain id/001 date/27-10-26 work/Repair outcome/Completed}
     * @return the newly added maintenance record
     * @throws MaintenanceRecordUnsuccessful if the command, equipment ID, date, or record save is invalid
     */
    public MaintenanceRecord addMaintenanceRecord(String input) throws MaintenanceRecordUnsuccessful {
        int previousSize = records.size();
        try {
            ParsedCommand parsed = parseCommand(input);
            if (isFutureDate(parsed.date())) {
                throw new MaintenanceRecordUnsuccessful("Maintenance date cannot be in the future.");
            }
            Equipment item = equipment.getEquipmentById(parsed.itemId());
            if (item == null) {
                throw new MaintenanceRecordUnsuccessful("Equipment ID " + parsed.itemId() + " does not exist.");
            }
            MaintenanceRecord record = new MaintenanceRecord(item.getRecordId(), parsed.date(),
                    parsed.work(), parsed.outcome());
            ArrayList<MaintenanceRecord> proposedRecords = new ArrayList<>(records);
            proposedRecords.add(record);
            save(proposedRecords);
            records.add(record);
            assert records.size() == previousSize + 1
                    : "Adding maintenance must increase the record count by one";
            assert records.getLast() == record : "The newly added maintenance record must be last";
            logger.log(Level.INFO, "Recorded maintenance for equipment with item ID {0}.", item.getItemId());
            return record;
        } catch (MaintenanceRecordUnsuccessful exception) {
            assert records.size() == previousSize : "Rejected maintenance must not change the record count";
            logger.log(Level.FINE, "Maintenance record rejected: {0}", exception.getMessage());
            throw exception;
        } catch (IllegalArgumentException exception) {
            assert records.size() == previousSize : "Invalid maintenance must not change records";
            logger.log(Level.FINE, "Maintenance record failed validation: {0}", exception.getMessage());
            throw new MaintenanceRecordUnsuccessful(exception.getMessage());
        } catch (IOException exception) {
            assert records.size() == previousSize : "Failed maintenance saves must not change records";
            logger.log(Level.WARNING, "Maintenance record could not be saved", exception);
            throw new MaintenanceRecordUnsuccessful("Could not save maintenance record: "
                    + exception.getMessage());
        }
    }

    /** Persists all current records when this handler has a backing file. */
    public void saveToStorage() throws IOException {
        save(records);
    }

    /** Parses the strict command format and validates its date and visible item ID. */
    private static ParsedCommand parseCommand(String input) throws MaintenanceRecordUnsuccessful {
        String command = input == null ? "" : input.strip();
        Matcher matcher = COMMAND.matcher(command);
        if (!matcher.matches()) {
            throw new MaintenanceRecordUnsuccessful(COMMAND_FORMAT);
        }

        String itemId = matcher.group(1);
        if (!isValidItemId(itemId)) {
            throw new MaintenanceRecordUnsuccessful("Item ID must be from 001 to 999.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(matcher.group(2), DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new MaintenanceRecordUnsuccessful("Maintenance date must use DD-MM-YY.");
        }
        return new ParsedCommand(itemId, date, matcher.group(3), matcher.group(4));
    }

    /** Saves the proposed record list without changing the live list first. */
    private void save(List<MaintenanceRecord> proposedRecords) throws IOException {
        if (storageFile != null) {
            MaintenanceStorage.save(storageFile, proposedRecords);
        }
    }

    /** Checks the visible three-digit item ID accepted by the existing equipment commands. */
    private static boolean isValidItemId(String itemId) {
        return itemId != null && itemId.matches("[0-9]{3}") && !itemId.equals("000");
    }

    /** Returns whether a maintenance date is later than today in the configured application time zone. */
    private boolean isFutureDate(LocalDate date) {
        return date.isAfter(LocalDate.now(clock));
    }

    /** Parsed and validated command values before resolving the equipment identity. */
    private record ParsedCommand(String itemId, LocalDate date, String work, String outcome) {
    }
}
