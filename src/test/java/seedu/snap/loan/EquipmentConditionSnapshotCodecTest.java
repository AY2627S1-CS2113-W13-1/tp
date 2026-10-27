package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// @@author Rohita-29

/** Tests storage encoding for equipment-condition snapshots. */
class EquipmentConditionSnapshotCodecTest {
    @Test
    void encodeAndDecode_specialCharacters_restoresSnapshot() {
        EquipmentConditionSnapshot original =
                new EquipmentConditionSnapshot(
                        "camera",
                        6,
                        "SENSOR_DAMAGE",
                        "Lens + mount / checked ✓");

        String encoded =
                EquipmentConditionSnapshotCodec.encode(original);
        EquipmentConditionSnapshot decoded =
                EquipmentConditionSnapshotCodec.decode(
                        encoded.split("\\s+"),
                        0);

        assertEquals(original, decoded);
    }

    @Test
    void encode_nullSnapshot_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentConditionSnapshotCodec.encode(null));
    }

    @Test
    void decode_incompleteFields_throwsException() {
        String[] incompleteFields = {
            "issueType/camera",
            "issueCondition/6",
            "issueDamage/NONE"
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentConditionSnapshotCodec.decode(
                        incompleteFields,
                        0));
    }

    @Test
    void decode_invalidCondition_throwsException() {
        String[] invalidFields = {
            "issueType/camera",
            "issueCondition/not-a-number",
            "issueDamage/NONE",
            "issueRemarks/"
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentConditionSnapshotCodec.decode(
                        invalidFields,
                        0));
    }
    @Test
    void decode_malformedPercentEncoding_throwsException() {
        String[] malformedFields = {
            "issueType/camera",
            "issueCondition/6",
            "issueDamage/NONE",
            "issueRemarks/%ZZ"
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> EquipmentConditionSnapshotCodec.decode(
                        malformedFields,
                        0));
    }

}

// @@author
