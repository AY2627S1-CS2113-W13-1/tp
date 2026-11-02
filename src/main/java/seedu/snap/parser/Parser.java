package seedu.snap.parser;

import java.util.Scanner;

/** Reads commands entered by the user. */
public class Parser {
    private static final String INPUT_DIVIDER = "-".repeat(100);
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Reads one command, separating the input prompt from the surrounding application output.
     * @return the next line entered by the user, or {@code null} when input has ended
     */
    public String getInput() {
        System.out.println();
        System.out.println(INPUT_DIVIDER);
        System.out.print("snAp > ");
        System.out.flush();
        // @@author Yi An
        if (!scanner.hasNextLine()) {
            // Keep redirected output readable when the input stream ends without a newline.
            System.out.println();
            return null;
        }
        // @@author
        String input = scanner.nextLine();
        // Start a fresh line even when redirected input is not echoed by a terminal.
        System.out.println();
        System.out.println(INPUT_DIVIDER);
        System.out.println();
        return input;
    }
}
