package seedu.duke;

public class ListCommand extends Command{
    public static void execute() {
        System.out.printf(
            "%-4s %-24s | %-6s | %-15s | %-9s%n",
            "No.", "NAME", "ID", "TYPE", "CONDITION"
        );

        for (int i = 0; i < equipmentList.size(); i++) {
            String[] item = equipmentList.get(i);

            System.out.printf(
                "%-4s %-24s | %-6s | %-15s | %-9s%n",
                (i + 1) + ")", item[0], item[1], item[2], item[3]
            );
        }
    }
}