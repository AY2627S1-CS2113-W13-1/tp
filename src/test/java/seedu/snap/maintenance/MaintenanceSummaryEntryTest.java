package seedu.snap.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Tests the validation and classification rules for maintenance summary rows. */
class MaintenanceSummaryEntryTest {
    @Test
    void constructor_validValues_preservesSummaryDetails() {
        LocalDate latestDate = LocalDate.of(2026, 10, 27);

        MaintenanceSummaryEntry entry = new MaintenanceSummaryEntry(
                "001", "Camera", true, 2, latestDate);

        assertEquals("001", entry.itemId());
        assertEquals("Camera", entry.itemName());
        assertTrue(entry.overdue());
        assertEquals(2, entry.repairCount());
        assertEquals(latestDate, entry.latestMaintenanceDate());
        assertTrue(entry.hasFrequentRepairs());
        assertTrue(entry.isRelevant());
    }

    @Test
    void repairThreshold_oneRepairIsNotFrequent_twoRepairsAreFrequent() {
        MaintenanceSummaryEntry oneRepair = new MaintenanceSummaryEntry(
                "001", "Camera", false, 1, null);
        MaintenanceSummaryEntry twoRepairs = new MaintenanceSummaryEntry(
                "002", "Lens", false, MaintenanceSummaryEntry.FREQUENT_REPAIR_THRESHOLD, null);

        assertFalse(oneRepair.hasFrequentRepairs());
        assertTrue(twoRepairs.hasFrequentRepairs());
        assertFalse(oneRepair.isRelevant());
        assertTrue(twoRepairs.isRelevant());
    }

    @Test
    void constructor_invalidValues_rejectsMalformedSummaryRows() {
        assertThrows(IllegalArgumentException.class,
                () -> new MaintenanceSummaryEntry("000", "Camera", false, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MaintenanceSummaryEntry("001", "", false, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MaintenanceSummaryEntry("001", "Camera", false, -1, null));
    }
}
