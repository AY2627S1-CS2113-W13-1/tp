package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

/** Tests equipment command parsing and inventory insertion. */
class EquipmentHandlerTest {
    @Test
    public void parseEquipment_validInput_returnsEquipment() {
        Equipment equipment = EquipmentHandler.parseEquipment(
                "add i/Canon EOS R10 id/001 type/camera cond/1");

        assertNotNull(equipment);
    }

    @Test
    public void parseEquipment_invalidId_returnsNull() {
        Equipment equipment = EquipmentHandler.parseEquipment(
                "add i/Canon EOS R10 id/000 type/camera cond/1");

        assertNull(equipment);
    }

    @Test
    public void parseEquipment_invalidCondition_returnsNull() {
        Equipment equipment = EquipmentHandler.parseEquipment(
                "add i/Canon EOS R10 id/001 type/camera cond/11");

        assertNull(equipment);
    }

    @Test
    public void addEquipment_validEquipment_addsToInventory() {
        ArrayList<Equipment> inventory = new ArrayList<>();
        Equipment equipment =
                new Equipment("Canon EOS R10", "001", "camera", 1);

        EquipmentHandler.addEquipment(inventory, equipment);

        assertEquals(1, inventory.size());
        assertSame(equipment, inventory.get(0));
    }
}
