package seedu.duke;

import java.util.Scanner;

public class ListCommandTest {
    public static void main(String[] args) {
        String banner = " ____        _        \n"
                + "|  _ \\ _   _| | _____ \n"
                + "| | | | | | | |/ / _ \\\n"
                + "| |_| | |_| |   <  __/\n"
                + "|____/ \\__,_|_|\\_\\___|\n";
        System.out.println(banner);
        System.out.println("What is your name?");
        Scanner in = new Scanner(System.in);
        System.out.println("Hello " + in.nextLine());

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
