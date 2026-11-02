// @@author yarnieee
package seedu.snap.parser;

import java.util.Scanner;

/** Reads commands entered by the user. */
public class Parser {
    // @@author
    private static final String INPUT_DIVIDER = "-".repeat(100);
    // @@author yarnieee
    private final Scanner scanner = new Scanner(System.in);

    // @@author
    /**
     * Reads one command, separating the input prompt from the surrounding application output.
     * @return the next line entered by the user, or null when the input stream has ended
     */
    // @@author yarnieee
    public String getInput() {
        // @@author
        System.out.println();
        System.out.println(INPUT_DIVIDER);
        System.out.print("snAp > ");
        System.out.flush();
        String input = scanner.hasNextLine() ? scanner.nextLine() : null;
        // Start a fresh line even when redirected input is not echoed by a terminal.
        System.out.println();
        System.out.println(INPUT_DIVIDER);
        System.out.println();
        return input;
        // @@author yarnieee
    }
}
    // @@author
