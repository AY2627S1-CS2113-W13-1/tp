package seedu.snap.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.EquipmentHandler;

/** Tests bundle storage validation, exact equipment identity matching and save failures. */
class BundleStorageTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void load_malformedRecord_reportsLineNumber() throws Exception {
        Path file = temporaryDirectory.resolve("bundles.txt");
        Files.writeString(file, "not a bundle record" + System.lineSeparator());
        BundleHandler handler = new BundleHandler(new EquipmentHandler(), file);

        IOException exception = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(exception.getMessage().contains("line 1"));
    }

    @Test
    void load_missingEquipment_preservesExistingBundles() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/001 type/camera cond/5 sensor/Full frame");
        Path file = temporaryDirectory.resolve("bundles.txt");
        String missingRecordId = UUID.randomUUID().toString();
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("Missing item bundle".getBytes(StandardCharsets.UTF_8));
        Files.writeString(file, "bundle id/001 name64/" + encodedName
                + " items/001~" + missingRecordId + System.lineSeparator());
        BundleHandler handler = new BundleHandler(equipment, file);
        handler.addBundle("add-bundle name/Existing bundle");

        IOException exception = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(exception.getMessage().contains("missing equipment ID 001"));
        assertEquals(1, handler.getBundleList().size());
        assertEquals("Existing bundle", handler.getBundleList().getFirst().getBundleName());
    }

    @Test
    void save_blockedDestination_preservesSentinel() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/001 type/camera cond/5 sensor/Full frame");
        Path file = temporaryDirectory.resolve("bundles.txt");
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep this data");
        BundleHandler handler = new BundleHandler(equipment, file);
        handler.addBundle("add-bundle name/Travel Kit");

        assertThrows(IOException.class, handler::saveToStorage);

        assertEquals("keep this data", Files.readString(sentinel));
    }
}
