package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests storage-location persistence and compatibility with older equipment records. */
class EquipmentStorageLocationPersistenceTest {
    private static final String RECORD_ID = "e21c9254-ab91-4624-82eb-9b4423e5a795";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void saveAndLoad_locationPreservesIdentityDetailsAndValue() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment camera = handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        camera.setStorageLocation("Cabinet A-3");
        handler.saveToStorage();

        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        Equipment restored = restarted.getEquipmentList().getFirst();

        assertEquals(camera.getRecordId(), restored.getRecordId());
        assertEquals(camera.getDetails(), restored.getDetails());
        assertEquals("Cabinet A-3", restored.getStorageLocation());
        assertTrue(Files.readString(file).contains("location/Cabinet A-3"));
    }

    @Test
    void load_legacyRecordsWithoutLocation_usesEmptyDefault() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.write(file, List.of(
                "recordId/" + RECORD_ID + "\tserviceable/yes\tadd i/Camera id/001 type/camera cond/5",
                "add i/Lens id/002 type/lens cond/5"));

        List<Equipment> loaded = EquipmentStorage.load(file);

        assertEquals(List.of("", ""), loaded.stream().map(Equipment::getStorageLocation).toList());
        assertTrue(Files.readAllLines(file).stream().allMatch(line -> line.contains("location/")));
    }

    @Test
    void load_invalidLocation_rejectsRecordWithoutPublishingIt() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        String invalidLocation = "x".repeat(Equipment.MAX_STORAGE_LOCATION_LENGTH + 1);
        String saved = "recordId/" + RECORD_ID + "\tserviceable/yes\tlocation/"
                + invalidLocation + "\tadd i/Camera id/001 type/camera cond/5";
        Files.writeString(file, saved);

        IOException exception = assertThrows(IOException.class, () -> EquipmentStorage.load(file));

        assertTrue(exception.getMessage().contains("line 1"));
        assertFalse(exception.getMessage().isBlank());
        assertEquals(saved, Files.readString(file));
    }

    @Test
    void returnAssessment_preservesLocationAndPhysicalIdentity() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment camera = handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        handler.updateEquipment("update id/001 location/Cabinet A-3");
        String recordId = camera.getRecordId();
        Equipment assessed = handler.prepareReturnAssessment(camera, false, "SHUTTER_FAULT", "Needs repair");

        handler.saveReturnAssessment(camera, assessed);
        camera.applyReturnAssessment(assessed);
        Equipment restored = EquipmentStorage.load(file).getFirst();

        assertEquals(recordId, camera.getRecordId());
        assertEquals(recordId, restored.getRecordId());
        assertEquals("Cabinet A-3", camera.getStorageLocation());
        assertEquals("Cabinet A-3", restored.getStorageLocation());
        assertFalse(camera.isServiceable());
        assertFalse(restored.isServiceable());
    }
}
