package seedu.duke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads and saves equipment in a UTF-8 text file, with one add command per line.
 */
class EquipmentStorage {
    /**
     * Treats a missing file as an empty inventory and rejects damaged or duplicate records.
     */
    static List<Equipment> load(Path file) throws IOException {
        List<Equipment> items = new ArrayList<>();
        if (Files.notExists(file)) {
            return items;
        }
        Set<String> ids = new HashSet<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            try {
                Equipment item = Equipment.parse(line);
                if (!ids.add(item.getId())) {
                    throw new IllegalArgumentException("Duplicate equipment ID " + item.getId() + ".");
                }
                items.add(item);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid record on line " + lineNumber + ": " + e.getMessage(), e);
            }
        }
        return items;
    }

    /**
     * Writes to a temporary file before replacing saved records, so a failed write does not erase them.
     */
    static void save(Path file, List<Equipment> items) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        List<String> lines = new ArrayList<>();
        for (Equipment item : items) {
            lines.add(item.toFileLine());
        }
        Path temporaryFile = Files.createTempFile(parent, "equipment-", ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
