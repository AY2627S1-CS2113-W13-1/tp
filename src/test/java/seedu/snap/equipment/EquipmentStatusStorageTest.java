package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies status metadata persistence, migration and corruption handling. */
class EquipmentStatusStorageTest {
    private static final String RECORD_ID = "e21c9254-ab91-4624-82eb-9b4423e5a795";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void saveAndLoad_preservesStatusAndHistory() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Equipment camera = new Camera("Camera", "001", 5);
        camera.restoreRecordId(RECORD_ID);
        camera.restoreStatusState(EquipmentStatus.LOST, List.of(
                new EquipmentStatusChange(EquipmentStatus.MISSING, Instant.parse("2026-10-27T10:00:00Z")),
                new EquipmentStatusChange(EquipmentStatus.LOST, Instant.parse("2026-10-27T11:00:00Z"))));

        EquipmentStorage.save(file, List.of(camera));
        Equipment restored = EquipmentStorage.load(file).getFirst();

        assertEquals(RECORD_ID, restored.getRecordId());
        assertEquals(EquipmentStatus.LOST, restored.getStatus());
        assertEquals(camera.getStatusHistory(), restored.getStatusHistory());
        assertTrue(Files.readString(file).contains("statusHistory/missing@2026-10-27T10:00:00Z"));
    }

    @Test
    void loadLegacyRecord_migratesToPresentStatusWithoutHistory() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(file, "add i/Camera id/001 type/camera cond/5\n");

        Equipment restored = EquipmentStorage.load(file).getFirst();

        assertEquals(EquipmentStatus.PRESENT, restored.getStatus());
        assertTrue(restored.getStatusHistory().isEmpty());
        assertTrue(Files.readString(file).contains("status/present"));
    }

    @Test
    void loadInvalidStatusMetadata_preservesSavedBytes() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        String invalid = "recordId/" + RECORD_ID + "\tserviceable/yes\tstatus/lost\t"
                + "statusHistory/missing@not-a-time\tadd i/Camera id/001 type/camera cond/5\n";
        Files.writeString(file, invalid);
        byte[] saved = Files.readAllBytes(file);

        assertThrows(Exception.class, () -> EquipmentStorage.load(file));
        assertArrayEquals(saved, Files.readAllBytes(file));
    }

    @Test
    void loadStatusHistoryWithWrongFinalStatus_rejectsRecord() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(file, "recordId/" + RECORD_ID + "\tserviceable/yes\tstatus/missing\t"
                + "statusHistory/lost@2026-10-27T10:00:00Z\tadd i/Camera id/001 type/camera cond/5\n");

        Exception error = assertThrows(Exception.class, () -> EquipmentStorage.load(file));

        assertTrue(error.getMessage().contains("line 1"));
    }
}
