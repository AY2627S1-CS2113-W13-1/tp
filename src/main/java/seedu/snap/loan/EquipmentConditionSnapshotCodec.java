package seedu.snap.loan;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// @@author Rohita-29

/** Converts equipment-condition snapshots to and from storage fields. */
final class EquipmentConditionSnapshotCodec {
    static final int FIELD_COUNT = 4;

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
        return encode(snapshot, "issue");
    }

    /** Shares escaping for issue and return values while keeping their storage prefixes distinct. */
    static String encode(EquipmentConditionSnapshot snapshot, String stage) {
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "Condition snapshot must not be null.");
        }

        return (stage + "Type/")
                + encodeValue(snapshot.getEquipmentType())
                + " "
                + (stage + "Condition/")
                + snapshot.getCondition()
                + " "
                + (stage + "Damage/")
                + encodeValue(snapshot.getDamage())
                + " "
                + (stage + "Remarks/")
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
        return decode(fields, startIndex, "issue");
    }

    /** Reads the same validated snapshot values using the selected issue or return prefixes. */
    static EquipmentConditionSnapshot decode(String[] fields, int startIndex, String stage) {
        if (fields == null
                || startIndex < 0
                || startIndex + FIELD_COUNT > fields.length) {
            throw new IllegalArgumentException(
                    "Incomplete condition snapshot.");
        }

        String equipmentType =
                decodeValue(extractValue(
                        fields[startIndex],
                        (stage + "Type/")));
        int condition =
                parseCondition(extractValue(
                        fields[startIndex + 1],
                        (stage + "Condition/")));
        String damage =
                decodeValue(extractValue(
                        fields[startIndex + 2],
                        (stage + "Damage/")));
        String remarks =
                decodeValue(extractValue(
                        fields[startIndex + 3],
                        (stage + "Remarks/")));

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
            String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
            if (decoded.matches("(?s).*\\R.*")) {
                throw new IllegalArgumentException("Condition values must stay on one line.");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid encoded condition value.",
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
                    "Condition must be an integer.",
                    exception);
        }
    }
}

// @@author
