package seedu.duke;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores one physical item and validates the fields of its add command.
 */
class Equipment {
    private static final Pattern ADD_PATTERN = Pattern.compile(
            "add\\s+i/(.+?)\\s+id/(\\S+)\\s+type/(.+?)\\s+cond/(\\S+)");

    private final String name;
    private final String id;
    private final String type;
    private final int condition;

    private Equipment(String name, String id, String type, int condition) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.condition = condition;
    }

    /**
     * Reads fields in the documented order and rejects missing or invalid values.
     * Types are free text because the specification gives examples rather than a fixed list.
     */
    static Equipment parse(String command) {
        Matcher matcher = ADD_PATTERN.matcher(command.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid add syntax.");
        }
        String name = matcher.group(1).trim();
        String id = matcher.group(2);
        String type = matcher.group(3).trim();
        String condition = matcher.group(4);
        if (name.isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("ITEM must contain 1 to 100 characters.");
        }
        if (!id.matches("[0-9]{3}") || id.equals("000")) {
            throw new IllegalArgumentException("ITEM ID must use three digits from 001 to 999.");
        }
        if (type.isEmpty()) {
            throw new IllegalArgumentException("TYPE must not be empty.");
        }
        if (!condition.matches("[1-9]|10")) {
            throw new IllegalArgumentException("CONDITION must be a number from 1 to 10.");
        }
        return new Equipment(name, id, type, Integer.parseInt(condition));
    }

    String getName() {
        return name;
    }

    String getId() {
        return id;
    }

    /**
     * Uses the add syntax as the text file format so records remain readable and reloadable.
     */
    String toFileLine() {
        return "add i/" + name + " id/" + id + " type/" + type + " cond/" + condition;
    }
}
