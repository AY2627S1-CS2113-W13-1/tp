package seedu.duke;

import java.util.ArrayList;
import java.util.Scanner;

public class Duke {

    private static final String ADD_COMMAND = "add";
    private static final String ITEM_MARKER = "i/";
    private static final String ID_MARKER = "id/";
    private static final String TYPE_MARKER = "type/";
    private static final String CONDITION_MARKER = "cond/";

    /**
     * Represents an item of equipment in the inventory.
     */
    public static class Equipment {
        private final String itemName;
        private final String itemId;
        private final String type;
        private final int condition;

        /**
         * Creates an equipment item.
         *
         * @param name Name of the equipment.
         * @param id Three-digit equipment ID.
         * @param type Type of equipment.
         * @param condition Condition of the equipment from 1 to 10.
         */
        public Equipment(String name, String id, String type, int condition) {
            this.itemName = name;
            this.itemId = id;
            this.type = type;
            this.condition = condition;
        }

    }

    /**
     * Adds an equipment item to the inventory.
     *
     * @param inventory Inventory to add the equipment to.
     * @param equipment Equipment to add.
     */
    public static void addEquipment(ArrayList<Equipment> inventory, Equipment equipment) {
        inventory.add(equipment);
    }

    /**
     * Parses and validates an add command.
     *
     * @param userInput Complete command entered by the user.
     * @return The parsed equipment, or {@code null} if the command is invalid.
     */
    static Equipment parseEquipment(String userInput) {
        String addDetails = userInput.substring(ADD_COMMAND.length()).trim();
        int itemIndex = addDetails.indexOf(ITEM_MARKER);
        int idIndex = addDetails.indexOf(ID_MARKER);
        int typeIndex = addDetails.indexOf(TYPE_MARKER);
        int conditionIndex = addDetails.indexOf(CONDITION_MARKER);

        boolean markersAreInOrder = itemIndex == 0
                && idIndex > itemIndex
                && typeIndex > idIndex
                && conditionIndex > typeIndex;
        if (!markersAreInOrder) {
            System.out.println("Invalid format. Use: add i/[ITEM] id/[ITEM ID] "
                    + "type/[TYPE] cond/[CONDITION]");
            return null;
        }

        String itemString = addDetails.substring(itemIndex + ITEM_MARKER.length(), idIndex).trim();
        String id = addDetails.substring(idIndex + ID_MARKER.length(), typeIndex).trim();
        String type = addDetails.substring(typeIndex + TYPE_MARKER.length(), conditionIndex).trim();
        String conditionString = addDetails.substring(conditionIndex + CONDITION_MARKER.length()).trim();

        if (itemString.isEmpty() || itemString.length() > 100) {
            System.out.println("Item name must contain 1 to 100 characters.");
            return null;
        }

        if (!id.matches("[0-9]{3}") || id.equals("000")) {
            System.out.println("Item ID must be from 001 to 999.");
            return null;
        }

        if (type.isEmpty()) {
            System.out.println("Type cannot be empty.");
            return null;
        }

        if (!conditionString.matches("10|[1-9]")) {
            System.out.println("Condition must be a whole number from 1 to 10.");
            return null;
        }

        int condition = Integer.parseInt(conditionString);

        return new Equipment(itemString, id, type, condition);
    }

    /**
     * Main entry-point for the java.duke.Duke application.
     *
     * @param args Command-line arguments, which are not used.
     */
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

        ArrayList<Equipment> inventory = new ArrayList<>();

        String userInput = in.nextLine();

        while (!userInput.equals("quit")) {
            if (userInput.startsWith("add ")) {
                Equipment equipment = parseEquipment(userInput);
                if (equipment != null) {
                    addEquipment(inventory, equipment);
                    System.out.println("Equipment added successfully.");
                }
            }

            userInput = in.nextLine();
        }
    }
}
