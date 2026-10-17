package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;

/** Tests equipment command parsing, insertion, and deletion. */
class EquipmentHandlerTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @BeforeEach
    void redirectOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    public void addEquipment_unicodeWhitespaceFields_throwValidationException() {
        EquipmentHandler handler = new EquipmentHandler();
        String[] invalidCommands = {
            "add i/\u2003 id/001 type/camera cond/1",
            "add i/Camera id/001 type/\u2003 cond/1"
        };
        for (String command : invalidCommands) {
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(command));
            assertEquals(0, handler.getEquipmentList().size());
        }
    }

    @Test
    public void addEquipment_repeatedValidAndInvalidInputs_preservesInventory()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        ArrayList<Equipment> expected = new ArrayList<>();
        Random random = new Random(25);
        String[] invalidCommands = {
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
            "add id/001 i/Camera type/camera cond/1"
        };
        for (int i = 1; i <= 999; i++) {
            String invalidCommand = invalidCommands[random.nextInt(invalidCommands.length)];
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(invalidCommand));
            assertEquals(expected, handler.getEquipmentList());

            String name = i % 2 == 0 ? "A".repeat(100) : "Camera \u76f8\u673a";
            String id = String.format(java.util.Locale.ROOT, "%03d", i);
            int condition = random.nextInt(10) + 1;
            Equipment equipment = handler.addEquipment("add i/" + name + " id/" + id
                    + " type/camera accessories cond/" + condition);
            expected.add(equipment);
            assertEquals(name, equipment.getItemName());
            assertEquals(id, equipment.getItemId());
            assertEquals(condition, equipment.getCondition());
            assertEquals(expected, handler.getEquipmentList());
        }
        assertThrows(UnsupportedOperationException.class, () -> handler.getEquipmentList().clear());
        assertEquals(expected, handler.getEquipmentList());
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
    public void addEquipment_invalidInputsWithAssertionsEnabled_throwValidationException() {
        assertTrue(EquipmentHandler.class.desiredAssertionStatus(), "Run this test with Java assertions enabled");
        EquipmentHandler handler = new EquipmentHandler();
        String[] invalidInputs = {
            null,
            "add",
            "add i/Camera id/000 type/camera cond/1",
            "add i/Camera id/001 type/camera cond/11"
        };

        for (String input : invalidInputs) {
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(input));
            assertEquals(0, handler.getEquipmentList().size());
        }
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
    public void deleteEquipment_unknownId_keepsInventoryUnchanged()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("delete item/999"));

        assertEquals("Equipment ID 999 does not exist.", exception.getMessage());
        assertEquals(1, handler.getEquipmentList().size());
    }

    @Test
    public void deleteEquipment_invalidCommands_printExpectedErrors()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        String expectedMessage = "Invalid delete syntax. ITEM ID must be from 001 to 999.";
        String[] invalidCommands = {
            "delete",
            "delete loan/001",
            "delete item/1",
            "delete item/000",
            "delete item/1000",
            "delete item/abc",
            "delete item/001 extra"
        };
        for (String command : invalidCommands) {
            EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                    () -> handler.deleteEquipment(command), command);
            assertEquals(expectedMessage, exception.getMessage(), command);
        }

        assertEquals(1, handler.getEquipmentList().size());
    }

    @Test
    public void deleteEquipment_nullCommand_printsExpectedError()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Canon EOS R10 id/001 type/camera cond/1");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment(null));

        assertEquals("Invalid delete syntax. ITEM ID must be from 001 to 999.", exception.getMessage());
        assertEquals(1, handler.getEquipmentList().size());
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
