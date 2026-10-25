package seedu.snap.equipment;

// @@author Rohita-29

/**
 * Validates text used by equipment compatibility attributes.
 * Missing optional fields use UNSPECIFIED; explicitly supplied values must be valid.
 */
final class CompatibilityText {
    static final String UNSPECIFIED = "unspecified";
    static final int MAX_LENGTH = 100;

    private CompatibilityText() {
    }

    /**
     * Removes surrounding whitespace and validates a single-line attribute.
     * Preserves case, punctuation and internal spaces for display and storage.
     *
     * @param value supplied compatibility attribute
     * @return validated text without surrounding whitespace
     * @throws IllegalArgumentException if the text is missing, too long or contains invalid characters
     */
    static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Compatibility detail must not be null.");
        }
        if (value.matches("(?s).*\\R.*")) {
            throw new IllegalArgumentException("Compatibility detail must not contain line breaks.");
        }

        String normalized = EquipmentFields.trimWhitespace(value);
        if (normalized.isEmpty() || normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Compatibility detail must contain 1 to 100 characters.");
        }

        for (int index = 0; index < normalized.length(); index++) {
            if (Character.isISOControl(normalized.charAt(index))
                    && normalized.charAt(index) != '\t') {
                throw new IllegalArgumentException(
                        "Compatibility detail must not contain control characters.");
            }
        }
        return normalized;
    }
}

// @@author

