package seedu.snap.equipment;

// @@author Rohita-29

/** Formats subtype-specific equipment fields for display and storage. */
final class EquipmentDetailFormatter {
    private EquipmentDetailFormatter() {
    }

    /**
     * Appends a supplied equipment field.
     * Fields using the common unspecified value are omitted.
     *
     * @param details destination containing the formatted details
     * @param field field being appended
     * @param value normalized field value
     */
    static void appendIfSpecified(
            StringBuilder details,
            EquipmentDetailField field,
            String value) {
        assert details != null : "Details destination must exist.";
        assert field != null : "Equipment detail field must exist.";
        assert value != null : "Equipment detail value must exist.";

        if (!CompatibilityText.UNSPECIFIED.equals(value)) {
            details.append(' ')
                    .append(field.getMarker())
                    .append(value);
        }
    }
}

// @@author
