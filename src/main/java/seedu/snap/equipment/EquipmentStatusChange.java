package seedu.snap.equipment;

import java.time.Instant;

/** Immutable record of a persistent physical-item status transition. */
public record EquipmentStatusChange(EquipmentStatus status, Instant changedAt) {
    /** Validates status history entries at construction time. */
    public EquipmentStatusChange {
        if (status == null) {
            throw new IllegalArgumentException("Status history entries require a status.");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("Status history entries require a timestamp.");
        }
    }
}
