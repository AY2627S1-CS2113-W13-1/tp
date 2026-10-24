package seedu.snap.equipment;

// @@author Rohita-29

/**
 * Identifies a supported subtype-specific field in an equipment command.
 * Each enum value owns the command field name used for parsing and storage.
 */
enum EquipmentDetailField {
    SENSOR("sensor"),
    MOUNT("mount"),
    ZOOM("zoom"),
    CAPACITY("capacity"),
    THREAD("thread"),
    BATTERY_TYPE("battery"),
    DAMAGE("damage");

    private final String fieldName;

    /**
     * Creates a detail-field definition.
     *
     * @param fieldName field name without the trailing slash
     */
    EquipmentDetailField(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Returns the field name without its trailing slash.
     *
     * @return command field name
     */
    String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the complete command marker.
     *
     * @return field marker ending in a slash
     */
    String getMarker() {
        return fieldName + "/";
    }
}

// @@author
