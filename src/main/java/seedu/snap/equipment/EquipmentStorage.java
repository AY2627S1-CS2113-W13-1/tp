package seedu.snap.equipment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Loads and saves equipment records in a readable UTF-8 text file. */
final class EquipmentStorage {
    /** Permanent physical-item identity appears before any existing storage metadata. */
    private static final String RECORD_ID_PREFIX = "recordId/";
    /** Storage-only metadata appears before the add record so free-form fields remain unambiguous. */
    private static final String SERVICEABILITY_PREFIX = "serviceable/";

    private EquipmentStorage() {
        // Utility class; do not instantiate.
    }

    /**
     * Loads one equipment record per non-blank line from the supplied file.
     * Legacy records receive permanent identities, saved before loaded records are returned.
     * A missing file represents an empty inventory.
     *
     * @param file file containing saved equipment
     * @return parsed equipment records
     * @throws IOException if reading, validation, or saving migrated identities fails
     */
    static List<Equipment> load(Path file) throws IOException {
        List<Equipment> loaded = new ArrayList<>();
        if (Files.notExists(file)) {
            return loaded;
        }

        Set<String> ids = new HashSet<>();
        Set<String> recordIds = new HashSet<>();
        boolean requiresMigration = false;
        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            try {
                Equipment equipment = parseStorageLine(line);
                if (!ids.add(equipment.getItemId())) {
                    throw new IllegalArgumentException("Duplicate equipment ID " + equipment.getItemId() + ".");
                }
                if (!recordIds.add(equipment.getRecordId())) {
                    throw new IllegalArgumentException("Duplicate equipment record ID "
                            + equipment.getRecordId() + ".");
                }
                loaded.add(equipment);
                requiresMigration |= !line.startsWith(RECORD_ID_PREFIX);
            } catch (EquipmentAdditionUnsuccessful | IllegalArgumentException exception) {
                throw new IOException("Invalid equipment record on line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }
        // Publish migrated identities only after they are durable, so a restart cannot assign different identities.
        if (requiresMigration) {
            try {
                save(file, loaded);
            } catch (IOException exception) {
                throw new IOException("Could not save migrated equipment identities: " + exception.getMessage(),
                        exception);
            }
        }
        return loaded;
    }

    /** Reads permanent identity metadata before parsing older serviceability or add record formats. */
    private static Equipment parseStorageLine(String line) throws EquipmentAdditionUnsuccessful {
        if (!line.startsWith(RECORD_ID_PREFIX)) {
            return parseServiceabilityLine(line);
        }
        int delimiter = line.indexOf('\t');
        if (delimiter < 0) {
            throw new IllegalArgumentException("Equipment record ID must precede a tab and serviceability metadata.");
        }
        String recordId = line.substring(RECORD_ID_PREFIX.length(), delimiter);
        String remaining = line.substring(delimiter + 1);
        if (!remaining.startsWith(SERVICEABILITY_PREFIX)) {
            throw new IllegalArgumentException("Equipment record ID must be followed by serviceability metadata.");
        }
        Equipment equipment = parseServiceabilityLine(remaining);
        equipment.restoreRecordId(recordId);
        return equipment;
    }

    /** Loads legacy add records as serviceable and reads assessment metadata without parsing free-form field text. */
    private static Equipment parseServiceabilityLine(String line) throws EquipmentAdditionUnsuccessful {
        if (!line.startsWith(SERVICEABILITY_PREFIX)) {
            return EquipmentHandler.parseEquipment(line);
        }
        int delimiter = line.indexOf('\t');
        if (delimiter < 0) {
            throw new IllegalArgumentException("Serviceability metadata must precede a tab and an add record.");
        }
        String serviceability = line.substring(SERVICEABILITY_PREFIX.length(), delimiter);
        if (!serviceability.equals("yes") && !serviceability.equals("no")) {
            throw new IllegalArgumentException("Saved serviceability must be yes or no.");
        }
        Equipment equipment = EquipmentHandler.parseEquipment(line.substring(delimiter + 1));
        equipment.setServiceable(serviceability.equals("yes"));
        return equipment;
    }

    /**
     * Saves records through a temporary file before replacing the destination.
     * This preserves the previous file if writing the new contents fails.
     *
     * @param file destination file
     * @param equipment records to save
     * @throws IOException if the directory or file cannot be written
     */
    static void save(Path file, List<Equipment> equipment) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "equipment-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (Equipment item : equipment) {
                assert item != null : "Equipment being saved must not be null";
                lines.add(toStorageLine(item));
            }
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** Adds permanent identity and serviceability metadata before the add record, preserving free-form field values. */
    private static String toStorageLine(Equipment equipment) {
        String record = RECORD_ID_PREFIX + equipment.getRecordId() + "\t"
                + SERVICEABILITY_PREFIX + (equipment.isServiceable() ? "yes" : "no") + "\tadd i/"
                + equipment.getItemName()
                + " id/" + equipment.getItemId()
                + " type/" + equipment.getType()
                + " cond/" + equipment.getCondition()
                + " " + equipment.getDetails();
        if (!equipment.getRemarks().isEmpty()) {
            record += " remarks/" + equipment.getRemarks();
        }
        return record;
    }
}
