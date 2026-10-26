package seedu.snap.maintenance;

import java.time.LocalDate;

/**
 * Immutable summary information for one currently registered equipment item.
 *
 * <p>An item is included in a summary when it is overdue for maintenance or
 * has reached the repeated-repair threshold. Inspection records contribute to
 * the latest maintenance date but are not counted as repairs.</p>
 *
 * @param itemId visible three-digit equipment identifier
 * @param itemName equipment name
 * @param overdue whether the item is currently unserviceable and needs maintenance
 * @param repairCount number of non-inspection maintenance records for the item
 * @param latestMaintenanceDate date of the most recent maintenance record, or null when none exists
 */
public record MaintenanceSummaryEntry(String itemId, String itemName, boolean overdue,
        int repairCount, LocalDate latestMaintenanceDate) {
    /** Minimum number of repairs required for an item to be frequently repaired. */
    public static final int FREQUENT_REPAIR_THRESHOLD = 2;

    /** Validates the immutable values used by one summary row. */
    public MaintenanceSummaryEntry {
        if (itemId == null || !itemId.matches("[0-9]{3}") || itemId.equals("000")) {
            throw new IllegalArgumentException("Summary item ID must be from 001 to 999.");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Summary item name must not be blank.");
        }
        if (repairCount < 0) {
            throw new IllegalArgumentException("Summary repair count must not be negative.");
        }
    }

    /** Returns whether the item has at least the repeated-repair threshold. */
    public boolean hasFrequentRepairs() {
        return repairCount >= FREQUENT_REPAIR_THRESHOLD;
    }

    /** Returns whether this row should be included in the maintenance summary. */
    public boolean isRelevant() {
        return overdue || hasFrequentRepairs();
    }
}
