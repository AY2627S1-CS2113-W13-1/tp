package seedu.snap.ui;

import java.io.Console;

/** Prints consistent headings and labelled outcomes, with the existing optional terminal colour policy. */
public final class ConsoleOutput {
    private static final String RESET = "\u001b[0m";

    private ConsoleOutput() {
        // Utility class; do not instantiate.
    }

    /** Prints a heading and underline without colouring the data that follows. */
    public static void heading(String title) {
        String text = inline(title);
        System.out.println(coloured(text, "36", usesColour()));
        System.out.println("-".repeat(Math.min(text.codePointCount(0, text.length()), 100)));
    }

    /** Reports a successful operation only after its handler has completed. */
    public static void success(String message) {
        System.out.println(formatOutcome("[OK]", message, false, usesColour()));
    }

    /** Reports a validation or persistence failure while retaining the full explanation. */
    public static void error(String message) {
        System.out.println(formatOutcome("[ERROR]", message, true, usesColour()));
    }

    /** Escapes control characters in record names so they cannot add terminal commands or new output rows. */
    public static String inline(String value) {
        StringBuilder text = new StringBuilder();
        value.codePoints().forEach(character -> {
            if (Character.isISOControl(character) || character == 0x2028 || character == 0x2029) {
                text.append(String.format(java.util.Locale.ROOT, "\\u%04x", character));
            } else {
                text.appendCodePoint(character);
            }
        });
        return text.toString();
    }

    /** Keeps outcome labels readable when ANSI colour is disabled. */
    static String formatOutcome(String label, String message, boolean error, boolean colour) {
        return coloured(label, error ? "31" : "32", colour) + " " + message;
    }

    /** Resets colour immediately so ordinary output and subsequent prompts keep their normal appearance. */
    private static String coloured(String text, String code, boolean enabled) {
        return enabled ? "\u001b[" + code + "m" + text + RESET : text;
    }

    /** Reuses the same environment settings as the welcome logo, including NO_COLOR and TERM=dumb. */
    private static boolean usesColour() {
        Console console = System.console();
        boolean terminal = console != null && console.isTerminal();
        boolean windows = System.getProperty("os.name", "").startsWith("Windows");
        return Welcome.usesColour(terminal, System.getenv(), windows);
    }
}
