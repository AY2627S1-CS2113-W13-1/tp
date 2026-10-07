package seedu.snap.parser;

import java.util.Scanner;

/** Reads commands entered by the user. */
public class Parser {
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Reads and returns one command.
     * @return the next line entered by the user
     */
    public String getInput() {
        System.out.print(">>> ");
        return scanner.nextLine();
    }
}
