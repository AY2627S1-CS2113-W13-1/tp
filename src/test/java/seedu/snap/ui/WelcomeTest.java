package seedu.snap.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Checks the packaged welcome artwork, ASCII fallback and terminal colour choices. */
class WelcomeTest {
    @Test
    void printLogo_packagedResource_preservesArtworkAndSpacing() throws Exception {
        String logo;
        try (InputStream resource = Welcome.class.getResourceAsStream("/logo.txt")) {
            assertNotNull(resource, "The logo must be available from the application classpath.");
            logo = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(14, logo.lines().filter(line -> !line.isBlank()).count());
        assertTrue(logo.contains("█████████████"));
        assertFalse(logo.contains("\uFFFD"), "The artwork must decode as UTF-8.");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            Welcome.printLogo();
        } finally {
            System.setOut(originalOutput);
        }

        String printed = output.toString(StandardCharsets.UTF_8)
                .replaceAll("\u001B\\[[;\\d]*m", "").replace("\r\n", "\n");
        Console console = System.console();
        boolean terminal = console != null && console.isTerminal();
        boolean unicode = Welcome.usesUnicode(terminal, System.getenv(), StandardCharsets.UTF_8);
        String expectedLogo = unicode ? logo : logo.replace('█', '#');
        assertTrue(printed.startsWith("\n"), "A blank line must precede the welcome artwork.");
        assertTrue(printed.contains(expectedLogo.stripTrailing().replace("\r\n", "\n")));
        assertTrue(printed.endsWith("\n\n"),
                "A blank line must separate the logo from the welcome text.");
    }

    @Test
    void formatLogo_plain_preservesArtworkWithoutEscapeCodes() {
        String logo = "██  ██\n  ████\n";

        assertEquals(logo, Welcome.formatLogo(logo, false, true));
    }

    @Test
    void formatLogo_coloured_resetsColourAfterArtwork() {
        String logo = "██  ██\n  ████\n";

        assertEquals("\u001B[92m" + logo + "\u001B[0m", Welcome.formatLogo(logo, true, true));
    }

    @Test
    void formatLogo_ascii_preservesEveryArtworkPosition() throws Exception {
        String logo;
        try (InputStream resource = Welcome.class.getResourceAsStream("/logo.txt")) {
            assertNotNull(resource);
            logo = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }

        String ascii = Welcome.formatLogo(logo, false, false);

        assertEquals(441, logo.chars().filter(character -> character == '█').count());
        assertEquals(logo.replace('█', '#'), ascii,
                "Only block characters should change, preserving every space and line break.");
        assertEquals(logo.lines().map(String::length).toList(), ascii.lines().map(String::length).toList());
        assertEquals(441, ascii.chars().filter(character -> character == '#').count());
        assertFalse(ascii.contains("█"));
    }

    @Test
    void formatLogo_asciiColour_replacesOnlyFullBlocksAndResetsColour() {
        String logo = "█ ░ ▓ #\r\n  ██\n";

        assertEquals("\u001B[92m# ░ ▓ #\r\n  ##\n\u001B[0m", Welcome.formatLogo(logo, true, false));
    }

    @Test
    void usesUnicode_auto_requiresTerminalWithSuitableEncoding() {
        assertTrue(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", "auto"), StandardCharsets.UTF_8));
        assertTrue(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", "auto"), Charset.forName("IBM437")));
        assertFalse(Welcome.usesUnicode(false, Map.of("SNAP_LOGO", "auto"), StandardCharsets.UTF_8));
        assertFalse(Welcome.usesUnicode(false, Map.of("SNAP_LOGO", "auto", "WT_SESSION", "session"),
                StandardCharsets.UTF_8));
        assertFalse(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", "auto", "TERM", "dumb"),
                StandardCharsets.UTF_8));
        assertFalse(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", "auto"), Charset.forName("windows-1252")));
    }

    @Test
    void usesUnicode_default_keepsAsciiEvenInUtf8Terminal() {
        assertFalse(Welcome.usesUnicode(true, Map.of(), StandardCharsets.UTF_8));
        assertFalse(Welcome.usesUnicode(false, Map.of(), StandardCharsets.UTF_8));
        assertFalse(Welcome.usesUnicode(true, Map.of("WT_SESSION", "session", "TERM", "xterm-256color"),
                StandardCharsets.UTF_8));
    }

    @Test
    void usesUnicode_explicitAscii_disablesBlocksEvenInCapableTerminal() {
        assertFalse(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", "ascii"), StandardCharsets.UTF_8));
        assertFalse(Welcome.usesUnicode(false, Map.of("SNAP_LOGO", "ascii"), StandardCharsets.UTF_8));
    }

    @Test
    void usesUnicode_explicitUnicode_overridesUncertainTerminalButRequiresSuitableEncoding() {
        assertTrue(Welcome.usesUnicode(false, Map.of("SNAP_LOGO", "unicode"), StandardCharsets.UTF_8));
        assertTrue(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", "unicode", "TERM", "dumb"),
                StandardCharsets.UTF_8));
        assertTrue(Welcome.usesUnicode(false, Map.of("SNAP_LOGO", "unicode"), Charset.forName("IBM437")));
        assertFalse(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", "unicode"), Charset.forName("windows-1252")));
        assertFalse(Welcome.usesUnicode(false, Map.of("SNAP_LOGO", "unicode"), StandardCharsets.US_ASCII));
    }

    @Test
    void usesUnicode_autoOrUnknownChoice_usesAutomaticDetection() {
        for (String mode : new String[]{"auto", "unexpected", ""}) {
            assertTrue(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", mode), StandardCharsets.UTF_8));
            assertFalse(Welcome.usesUnicode(false, Map.of("SNAP_LOGO", mode), StandardCharsets.UTF_8));
            assertFalse(Welcome.usesUnicode(true, Map.of("SNAP_LOGO", mode, "TERM", "dumb"),
                    StandardCharsets.UTF_8));
        }
    }

    @Test
    void usesColour_auto_requiresTerminal() {
        assertTrue(Welcome.usesColour(true, Map.of(), false));
        assertFalse(Welcome.usesColour(false, Map.of(), false));
        assertFalse(Welcome.usesColour(false, Map.of("TERM", "xterm-256color"), false));
        assertFalse(Welcome.usesColour(false, Map.of("WT_SESSION", "session"), true));
    }

    @Test
    void usesColour_windowsAuto_requiresRecognisedTerminal() {
        assertFalse(Welcome.usesColour(true, Map.of(), true));
        assertTrue(Welcome.usesColour(true, Map.of("WT_SESSION", "session"), true));
        assertTrue(Welcome.usesColour(true, Map.of("ANSICON", "terminal"), true));
        assertTrue(Welcome.usesColour(true, Map.of("ConEmuANSI", "ON"), true));
        assertTrue(Welcome.usesColour(true, Map.of("TERM", "xterm-256color"), true));
        assertFalse(Welcome.usesColour(true, Map.of("WT_SESSION", "", "TERM", ""), true));
        assertFalse(Welcome.usesColour(true, Map.of("ConEmuANSI", "OFF"), true));
    }

    @Test
    void usesColour_explicitChoice_canEnablePipesOrDisableTerminals() {
        assertTrue(Welcome.usesColour(false, Map.of("SNAP_COLOUR", "always"), false));
        assertTrue(Welcome.usesColour(false, Map.of("SNAP_COLOUR", "always"), true));
        assertFalse(Welcome.usesColour(true, Map.of("SNAP_COLOUR", "never"), false));
        assertFalse(Welcome.usesColour(true, Map.of("SNAP_COLOUR", "never", "WT_SESSION", "session"), true));
    }

    @Test
    void usesColour_noColourOrDumbTerminal_overridesExplicitColour() {
        assertFalse(Welcome.usesColour(true, Map.of("NO_COLOR", "1"), false));
        assertFalse(Welcome.usesColour(false, Map.of("NO_COLOR", "1", "SNAP_COLOUR", "always"), false));
        assertFalse(Welcome.usesColour(true, Map.of("TERM", "dumb"), false));
        assertFalse(Welcome.usesColour(false, Map.of("TERM", "dumb", "SNAP_COLOUR", "always"), true));
        assertTrue(Welcome.usesColour(true, Map.of("NO_COLOR", ""), false));
    }

    @Test
    void usesColour_unknownChoice_usesAutomaticDetection() {
        assertTrue(Welcome.usesColour(true, Map.of("SNAP_COLOUR", "unexpected"), false));
        assertFalse(Welcome.usesColour(false, Map.of("SNAP_COLOUR", "unexpected"), false));
        assertFalse(Welcome.usesColour(true, Map.of("SNAP_COLOUR", "unexpected"), true));
    }
}
