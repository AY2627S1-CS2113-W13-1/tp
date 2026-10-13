package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

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
