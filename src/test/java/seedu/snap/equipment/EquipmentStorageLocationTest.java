package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Tests equipment storage-location validation and state preservation. */
class EquipmentStorageLocationTest {
    @Test
    void storageLocation_defaultIsUnspecified_andValidValuesAreTrimmed() {
        Equipment camera = new Camera("Camera", "001", 5);

        assertEquals("", camera.getStorageLocation());
        camera.setStorageLocation("  Cabinet A-3  ");

        assertEquals("Cabinet A-3", camera.getStorageLocation());
    }

    @Test
    void storageLocation_invalidValues_areRejectedWithoutChangingPreviousValue() {
        Equipment camera = new Camera("Camera", "001", 5);
        camera.setStorageLocation("Cabinet A-3");

        for (String invalid : new String[]{null, "x".repeat(Equipment.MAX_STORAGE_LOCATION_LENGTH + 1),
                "two\nlines", "two\rlines", "two\tcolumns"}) {
            assertThrows(IllegalArgumentException.class, () -> camera.setStorageLocation(invalid), invalid);
            assertEquals("Cabinet A-3", camera.getStorageLocation(), invalid);
        }
    }

    @Test
    void storageLocation_emptyValue_clearsExistingLocation() {
        Equipment camera = new Camera("Camera", "001", 5);
        camera.setStorageLocation("Cabinet A-3");

        camera.setStorageLocation("");

        assertEquals("", camera.getStorageLocation());
    }
}
