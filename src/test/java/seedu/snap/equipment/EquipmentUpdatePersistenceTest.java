package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;

/** Verifies that updates preserve both equipment identity and failure-safe persistence. */
class EquipmentUpdatePersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void updateEquipment_restart_restoresOnlyTheSuppliedChanges() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        assertSame(original, handler.updateEquipment("update id/001 i/Film Camera cond/5"));
        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        Equipment restored = restarted.getEquipmentList().getFirst();
        assertEquals("Film Camera", restored.getItemName());
        assertEquals("camera", restored.getType());
        assertEquals(5, restored.getCondition());
        assertEquals("001", restored.getItemId());
    }

    @Test
    void updateEquipment_rejectedValues_preserveSavedFileAndAllowRecovery() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        String saved = Files.readString(file);
        assertThrows(EquipmentUpdateUnsuccessful.class,
                () -> handler.updateEquipment("update id/001 i/Changed cond/11"));
        assertEquals(saved, Files.readString(file));
        assertEquals("Camera", original.getItemName());
        assertEquals(1, original.getCondition());
        handler.updateEquipment("update id/001 cond/10");
        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        assertEquals(10, restarted.getEquipmentList().getFirst().getCondition());
    }

    @Test
    void updateEquipment_failedWrite_preservesObjectAndRecoversWhenStorageIsFixed() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        Files.delete(file);
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep");
        EquipmentUpdateUnsuccessful error = assertThrows(EquipmentUpdateUnsuccessful.class,
                () -> handler.updateEquipment("update id/001 i/Changed type/camera cond/5"));
        assertTrue(error.getMessage().startsWith("Could not save equipment update:"));
        assertEquals("Camera", original.getItemName());
        assertEquals("camera", original.getType());
        assertEquals(1, original.getCondition());
        assertSame(original, handler.getEquipmentList().getFirst());
        assertEquals(List.of(original), handler.getEquipmentList());
        assertEquals("keep", Files.readString(sentinel));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("equipment-")));
        }
        Files.delete(sentinel);
        Files.delete(file);
        assertSame(original, handler.updateEquipment("update id/001 cond/5"));
        assertTrue(Files.readString(file).contains("cond/5"));
    }
}
