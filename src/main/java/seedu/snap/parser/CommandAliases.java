package seedu.snap.parser;

import java.util.HashMap;
import java.util.Map;

/** Provides the exact short aliases for all interactive commands. */
public final class CommandAliases {
    private static final Map<String, String> ALIAS_TO_COMMAND = Map.ofEntries(
            Map.entry("ae", "add-eqm"),
            Map.entry("le", "list-eqm"),
            Map.entry("es", "eqm-show"),
            Map.entry("ed", "eqm-delete"),
            Map.entry("fe", "filter-eqm"),
            Map.entry("eu", "eqm-update"),
            Map.entry("em", "eqm-maintain"),
            Map.entry("elh", "eqm-loan-history"),
            Map.entry("al", "add-loan"),
            Map.entry("lr", "loan-return"),
            Map.entry("ld", "loan-delete"),
            Map.entry("lu", "loan-update"),
            Map.entry("ll", "list-loan"),
            Map.entry("ls", "loan-show"),
            Map.entry("llb", "list-loan-borrower"),
            Map.entry("llo", "list-loan-overdue"),
            Map.entry("ab", "add-bundle"),
            Map.entry("bae", "bundle-add-eqm"),
            Map.entry("bre", "bundle-remove-eqm"),
            Map.entry("lb", "list-bundle"),
            Map.entry("bh", "bundle-history"),
            Map.entry("bk", "backup"),
            Map.entry("rs", "restore"),
            Map.entry("h", "help"),
            Map.entry("x", "exit"));
    private static final Map<String, String> COMMAND_TO_ALIAS = createReverseMap();

    private CommandAliases() {
        // Utility class; do not instantiate.
    }

    /** Returns the canonical command for an alias, or the supplied command when it is already canonical. */
    public static String canonicalCommand(String command) {
        return ALIAS_TO_COMMAND.getOrDefault(command, command);
    }

    /** Returns the configured alias for a canonical command, or null for an unknown command. */
    public static String aliasFor(String command) {
        return COMMAND_TO_ALIAS.get(command);
    }

    /** Replaces only the first command token, preserving the command's arguments. */
    public static String normalizeInput(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.replaceAll("(?U)^\\s+|\\s+$", "");
        if (normalized.isEmpty()) {
            return normalized;
        }
        String[] parts = normalized.split("(?U)\\s+", 2);
        String canonical = canonicalCommand(parts[0]);
        if (canonical.equals(parts[0])) {
            return normalized;
        }
        return parts.length == 1 ? canonical : canonical + " " + parts[1];
    }

    private static Map<String, String> createReverseMap() {
        Map<String, String> reverse = new HashMap<>();
        ALIAS_TO_COMMAND.forEach((alias, command) -> reverse.put(command, alias));
        return Map.copyOf(reverse);
    }
}
