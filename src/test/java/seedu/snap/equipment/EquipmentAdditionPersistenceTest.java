package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Verifies that the addition refactoring preserves failure-safe saves and Unicode validation. */
class EquipmentAdditionPersistenceTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void addEquipment_failedSave_keepsInventoryEmptyAndAllowsRecovery() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep");
        EquipmentHandler handler = new EquipmentHandler(file);
        assertThrows(EquipmentAdditionUnsuccessful.class,
                // @@author yarnieee
                () -> handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1"));
        // @@author
        assertEquals(List.of(), handler.getEquipmentList());
        assertEquals("keep", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(file);
        // @@author yarnieee
        Equipment created = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author
        assertEquals(List.of(created), handler.getEquipmentList());
        EquipmentHandler restored = new EquipmentHandler(file);
        restored.loadFromStorage();
        assertEquals("Camera", restored.getEquipmentList().getFirst().getItemName());
    }

    @Test
    void addEquipment_nonbreakingWhitespaceOnlyFields_doesNotOverwriteSavedRecords() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        // @@author yarnieee
        Equipment original = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author
        String saved = Files.readString(file);
        for (String space : new String[]{"\u00a0", "\u2007", "\u202f"}) {
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    // @@author yarnieee
                    () -> handler.addEquipment("eqm-add name/" + space + " e/E002 type/camera cond/1"));
            // @@author
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    // @@author yarnieee
                    () -> handler.addEquipment("eqm-add name/Tripod e/E002 type/" + space + " cond/1"));
            // @@author
            assertEquals(saved, Files.readString(file));
            assertEquals(List.of(original), handler.getEquipmentList());
        }
    }
}
