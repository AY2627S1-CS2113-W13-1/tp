package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Checks that ordinary and nonbreaking whitespace follow the same equipment field rules. */
class EquipmentWhitespaceTest {
    private static final String[] SPACES = {"\u00a0", "\u2007", "\u202f", "\u2003", "\t", " "};

    @Test
    void addEquipment_whitespaceOnlyNamesAndTypes_preservesInventory() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        for (String space : SPACES) {
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    () -> handler.addEquipment("add i/" + space + " id/002 type/camera cond/1"));
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    () -> handler.addEquipment("add i/Tripod id/002 type/" + space + " cond/1"));
            assertEquals(List.of(original), handler.getEquipmentList());
        }
        handler.addEquipment("add i/Tripod id/002 type/tripod cond/1");
        assertEquals(2, handler.getEquipmentList().size());
    }

    @Test
    void addEquipment_unicodeFieldEdges_trimsEdgesAndPreservesInternalSpaces() throws Exception {
        for (String space : SPACES) {
            EquipmentHandler handler = new EquipmentHandler();
            Equipment item = handler.addEquipment("add" + space + "i/" + space + "Film" + space + "Camera"
                    + space + "id/001" + space + "type/" + space + "camera" + space + "cond/1" + space);
            assertEquals("Film" + space + "Camera", item.getItemName());
            assertEquals("camera", item.getType());
            assertEquals(1, item.getCondition());
        }
    }

    @Test
    void addEquipment_nonbreakingWhitespaceAfterRepeatedId_rejectsMalformedCommand() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        for (String space : SPACES) {
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    () -> handler.addEquipment("add i/Camera id/" + space + "999 id/001 type/camera cond/1"));
            assertEquals(List.of(), handler.getEquipmentList());
        }
        assertEquals("USB id/card reader",
                handler.addEquipment("add i/USB id/card reader id/001 type/tripod cond/1").getItemName());
    }

    @Test
    void constructor_whitespaceOnlyRequiredFields_failAssertions() {
        for (String space : SPACES) {
            assertThrows(AssertionError.class, () -> new Camera(space, "001", 1));
            assertThrows(AssertionError.class, () -> new Camera("Camera", space, 1));
        }
    }
}
