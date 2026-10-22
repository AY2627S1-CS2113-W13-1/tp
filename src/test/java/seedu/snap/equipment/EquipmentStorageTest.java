package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests equipment storage validation and failure-safe file replacement. */
class EquipmentStorageTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void load_malformedRecord_reportsTheLineNumber() throws IOException {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(file, "add i/Camera id/001 type/camera cond/1\nnot an equipment record\n");

        IOException exception = assertThrows(IOException.class, () -> EquipmentStorage.load(file));

        assertTrue(exception.getMessage().contains("line 2"));
    }

    @Test
    void load_duplicateId_rejectsTheSavedFile() throws IOException {
        Path file = temporaryDirectory.resolve("equipment.txt");
        String record = "add i/Camera id/001 type/camera cond/1";
        Files.writeString(file, record + System.lineSeparator() + record + System.lineSeparator());

        IOException exception = assertThrows(IOException.class, () -> EquipmentStorage.load(file));

        assertTrue(exception.getMessage().contains("Duplicate equipment ID 001"));
    }

    @Test
    void save_failedReplacement_preservesDestinationAndCleansTemporaryFile() throws IOException {
        Path destination = temporaryDirectory.resolve("equipment.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        Equipment equipment = new Equipment("Camera", "001", "camera", 1);

        assertThrows(IOException.class, () -> EquipmentStorage.save(destination, List.of(equipment)));

        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("equipment-")));
        }
    }
}
