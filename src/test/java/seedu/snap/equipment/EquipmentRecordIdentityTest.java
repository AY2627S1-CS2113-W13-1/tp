package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies permanent equipment identities remain distinct when visible Item IDs are reused. */
class EquipmentRecordIdentityTest {
    private static final String FIRST_RECORD_ID = "e21c9254-ab91-4624-82eb-9b4423e5a795";
    private static final String SECOND_RECORD_ID = "98abfd72-32b3-49b7-b2af-a1f4fe949435";
    private static final String LEGACY_CAMERA = "add i/Camera id/001 type/camera cond/7";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void newEquipment_identicalVisibleDetails_haveDifferentCanonicalIdentities() {
        Camera first = new Camera("Camera", "001", 7);
        Camera second = new Camera("Camera", "001", 7);

        assertEquals(UUID.fromString(first.getRecordId()).toString(), first.getRecordId());
        assertEquals(UUID.fromString(second.getRecordId()).toString(), second.getRecordId());
        assertNotEquals(first.getRecordId(), second.getRecordId());
    }

    @Test
    void saveAndLoad_keyedRecords_preserveIdentityWithoutRewritingFile() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(LEGACY_CAMERA);
        FileTime previousTime = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(file, previousTime);
        byte[] saved = Files.readAllBytes(file);

        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();

        assertEquals(original.getRecordId(), restarted.getEquipmentList().getFirst().getRecordId());
        assertArrayEquals(saved, Files.readAllBytes(file));
        assertEquals(previousTime, Files.getLastModifiedTime(file));
        assertTrue(Files.readString(file).startsWith("recordId/" + original.getRecordId() + "\tserviceable/yes\t"));
    }

    @Test
    void load_legacyFormats_saveIdentitiesBeforeReturningAndPreserveFreeText() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.write(file, List.of(
                "add i/Camera recordId/label id/001 type/camera cond/7 remarks/recordId/not-an-id id/002",
                "serviceable/no\tadd i/Lens id/002 type/lens cond/6 zoom/recordId/label remarks/serviceable/yes"));

        List<Equipment> loaded = EquipmentStorage.load(file);
        byte[] migrated = Files.readAllBytes(file);
        List<Equipment> restarted = EquipmentStorage.load(file);

        assertEquals(loaded.stream().map(Equipment::getRecordId).toList(),
                restarted.stream().map(Equipment::getRecordId).toList());
        assertNotEquals(loaded.get(0).getRecordId(), loaded.get(1).getRecordId());
        assertTrue(Files.readAllLines(file).stream().allMatch(line -> line.startsWith("recordId/")));
        assertArrayEquals(migrated, Files.readAllBytes(file));
        assertEquals("Camera recordId/label", restarted.get(0).getItemName());
        assertEquals("recordId/not-an-id id/002", restarted.get(0).getRemarks());
        assertTrue(restarted.get(0).isServiceable());
        assertFalse(restarted.get(1).isServiceable());
        assertEquals("zoom/recordId/label damage/NONE", restarted.get(1).getDetails());
        assertEquals("serviceable/yes", restarted.get(1).getRemarks());
    }

    @Test
    void load_mixedFormats_preservesExplicitIdentityAndAssignsOnlyMissingIdentity() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.write(file, List.of(keyed(FIRST_RECORD_ID, LEGACY_CAMERA),
                "serviceable/yes\tadd i/Tripod id/002 type/tripod cond/8"));

        List<Equipment> loaded = EquipmentStorage.load(file);

        assertEquals(FIRST_RECORD_ID, loaded.get(0).getRecordId());
        assertNotEquals(FIRST_RECORD_ID, loaded.get(1).getRecordId());
        assertEquals(loaded.stream().map(Equipment::getRecordId).toList(),
                EquipmentStorage.load(file).stream().map(Equipment::getRecordId).toList());
    }

    @Test
    void load_invalidLaterLegacyRecord_keepsOriginalFileAndCurrentInventory() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(LEGACY_CAMERA);
        Files.write(file, List.of(LEGACY_CAMERA, "not an equipment record"));
        byte[] saved = Files.readAllBytes(file);

        IOException exception = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(exception.getMessage().contains("line 2"));
        assertArrayEquals(saved, Files.readAllBytes(file));
        assertSame(original, handler.getEquipmentList().getFirst());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-uuid", "1-1-1-1-1", "E21C9254-AB91-4624-82EB-9B4423E5A795",
        " e21c9254-ab91-4624-82eb-9b4423e5a795", "e21c9254-ab91-4624-82eb-9b4423e5a795 "})
    void restoreRecordId_invalidCanonicalValue_rejectsAtRuntimeWithoutChangingIdentity(String value) {
        Equipment equipment = new Camera("Camera", "001", 7);
        String original = equipment.getRecordId();

        assertThrows(IllegalArgumentException.class, () -> equipment.restoreRecordId(value));

        assertEquals(original, equipment.getRecordId());
    }

    @Test
    void restoreRecordId_null_rejectsWithoutChangingIdentity() {
        Equipment equipment = new Camera("Camera", "001", 7);
        String original = equipment.getRecordId();

        assertThrows(IllegalArgumentException.class, () -> equipment.restoreRecordId(null));

        assertEquals(original, equipment.getRecordId());
    }

    @Test
    void load_malformedMetadata_rejectsWithLineNumberAndPreservesSavedBytes() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        List<String> invalidRecords = List.of(
                keyed("not-a-uuid", LEGACY_CAMERA),
                keyed("1-1-1-1-1", LEGACY_CAMERA),
                "recordId/" + FIRST_RECORD_ID + " " + LEGACY_CAMERA,
                "recordId/" + FIRST_RECORD_ID + "\t" + LEGACY_CAMERA,
                "recordId/" + FIRST_RECORD_ID + "\t" + keyed(SECOND_RECORD_ID, LEGACY_CAMERA),
                "recordId/" + FIRST_RECORD_ID + "\tserviceable/maybe\t" + LEGACY_CAMERA,
                "serviceable/yes\t" + keyed(FIRST_RECORD_ID, LEGACY_CAMERA));
        for (String record : invalidRecords) {
            Files.writeString(file, record);
            byte[] saved = Files.readAllBytes(file);

            IOException exception = assertThrows(IOException.class, () -> EquipmentStorage.load(file));

            assertTrue(exception.getMessage().contains("line 1"));
            assertArrayEquals(saved, Files.readAllBytes(file));
        }
    }

    @Test
    void load_duplicatePermanentIdentityWithDifferentItemIds_rejectsWithoutMigration() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.write(file, List.of(keyed(FIRST_RECORD_ID, LEGACY_CAMERA), keyed(FIRST_RECORD_ID,
                "add i/Lens id/002 type/lens cond/7")));
        byte[] saved = Files.readAllBytes(file);

        IOException exception = assertThrows(IOException.class, () -> EquipmentStorage.load(file));

        assertTrue(exception.getMessage().contains("line 2"));
        assertTrue(exception.getMessage().contains("Duplicate equipment record ID " + FIRST_RECORD_ID));
        assertArrayEquals(saved, Files.readAllBytes(file));
    }

    @Test
    void load_legacyMigrationWriteFailure_keepsCurrentObjectsAndOriginalSavedBytes() throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"),
                "Windows read-only files permit reading but reject replacement during migration.");
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(LEGACY_CAMERA);
        Files.writeString(file, LEGACY_CAMERA, StandardCharsets.UTF_8);
        byte[] saved = Files.readAllBytes(file);
        Files.setAttribute(file, "dos:readonly", true);
        try {
            IOException exception = assertThrows(IOException.class, handler::loadFromStorage);

            assertTrue(exception.getMessage().startsWith("Could not save migrated equipment identities:"));
            assertSame(original, handler.getEquipmentList().getFirst());
            assertArrayEquals(saved, Files.readAllBytes(file));
            try (Stream<Path> files = Files.list(temporaryDirectory)) {
                assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("equipment-")));
            }
        } finally {
            Files.setAttribute(file, "dos:readonly", false);
        }
        handler.loadFromStorage();
        String migratedIdentity = handler.getEquipmentList().getFirst().getRecordId();
        assertEquals(migratedIdentity, EquipmentStorage.load(file).getFirst().getRecordId());
    }

    @Test
    void deleteThenReuseSameItemIdAndName_afterRestart_createsNewPermanentIdentity() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler firstSession = new EquipmentHandler(file);
        String oldIdentity = firstSession.addEquipment(LEGACY_CAMERA).getRecordId();
        EquipmentHandler secondSession = new EquipmentHandler(file);
        secondSession.loadFromStorage();
        secondSession.deleteEquipment("delete item/001");

        Equipment replacement = secondSession.addEquipment(LEGACY_CAMERA);
        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();

        assertNotEquals(oldIdentity, replacement.getRecordId());
        assertEquals(replacement.getRecordId(), restarted.getEquipmentList().getFirst().getRecordId());
        assertEquals("001", replacement.getItemId());
        assertEquals("Camera", replacement.getItemName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"camera", "lens", "battery", "tripod"})
    void returnAssessmentAndNormalUpdate_preservePermanentIdentityInMemoryAndStorage(String type) throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Original id/001 type/" + type + " cond/7");
        String recordId = original.getRecordId();
        Equipment assessed = handler.prepareReturnAssessment(original, false, "NONE", "Repair required");

        assertEquals(recordId, assessed.getRecordId());
        handler.saveReturnAssessment(original, assessed);
        original.applyReturnAssessment(assessed);
        assertSame(original, handler.updateEquipment("update id/001 i/Updated cond/8"));
        Equipment restored = EquipmentStorage.load(file).getFirst();

        assertEquals(recordId, original.getRecordId());
        assertEquals(recordId, restored.getRecordId());
        assertFalse(restored.isServiceable());
        assertEquals("Repair required", restored.getRemarks());
        assertEquals("Updated", restored.getItemName());
        assertEquals(8, restored.getCondition());
    }

    @Test
    void returnAssessment_differentPhysicalItemWithSameVisibleDetails_rejectsBeforeSaveAndCommit() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(LEGACY_CAMERA);
        Equipment replacement = new Camera("Camera", "001", 7);
        Equipment wrongAssessment = handler.prepareReturnAssessment(replacement, false, "SENSOR_DAMAGE", "Repair");
        byte[] saved = Files.readAllBytes(file);

        assertThrows(IllegalArgumentException.class, () -> handler.saveReturnAssessment(original, wrongAssessment));
        assertThrows(IllegalArgumentException.class, () -> original.applyReturnAssessment(wrongAssessment));

        assertTrue(original.isServiceable());
        assertEquals(Camera.CameraDamage.NONE, ((Camera) original).getDamage());
        assertArrayEquals(saved, Files.readAllBytes(file));
    }

    @Test
    void load_missingOrBlankFile_doesNotWriteStorage() throws Exception {
        Path missing = temporaryDirectory.resolve("missing.txt");
        assertTrue(EquipmentStorage.load(missing).isEmpty());
        assertFalse(Files.exists(missing));
        Path blank = temporaryDirectory.resolve("blank.txt");
        Files.writeString(blank, "\n  \n");
        byte[] saved = Files.readAllBytes(blank);
        FileTime previousTime = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(blank, previousTime);

        assertTrue(EquipmentStorage.load(blank).isEmpty());

        assertArrayEquals(saved, Files.readAllBytes(blank));
        assertEquals(previousTime, Files.getLastModifiedTime(blank));
    }

    /** Builds a stored equipment record with permanent identity and explicit serviceability metadata. */
    private static String keyed(String recordId, String command) {
        return "recordId/" + recordId + "\tserviceable/yes\t" + command;
    }
}
