package seedu.duke;

import java.util.Scanner;

public class ListCommandTest {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        AddCommand.execute(new String[]{"Canon G7X", "001", "Camera", "8"});
        AddCommand.execute(new String[]{"Canon G7X Mark III", "002", "Camera", "Poor"});
        AddCommand.execute(new String[]{"Canon Tripod", "011", "Tripod", "Good"});
        AddCommand.execute(new String[]{"Canon Tripod", "012", "Tripod", "Good"});

        while (true) {
            String command = in.nextLine().trim();

            if (command.equals("list")) {
                ListCommand.execute();
            } else if (command.equals("bye")) {
                break;
            } else {
                System.out.println("Unknown command.");
            }
        }
    }
}
