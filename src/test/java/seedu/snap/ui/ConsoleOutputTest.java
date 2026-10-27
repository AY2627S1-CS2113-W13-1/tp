package seedu.snap.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies that outcome meaning survives plain output and colour never leaks into later text. */
class ConsoleOutputTest {
    @Test
    void formatOutcome_plain_retainsSuccessAndErrorLabels() {
        assertEquals("[OK] Loan 001 issued.", ConsoleOutput.formatOutcome("[OK]", "Loan 001 issued.", false, false));
        assertEquals("[ERROR] Invalid ID.", ConsoleOutput.formatOutcome("[ERROR]", "Invalid ID.", true, false));
    }

    @Test
    void formatOutcome_coloured_resetsBeforeTheMessage() {
        assertEquals("\u001b[32m[OK]\u001b[0m Loan 001 issued.",
                ConsoleOutput.formatOutcome("[OK]", "Loan 001 issued.", false, true));
        assertEquals("\u001b[31m[ERROR]\u001b[0m Invalid ID.",
                ConsoleOutput.formatOutcome("[ERROR]", "Invalid ID.", true, true));
    }

    @Test
    void inline_controlCharacters_areVisibleAndOrdinaryNamesArePreserved() {
        assertEquals("Camera 相机", ConsoleOutput.inline("Camera 相机"));
        assertEquals("Kit\\u000a\\u001b[31m", ConsoleOutput.inline("Kit\n\u001b[31m"));
    }
}
