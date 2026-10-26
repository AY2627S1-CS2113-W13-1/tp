package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests Tripod compatibility parsing, display and persistence. */
class TripodCompatibilityIntegrationTest {
    private static final String FULL_TRIPOD_COMMAND =
            "add i/Studio Tripod id/301 type/tripod cond/8 "
                    + "thread/1/4\"-20 damage/NONE remarks/Stable";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void parseEquipment_allTripodFields_preservesValues()
            throws EquipmentAdditionUnsuccessful {
        Tripod tripod = assertInstanceOf(
                Tripod.class,
                EquipmentHandler.parseEquipment(
                        FULL_TRIPOD_COMMAND));

        assertFullCompatibility(tripod);
        assertEquals("Stable", tripod.getRemarks());
    }

    @Test
    void parseEquipment_omittedFields_usesDefaults()
            throws EquipmentAdditionUnsuccessful {
        Tripod tripod = assertInstanceOf(
                Tripod.class,
                EquipmentHandler.parseEquipment(
                        "add i/Tripod id/302 "
                                + "type/tripod cond/7"));

        assertEquals(
                CompatibilityText.UNSPECIFIED,
                tripod.getThread());
        assertEquals(
                Tripod.TripodDamage.NONE,
                tripod.getDamage());
    }

    @Test
    void parseEquipment_invalidTripodFields_throwException() {
        String repeatedThread =
                "add i/Tripod id/303 type/tripod cond/5 "
                        + "thread/1/4\"-20 thread/3/8\"-16";
        String mountField =
                "add i/Tripod id/304 type/tripod cond/5 "
                        + "mount/Canon RF";
        String invalidDamage =
                "add i/Tripod id/305 type/tripod cond/5 "
                        + "damage/CRACKED";

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(
                        repeatedThread));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(
                        mountField));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(
                        invalidDamage));
    }

    @Test
    void persistence_fullTripodDetails_surviveReload()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler originalHandler =
                new EquipmentHandler(file);
        originalHandler.addEquipment(FULL_TRIPOD_COMMAND);

        String savedRecord = Files.readString(file);
        assertTrue(savedRecord.contains("thread/1/4\"-20"));
        assertTrue(savedRecord.contains("damage/NONE"));

        EquipmentHandler reloadedHandler =
                new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();

        Tripod reloadedTripod = assertInstanceOf(
                Tripod.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertFullCompatibility(reloadedTripod);
        assertEquals("Stable", reloadedTripod.getRemarks());
    }

    @Test
    void persistence_legacyTripodRecord_loadsSuccessfully()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(
                file,
                "add i/Legacy Tripod id/301 type/tripod cond/5 "
                        + "thread/3/8\"-16 damage/NONE"
                        + System.lineSeparator());

        EquipmentHandler handler = new EquipmentHandler(file);
        handler.loadFromStorage();

        Tripod tripod = assertInstanceOf(
                Tripod.class,
                handler.getEquipmentList().getFirst());

        assertEquals("3/8\"-16", tripod.getThread());
        assertEquals(
                Tripod.TripodDamage.NONE,
                tripod.getDamage());
    }

    @Test
    void updateEquipment_tripodDetails_preservesValuesAndStorage()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        handler.addEquipment(FULL_TRIPOD_COMMAND);

        Tripod updatedTripod = assertInstanceOf(
                Tripod.class,
                handler.updateEquipment("update id/301 cond/9"));

        assertEquals(9, updatedTripod.getCondition());
        assertFullCompatibility(updatedTripod);

        EquipmentHandler reloadedHandler =
                new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();

        Tripod reloadedTripod = assertInstanceOf(
                Tripod.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertEquals(9, reloadedTripod.getCondition());
        assertFullCompatibility(reloadedTripod);
    }

    @Test
    void viewAllEquipment_tripodDetails_displaysValues()
            throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment(FULL_TRIPOD_COMMAND);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(
                    new PrintStream(
                            output,
                            true,
                            StandardCharsets.UTF_8));
            handler.viewAllEquipment();
        } finally {
            System.setOut(originalOutput);
        }

        String displayed =
                output.toString(StandardCharsets.UTF_8);
        assertTrue(displayed.contains("thread/1/4\"-20"));
        assertTrue(displayed.contains("damage/NONE"));
    }

    /** Checks every explicit compatibility field on a Tripod. */
    private void assertFullCompatibility(Tripod tripod) {
        assertEquals("1/4\"-20", tripod.getThread());
        assertEquals(
                Tripod.TripodDamage.NONE,
                tripod.getDamage());
    }
}

// @@author
