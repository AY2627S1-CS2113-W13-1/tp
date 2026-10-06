package seedu.duke;

public class AddCommand extends Command {
    public static void execute(String[] item) {
        addEquipment(item);

        System.out.printf(
            "%-4s %-24s | %-6s | %-15s | %-9s%n",
            "No.", "NAME", "ID", "TYPE", "CONDITION"
        );

        System.out.printf(
            "%-4s %-24s | %-6s | %-15s | %-9s%n",
            (getEquipmentList().size()) + ")", item[0], item[1], item[2], item[3]
        );

        System.out.printf("ADDED%n%n");
    }
}
