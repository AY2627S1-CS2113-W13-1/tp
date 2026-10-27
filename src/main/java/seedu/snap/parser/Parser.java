package seedu.snap.parser;

import java.util.Scanner;

/** Reads commands entered by the user. */
public class Parser {
    private static final String INPUT_DIVIDER = "-".repeat(100);
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Reads one command, separating the input prompt from the surrounding application output.
     * @return the next line entered by the user
     */
    public String getInput() {
        System.out.println();
        System.out.println(INPUT_DIVIDER);
        System.out.print(">>> ");
        System.out.flush();
        String input = scanner.nextLine();
        // Start a fresh line even when redirected input is not echoed by a terminal.
        System.out.println();
        System.out.println(INPUT_DIVIDER);
        System.out.println();
        return input;
    }
}
