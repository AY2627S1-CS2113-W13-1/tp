package seedu.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class DukeTest {
    @Test
    public void parseEquipment_validInput_returnsEquipment() {
        Duke.Equipment equipment = Duke.parseEquipment(
                "add i/Canon EOS R10 id/001 type/camera cond/1");

        assertNotNull(equipment);
    }

    @Test
    public void parseEquipment_invalidId_returnsNull() {
        Duke.Equipment equipment = Duke.parseEquipment(
                "add i/Canon EOS R10 id/000 type/camera cond/1");

        assertNull(equipment);
    }

    @Test
    public void parseEquipment_invalidCondition_returnsNull() {
        Duke.Equipment equipment = Duke.parseEquipment(
                "add i/Canon EOS R10 id/001 type/camera cond/11");

        assertNull(equipment);
    }

    @Test
    public void addEquipment_validEquipment_addsToInventory() {
        ArrayList<Duke.Equipment> inventory = new ArrayList<>();
        Duke.Equipment equipment =
                new Duke.Equipment("Canon EOS R10", "001", "camera", 1);

        Duke.addEquipment(inventory, equipment);

        assertEquals(1, inventory.size());
        assertSame(equipment, inventory.get(0));
    }
}
