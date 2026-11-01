package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;

/** Tests return persistence when loan data is intentionally kept in memory. */
class LoanReturnStorageTest {
    @Test
    void save_inMemoryHandlers_skipOptionalPersistenceFiles() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        Equipment current = inventory.addEquipment(
                "add i/Camera id/001 type/camera cond/5 sensor/Full frame damage/NONE");
        Equipment assessed = inventory.prepareReturnAssessment(current, false, "SHUTTER_FAULT", "Needs repair");

        LoanReturnStorage.save(inventory, current, assessed, null, List.of(), null, 1);

        assertEquals("sensor/Full frame damage/NONE", current.getDetails());
        assertEquals("", current.getRemarks());
    }
}
