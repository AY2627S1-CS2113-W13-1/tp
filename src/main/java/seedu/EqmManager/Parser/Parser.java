package seedu.EqmManager.Parser;

import java.util.Scanner;

/** Reads commands from standard input for {@code EqmManager}. */
public class Parser {
    /** Reads and returns one command; {@code AbstractFella.run()} calls this method.
     * @return the next line entered by the user
     */
    public String getInput() {
        String input;

        Scanner scanner = new Scanner(System.in); //should be closed at some point?
        System.out.print(">>> "); // your inputs will be denoted by triple ">>>"
        input = scanner.nextLine();

        return input;
    }
}
