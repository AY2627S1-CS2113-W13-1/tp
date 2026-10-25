package seedu.snap.ui;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Displays the bundled welcome artwork in ASCII by default, with optional terminal colour. */
public final class Welcome {
    private static final Logger logger = Logger.getLogger(Welcome.class.getName());
    private static final String GREEN = "\u001b[92m";
    private static final String RESET = "\u001b[0m";

    private Welcome() {
        // Utility class; do not instantiate.
    }

    /** Prints the UTF-8 resource with suitable glyphs, leaving welcome text usable if the resource cannot be read. */
    public static void printLogo() {
        try (InputStream input = Welcome.class.getResourceAsStream("/logo.txt")) {
            if (input == null) {
                logger.warning("Welcome artwork /logo.txt was not found.");
                return;
            }
            String logo = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n").replace("\n", System.lineSeparator());
            Console console = System.console();
            boolean terminal = console != null && console.isTerminal();
            boolean windows = System.getProperty("os.name", "").startsWith("Windows");
            Map<String, String> environment = System.getenv();
            System.out.println();
            System.out.print(formatLogo(logo, usesColour(terminal, environment, windows),
                    usesUnicode(terminal, environment, System.out.charset())));
            System.out.println();
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Could not read welcome artwork /logo.txt", exception);
        }
    }

    /** Preserves the artwork's spacing and resets any colour before the normal welcome text. */
    static String formatLogo(String logo, boolean colour, boolean unicode) {
        String artwork = unicode ? logo : logo.replace('\u2588', '#');
        return colour ? GREEN + artwork + RESET : artwork;
    }

    /**
     * Keeps ASCII as the default; blocks require an explicit unicode or auto selection and a suitable encoding.
     * Font support cannot be detected, so automatic selection still requires a known terminal.
     */
    static boolean usesUnicode(boolean terminal, Map<String, String> environment, Charset outputCharset) {
        String mode = environment.getOrDefault("SNAP_LOGO", "ascii");
        if (mode.equalsIgnoreCase("ascii") || !outputCharset.newEncoder().canEncode('\u2588')) {
            return false;
        }
        if (mode.equalsIgnoreCase("unicode")) {
            return true;
        }
        return terminal && !environment.getOrDefault("TERM", "").equalsIgnoreCase("dumb");
    }

    /**
     * Uses colour only for a recognised terminal, unless explicitly requested for a piped Gradle run.
     * NO_COLOR and a dumb terminal always keep the output plain.
     */
    static boolean usesColour(boolean terminal, Map<String, String> environment, boolean windows) {
        String mode = environment.getOrDefault("SNAP_COLOUR", "auto");
        String term = environment.getOrDefault("TERM", "");
        if (!environment.getOrDefault("NO_COLOR", "").isEmpty() || term.equalsIgnoreCase("dumb")
                || mode.equalsIgnoreCase("never")) {
            return false;
        }
        if (mode.equalsIgnoreCase("always")) {
            return true;
        }
        if (!terminal) {
            return false;
        }
        return !windows || !environment.getOrDefault("WT_SESSION", "").isEmpty()
                || !environment.getOrDefault("ANSICON", "").isEmpty()
                || environment.getOrDefault("ConEmuANSI", "").equalsIgnoreCase("ON") || !term.isEmpty();
    }
}
