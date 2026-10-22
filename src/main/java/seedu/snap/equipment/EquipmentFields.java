package seedu.snap.equipment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads named fields for equipment commands, retaining spaces within names and types. */
final class EquipmentFields {
    private static final Pattern FIELD = Pattern.compile("(?<!\\S)(id|i|type|cond|status)/",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern EDGE_WHITESPACE = Pattern.compile("^\\s+|\\s+$",
            Pattern.UNICODE_CHARACTER_CLASS);

    private EquipmentFields() {
    }

    /**
     * Reads fields in any order and rejects blank, duplicate, or unsupported reserved fields.
     * Earlier id/card-like text within a name remains part of the name.
     *
     * @throws IllegalArgumentException when the command or its fields are malformed
     */
    static Map<String, String> parse(String input, String command, String... allowedFields) {
        String stripped = input == null ? "" : EDGE_WHITESPACE.matcher(input).replaceAll("");
        if (!stripped.matches("(?U)" + command + "\\s+.+")) {
            throw new IllegalArgumentException("Supply at least one field after " + command + ".");
        }
        String details = EDGE_WHITESPACE.matcher(stripped.substring(command.length())).replaceAll("");
        Set<String> allowed = Set.of(allowedFields);
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = FIELD.matcher(details);
        String currentField = null;
        int valueStart = 0;
        while (matcher.find()) {
            String field = matcher.group(1);
            if (field.equals("id") && "i".equals(currentField) && isNameIdText(details, matcher.end())) {
                continue;
            }
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException("Field " + field + "/ is not supported by " + command + ".");
            }
            if (currentField == null) {
                if (matcher.start() != 0) {
                    throw new IllegalArgumentException("Use a field prefix after " + command + ".");
                }
            } else {
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

    /** Keeps textual ID fragments in names while reserving numeric and signed ID fields. */
    private static boolean isNameIdText(String details, int index) {
        if (index >= details.length()) {
            return false;
        }
        char first = details.charAt(index);
        return !Character.isWhitespace(first) && !Character.isDigit(first) && first != '+' && first != '-';
    }

    /** Stores each nonempty field once so later values cannot silently overwrite earlier ones. */
    private static void addField(Map<String, String> values, String field, String value) {
        String trimmed = EDGE_WHITESPACE.matcher(value).replaceAll("");
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Field " + field + "/ cannot be empty.");
        }
        if (values.putIfAbsent(field, trimmed) != null) {
            throw new IllegalArgumentException("Field " + field + "/ must appear only once.");
        }
    }
}
