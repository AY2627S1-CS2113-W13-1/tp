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
        // @@author yarnieee
        Equipment original = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author
        for (String space : SPACES) {
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    // @@author yarnieee
                    () -> handler.addEquipment("eqm-add name/" + space + " e/E002 type/camera cond/1"));
            // @@author
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    // @@author yarnieee
                    () -> handler.addEquipment("eqm-add name/Tripod e/E002 type/" + space + " cond/1"));
            // @@author
            assertEquals(List.of(original), handler.getEquipmentList());
        }
        // @@author yarnieee
        handler.addEquipment("eqm-add name/Tripod e/E002 type/tripod cond/1");
        // @@author
        assertEquals(2, handler.getEquipmentList().size());
    }

    @Test
    void addEquipment_unicodeFieldEdges_trimsEdgesAndPreservesInternalSpaces() throws Exception {
        for (String space : SPACES) {
            EquipmentHandler handler = new EquipmentHandler();
            // @@author yarnieee
            Equipment item = handler.addEquipment("eqm-add" + space + "name/" + space + "Film" + space + "Camera"
                    + space + "e/E001" + space + "type/" + space + "camera" + space + "cond/1" + space);
            // @@author
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
                    // @@author yarnieee
                    () -> handler.addEquipment("eqm-add name/Camera e/" + space + "999 e/E001 type/camera cond/1"));
            // @@author
            assertEquals(List.of(), handler.getEquipmentList());
        }
        // @@author yarnieee
        assertEquals("USB e/card reader",
                handler.addEquipment("eqm-add name/USB e/card reader e/E001 type/tripod cond/1").getItemName());
        // @@author
    }

    @Test
    void constructor_whitespaceOnlyRequiredFields_failAssertions() {
        for (String space : SPACES) {
            // @@author yarnieee
            assertThrows(AssertionError.class, () -> new Camera(space, "E001", 1));
            // @@author
            assertThrows(AssertionError.class, () -> new Camera("Camera", space, 1));
        }
    }
}
