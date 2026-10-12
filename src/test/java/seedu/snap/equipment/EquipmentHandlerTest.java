package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;

/** Tests equipment command parsing, insertion, and deletion. */
class EquipmentHandlerTest {
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
                // Records are captured immediately; nothing needs flushing.
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
}
