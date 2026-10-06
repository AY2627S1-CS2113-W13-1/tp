package seedu.snap.parser;

import java.util.Scanner;

public class Parser {
    /** Reads and returns one command.
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
