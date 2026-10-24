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
    /** Storage-only metadata appears before the add record so free-form fields remain unambiguous. */
    private static final String SERVICEABILITY_PREFIX = "serviceable/";

    private EquipmentStorage() {
        // Utility class; do not instantiate.
    }

    /**
     * Loads one add command per non-blank line from the supplied file.
     * A missing file represents an empty inventory.
     *
     * @param file file containing saved equipment
     * @return parsed equipment records
     * @throws IOException if the file cannot be read or contains an invalid record
     */
    static List<Equipment> load(Path file) throws IOException {
        List<Equipment> loaded = new ArrayList<>();
        if (Files.notExists(file)) {
            return loaded;
        }

        Set<String> ids = new HashSet<>();
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
                loaded.add(equipment);
            } catch (EquipmentAdditionUnsuccessful | IllegalArgumentException exception) {
                throw new IOException("Invalid equipment record on line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }
        return loaded;
    }

    /** Loads legacy add records as serviceable and reads the assessment metadata on newer records. */
    private static Equipment parseStorageLine(String line) throws EquipmentAdditionUnsuccessful {
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

    /** Adds serviceability metadata before the existing add record to preserve free-form field values. */
    private static String toStorageLine(Equipment equipment) {
        String record = SERVICEABILITY_PREFIX + (equipment.isServiceable() ? "yes" : "no") + "\tadd i/"
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
