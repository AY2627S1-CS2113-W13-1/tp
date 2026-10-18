package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;

/** Tests equipment command parsing, insertion, and deletion. */
class EquipmentHandlerTest {
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
    public void deleteEquipment_duplicateId_keepsInventoryUnchanged()
            throws EquipmentAdditionUnsuccessful {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment first = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        Equipment second = handler.addEquipment("add i/Tripod id/001 type/accessories cond/5");
        Equipment unrelated = handler.addEquipment("add i/Microphone id/002 type/audio cond/10");

        EquipmentDeletionUnsuccessful exception = assertThrows(EquipmentDeletionUnsuccessful.class,
                () -> handler.deleteEquipment("delete item/001"));

        assertEquals("Equipment ID 001 is not unique. No equipment was deleted.", exception.getMessage());
        assertEquals(List.of(first, second, unrelated), handler.getEquipmentList());
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
}
