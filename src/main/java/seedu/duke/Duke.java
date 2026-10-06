package seedu.duke;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the snAp command line application for adding and deleting equipment.
 * Labels application errors with [Error] to distinguish them from compiler diagnostics.
 */
public class Duke {
    private static final String ADD_SYNTAX = "add i/[ITEM] id/[ITEM ID] type/[TYPE] cond/[CONDITION]";
    private static final Pattern DELETE_PATTERN = Pattern.compile("delete\\s+item/([0-9]{3})");

    /**
     * Starts the application using a text file relative to the working directory.
     */
    public static void main(String[] args) {
        run(new Scanner(System.in), System.out, Path.of("data", "equipment.txt"));
    }

    /**
     * Loads saved equipment and processes commands until bye or the end of input.
     * Stops if saved data cannot be read, so it cannot be overwritten accidentally.
     */
    static void run(Scanner input, PrintStream output, Path file) {
        try {
            runSession(input, output, file);
        } catch (RuntimeException e) {
            output.println();
            output.println("[Error] An unexpected error occurred. Please restart snAp.");
            output.println("Details: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            printSeparator(output);
        }
    }

    /**
     * Handles command errors within the loop so the user can enter another command.
     * Startup and console failures that prevent further input are handled by run.
     */
    private static void runSession(Scanner input, PrintStream output, Path file) {
        printSeparator(output);
        List<Equipment> items;
        try {
            items = EquipmentStorage.load(file);
        } catch (IOException | SecurityException e) {
            output.println("[Error] Could not load equipment. " + e.getMessage());
            output.println("Check the contents and read permissions of " + file + " before restarting.");
            printSeparator(output);
            return;
        }
        output.println("Welcome to snAp. Loaded " + items.size() + " equipment record(s).");
        output.println();
        printCommandHelp(output);
        printSeparator(output);

        while (true) {
            printPrompt(output);
            if (!input.hasNextLine()) {
                output.println();
                printInputEnd(input, output);
                printSeparator(output);
                return;
            }
            String command = input.nextLine().trim();
            output.println();
            if (command.equals("bye")) {
                output.println("Goodbye.");
                printSeparator(output);
                return;
            }
            try {
                String commandName = command.split("\\s+", 2)[0];
                if (command.isEmpty()) {
                    output.println("[Error] No command entered.");
                    printCommandHelp(output);
                } else if (commandName.equals("add")) {
                    addEquipment(command, items, file, output);
                } else if (commandName.equals("delete")) {
                    deleteEquipment(command, items, file, input, output);
                } else {
                    output.println("[Error] Unknown command. Use add, delete or bye.");
                    printCommandHelp(output);
                }
            } catch (IllegalArgumentException e) {
                output.println("[Error] " + e.getMessage());
                output.println("Correct syntax: " + ADD_SYNTAX);
            } catch (IOException | SecurityException e) {
                output.println("[Error] Could not save equipment. Command was not completed. " + e.getMessage());
                output.println("Check that " + file + " and its directory can be written to, then try again.");
            } catch (RuntimeException e) {
                output.println("[Error] Could not complete the command. Please try again.");
                output.println("Details: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            printSeparator(output);
        }
    }

    /**
     * Rejects duplicate identifiers and saves the new record before changing the in-memory list.
     */
    private static void addEquipment(String command, List<Equipment> items, Path file, PrintStream output)
            throws IOException {
        Equipment item = Equipment.parse(command);
        if (findEquipment(items, item.getId()) != null) {
            throw new IllegalArgumentException("Equipment ID " + item.getId() + " already exists.");
        }
        List<Equipment> updatedItems = new ArrayList<>(items);
        updatedItems.add(item);
        EquipmentStorage.save(file, updatedItems);
        items.add(item);
        output.println("Added equipment: " + item.getName() + " (ID: " + item.getId() + ").");
    }

    /**
     * Validates delete syntax and removes an item only when the user enters yes.
     */
    private static void deleteEquipment(String command, List<Equipment> items, Path file,
            Scanner input, PrintStream output) throws IOException {
        Matcher matcher = DELETE_PATTERN.matcher(command);
        if (!matcher.matches() || matcher.group(1).equals("000")) {
            output.println("[Error] Invalid delete syntax. ITEM ID must be from 001 to 999.");
            printDeleteHelp(output);
            return;
        }
        Equipment item = findEquipment(items, matcher.group(1));
        if (item == null) {
            output.println("[Error] Equipment ID " + matcher.group(1) + " does not exist.");
            return;
        }
        output.println("Delete " + item.getName() + " (ID: " + item.getId() + ")? Type yes to confirm.");
        output.println();
        printPrompt(output);
        boolean confirmed = input.hasNextLine() && input.nextLine().trim().equalsIgnoreCase("yes");
        output.println();
        if (!confirmed) {
            output.println("Deletion cancelled.");
            return;
        }
        List<Equipment> updatedItems = new ArrayList<>(items);
        updatedItems.remove(item);
        EquipmentStorage.save(file, updatedItems);
        items.remove(item);
        output.println("Deleted equipment: " + item.getName() + " (ID: " + item.getId() + ").");
    }

    private static Equipment findEquipment(List<Equipment> items, String id) {
        for (Equipment item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Prints the delete command format for help and syntax errors.
     */
    private static void printDeleteHelp(PrintStream output) {
        output.println("Delete: delete item/[ITEM ID]");
    }

    /**
     * Shows the available commands when input is empty or unrecognised.
     */
    private static void printCommandHelp(PrintStream output) {
        output.println("Add: " + ADD_SYNTAX);
        printDeleteHelp(output);
        output.println();
        output.println("Type bye to exit.");
    }

    /**
     * Distinguishes a normal end of input from a failed console read.
     */
    private static void printInputEnd(Scanner input, PrintStream output) {
        if (input.ioException() == null) {
            output.println("Goodbye.");
        } else {
            output.println("[Error] Could not read input. " + input.ioException().getMessage());
            output.println("Please restart snAp. Previously saved equipment remains in the text file.");
        }
    }

    /**
     * Separates responses with a divider and blank lines.
     */
    private static void printSeparator(PrintStream output) {
        output.println();
        output.println("===============");
        output.println();
    }

    /**
     * Shows the input prompt before waiting for a command or confirmation.
     */
    private static void printPrompt(PrintStream output) {
        output.print("> ");
        output.flush();
    }
}
