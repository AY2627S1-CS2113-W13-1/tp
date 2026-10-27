package seedu.snap.maintenance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.MaintenanceHistoryUnsuccessful;
import seedu.snap.exceptions.MaintenanceInspectionUnsuccessful;
import seedu.snap.exceptions.MaintenanceRecordUnsuccessful;

/** Handles validated maintenance-record commands and their persistent records. */
public class MaintenanceHandler {
    private static final Logger logger = Logger.getLogger(MaintenanceHandler.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern COMMAND = Pattern.compile(
            "(?sU)\\Amaintain\\s+id/([^\\s]+)\\s+date/([^\\s]+)\\s+work/(.*?)\\s+outcome/(.+)\\z");
    private static final Pattern HISTORY_COMMAND = Pattern.compile(
            "(?U)\\Ahistory\\s+maintenance/id/([0-9]{3})\\z");
    private static final Pattern INSPECTION_COMMAND = Pattern.compile(
            "(?U)\\Ainspect\\s+id/([^\\s]+)\\s+date/([^\\s]+)\\s+outcome/([^\\s]+)\\z");
    private static final String COMMAND_FORMAT = "Invalid format. Use: maintain id/[ITEM ID] "
            + "date/[DD-MM-YY] work/[DESCRIPTION] outcome/[OUTCOME].";
    private static final String HISTORY_FORMAT = "Invalid format. Use: history maintenance/id/[ITEM ID], "
            + "from 001 to 999.";
    private static final String INSPECTION_FORMAT = "Invalid format. Use: inspect id/[ITEM ID] "
            + "date/[DD-MM-YY] outcome/[PASS|FAIL].";
    private static final String INSPECTION_WORK = "Inspection";

    /** Equipment inventory used to resolve the physical record identity for each command. */
    private final EquipmentHandler equipment;
    /** In-memory maintenance records in their insertion order. */
    private final ArrayList<MaintenanceRecord> records = new ArrayList<>();
    /** File used for persistence, or null for an in-memory handler. */
    private final Path storageFile;
    /** Clock used to reject maintenance and inspection dates later than the application date. */
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

    /**
     * Selects equipment that is currently unserviceable and therefore due for maintenance or inspection.
     * Equipment is returned in visible Item ID order without changing the inventory.
     *
     * @return an unmodifiable list of equipment due for maintenance or inspection
     */
    public List<Equipment> getMaintenanceDueEquipment() {
        ArrayList<Equipment> due = new ArrayList<>();
        for (Equipment item : equipment.getEquipmentList()) {
            if (!item.isServiceable()) {
                due.add(item);
            }
        }
        due.sort(Comparator.comparing(Equipment::getItemId));
        logger.log(Level.FINE, "Selected {0} equipment item(s) due for maintenance or inspection.", due.size());
        assert due.stream().allMatch(item -> item != null && !item.isServiceable())
                : "Due equipment must be unserviceable";
        return List.copyOf(due);
    }

    /** Displays equipment that is due for maintenance or inspection. */
    public void viewMaintenanceDue(String input) throws MaintenanceInspectionUnsuccessful {
        if (input == null || !input.strip().equals("list maintenance-due")) {
            throw new MaintenanceInspectionUnsuccessful(
                    "Invalid format. Use: list maintenance-due.");
        }

        List<Equipment> due = getMaintenanceDueEquipment();
        if (due.isEmpty()) {
            System.out.println("No equipment is due for maintenance or inspection.");
            return;
        }

        System.out.println("Equipment due for maintenance or inspection:");
        System.out.printf("%-4s %-10s | %-24s | %-20s | %-10s%n",
                "No.", "ITEM ID", "NAME", "STATUS", "CONDITION");
        for (int index = 0; index < due.size(); index++) {
            Equipment item = due.get(index);
            assert item != null : "Due equipment rows must not contain null items";
            String status = item.isLoaned() ? "Loaned" : "Needs inspection";
            System.out.printf("%-4s %-10s | %-24s | %-20s | %-10d%n", (index + 1) + ")",
                    item.getItemId(), item.getItemName(), status, item.getCondition());
        }
    }

    /**
     * Completes an inspection for equipment due for inspection and records its outcome.
     * Both the equipment state and maintenance record are saved before either live object changes.
     *
     * @param input command in the form {@code inspect id/001 date/27-10-26 outcome/PASS}
     * @return the persisted inspection record
     * @throws MaintenanceInspectionUnsuccessful if validation or either save fails
     */
    public MaintenanceRecord completeInspection(String input) throws MaintenanceInspectionUnsuccessful {
        ParsedInspection inspection = parseInspectionCommand(input);
        Equipment item = equipment.getEquipmentById(inspection.itemId());
        if (item == null) {
            throw new MaintenanceInspectionUnsuccessful("Equipment ID " + inspection.itemId()
                    + " does not exist.");
        }
        if (item.isLoaned()) {
            throw new MaintenanceInspectionUnsuccessful("Cannot inspect item ID " + inspection.itemId()
                    + " while it has an active loan.");
        }
        if (item.isServiceable()) {
            throw new MaintenanceInspectionUnsuccessful("Item ID " + inspection.itemId()
                    + " is not due for maintenance or inspection.");
        }

        MaintenanceRecord record = new MaintenanceRecord(item.getRecordId(), inspection.date(), INSPECTION_WORK,
                inspection.passed() ? "PASSED" : "FAILED");
        Equipment proposal;
        try {
            proposal = equipment.prepareInspectionAssessment(item, inspection.passed());
        } catch (IllegalArgumentException exception) {
            throw new MaintenanceInspectionUnsuccessful(exception.getMessage());
        }

        ArrayList<MaintenanceRecord> proposedRecords = new ArrayList<>(records);
        proposedRecords.add(record);
        FileSnapshot previousMaintenanceFile = null;
        try {
            previousMaintenanceFile = FileSnapshot.capture(storageFile);
            save(proposedRecords);
            equipment.saveInspectionAssessment(item, proposal);
        } catch (IOException | IllegalArgumentException exception) {
            rollbackMaintenanceFile(previousMaintenanceFile);
            logger.log(Level.WARNING, "Inspection result could not be saved", exception);
            throw new MaintenanceInspectionUnsuccessful("Could not save inspection: " + exception.getMessage());
        }

        item.applyReturnAssessment(proposal);
        records.add(record);
        assert records.getLast() == record : "A successful inspection must append its maintenance record";
        assert item.isServiceable() == inspection.passed() : "Inspection result must update serviceability";
        logger.log(Level.INFO, "Recorded {0} inspection for item ID {1}.",
                new Object[]{inspection.passed() ? "passing" : "failing", item.getItemId()});
        return record;
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

    /**
     * Selects maintenance records belonging to the currently registered physical item.
     * The returned copy is sorted by maintenance date without changing stored records.
     *
     * @param input command in the form {@code history maintenance/id/001}
     * @return an unmodifiable list in chronological order
     * @throws MaintenanceHistoryUnsuccessful if syntax is invalid or the item is missing
     */
    public List<MaintenanceRecord> getMaintenanceHistory(String input)
            throws MaintenanceHistoryUnsuccessful {
        String itemId = parseHistoryItemId(input);
        Equipment item = equipment.getEquipmentById(itemId);
        if (item == null) {
            throw new MaintenanceHistoryUnsuccessful("Equipment ID " + itemId + " does not exist.");
        }

        ArrayList<MaintenanceRecord> history = new ArrayList<>();
        for (MaintenanceRecord record : records) {
            if (item.getRecordId().equals(record.getEquipmentRecordId())) {
                history.add(record);
            }
        }
        history.sort(Comparator.comparing(MaintenanceRecord::getMaintenanceDate));
        logger.log(Level.FINE, "Selected {0} maintenance record(s) for item ID {1}.",
                new Object[]{history.size(), itemId});
        assert history.stream().allMatch(record -> record != null)
                : "Maintenance history must not contain null records";
        return List.copyOf(history);
    }

    /** Displays a physical item's maintenance history without changing application state. */
    public void viewMaintenanceHistory(String input) throws MaintenanceHistoryUnsuccessful {
        String itemId = parseHistoryItemId(input);
        List<MaintenanceRecord> history = getMaintenanceHistory(input);
        if (history.isEmpty()) {
            System.out.println("No maintenance history for item ID " + itemId + ".");
            return;
        }

        System.out.println("Maintenance history for item ID " + itemId + ":");
        printMaintenanceRecords(history);
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
        try {
            return new ParsedCommand(itemId, date, matcher.group(3), matcher.group(4));
        } catch (IllegalArgumentException exception) {
            throw new MaintenanceRecordUnsuccessful(exception.getMessage());
        }
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

    /** Returns whether a date is later than today in the configured application time zone. */
    private boolean isFutureDate(LocalDate date) {
        return date.isAfter(LocalDate.now(clock));
    }

    /** Validates the read-only maintenance-history command and returns its item ID. */
    private static String parseHistoryItemId(String input) throws MaintenanceHistoryUnsuccessful {
        if (input != null && !input.matches("(?s).*\\R.*")) {
            Matcher matcher = HISTORY_COMMAND.matcher(input.strip());
            if (matcher.matches() && isValidItemId(matcher.group(1))) {
                return matcher.group(1);
            }
        }
        throw new MaintenanceHistoryUnsuccessful(HISTORY_FORMAT);
    }

    /** Parses and validates the inspection command without changing equipment or saved records. */
    private ParsedInspection parseInspectionCommand(String input)
            throws MaintenanceInspectionUnsuccessful {
        String command = input == null ? "" : input.strip();
        Matcher matcher = INSPECTION_COMMAND.matcher(command);
        if (!matcher.matches() || !isValidItemId(matcher.group(1))) {
            throw new MaintenanceInspectionUnsuccessful(INSPECTION_FORMAT);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(matcher.group(2), DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new MaintenanceInspectionUnsuccessful("Inspection date must use DD-MM-YY.");
        }
        if (isFutureDate(date)) {
            throw new MaintenanceInspectionUnsuccessful("Inspection date cannot be in the future.");
        }
        String result = matcher.group(3).toUpperCase(Locale.ROOT);
        if (!result.equals("PASS") && !result.equals("FAIL")) {
            throw new MaintenanceInspectionUnsuccessful("Inspection outcome must be PASS or FAIL.");
        }
        return new ParsedInspection(matcher.group(1), date, result.equals("PASS"));
    }

    /** Restores the maintenance file after an equipment save failure. */
    private void rollbackMaintenanceFile(FileSnapshot previousFile) {
        if (previousFile == null) {
            return;
        }
        try {
            previousFile.restore();
        } catch (IOException rollbackException) {
            logger.log(Level.SEVERE, "Could not roll back maintenance data after inspection failure",
                    rollbackException);
        }
    }

    /** Prints the selected maintenance records in a stable table format. */
    private static void printMaintenanceRecords(List<MaintenanceRecord> history) {
        System.out.printf("%-4s %-16s | %-30s | %-30s%n", "No.", "DATE", "WORK", "OUTCOME");
        for (int index = 0; index < history.size(); index++) {
            MaintenanceRecord record = history.get(index);
            assert record != null : "Maintenance history rows must not contain null records";
            System.out.printf("%-4s %-16s | %-30s | %-30s%n", (index + 1) + ")",
                    record.getMaintenanceDate(), record.getWork(), record.getOutcome());
        }
    }

    /** Parsed values for a validated inspection command. */
    private record ParsedInspection(String itemId, LocalDate date, boolean passed) {
    }

    /** Captures the maintenance file so a later equipment-save failure can be rolled back. */
    private record FileSnapshot(Path path, boolean existed, byte[] contents) {
        static FileSnapshot capture(Path path) throws IOException {
            if (path == null || Files.notExists(path)) {
                return path == null ? null : new FileSnapshot(path, false, new byte[0]);
            }
            return new FileSnapshot(path, true, Files.readAllBytes(path));
        }

        void restore() throws IOException {
            if (!existed) {
                Files.deleteIfExists(path);
                return;
            }
            Path parent = path.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, "maintenance-rollback-", ".tmp");
            try {
                Files.write(temporary, contents);
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    /** Parsed and validated command values before resolving the equipment identity. */
    private record ParsedCommand(String itemId, LocalDate date, String work, String outcome) {
    }
}
