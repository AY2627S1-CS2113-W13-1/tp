package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// @@author Rohita-29

/** Tests formatting of optional subtype-specific equipment fields. */
class EquipmentDetailFormatterTest {
    @Test
    void appendIfSpecified_suppliedValue_appendsField() {
        StringBuilder details = new StringBuilder("sensor/APS-C");

        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.MOUNT,
                "Canon RF");

        assertEquals(
                "sensor/APS-C mount/Canon RF",
                details.toString());
    }

    @Test
    void appendIfSpecified_unspecifiedValue_leavesTextUnchanged() {
        StringBuilder details = new StringBuilder("sensor/APS-C");

        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.MOUNT,
                CompatibilityText.UNSPECIFIED);

        assertEquals("sensor/APS-C", details.toString());
    }

    @Test
    void appendIfSpecified_multipleValues_preservesOrder() {
        StringBuilder details = new StringBuilder("sensor/APS-C");

        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.MOUNT,
                "Canon RF");
        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.THREAD,
                "1/4\"-20");
        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.BATTERY_TYPE,
                "LP-E17");

        assertEquals(
                "sensor/APS-C mount/Canon RF "
                        + "thread/1/4\"-20 battery/LP-E17",
                details.toString());
    }
}

// @@author
