package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Tests equipment addition boundaries, malformed input, and recovery without changing other features. */
class EquipmentAdditionTest {
    private static final String FORMAT_ERROR = "Invalid format. Use: add i/[ITEM] id/[ITEM ID] "
            + "type/[TYPE] cond/[CONDITION]";

    @Test
    void addEquipment_validBoundaries_preservesFields() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        String[] names = {"A", "A".repeat(100), "Camera \u76f8\u673a", "fluid/head tripod"};
        String[] ids = {"001", "002", "998", "999"};
        for (int i = 0; i < names.length; i++) {
            Equipment item = handler.addEquipment(command(names[i], ids[i], "camera accessories", "10"));
            assertEquals(names[i], item.getItemName());
            assertEquals(ids[i], item.getItemId());
            assertEquals("camera accessories", item.getType());
            assertEquals(10, item.getCondition());
            assertEquals(i + 1, handler.getEquipmentList().size());
        }
    }

    @Test
    void parseEquipment_prefixTextInsideWords_preservesNamesAndTypes()
            throws EquipmentAdditionUnsuccessful {
        String[] values = {
            "mini/camera", "fluid/head", "prototype/body", "second/hand",
            "mini/camera fluid/head prototype/body second/hand"
        };
        for (String value : values) {
            Equipment item = EquipmentHandler.parseEquipment(command(value, "002", value, "5"));
            assertEquals(value, item.getItemName());
            assertEquals(value, item.getType());
            assertEquals("002", item.getItemId());
            assertEquals(5, item.getCondition());
        }
    }

    @Test
    void addEquipment_missingSeparators_preservesInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String[] commands = {
            "addi/Camera id/002 type/camera cond/1",
            "add i/Cameraid/002 type/camera cond/1",
            "add i/Camera id/002type/camera cond/1",
            "add i/Camera id/002 type/cameracond/1",
            "add i/Cameraid/002type/cameracond/1"
        };
        for (String input : commands) {
            assertRejected(handler, input, FORMAT_ERROR);
        }
    }

    @Test
    void parseEquipment_textIdPrefixesInName_preservesName() throws EquipmentAdditionUnsuccessful {
        String[] names = {"USB id/card reader", "id/card reader", "USB id/card id/tag reader"};
        for (String name : names) {
            Equipment item = EquipmentHandler.parseEquipment(command(name, "002", "accessory", "1"));
            assertEquals(name, item.getItemName());
            assertEquals("002", item.getItemId());
            assertEquals("accessory", item.getType());
        }
    }

    @Test
    void addEquipment_reservedIdSyntaxAndMisplacedText_preservesInventory()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String[] inputs = {
            "add i/USB id/003 reader id/002 type/accessory cond/1",
            "add i/USB id/+03 reader id/002 type/accessory cond/1",
            "add i/USB id/-03 reader id/002 type/accessory cond/1",
            "add i/USB id/ reader id/002 type/accessory cond/1",
            "add i/USB id/002 id/card type/accessory cond/1",
            "add i/USB id/002 type/accessory id/card cond/1"
        };
        for (String input : inputs) {
            assertRejected(handler, input, FORMAT_ERROR);
        }
        assertRejected(handler, command("USB reader", "card", "accessory", "1"),
                "Item ID must be from 001 to 999.");
    }

    @Test
    void parseEquipment_allValidConditions_acceptsEach() throws EquipmentAdditionUnsuccessful {
        for (int condition = 1; condition <= 10; condition++) {
            Equipment item = EquipmentHandler.parseEquipment(command("Camera", "001", "camera", "" + condition));
            assertEquals(condition, item.getCondition());
        }
    }

    @Test
    void parseEquipment_fieldWhitespace_trimsValues() throws EquipmentAdditionUnsuccessful {
        Equipment item = EquipmentHandler.parseEquipment("add   i/  Film Camera  \tid/001  type/ camera kit  cond/1  ");
        assertEquals("Film Camera", item.getItemName());
        assertEquals("001", item.getItemId());
        assertEquals("camera kit", item.getType());
        assertEquals(1, item.getCondition());
    }

    @Test
    void addEquipment_invalidNamesAndTypes_preservesInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        for (String name : new String[]{"", "   ", "\t", "\u2003", "A".repeat(101)}) {
            assertRejected(handler, command(name, "002", "camera", "1"),
                    "Item name must contain 1 to 100 characters.");
        }
        for (String type : new String[]{"", "   ", "\t", "\u2003"}) {
            assertRejected(handler, command("Camera", "002", type, "1"), "Type cannot be empty.");
        }
    }

    @Test
    void addEquipment_invalidIds_preservesInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String[] ids = {"", "000", "0", "01", "0001", "1000", "-01", "+01", "1.0", "abc", "0 02", "\u0660\u0660\u0662"};
        for (String id : ids) {
            assertRejected(handler, command("Camera", id, "camera", "1"), "Item ID must be from 001 to 999.");
        }
    }

    @Test
    void addEquipment_invalidConditions_preservesInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String[] conditions = {"", "0", "11", "-1", "+1", "01", "1.0", "1e0", "2147483648", "one", "1 extra"};
        for (String condition : conditions) {
            assertRejected(handler, command("Camera", "002", "camera", condition),
                    "Condition must be a whole number from 1 to 10.");
        }
    }

    @Test
    void addEquipment_malformedCommands_preservesInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String[] commands = {
            null, "", " ", "add", "additions i/Camera id/002 type/camera cond/1",
            "add Camera id/002 type/camera cond/1",
            "add i/Camera type/camera cond/1",
            "add i/Camera id/002 cond/1",
            "add i/Camera id/002 type/camera",
            "add id/002 i/Camera type/camera cond/1",
            "add i/Camera type/camera id/002 cond/1",
            "add unexpected i/Camera id/002 type/camera cond/1",
            "add i/Camera i/Tripod id/002 type/camera cond/1",
            "add i/Camera id/002 id/003 type/camera cond/1",
            "add i/Camera id/002 type/camera type/audio cond/1",
            "add i/Camera id/002 type/camera cond/1 cond/2"
        };
        for (String input : commands) {
            List<Equipment> before = List.copyOf(handler.getEquipmentList());
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(input), input);
            assertEquals(before, handler.getEquipmentList());
        }
    }

    @Test
    void addEquipment_multipleErrors_validatesFieldsBeforeCheckingDuplicates()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        assertRejected(handler, command("Camera", "001", "camera", "11"),
                "Condition must be a whole number from 1 to 10.");
        assertRejected(handler, "add i/Camera id/001", FORMAT_ERROR);
    }

    @Test
    void addEquipment_repeatedRejections_acceptsNextValidItem() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        for (int i = 0; i < 3; i++) {
            assertRejected(handler, command("Replacement", "001", "audio", "5"),
                    "Item ID 001 already exists.");
            assertRejected(handler, command("Tripod", "002", "accessories", "11"),
                    "Condition must be a whole number from 1 to 10.");
        }
        Equipment original = handler.getEquipmentList().getFirst();
        Equipment next = handler.addEquipment(command("Tripod", "002", "accessories", "5"));
        assertEquals(List.of(original, next), handler.getEquipmentList());
    }

    /** Gives rejection tests an existing record to protect. */
    private EquipmentHandler seededInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment(command("Original camera", "001", "camera", "1"));
        return handler;
    }

    /** Builds the documented command format while keeping boundary values readable in tests. */
    private String command(String name, String id, String type, String condition) {
        return "add i/" + name + " id/" + id + " type/" + type + " cond/" + condition;
    }

    /** Checks the error reason and protects the inventory's contents and order on every rejection. */
    private void assertRejected(EquipmentHandler handler, String input, String expectedMessage) {
        List<Equipment> before = List.copyOf(handler.getEquipmentList());
        EquipmentAdditionUnsuccessful exception = assertThrows(EquipmentAdditionUnsuccessful.class,
                () -> handler.addEquipment(input), input);
        assertEquals(expectedMessage, exception.getMessage(), input);
        assertEquals(before, handler.getEquipmentList(), input);
    }
}
