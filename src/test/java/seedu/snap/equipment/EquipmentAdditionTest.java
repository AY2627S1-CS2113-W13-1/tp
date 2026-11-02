package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Tests equipment addition boundaries, malformed input, and recovery without changing other features. */
class EquipmentAdditionTest {
    private static final String FORMAT_ERROR = "Invalid format. Use: eqm-add name/[ITEM] e/[ITEM ID] "
            + "type/[TYPE] cond/[CONDITION]";

    @Test
    void addEquipment_validBoundaries_preservesFields() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        String[] names = {"A", "A".repeat(100), "Camera \u76f8\u673a", "fluid/head tripod"};
        String[] ids = {"001", "002", "998", "999"};
        for (int i = 0; i < names.length; i++) {
            Equipment item = handler.addEquipment(command(names[i], ids[i], "camera", "10"));
            assertEquals(names[i], item.getItemName());
            assertEquals(ids[i], item.getItemId());
            assertEquals("camera", item.getType());
            assertEquals(10, item.getCondition());
            assertEquals(i + 1, handler.getEquipmentList().size());
        }
    }

    @Test
    void addEquipment_withoutRemarks_usesEmptyDefault() throws EquipmentAdditionUnsuccessful {
        Equipment item = new EquipmentHandler().addEquipment(command("Camera", "001", "camera", "1"));

        assertEquals("", item.getRemarks());
    }

    @Test
    void addEquipment_withRemarks_preservesFreeFormText() throws EquipmentAdditionUnsuccessful {
        String remarks = "Lens has a small scratch; handle with care.";

        Equipment item = new EquipmentHandler().addEquipment(
                command("Lens", "001", "lens", "3", remarks));

        assertEquals(remarks, item.getRemarks());
    }

    @Test
    void parseEquipment_remarksTextInsideName_preservesName()
            throws EquipmentAdditionUnsuccessful {
        Equipment item = EquipmentHandler.parseEquipment(
                command("Camera remarks/adapter", "002", "camera", "1"));

        assertEquals("Camera remarks/adapter", item.getItemName());
        assertEquals("", item.getRemarks());
    }

    @Test
    void addEquipment_invalidRemarks_rejectsWithoutChangingInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String expectedMessage = "Remarks must contain at most 200 characters and no line breaks.";

        assertRejected(handler, command("Tripod", "002", "tripod", "5", "r".repeat(201)),
                expectedMessage);
        assertRejected(handler, command("Tripod", "002", "tripod", "5", "line one\nline two"),
                expectedMessage);
    }

    @Test
    void parseEquipment_prefixTextInsideWords_preservesNamesAndTypes()
            throws EquipmentAdditionUnsuccessful {
        String[] values = {
            "mini/camera", "fluid/head", "prototype/body", "second/hand",
            "mini/camera fluid/head prototype/body second/hand"
        };
        for (String value : values) {
            Equipment item = EquipmentHandler.parseEquipment(command(value, "002", "camera", "5"));
            assertEquals(value, item.getItemName());
            assertEquals("camera", item.getType());
            assertEquals("002", item.getItemId());
            assertEquals(5, item.getCondition());
        }
    }

    @Test
    void addEquipment_missingSeparators_preservesInventory() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String[] commands = {
            "addi/Camera e/002 type/camera cond/1",
            "eqm-add name/Cameraid/002 type/camera cond/1",
            "eqm-add name/Camera e/002type/camera cond/1",
            "eqm-add name/Camera e/002 type/cameracond/1",
            "eqm-add name/Cameraid/002type/cameracond/1"
        };
        for (String input : commands) {
            assertRejected(handler, input, FORMAT_ERROR);
        }
    }

    @Test
    void parseEquipment_textIdPrefixesInName_preservesName() throws EquipmentAdditionUnsuccessful {
        String[] names = {"USB e/card reader", "e/card reader", "USB e/card e/tag reader"};
        for (String name : names) {
            Equipment item = EquipmentHandler.parseEquipment(command(name, "002", "lens", "1"));
            assertEquals(name, item.getItemName());
            assertEquals("002", item.getItemId());
            assertEquals("lens", item.getType());
        }
    }

    @Test
    void addEquipment_reservedIdSyntaxAndMisplacedText_preservesInventory()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        String[] inputs = {
            "eqm-add name/USB e/003 reader e/002 type/lens cond/1",
            "eqm-add name/USB e/+03 reader e/002 type/lens cond/1",
            "eqm-add name/USB e/-03 reader e/002 type/lens cond/1",
            "eqm-add name/USB e/ reader e/002 type/lens cond/1",
            "eqm-add name/USB e/002 e/card type/lens cond/1",
            "eqm-add name/USB e/002 type/lens e/card cond/1"
        };
        for (String input : inputs) {
            assertRejected(handler, input, FORMAT_ERROR);
        }
        assertRejected(handler, command("USB reader", "card", "lens", "1"),
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
        Equipment item = EquipmentHandler.parseEquipment(
                "eqm-add   name/  Film Camera  \te/001  type/ camera  cond/1  ");
        assertEquals("Film Camera", item.getItemName());
        assertEquals("001", item.getItemId());
        assertEquals("camera", item.getType());
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
            null, "", " ", "add", "additions name/Camera e/002 type/camera cond/1",
            "add Camera e/002 type/camera cond/1",
            "add i/Camera id/002 type/camera cond/1",
            "eqm-add name/Camera type/camera cond/1",
            "eqm-add name/Camera e/002 cond/1",
            "eqm-add name/Camera e/002 type/camera",
            "add e/002 name/Camera type/camera cond/1",
            "eqm-add name/Camera type/camera e/002 cond/1",
            "add unexpected name/Camera e/002 type/camera cond/1",
            "eqm-add name/Camera name/Tripod e/002 type/camera cond/1",
            "eqm-add name/Camera e/002 e/003 type/camera cond/1",
            "eqm-add name/Camera e/002 type/camera type/battery cond/1",
            "eqm-add name/Camera e/002 type/camera cond/1 cond/2"
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
        assertRejected(handler, "eqm-add name/Camera e/001", FORMAT_ERROR);
    }

    @Test
    void addEquipment_repeatedRejections_acceptsNextValidItem() throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = seededInventory();
        for (int i = 0; i < 3; i++) {
            assertRejected(handler, command("Replacement", "001", "battery", "5"),
                    "Item ID 001 already exists.");
            assertRejected(handler, command("Tripod", "002", "tripod", "11"),
                    "Condition must be a whole number from 1 to 10.");
        }
        Equipment original = handler.getEquipmentList().getFirst();
        Equipment next = handler.addEquipment(command("Tripod", "002", "tripod", "5"));
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
        return command(name, id, type, condition, "");
    }

    /** Builds an add command with optional remarks. */
    private String command(String name, String id, String type, String condition, String remarks) {
        String remarkField = remarks.isEmpty() ? "" : " remarks/" + remarks;
        return "eqm-add name/" + name + " e/" + id + " type/" + type + " cond/" + condition + remarkField;
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
