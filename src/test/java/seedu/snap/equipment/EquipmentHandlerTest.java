package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;

/** Tests equipment command parsing, insertion, and deletion. */
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
        Equipment original = handler.addEquipment("add i/Original camera id/001 type/camera cond/1");

        assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(command));

        assertEquals(1, handler.getEquipmentList().size());
        assertSame(original, handler.getEquipmentList().getFirst());
    }

    /** Supplies each invalid input explicitly, including Unicode blanks and the name-length limit. */
    private static String[] invalidAddCommands() {
        return new String[]{
            null, "", "add", "ADD i/Camera id/001 type/camera cond/1",
            "add i/Camera id/000 type/camera cond/1",
            "add i/Camera id/1000 type/camera cond/1",
            "add i/Camera id/001 type/camera cond/-1",
            "add i/Camera id/001 type/camera cond/2147483648",
            "add i/Camera id/001 type/camera cond/1.0",
            "add i/Camera id/001 type/camera cond/01",
            "add i/Camera id/001 type/camera cond/1 extra",
            "add i/Camera id/001 type/ cond/1",
            "add i/" + "A".repeat(101) + " id/001 type/camera cond/1",
            "add id/001 i/Camera type/camera cond/1",
            "add i/\u2003 id/001 type/camera cond/1",
            "add i/Camera id/001 type/\u2003 cond/1"
        };
    }

    @ParameterizedTest(name = "[{index}] preserves name: {0}")
    @MethodSource("validEquipmentNames")
    void addEquipment_nameBoundariesAndUnicode_preservesFields(String name) throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment item = handler.addEquipment("add i/" + name + " id/001 type/camera accessories cond/10");

        assertEquals(name, item.getItemName());
        assertEquals("001", item.getItemId());
        assertEquals("camera accessories", item.getType());
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
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
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

            String id = String.format(java.util.Locale.ROOT, "%03d", i);
            Equipment equipment = handler.addEquipment("add i/Camera id/" + id + " type/camera cond/1");
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
            handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");
            EquipmentAdditionUnsuccessful exception = assertThrows(EquipmentAdditionUnsuccessful.class,
                    () -> handler.addEquipment("add i/Tripod id/002 type/accessories cond/11"));

            assertEquals(2, records.size());
            assertEquals(Level.FINE, records.get(0).getLevel());
            assertEquals("Added equipment with item ID {0}", records.get(0).getMessage());
            assertEquals("001", records.get(0).getParameters()[0]);
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
                "add i/Canon EOS R10 id/001 type/camera cond/1");

        assertNotNull(equipment);
    }

    @Test
    public void parseEquipment_invalidId_throwsException() {
        assertThrows(EquipmentAdditionUnsuccessful.class, () -> EquipmentHandler.parseEquipment(
                "add i/Canon EOS R10 id/000 type/camera cond/1"));
    }

    @Test
    public void parseEquipment_invalidCondition_throwsException() {
        assertThrows(EquipmentAdditionUnsuccessful.class, () -> EquipmentHandler.parseEquipment(
                "add i/Canon EOS R10 id/001 type/camera cond/11"));
    }

    @Test
    public void addEquipment_validEquipment_addsToInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        assertEquals(1, handler.getEquipmentList().size());
        assertSame(equipment, handler.getEquipmentList().getFirst());
    }

    @Test
    public void persistence_addDeleteAndReload_preservesInventoryAcrossHandlers() throws Exception {
        Path file = temporaryDirectory.resolve("data/equipment.txt");
        EquipmentHandler firstHandler = new EquipmentHandler(file);
        firstHandler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        EquipmentHandler secondHandler = new EquipmentHandler(file);
        secondHandler.loadFromStorage();
        assertEquals(1, secondHandler.getEquipmentList().size());
        assertEquals("Canon EOS R10", secondHandler.getEquipmentList().getFirst().getItemName());

        secondHandler.deleteEquipment("delete item/001");
        EquipmentHandler thirdHandler = new EquipmentHandler(file);
        thirdHandler.loadFromStorage();
        assertTrue(thirdHandler.getEquipmentList().isEmpty());
    }

    @Test
    public void persistence_duplicateId_isRejectedWithoutChangingSavedRecords() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        handler.addEquipment("add i/Camera id/001 type/camera cond/1");

        EquipmentAdditionUnsuccessful exception = org.junit.jupiter.api.Assertions.assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> handler.addEquipment("add i/Tripod id/001 type/accessories cond/5"));

        assertEquals("Item ID 001 already exists.", exception.getMessage());
        assertEquals(1, EquipmentStorage.load(file).size());
    }

    @Test
    public void storage_blankLines_areIgnoredDuringLoad() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(file, System.lineSeparator()
                + "  " + System.lineSeparator()
                + "add i/Camera id/001 type/camera cond/1" + System.lineSeparator());

        List<Equipment> loaded = EquipmentStorage.load(file);

        assertEquals(1, loaded.size());
        assertEquals("001", loaded.getFirst().getItemId());
    }

    @Test
    public void addEquipment_multipleItems_increasesInventoryOneAtATime() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment camera = handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");
        assertEquals(1, handler.getEquipmentList().size());

        Equipment tripod = handler.addEquipment("add i/Tripod id/002 type/accessories cond/10");
        assertEquals(2, handler.getEquipmentList().size());
        assertSame(camera, handler.getEquipmentList().get(0));
        assertSame(tripod, handler.getEquipmentList().get(1));
    }

    @Test
    void addEquipment_duplicateIds_preservesAllExistingEquipment() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment first = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        Equipment middle = handler.addEquipment("add i/Tripod id/500 type/accessories cond/5");
        Equipment last = handler.addEquipment("add i/Lens id/999 type/lens cond/10");
        List<Equipment> expected = List.of(first, middle, last);

        for (Equipment existing : expected) {
            String[] duplicateCommands = {
                "add i/" + existing.getItemName() + " id/" + existing.getItemId()
                        + " type/" + existing.getType() + " cond/" + existing.getCondition(),
                "add i/Replacement id/" + existing.getItemId() + " type/audio cond/3"
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
        Equipment first = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        Equipment second = handler.addEquipment("add i/Camera id/002 type/camera cond/1");

        assertEquals(List.of(first, second), handler.getEquipmentList());
    }

    @Test
    void addEquipment_deletedId_acceptsReplacement()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        handler.deleteEquipment("delete item/001");

        Equipment replacement = handler.addEquipment("add i/Tripod id/001 type/accessories cond/5");

        assertEquals(List.of(replacement), handler.getEquipmentList());
        assertEquals("Tripod", replacement.getItemName());
    }

    @Test
    public void addEquipment_invalidInput_keepsInventoryUnchanged() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        assertThrows(EquipmentAdditionUnsuccessful.class,
                () -> handler.addEquipment("add i/Tripod id/002 type/accessories cond/11"));

        assertEquals(1, handler.getEquipmentList().size());
        assertSame(equipment, handler.getEquipmentList().getFirst());
    }

    @Test
    public void deleteEquipment_validCommand_removesMatchingEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        Equipment deleted = handler.deleteEquipment("delete item/001");

        assertEquals("001", deleted.getItemId());
        assertEquals(0, handler.getEquipmentList().size());
    }

    @Test
    public void deleteEquipment_middleItem_preservesOtherEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment first = handler.addEquipment("add i/Camera id/003 type/camera cond/1");
        Equipment target = handler.addEquipment("add i/Tripod id/001 type/accessories cond/5");
        Equipment last = handler.addEquipment("add i/Microphone id/002 type/audio cond/10");

        Equipment deleted = handler.deleteEquipment("delete item/001");

        assertSame(target, deleted);
        assertEquals(List.of(first, last), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_boundaryIds_removesMatchingEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        for (String itemId : List.of("001", "999")) {
            EquipmentHandler handler = new EquipmentHandler();
            Equipment equipment = handler.addEquipment("add i/Camera id/" + itemId + " type/camera cond/1");

            assertSame(equipment, handler.deleteEquipment("delete item/" + itemId));
            assertEquals(List.of(), handler.getEquipmentList());
        }
    }

    @Test
    public void deleteEquipment_extraWhitespace_removesMatchingEquipment()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("add i/Camera id/001 type/camera cond/1");

        assertSame(equipment, handler.deleteEquipment("  delete   item/001  "));
        assertEquals(List.of(), handler.getEquipmentList());
    }

    @Test
    public void getEquipmentForDeletion_validCommand_preservesInventory()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("add i/Camera id/001 type/camera cond/1");

        assertSame(equipment, handler.getEquipmentForDeletion("delete item/001"));
        assertEquals(List.of(equipment), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_emptyInventory_throwsException() {
        EquipmentHandler handler = new EquipmentHandler();

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("delete item/001"));

        assertEquals("Equipment ID 001 does not exist.", exception.getMessage());
        assertEquals(List.of(), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_corruptedDuplicateIds_preservesRecordsAndAllowsOtherDeletion()
            throws ReflectiveOperationException, EquipmentDeletionUnsuccessful {
        Equipment first = new Equipment("Camera", "001", "camera", 1);
        Equipment second = new Equipment("Tripod", "001", "accessories", 5);
        Equipment unrelated = new Equipment("Microphone", "002", "audio", 10);
        EquipmentHandler handler = handlerWithCorruptedInventory(first, second, unrelated);

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("delete item/001"));

        assertEquals("Equipment ID 001 is not unique. No equipment was deleted.", exception.getMessage());
        assertEquals(List.of(first, second, unrelated), handler.getEquipmentList());

        assertSame(unrelated, handler.deleteEquipment("delete item/002"));
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
        handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        Equipment remaining = handler.addEquipment("add i/Tripod id/002 type/accessories cond/5");
        handler.deleteEquipment("delete item/001");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("delete item/001"));

        assertEquals("Equipment ID 001 does not exist.", exception.getMessage());
        assertEquals(List.of(remaining), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_reusedId_removesReplacement()
            throws EquipmentAdditionUnsuccessful, EquipmentDeletionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        handler.deleteEquipment("delete item/001");
        Equipment replacement = handler.addEquipment("add i/Tripod id/001 type/accessories cond/5");

        assertSame(replacement, handler.deleteEquipment("delete item/001"));
        assertEquals(List.of(), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_unknownId_keepsInventoryUnchanged()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("delete item/999"));

        assertEquals("Equipment ID 999 does not exist.", exception.getMessage());
        assertEquals(List.of(equipment), handler.getEquipmentList());
    }

    @Test
    public void deleteEquipment_invalidCommands_throwExpectedErrors()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        String expectedMessage = "Invalid delete syntax. ITEM ID must be from 001 to 999.";
        String[] invalidCommands = {
            "",
            "   ",
            "delete",
            "delete loan/001",
            "delete item/1",
            "delete item/01",
            "delete item/-01",
            "delete item/+01",
            "delete item/000",
            "delete item/1000",
            "delete item/abc",
            "delete item/001 extra",
            "delete item/001 item/002",
            "delete item/ 001",
            "delete item/",
            "deleteitem/001",
            "DELETE item/001"
        };
        for (String command : invalidCommands) {
            EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                    () -> handler.deleteEquipment(command), command);
            assertEquals(expectedMessage, exception.getMessage(), command);
            assertEquals(List.of(equipment), handler.getEquipmentList(), command);
        }
    }

    @Test
    public void deleteEquipment_nullCommand_throwsExpectedError()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment equipment = handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment(null));

        assertEquals("Invalid delete syntax. ITEM ID must be from 001 to 999.", exception.getMessage());
        assertEquals(List.of(equipment), handler.getEquipmentList());
    }

    @Test
    void viewAllEquipment_emptyInventory_printsHeaderOnly() {
        EquipmentHandler handler = new EquipmentHandler();

        handler.viewAllEquipment();

        String expected = String.format(
                "%-4s %-24s | %-6s | %-15s | %-9s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION");
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewAllEquipment_populatedInventory_printsHeaderAndRows()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");
        handler.addEquipment("add i/Manfrotto Tripod id/002 type/accessories cond/8");

        handler.viewAllEquipment();

        String expected = String.format(
                "%-4s %-24s | %-6s | %-15s | %-9s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION");
        expected += String.format(
                "%-4s %-24s | %-6s | %-15s | %-9d%n",
                "1)", "Canon EOS R10", "001", "camera", 1);
        expected += String.format(
                "%-4s %-24s | %-6s | %-15s | %-9d%n",
                "2)", "Manfrotto Tripod", "002", "accessories", 8);

        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }
}
