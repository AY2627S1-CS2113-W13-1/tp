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
                Equipment equipment = EquipmentHandler.parseEquipment(line);
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

    /** Converts an equipment record to the documented add-command format. */
    private static String toStorageLine(Equipment equipment) {
        return "add i/" + equipment.getItemName()
                + " id/" + equipment.getItemId()
                + " type/" + equipment.getType()
                + " cond/" + equipment.getCondition();
    }
}
