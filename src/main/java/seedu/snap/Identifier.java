package seedu.snap;

import java.util.Locale;

/** Provides the canonical formats and conversions for typed application identifiers. */
public final class Identifier {
    /** The supported identifier kinds. */
    public enum Type {
        /** Visible equipment/item identifier. */
        EQUIPMENT('E', "equipment"),
        /** Loan record identifier. */
        LOAN('L', "loan"),
        /** Bundle identifier. */
        BUNDLE('B', "bundle"),
        /** Borrower/user identifier. */
        BORROWER('U', "borrower");

        private final char prefix;
        private final String displayName;

        Type(char prefix, String displayName) {
            this.prefix = prefix;
            this.displayName = displayName;
        }

        /** Returns the canonical prefix character. */
        public char prefix() {
            return prefix;
        }

        /** Returns a human-readable identifier name. */
        public String displayName() {
            return displayName;
        }
    }

    private Identifier() {
        // Utility class; do not instantiate.
    }

    /** Returns whether a value is a canonical, non-zero typed identifier. */
    public static boolean isValid(String value, Type type) {
        return value != null && type != null
                && value.length() == 4
                && value.charAt(0) == type.prefix
                && value.substring(1).matches("[0-9]{3}")
                && (type == Type.BORROWER || !value.endsWith("000"));
    }

    /** Normalises a user-supplied typed identifier to its uppercase canonical form. */
    public static String normalize(String value, Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Identifier type must not be null.");
        }
        if (value == null || value.length() != 4
                || Character.toUpperCase(value.charAt(0)) != type.prefix
                || !value.substring(1).matches("[0-9]{3}")) {
            throw new IllegalArgumentException("Invalid " + type.displayName + " identifier: " + value);
        }
        String canonical = type.prefix + value.substring(1);
        if (!isValid(canonical, type)) {
            throw new IllegalArgumentException("Invalid " + type.displayName + " identifier: " + value);
        }
        return canonical;
    }

    /** Returns whether a value is a legacy three-digit identifier suitable for migration. */
    public static boolean isLegacy(String value) {
        return value != null && value.matches("[0-9]{3}");
    }

    /** Converts a legacy numeric identifier to the requested canonical type. */
    public static String migrate(String value, Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Identifier type must not be null.");
        }
        try {
            return normalize(value, type);
        } catch (IllegalArgumentException exception) {
            // Continue below for the numeric format accepted only in legacy storage.
        }
        if (isLegacy(value) && (type == Type.BORROWER || !value.equals("000"))) {
            return type.prefix + value;
        }
        throw new IllegalArgumentException("Invalid " + type.displayName + " identifier: " + value);
    }

    /** Formats a supported numeric sequence value as a typed identifier. */
    public static String format(int number, Type type) {
        if (type == null || number < (type == Type.BORROWER ? 0 : 1) || number > 999) {
            throw new IllegalArgumentException("Identifier number is outside its supported range.");
        }
        return String.format(Locale.ROOT, "%c%03d", type.prefix, number);
    }

    /** Extracts the numeric portion of a canonical typed identifier. */
    public static int number(String value, Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Identifier type must not be null.");
        }
        if (!isValid(value, type)) {
            throw new IllegalArgumentException("Invalid " + type.displayName + " identifier: " + value);
        }
        return Integer.parseInt(value.substring(1));
    }

    /** Returns whether a value uses a different supported type prefix. */
    public static boolean isMismatched(String value, Type expected) {
        if (value == null || expected == null || value.length() != 4 || !value.substring(1).matches("[0-9]{3}")) {
            return false;
        }
        char prefix = Character.toUpperCase(value.charAt(0));
        return prefix != expected.prefix && "ELBU".indexOf(prefix) >= 0;
    }
}
