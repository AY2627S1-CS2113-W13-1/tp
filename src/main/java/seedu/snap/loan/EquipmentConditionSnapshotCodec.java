package seedu.snap.loan;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// @@author Rohita-29

/** Converts equipment-condition snapshots to and from storage fields. */
final class EquipmentConditionSnapshotCodec {
    static final int FIELD_COUNT = 4;

    private static final String TYPE_PREFIX = "issueType/";
    private static final String CONDITION_PREFIX = "issueCondition/";
    private static final String DAMAGE_PREFIX = "issueDamage/";
    private static final String REMARKS_PREFIX = "issueRemarks/";

    private EquipmentConditionSnapshotCodec() {
        // Utility class; do not instantiate.
    }

    /**
     * Converts a snapshot into four whitespace-safe storage fields.
     *
     * @param snapshot snapshot to encode
     * @return encoded storage fields
     * @throws IllegalArgumentException if the snapshot is null
     */
    static String encode(EquipmentConditionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "Issue-condition snapshot must not be null.");
        }

        return TYPE_PREFIX
                + encodeValue(snapshot.getEquipmentType())
                + " "
                + CONDITION_PREFIX
                + snapshot.getCondition()
                + " "
                + DAMAGE_PREFIX
                + encodeValue(snapshot.getDamage())
                + " "
                + REMARKS_PREFIX
                + encodeValue(snapshot.getRemarks());
    }

    /**
     * Recreates a snapshot from four consecutive storage fields.
     *
     * @param fields complete storage-record fields
     * @param startIndex index of the issue-type field
     * @return decoded condition snapshot
     * @throws IllegalArgumentException if the fields are incomplete or invalid
     */
    static EquipmentConditionSnapshot decode(
            String[] fields,
            int startIndex) {
        if (fields == null
                || startIndex < 0
                || startIndex + FIELD_COUNT > fields.length) {
            throw new IllegalArgumentException(
                    "Incomplete issue-condition snapshot.");
        }

        String equipmentType =
                decodeValue(extractValue(
                        fields[startIndex],
                        TYPE_PREFIX));
        int condition =
                parseCondition(extractValue(
                        fields[startIndex + 1],
                        CONDITION_PREFIX));
        String damage =
                decodeValue(extractValue(
                        fields[startIndex + 2],
                        DAMAGE_PREFIX));
        String remarks =
                decodeValue(extractValue(
                        fields[startIndex + 3],
                        REMARKS_PREFIX));

        return new EquipmentConditionSnapshot(
                equipmentType,
                condition,
                damage,
                remarks);
    }

    /** Encodes one text value so it remains within a single storage token. */
    private static String encodeValue(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8);
    }

    /** Decodes one storage token into its original text value. */
    private static String decodeValue(String value) {
        try {
            return URLDecoder.decode(
                    value,
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid encoded issue-condition value.",
                    exception);
        }
    }

    /** Extracts a value from a field with the required prefix. */
    private static String extractValue(
            String field,
            String prefix) {
        if (field == null || !field.startsWith(prefix)) {
            throw new IllegalArgumentException(
                    "Expected " + prefix + " value.");
        }

        return field.substring(prefix.length());
    }

    /** Parses the numeric condition stored in a snapshot field. */
    private static int parseCondition(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Issue condition must be an integer.",
                    exception);
        }
    }
}

// @@author
