package seedu.snap.equipment;

/** Persistent physical-item states that are independent of temporary loan availability. */
public enum EquipmentStatus {
    /** The physical item is present in the inventory. */
    PRESENT("present"),
    /** The physical item cannot currently be located. */
    MISSING("missing"),
    /** The physical item is believed to be permanently lost. */
    LOST("lost");

    private final String storageValue;

    EquipmentStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    /** Returns the lower-case value used in saved equipment records. */
    public String getStorageValue() {
        return storageValue;
    }

    /** Parses a case-insensitive storage or command value. */
    public static EquipmentStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Equipment status must be specified.");
        }
        for (EquipmentStatus status : values()) {
            if (status.storageValue.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Equipment status must be present, missing, or lost.");
    }
}
