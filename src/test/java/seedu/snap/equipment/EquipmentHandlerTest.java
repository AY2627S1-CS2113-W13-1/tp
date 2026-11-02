package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
// @@author yarnieee
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
// @@author
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
// @@author yarnieee
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;

/** Tests equipment command parsing, insertion, and deletion. */
// @@author
class EquipmentHandlerTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void redirectOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @ParameterizedTest(name = "[{index}] rejects {0}")
    @MethodSource("invalidAddCommands")
    void addEquipment_invalidCommands_preservesInventory(String command) throws EquipmentAdditionUnsuccessful {
        assertTrue(EquipmentHandler.class.desiredAssertionStatus(), "Run this test with Java assertions enabled");
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment original = handler.addEquipment("eqm-add name/Original camera e/E001 type/camera cond/1");
        // @@author

        assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(command));

        assertEquals(1, handler.getEquipmentList().size());
        assertSame(original, handler.getEquipmentList().getFirst());
    }

    /** Supplies each invalid input explicitly, including Unicode blanks and the name-length limit. */
    private static String[] invalidAddCommands() {
        return new String[]{
            // @@author yarnieee
            null, "", "eqm-add", "ADD name/Camera e/E001 type/camera cond/1",
            "eqm-add name/Camera e/E000 type/camera cond/1",
            "eqm-add name/Camera e/1000 type/camera cond/1",
            "eqm-add name/Camera e/E001 type/camera cond/-1",
            "eqm-add name/Camera e/E001 type/camera cond/2147483648",
            "eqm-add name/Camera e/E001 type/camera cond/1.0",
            "eqm-add name/Camera e/E001 type/camera cond/01",
            "eqm-add name/Camera e/E001 type/camera cond/1 extra",
            "eqm-add name/Camera e/E001 type/ cond/1",
            "eqm-add name/" + "A".repeat(101) + " e/E001 type/camera cond/1",
            "add id/E001 i/Camera type/camera cond/1",
            "eqm-add name/\u2003 e/E001 type/camera cond/1",
            "eqm-add name/Camera e/E001 type/\u2003 cond/1"
            // @@author
        };
    }

    @ParameterizedTest(name = "[{index}] preserves name: {0}")
    @MethodSource("validEquipmentNames")
    void addEquipment_nameBoundariesAndUnicode_preservesFields(String name) throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment item = handler.addEquipment("eqm-add name/" + name + " e/E001 type/camera cond/10");
        // @@author

        assertEquals(name, item.getItemName());
        // @@author yarnieee
        assertEquals("E001", item.getItemId());
        // @@author
        assertEquals("camera", item.getType());
        assertEquals(10, item.getCondition());
        assertSame(item, handler.getEquipmentList().getFirst());
    }

    /** Supplies the shortest and longest valid names plus a name containing Unicode text. */
    private static String[] validEquipmentNames() {
        return new String[]{"A", "A".repeat(100), "Camera \u76f8\u673a"};
    }

    @Test
    void getEquipmentList_readOnlyView_rejectsExternalChanges() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment original = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author
        List<Equipment> view = handler.getEquipmentList();

        assertThrows(UnsupportedOperationException.class, () -> view.add(original));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(0));
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertEquals(List.of(original), handler.getEquipmentList());
    }

    @Test
    public void addEquipment_repeatedValidAndInvalidInputs_preservesInventory()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        ArrayList<Equipment> expected = new ArrayList<>();
        String[] invalidCommands = invalidAddCommands();
        for (int i = 1; i <= 20; i++) {
            String invalidCommand = invalidCommands[(i - 1) % invalidCommands.length];
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(invalidCommand));
            assertEquals(expected, handler.getEquipmentList());

            // @@author yarnieee
            String id = "E" + String.format(java.util.Locale.ROOT, "%03d", i);
            Equipment equipment = handler.addEquipment("eqm-add name/Camera e/" + id + " type/camera cond/1");
            // @@author
            expected.add(equipment);
            assertEquals(expected, handler.getEquipmentList());
        }
    }

    @Test
    public void addEquipment_validAndInvalidInputs_logsOutcome() throws EquipmentAdditionUnsuccessful {
        Logger logger = Logger.getLogger(EquipmentHandler.class.getName());
        Level originalLevel = logger.getLevel();
        ArrayList<LogRecord> records = new ArrayList<>();
        Handler logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
                // Records are captured immediately. Nothing needs flushing.
            }

            @Override
            public void close() {
                // This in-memory handler has no resources to close.
            }
        };
        logger.addHandler(logHandler);
        logger.setLevel(Level.FINE);
        try {
            EquipmentHandler handler = new EquipmentHandler();
            // @@author yarnieee
            handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");
            // @@author
            EquipmentAdditionUnsuccessful exception = assertThrows(EquipmentAdditionUnsuccessful.class,
                    // @@author yarnieee
                    () -> handler.addEquipment("eqm-add name/Tripod e/E002 type/tripod cond/11"));
            // @@author

            assertEquals(2, records.size());
            assertEquals(Level.FINE, records.get(0).getLevel());
            assertEquals("Added equipment with item ID {0}", records.get(0).getMessage());
            // @@author yarnieee
            assertEquals("E001", records.get(0).getParameters()[0]);
            // @@author
            assertEquals(Level.FINE, records.get(1).getLevel());
            assertEquals("Equipment addition rejected: {0}", records.get(1).getMessage());
            assertEquals(exception.getMessage(), records.get(1).getParameters()[0]);
            assertEquals(1, handler.getEquipmentList().size());
        } finally {
            logger.removeHandler(logHandler);
            logger.setLevel(originalLevel);
        }
    }

    @Test
    public void parseEquipment_validInput_returnsEquipment() throws EquipmentAdditionUnsuccessful {
        Equipment equipment = EquipmentHandler.parseEquipment(
                // @@author yarnieee
                "eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");
        // @@author

        assertNotNull(equipment);
        // @@author yarnieee
        assertInstanceOf(Camera.class, equipment);
    }

    @Test
    public void parseEquipment_supportedTypes_returnsMatchingSubclasses() throws EquipmentAdditionUnsuccessful {
        assertInstanceOf(Camera.class, EquipmentHandler.parseEquipment(
                "eqm-add name/Camera e/E001 type/camera cond/1"));
        assertInstanceOf(Tripod.class, EquipmentHandler.parseEquipment(
                "eqm-add name/Tripod e/E002 type/tripod cond/2"));
        assertInstanceOf(Lens.class, EquipmentHandler.parseEquipment(
                "eqm-add name/Lens e/E003 type/lens cond/3"));
        assertInstanceOf(Battery.class, EquipmentHandler.parseEquipment(
                "eqm-add name/Battery e/E004 type/battery cond/4"));
    }

    // @@author
    @Test
    void parseEquipment_mixedCaseTypes_normalisesTypeNames() throws EquipmentAdditionUnsuccessful {
        String[] types = {"CaMeRa", "LENS", "Battery", "TRIPOD"};
        String[] expectedTypes = {"camera", "lens", "battery", "tripod"};
        for (int i = 0; i < types.length; i++) {
            Equipment item = EquipmentHandler.parseEquipment(
                    // @@author yarnieee
                    "eqm-add name/Item e/E001 type/" + types[i] + " cond/5");
            // @@author
            assertEquals(expectedTypes[i], item.getType());
        }
    }

    @Test
    void parseEquipment_explicitSubtypeDetails_preservesFields() throws EquipmentAdditionUnsuccessful {
        Camera camera = assertInstanceOf(Camera.class, EquipmentHandler.parseEquipment(
                // @@author yarnieee
                "eqm-add name/Camera e/E001 type/camera cond/7 sensor/Full frame damage/sensor_damage"));
        // @@author
        Lens lens = assertInstanceOf(Lens.class, EquipmentHandler.parseEquipment(
                // @@author yarnieee
                "eqm-add name/Lens e/E002 type/lens cond/6 zoom/24 to 70 mm damage/scratched_glass"));
        // @@author
        Battery battery = assertInstanceOf(Battery.class, EquipmentHandler.parseEquipment(
                // @@author yarnieee
                "eqm-add name/Battery e/E003 type/battery cond/5 capacity/2000 mAh damage/swelling"));
        // @@author
        Tripod tripod = assertInstanceOf(Tripod.class, EquipmentHandler.parseEquipment(
                // @@author yarnieee
                "eqm-add name/Tripod e/E004 type/tripod cond/4 thread/Quarter inch damage/bent_leg"));
        // @@author

        assertEquals("Full frame", camera.getSensorFormat());
        assertEquals("SENSOR_DAMAGE", camera.getDamage().name());
        assertEquals("24 to 70 mm", lens.getZoomRange());
        assertEquals("SCRATCHED_GLASS", lens.getDamage().name());
        assertEquals("2000 mAh", battery.getCapacity());
        assertEquals("SWELLING", battery.getDamage().name());
        assertEquals("Quarter inch", tripod.getThread());
        assertEquals("BENT_LEG", tripod.getDamage().name());
    }

    @Test
    void parseEquipment_optionalDetails_usesDefaultsForOmittedFields() throws EquipmentAdditionUnsuccessful {
        String[] types = {"camera", "lens", "battery", "tripod"};
        String[] markers = {"sensor", "zoom", "capacity", "thread"};
        for (int i = 0; i < types.length; i++) {
            // @@author yarnieee
            String command = "eqm-add name/Item e/E001 type/" + types[i] + " cond/5";
            // @@author
            assertEquals(markers[i] + "/unspecified damage/NONE",
                    EquipmentHandler.parseEquipment(command).getDetails());
            assertEquals(markers[i] + "/Custom value damage/NONE",
                    EquipmentHandler.parseEquipment(command + " " + markers[i] + "/Custom value").getDetails());
            assertEquals(markers[i] + "/unspecified damage/NONE",
                    EquipmentHandler.parseEquipment(command + " damage/none").getDetails());
        }
    }

    @Test
    void parseEquipment_attributeBoundaries_preservesValues() throws EquipmentAdditionUnsuccessful {
        String[] values = {"A", "A".repeat(100), "Full frame \u76f8\u673a", "mini/sensor"};
        for (String value : values) {
            Camera camera = assertInstanceOf(Camera.class, EquipmentHandler.parseEquipment(
                    // @@author yarnieee
                    "eqm-add name/Camera e/E001 type/camera cond/5 sensor/" + value));
            // @@author
            assertEquals(value, camera.getSensorFormat());
        }
    }

    @Test
    void addEquipment_invalidSubtypeFields_preservesInventoryAndSavedRecords() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        // @@author yarnieee
        Equipment original = handler.addEquipment("eqm-add name/Original camera e/E001 type/camera cond/1");
        // @@author
        String saved = Files.readString(file);
        String[] invalidTails = {
            "zoom/24 mm", "capacity/2000 mAh", "unknown/value",
            "sensor/", "sensor/   ", "damage/", "damage/   ",
            "sensor/Full frame sensor/Crop", "damage/NONE damage/SHUTTER_FAULT",
            "damage/NONE sensor/Full frame", "sensor/Full frame zoom/24 mm",
            "sensor/Full frame damage/SWELLING", "sensor/" + "A".repeat(101),
            // @@author yarnieee
            "sensor/Full frame e/E003", "sensor/Full frame cond/5"
            // @@author
        };

        for (String tail : invalidTails) {
            // @@author yarnieee
            String command = "eqm-add name/Camera e/E002 type/camera cond/5 " + tail;
            // @@author
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(command), command);
            assertEquals(List.of(original), handler.getEquipmentList(), command);
            assertEquals(saved, Files.readString(file), command);
        }

        // @@author yarnieee
        handler.addEquipment("eqm-add name/Camera e/E002 type/camera cond/5 sensor/Full frame damage/NONE");
        // @@author
        assertEquals(2, handler.getEquipmentList().size());
    }

    @Test
    void addEquipment_damageForAnotherSubtype_rejectsWithoutAddingItems() {
        EquipmentHandler handler = new EquipmentHandler();
        String[] commands = {
            // @@author yarnieee
            "eqm-add name/Camera e/E001 type/camera cond/5 damage/SCRATCHED_GLASS",
            "eqm-add name/Lens e/E002 type/lens cond/5 damage/SENSOR_DAMAGE",
            "eqm-add name/Battery e/E003 type/battery cond/5 damage/BENT_LEG",
            "eqm-add name/Tripod e/E004 type/tripod cond/5 damage/CONTACT_DAMAGE"
            // @@author
        };
        for (String command : commands) {
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(command), command);
            assertTrue(handler.getEquipmentList().isEmpty(), command);
        }
    }

    // @@author yarnieee
    @Test
    public void parseEquipment_unsupportedType_throwsException() {
        assertThrows(EquipmentAdditionUnsuccessful.class, () -> EquipmentHandler.parseEquipment(
                "eqm-add name/Flash e/E001 type/flash cond/1"));
        // @@author
    }

    @Test
    public void parseEquipment_invalidId_throwsException() {
        assertThrows(EquipmentAdditionUnsuccessful.class, () -> EquipmentHandler.parseEquipment(
                // @@author yarnieee
                "eqm-add name/Canon EOS R10 e/E000 type/camera cond/1"));
        // @@author
    }

    @Test
    public void parseEquipment_invalidCondition_throwsException() {
        assertThrows(EquipmentAdditionUnsuccessful.class, () -> EquipmentHandler.parseEquipment(
                // @@author yarnieee
                "eqm-add name/Canon EOS R10 e/E001 type/camera cond/11"));
        // @@author
    }

    @Test
    public void addEquipment_validEquipment_addsToInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment equipment = handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");
        // @@author

        assertEquals(1, handler.getEquipmentList().size());
        assertSame(equipment, handler.getEquipmentList().getFirst());
    }

    @Test
    public void persistence_addDeleteAndReload_preservesInventoryAcrossHandlers() throws Exception {
        Path file = temporaryDirectory.resolve("data/equipment.txt");
        EquipmentHandler firstHandler = new EquipmentHandler(file);
        // @@author yarnieee
        firstHandler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");
        // @@author

        EquipmentHandler secondHandler = new EquipmentHandler(file);
        secondHandler.loadFromStorage();
        assertEquals(1, secondHandler.getEquipmentList().size());
        assertEquals("Canon EOS R10", secondHandler.getEquipmentList().getFirst().getItemName());
        assertEquals("", secondHandler.getEquipmentList().getFirst().getRemarks());

        // @@author yarnieee
        secondHandler.deleteEquipment("eqm-delete e/E001");
        // @@author
        EquipmentHandler thirdHandler = new EquipmentHandler(file);
        thirdHandler.loadFromStorage();
        assertTrue(thirdHandler.getEquipmentList().isEmpty());
    }

    @Test
    public void persistence_remarksSurviveReload() throws Exception {
        Path file = temporaryDirectory.resolve("data/equipment.txt");
        String remarks = "Lens has a small scratch; handle with care.";
        EquipmentHandler firstHandler = new EquipmentHandler(file);
        // @@author yarnieee
        firstHandler.addEquipment("eqm-add name/Lens e/E001 type/lens cond/3 remarks/" + remarks);
        // @@author

        EquipmentHandler secondHandler = new EquipmentHandler(file);
        secondHandler.loadFromStorage();

        assertEquals(remarks, secondHandler.getEquipmentList().getFirst().getRemarks());
        assertTrue(Files.readString(file).contains("remarks/" + remarks));
    }

    @Test
    public void persistence_maxLengthRemarksSurviveReload() throws Exception {
        Path file = temporaryDirectory.resolve("data/equipment.txt");
        String remarks = "r".repeat(Equipment.MAX_REMARKS_LENGTH);
        EquipmentHandler firstHandler = new EquipmentHandler(file);

        // @@author yarnieee
        firstHandler.addEquipment("eqm-add name/Lens e/E001 type/lens cond/3 remarks/" + remarks);
        // @@author

        EquipmentHandler secondHandler = new EquipmentHandler(file);
        secondHandler.loadFromStorage();

        String reloadedRemarks = secondHandler.getEquipmentList().getFirst().getRemarks();
        assertEquals(Equipment.MAX_REMARKS_LENGTH, reloadedRemarks.length());
        assertEquals(remarks, reloadedRemarks);
        assertTrue(Files.readString(file).contains("remarks/" + remarks));
    }

    @Test
    public void persistence_duplicateId_isRejectedWithoutChangingSavedRecords() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        // @@author yarnieee
        handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author

        EquipmentAdditionUnsuccessful exception = org.junit.jupiter.api.Assertions.assertThrows(
                EquipmentAdditionUnsuccessful.class,
                // @@author yarnieee
                () -> handler.addEquipment("eqm-add name/Tripod e/E001 type/tripod cond/5"));

        assertEquals("Item ID E001 already exists.", exception.getMessage());
        // @@author
        assertEquals(1, EquipmentStorage.load(file).size());
    }

    @Test
    public void storage_blankLines_areIgnoredDuringLoad() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(file, System.lineSeparator()
                + "  " + System.lineSeparator()
                // @@author yarnieee
                + "add i/Camera id/E001 type/camera cond/1" + System.lineSeparator());
        // @@author

        List<Equipment> loaded = EquipmentStorage.load(file);

        assertEquals(1, loaded.size());
        // @@author yarnieee
        assertEquals("E001", loaded.getFirst().getItemId());
        // @@author
        assertEquals("", loaded.getFirst().getRemarks());
    }

    @Test
    public void addEquipment_multipleItems_increasesInventoryOneAtATime() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment camera = handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");
        // @@author
        assertEquals(1, handler.getEquipmentList().size());

        // @@author yarnieee
        Equipment tripod = handler.addEquipment("eqm-add name/Tripod e/E002 type/tripod cond/10");
        // @@author
        assertEquals(2, handler.getEquipmentList().size());
        assertSame(camera, handler.getEquipmentList().get(0));
        assertSame(tripod, handler.getEquipmentList().get(1));
    }

    @Test
    void addEquipment_duplicateIds_preservesAllExistingEquipment() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment first = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        Equipment middle = handler.addEquipment("eqm-add name/Tripod e/E500 type/tripod cond/5");
        Equipment last = handler.addEquipment("eqm-add name/Lens e/E999 type/lens cond/10");
        // @@author
        List<Equipment> expected = List.of(first, middle, last);

        for (Equipment existing : expected) {
            String[] duplicateCommands = {
                // @@author yarnieee
                "eqm-add name/" + existing.getItemName() + " e/" + existing.getItemId()
                        // @@author
                        + " type/" + existing.getType() + " cond/" + existing.getCondition(),
                // @@author yarnieee
                "eqm-add name/Replacement e/" + existing.getItemId() + " type/battery cond/3"
                // @@author
            };
            for (String command : duplicateCommands) {
                EquipmentAdditionUnsuccessful exception = assertThrows(EquipmentAdditionUnsuccessful.class,
                        () -> handler.addEquipment(command));
                assertEquals("Item ID " + existing.getItemId() + " already exists.", exception.getMessage());
                assertEquals(expected, handler.getEquipmentList());
            }
        }
    }

    @Test
    void addEquipment_sameNameWithDifferentIds_acceptsBoth() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment first = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        Equipment second = handler.addEquipment("eqm-add name/Camera e/E002 type/camera cond/1");
        // @@author

        assertEquals(List.of(first, second), handler.getEquipmentList());
    }

    @Test
    void addEquipment_deletedId_acceptsReplacement()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        handler.deleteEquipment("eqm-delete e/E001");

        Equipment replacement = handler.addEquipment("eqm-add name/Tripod e/E001 type/tripod cond/5");
        // @@author

        assertEquals(List.of(replacement), handler.getEquipmentList());
        assertEquals("Tripod", replacement.getItemName());
    }

    @Test
    public void addEquipment_invalidInput_keepsInventoryUnchanged() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment equipment = handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");
        // @@author

        assertThrows(EquipmentAdditionUnsuccessful.class,
                // @@author yarnieee
                () -> handler.addEquipment("eqm-add name/Tripod e/E002 type/tripod cond/11"));
        // @@author

        assertEquals(1, handler.getEquipmentList().size());
        assertSame(equipment, handler.getEquipmentList().getFirst());
    }
    // @@author yarnieee

    @Test
    public void deleteEquipment_validCommand_removesMatchingEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");

        Equipment deleted = handler.deleteEquipment("eqm-delete e/E001");

        assertEquals("E001", deleted.getItemId());
        assertEquals(0, handler.getEquipmentList().size());
    }

    // @@author
    @Test
    public void deleteEquipment_middleItem_preservesOtherEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment first = handler.addEquipment("eqm-add name/Camera e/E003 type/camera cond/1");
        Equipment target = handler.addEquipment("eqm-add name/Tripod e/E001 type/tripod cond/5");
        Equipment last = handler.addEquipment("eqm-add name/Microphone e/E002 type/battery cond/10");

        Equipment deleted = handler.deleteEquipment("eqm-delete e/E001");
        // @@author

        assertSame(target, deleted);
        assertEquals(List.of(first, last), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_boundaryIds_removesMatchingEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        for (String itemId : List.of("E001", "E999")) {
            EquipmentHandler handler = new EquipmentHandler();
            // @@author yarnieee
            Equipment equipment = handler.addEquipment("eqm-add name/Camera e/" + itemId + " type/camera cond/1");

            assertSame(equipment, handler.deleteEquipment("eqm-delete e/" + itemId));
            // @@author
            assertEquals(List.of(), handler.getEquipmentList());
        }
    }

    @Test
    public void deleteEquipment_extraWhitespace_removesMatchingEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment equipment = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");

        assertSame(equipment, handler.deleteEquipment("  eqm-delete e/E001  "));
        // @@author
        assertEquals(List.of(), handler.getEquipmentList());
    }

    @Test
    public void getEquipmentForDeletion_validCommand_preservesInventory()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment equipment = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");

        assertSame(equipment, handler.getEquipmentForDeletion("eqm-delete e/E001"));
        // @@author
        assertEquals(List.of(equipment), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_emptyInventory_throwsException() {
        EquipmentHandler handler = new EquipmentHandler();

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("eqm-delete e/E001"));

        assertEquals("Equipment ID E001 does not exist.", exception.getMessage());
        assertEquals(List.of(), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_corruptedDuplicateIds_preservesRecordsAndAllowsOtherDeletion()
            throws ReflectiveOperationException, EquipmentDeletionUnsuccessful {
        // @@author yarnieee
        Equipment first = new Camera("Camera", "E001", 1);
        Equipment second = new Tripod("Tripod", "E001", 5);
        Equipment unrelated = new Battery("Battery", "E002", 10);
        // @@author
        EquipmentHandler handler = handlerWithCorruptedInventory(first, second, unrelated);

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("eqm-delete e/E001"));

        assertEquals("Equipment ID E001 is not unique. No equipment was deleted.", exception.getMessage());
        assertEquals(List.of(first, second, unrelated), handler.getEquipmentList());

        // @@author yarnieee
        assertSame(unrelated, handler.deleteEquipment("eqm-delete e/E002"));
        // @@author
        assertEquals(List.of(first, second), handler.getEquipmentList());
    }

    /**
     * Creates invalid stored data without relying on add commands accepting duplicate IDs.
     * Reflection stays in this test fixture so production code does not expose mutable inventory.
     */
    @SuppressWarnings("unchecked")
    private EquipmentHandler handlerWithCorruptedInventory(Equipment... equipment)
            throws ReflectiveOperationException {
        EquipmentHandler handler = new EquipmentHandler();
        Field inventoryField = EquipmentHandler.class.getDeclaredField("inventory");
        inventoryField.setAccessible(true);
        List<Equipment> inventory = (List<Equipment>) inventoryField.get(handler);
        inventory.addAll(List.of(equipment));
        return handler;
    }

    @Test
    public void deleteEquipment_repeatedDeletion_preservesRemainingEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        Equipment remaining = handler.addEquipment("eqm-add name/Tripod e/E002 type/tripod cond/5");
        handler.deleteEquipment("eqm-delete e/E001");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("eqm-delete e/E001"));

        assertEquals("Equipment ID E001 does not exist.", exception.getMessage());
        assertEquals(List.of(remaining), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_reusedId_removesReplacement()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        handler.deleteEquipment("eqm-delete e/E001");
        Equipment replacement = handler.addEquipment("eqm-add name/Tripod e/E001 type/tripod cond/5");

        assertSame(replacement, handler.deleteEquipment("eqm-delete e/E001"));
        // @@author
        assertEquals(List.of(), handler.getEquipmentList());
    }

    // @@author yarnieee
    @Test
    public void deleteEquipment_unknownId_keepsInventoryUnchanged()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("eqm-delete e/E999"));

        assertEquals("Equipment ID E999 does not exist.", exception.getMessage());
        // @@author
        assertEquals(List.of(equipment), handler.getEquipmentList());
        // @@author yarnieee
    }

    @Test
    void deleteEquipment_lowercaseId_deletesCanonicalItem() throws Exception {
        // @@author
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment equipment = handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");

        assertSame(equipment, handler.deleteEquipment("eqm-delete e/e001"));
        assertTrue(handler.getEquipmentList().isEmpty());
    }

    @Test
    // @@author
    public void deleteEquipment_invalidCommands_throwExpectedErrors()
            // @@author yarnieee
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");

        String expectedMessage = "Invalid delete syntax. ITEM ID must be from E001 to E999.";
        String[] invalidCommands = {
            // @@author
            "",
            "   ",
            // @@author yarnieee
            "delete",
            "loan-delete l/L001",
            "eqm-delete e/1",
            "eqm-delete e/01",
            "eqm-delete e/001",
            "eqm-delete e/-01",
            "eqm-delete e/+01",
            "eqm-delete e/E000",
            "eqm-delete e/1000",
            "eqm-delete e/abc",
            "eqm-delete e/E001 extra",
            "eqm-delete e/E001 item/002",
            "eqm-delete e/ 001",
            "eqm-delete e/",
            // @@author
            "deleteitem/001",
            "DELETE item/001"
            // @@author yarnieee
        };
        for (String command : invalidCommands) {
            EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                    () -> handler.deleteEquipment(command), command);
            assertEquals(expectedMessage, exception.getMessage(), command);
            // @@author
            assertEquals(List.of(equipment), handler.getEquipmentList(), command);
            // @@author yarnieee
        }
    }

    @Test
    // @@author
    public void deleteEquipment_nullCommand_throwsExpectedError()
            // @@author yarnieee
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment(null));

        assertEquals("Invalid delete syntax. ITEM ID must be from E001 to E999.", exception.getMessage());
        // @@author
        assertEquals(List.of(equipment), handler.getEquipmentList());
        // @@author yarnieee
    }
    // @@author

    @Test
    void viewAllEquipment_emptyInventory_printsCountAndValidExample() {
        EquipmentHandler handler = new EquipmentHandler();

        handler.viewAllEquipment();

        String expected = String.join(System.lineSeparator(), "Equipment", "No equipment in the inventory.",
                // @@author yarnieee
                "0 equipment items.", "Add an item: eqm-add name/Camera e/E001 type/camera cond/1", "");
        // @@author
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewAllEquipment_populatedInventory_printsSummaryAndCount() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        handler.addEquipment("eqm-add name/Canon EOS R10 e/E001 type/camera cond/1");
        handler.addEquipment("eqm-add name/Manfrotto Tripod e/E002 type/tripod cond/8 remarks/Needs cleaning");
        // @@author

        handler.viewAllEquipment();

        String actual = output.toString(StandardCharsets.UTF_8);
        // @@author yarnieee
        assertTrue(actual.contains("| E001 | Canon EOS R10"));
        assertTrue(actual.contains("| E002 | Manfrotto Tripod"));
        // @@author
        assertEquals(2, actual.lines().filter(line -> line.contains("Available")).count());
        assertTrue(actual.contains("2 equipment items."));
        assertTrue(actual.lines().allMatch(line -> line.length() <= 100));
        assertTrue(!actual.contains("Needs cleaning"));
    }
}
