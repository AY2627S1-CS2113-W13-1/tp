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

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

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
    void saveAndLoad_allSubtypeDetails_preservesFieldsAndTypes() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        String[] commands = {
            "add i/Camera id/001 type/CAMERA cond/7 sensor/Full frame damage/SHUTTER_FAULT",
            "add i/Lens id/002 type/lens cond/6 zoom/24 to 70 mm damage/AUTOFOCUS_FAULT",
            "add i/Battery id/003 type/battery cond/5 capacity/2000 mAh damage/CONTACT_DAMAGE",
            "add i/Tripod id/004 type/tripod cond/4 thread/Quarter inch damage/HEAD_FAULT"
        };
        for (String command : commands) {
            handler.addEquipment(command);
        }

        EquipmentHandler reloadedHandler = new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();
        List<Equipment> expected = handler.getEquipmentList();
        List<Equipment> actual = reloadedHandler.getEquipmentList();
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getClass(), actual.get(i).getClass());
            assertEquals(expected.get(i).getItemName(), actual.get(i).getItemName());
            assertEquals(expected.get(i).getItemId(), actual.get(i).getItemId());
            assertEquals(expected.get(i).getType(), actual.get(i).getType());
            assertEquals(expected.get(i).getCondition(), actual.get(i).getCondition());
            assertEquals(expected.get(i).getDetails(), actual.get(i).getDetails());
        }
        assertTrue(Files.readString(file).contains("sensor/Full frame damage/SHUTTER_FAULT"));
        assertTrue(Files.readString(file).contains("zoom/24 to 70 mm damage/AUTOFOCUS_FAULT"));
        assertTrue(Files.readString(file).contains("capacity/2000 mAh damage/CONTACT_DAMAGE"));
        assertTrue(Files.readString(file).contains("thread/Quarter inch damage/HEAD_FAULT"));
    }

    @Test
    void load_recordsWithoutSubtypeFields_usesDefaults() throws IOException {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(file, "add i/Camera id/001 type/camera cond/1\n"
                + "add i/Lens id/002 type/lens cond/2\n"
                + "add i/Battery id/003 type/battery cond/3\n"
                + "add i/Tripod id/004 type/tripod cond/4\n");

        List<Equipment> loaded = EquipmentStorage.load(file);

        assertEquals(List.of("sensor/unspecified damage/NONE", "zoom/unspecified damage/NONE",
                "capacity/unspecified damage/NONE", "thread/unspecified damage/NONE"),
                loaded.stream().map(Equipment::getDetails).toList());
    }

    @Test
    void load_oldRecordsWithDetailMarkersInNames_preservesNamesAndTypesAfterSaving() throws IOException {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.write(file, List.of(
                "add i/Camera sensor/cleaning kit id/001 type/camera cond/1",
                "add i/Lens zoom/range guide id/002 type/lens cond/2",
                "add i/Battery capacity/test unit id/003 type/battery cond/3",
                "add i/Tripod thread/adapter id/004 type/tripod cond/4",
                "add i/Camera damage/check unit id/005 type/camera cond/5"));
        List<String> expectedNames = List.of("Camera sensor/cleaning kit", "Lens zoom/range guide",
                "Battery capacity/test unit", "Tripod thread/adapter", "Camera damage/check unit");
        List<Class<? extends Equipment>> expectedClasses = List.of(Camera.class, Lens.class,
                Battery.class, Tripod.class, Camera.class);

        List<Equipment> loaded = EquipmentStorage.load(file);

        assertEquals(expectedNames, loaded.stream().map(Equipment::getItemName).toList());
        assertEquals(expectedClasses, loaded.stream().map(Equipment::getClass).toList());

        EquipmentStorage.save(file, loaded);
        List<Equipment> reloaded = EquipmentStorage.load(file);

        assertEquals(expectedNames, reloaded.stream().map(Equipment::getItemName).toList());
        assertEquals(expectedClasses, reloaded.stream().map(Equipment::getClass).toList());
    }

    @Test
    void load_invalidSubtypeDetails_reportsLineAndPreservesCurrentInventory() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Original camera id/001 type/camera cond/1");
        Files.writeString(file, "add i/Camera id/001 type/camera cond/1\n"
                + "add i/Lens id/002 type/lens cond/5 damage/SHUTTER_FAULT\n");

        IOException exception = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(exception.getMessage().contains("line 2"));
        assertEquals(List.of(original), handler.getEquipmentList());
    }

    @Test
    void addEquipment_failedSave_preservesCurrentInventory() throws IOException {
        Path destination = temporaryDirectory.resolve("equipment.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        EquipmentHandler handler = new EquipmentHandler(destination);

        assertThrows(EquipmentAdditionUnsuccessful.class,
                () -> handler.addEquipment("add i/Camera id/001 type/camera cond/5 sensor/Full frame"));

        assertTrue(handler.getEquipmentList().isEmpty());
        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
    }

    @Test
    void load_legacyRecordWithRemarksMarkerInName_preservesName() throws IOException {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(file, "add i/Camera remarks/adapter id/001 type/camera cond/1");

        Equipment loaded = EquipmentStorage.load(file).getFirst();

        assertEquals("Camera remarks/adapter", loaded.getItemName());
        assertEquals("", loaded.getRemarks());
    }

    @Test
    void save_failedReplacement_preservesDestinationAndCleansTemporaryFile() throws IOException {
        Path destination = temporaryDirectory.resolve("equipment.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        Equipment equipment = new Camera("Camera", "001", 1);

        assertThrows(IOException.class, () -> EquipmentStorage.save(destination, List.of(equipment)));

        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("equipment-")));
        }
    }
}
