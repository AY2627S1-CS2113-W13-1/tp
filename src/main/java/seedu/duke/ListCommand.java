package seedu.duke;

import java.util.List;

public class ListCommand {
    private static final List<String[]> ITEMS = List.of(
            new String[]{"Canon G7X", "001", "Camera", "8"},
            new String[]{"Canon G7X Mark III", "002", "Camera", "Poor"},
            new String[]{"Canon Tripod", "011", "Tripod", "Good"},
            new String[]{"Canon Tripod", "012", "Tripod", "Good"}
    );

    public static void execute() {
        System.out.printf(
                "%-4s %-24s | %-6s | %-15s | %-9s%n",
                "No.", "NAME", "ID", "TYPE", "CONDITION"
        );

        for (int i = 0; i < ITEMS.size(); i++) {
            String[] item = ITEMS.get(i);

            System.out.printf(
                    "%-4s %-24s | %-6s | %-15s | %-9s%n",
                    (i + 1) + ")",
                    item[0],
                    item[1],
                    item[2],
                    item[3]
            );
        }
    }
}