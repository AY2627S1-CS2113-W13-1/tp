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
import seedu.snap.exceptions.BundleAdditionUnsuccessful;

/** Tests bundle storage validation, exact equipment identity matching and save failures. */
class BundleStorageTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void load_invalidTypedRecords_rejectsEachWithoutPublishing() throws Exception {
        Path file = Files.createTempFile("invalid-bundles-", ".txt");
        String uuid = UUID.randomUUID().toString();
        String secondUuid = UUID.randomUUID().toString();
        String[] records = {
            "bundle wrong/B001 name64/S2l0 items/",
            "bundle id/E001 name64/S2l0 items/",
            "bundle id/B001 wrong/S2l0 items/",
            "bundle id/B001 name64/! items/",
            "bundle id/B001 name64/ items/",
            "bundle id/B001 name64/S2l0 wrong/",
            "bundle id/B001 name64/S2l0 items/E001~invalid",
            "bundle id/B001 name64/S2l0 items/L001~" + uuid,
            "bundle id/B001 name64/S2l0 items/E001~" + uuid + ",E001~" + secondUuid,
            "bundle id/B001 name64/S2l0 items/E001~" + uuid + ",E002~" + uuid
        };
        try {
            for (String record : records) {
                Files.writeString(file, record);
                assertThrows(IOException.class, () -> BundleStorage.load(file), record);
            }
            Files.writeString(file, "\n");
            assertTrue(BundleStorage.load(file).bundles().isEmpty());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void defaultHandler_startsWithEmptyInventory() {
        BundleHandler handler = new BundleHandler();
        assertTrue(handler.getBundleList().isEmpty());
        assertThrows(BundleAdditionUnsuccessful.class, () -> handler.addBundle(null));
        assertTrue(handler.getBundleList().isEmpty());
    }

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
        equipment.addEquipment("add i/Camera id/E001 type/camera cond/5 sensor/Full frame");
        Path file = temporaryDirectory.resolve("bundles.txt");
        String missingRecordId = UUID.randomUUID().toString();
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("Missing item bundle".getBytes(StandardCharsets.UTF_8));
        Files.writeString(file, "bundle id/B001 name64/" + encodedName
                + " items/001~" + missingRecordId + System.lineSeparator());
        BundleHandler handler = new BundleHandler(equipment, file);
        handler.addBundle("add-bundle name/Existing bundle");

        IOException exception = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(exception.getMessage().contains("missing equipment ID E001"));
        assertEquals(1, handler.getBundleList().size());
        assertEquals("Existing bundle", handler.getBundleList().getFirst().getBundleName());
    }

    @Test
    void load_legacyBareIdentifiers_migratesBundleAndMembershipOnSave() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/E001 type/camera cond/5 sensor/Full frame");
        String recordId = equipment.getEquipmentList().getFirst().getRecordId();
        Path file = temporaryDirectory.resolve("bundles.txt");
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("Legacy kit".getBytes(StandardCharsets.UTF_8));
        Files.writeString(file, "bundle id/001 name64/" + encodedName
                + " items/001~" + recordId + System.lineSeparator());

        BundleHandler handler = new BundleHandler(equipment, file);
        handler.loadFromStorage();
        handler.saveToStorage();

        assertEquals("B001", handler.getBundleList().getFirst().getBundleId());
        assertTrue(Files.readString(file).contains("id/B001"));
        assertTrue(Files.readString(file).contains("items/E001~" + recordId));
    }

    @Test
    void load_lowercaseIdentifiers_migratesAndPersistsUppercaseValues() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/E001 type/camera cond/5 sensor/Full frame");
        String recordId = equipment.getEquipmentList().getFirst().getRecordId();
        Path file = temporaryDirectory.resolve("bundles.txt");
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("Lowercase kit".getBytes(StandardCharsets.UTF_8));
        Files.writeString(file, "bundle id/b001 name64/" + encodedName
                + " items/e001~" + recordId + System.lineSeparator());

        BundleHandler handler = new BundleHandler(equipment, file);
        handler.loadFromStorage();
        handler.saveToStorage();

        assertEquals("B001", handler.getBundleList().getFirst().getBundleId());
        assertTrue(Files.readString(file).contains("id/B001"));
        assertTrue(Files.readString(file).contains("items/E001~" + recordId));
    }

    @Test
    void load_collidingLegacyAndCanonicalBundleIds_rejectsFile() throws Exception {
        Path file = temporaryDirectory.resolve("bundles.txt");
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("Kit".getBytes(StandardCharsets.UTF_8));
        String first = "bundle id/001 name64/" + encodedName + " items/";
        String second = "bundle id/B001 name64/" + encodedName + " items/";
        Files.writeString(file, first + System.lineSeparator() + second + System.lineSeparator());

        IOException exception = assertThrows(IOException.class,
                () -> BundleStorage.load(file));

        assertTrue(exception.getMessage().contains("Duplicate bundle ID B001"));
    }

    @Test
    void save_blockedDestination_preservesSentinel() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/E001 type/camera cond/5 sensor/Full frame");
        Path file = temporaryDirectory.resolve("bundles.txt");
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep this data");
        BundleHandler handler = new BundleHandler(equipment, file);
        handler.addBundle("add-bundle name/Travel Kit");

        assertThrows(IOException.class, handler::saveToStorage);

        assertEquals("keep this data", Files.readString(sentinel));
    }

    @Test
    void removeMembership_saveFailure_restoresMembership() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/E001 type/camera cond/5 sensor/Full frame");
        Path file = temporaryDirectory.resolve("bundles.txt");
        Files.createDirectory(file);
        Files.writeString(file.resolve("keep.txt"), "keep this data");
        BundleHandler handler = new BundleHandler(equipment, file);
        handler.addBundle("add-bundle name/Travel Kit");
        handler.addItemsToBundle("add-to-bundle id/B001 E001");

        BundleAdditionUnsuccessful exception = assertThrows(BundleAdditionUnsuccessful.class,
                () -> handler.removeItemsFromBundle("remove-from-bundle id/B001 E001"));

        assertTrue(exception.getMessage().contains("Could not save bundle removal:"));
        assertTrue(handler.containsEquipmentId("E001"));
    }
}
