package seedu.snap.equipment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads named equipment fields without losing spaces within names and types. */
final class EquipmentFields {
    static final String ID_FIELD = "id";
    static final String NAME_FIELD = "i";
    static final String TYPE_FIELD = "type";
    static final String CONDITION_FIELD = "cond";
    static final String STATUS_FIELD = "status";

    private static final Pattern FIELD = Pattern.compile("(?<!\\S)("
            + String.join("|", ID_FIELD, NAME_FIELD, TYPE_FIELD, CONDITION_FIELD, STATUS_FIELD) + ")/",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern EDGE_WHITESPACE = Pattern.compile("^\\s+|\\s+$",
            Pattern.UNICODE_CHARACTER_CLASS);

    private EquipmentFields() {
    }

    /** Reads fields in any order, rejecting empty, repeated, and unsupported reserved fields. */
    static Map<String, String> parse(String input, String command, String... allowedFields) {
        String details = commandDetails(input, command);
        Set<String> allowed = Set.of(allowedFields);
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = FIELD.matcher(details);
        String currentField = null;
        int valueStart = 0;
        while (matcher.find()) {
            String field = matcher.group(1);
            if (field.equals(ID_FIELD) && NAME_FIELD.equals(currentField) && isNameIdText(details, matcher.end())) {
                continue;
            }
            validateFieldPrefix(field, allowed, currentField, matcher.start(), command);
            if (currentField != null) {
                addField(values, currentField, details.substring(valueStart, matcher.start()));
            }
            currentField = field;
            valueStart = matcher.end();
        }
        if (currentField == null) {
            throw new IllegalArgumentException("Use a supported field prefix after " + command + ".");
        }
        addField(values, currentField, details.substring(valueStart));
        return values;
    }

    /** Checks the command before its field values are collected. */
    private static String commandDetails(String input, String command) {
        String stripped = input == null ? "" : trimWhitespace(input);
        if (!stripped.matches("(?U)" + command + "\\s+.+")) {
            throw new IllegalArgumentException("Supply at least one field after " + command + ".");
        }
        return trimWhitespace(stripped.substring(command.length()));
    }

    /** Prevents unsupported prefixes and unlabelled text before the first field. */
    private static void validateFieldPrefix(String field, Set<String> allowed, String currentField,
            int startIndex, String command) {
        if (!allowed.contains(field)) {
            throw new IllegalArgumentException("Field " + field + "/ is not supported by " + command + ".");
        }
        if (currentField == null && startIndex != 0) {
            throw new IllegalArgumentException("Use a field prefix after " + command + ".");
        }
    }

    /** Removes Unicode whitespace at the edges without changing spaces inside a field. */
    static String trimWhitespace(String value) {
        return EDGE_WHITESPACE.matcher(value).replaceAll("");
    }

    /** Includes nonbreaking spaces recognised by the Unicode field separator pattern. */
    static boolean isWhitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }

    /** Keeps id/card-like text in names, while reserving empty, numeric, and signed ID fields. */
    private static boolean isNameIdText(String details, int index) {
        if (index >= details.length()) {
            return false;
        }
        char first = details.charAt(index);
        return !isWhitespace(first) && !Character.isDigit(first) && first != '+' && first != '-';
    }

    /** Stores each nonempty value once so a later field cannot silently overwrite it. */
    private static void addField(Map<String, String> values, String field, String value) {
        String trimmed = trimWhitespace(value);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Field " + field + "/ cannot be empty.");
        }
        if (values.putIfAbsent(field, trimmed) != null) {
            throw new IllegalArgumentException("Field " + field + "/ must appear only once.");
        }
    }
}
