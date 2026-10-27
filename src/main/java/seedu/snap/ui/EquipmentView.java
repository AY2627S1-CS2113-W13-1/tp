package seedu.snap.ui;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.Lens;
import seedu.snap.equipment.Tripod;

/** Renders compact equipment tables and complete item details without changing equipment records. */
public final class EquipmentView {
    private static final int LINE_WIDTH = 100;
    private static final int[] COLUMN_WIDTHS = {3, 48, 7, 9, 13};
    private static final String TABLE_BORDER = "+-----+" + "-".repeat(50)
            + "+---------+-----------+---------------+";

    private EquipmentView() {
        // Utility class; do not instantiate.
    }

    /** Displays inventory records, including an actionable example when the inventory is empty. */
    public static void printInventory(List<Equipment> items) {
        printList(items, "Equipment", "No equipment in the inventory.");
        if (items.isEmpty()) {
            System.out.println("Add an item: add i/Camera id/001 type/camera cond/1");
        }
    }

    /** Displays filtered records using the same columns and order as the inventory. */
    public static void printMatches(List<Equipment> items) {
        printList(items, "Matching equipment", "No equipment matches the filters.");
    }

    /** Prints only summary fields; full values are accessible through the documented detail command. */
    private static void printList(List<Equipment> items, String heading, String emptyMessage) {
        System.out.println(heading);
        if (items.isEmpty()) {
            System.out.println(emptyMessage);
        } else {
            System.out.println(TABLE_BORDER);
            printRow("ID", "NAME", "TYPE", "CONDITION", "STATUS");
            System.out.println(TABLE_BORDER);
            for (Equipment item : items) {
                printRow(item.getItemId(), item.getItemName(), item.getType(),
                        Integer.toString(item.getCondition()), status(item));
            }
            System.out.println(TABLE_BORDER);
            System.out.println("Names ending in ... are shortened. Use show id/<ID> for full details.");
        }
        System.out.println(items.size() + " equipment " + (items.size() == 1 ? "item." : "items."));
    }

    /** Prints all current subtype and compatibility values, including unspecified and empty values. */
    public static void printDetails(Equipment item) {
        System.out.println("Equipment " + item.getItemId());
        System.out.println("-".repeat(13));
        printField("Name", item.getItemName());
        printField("Type", item.getType());
        printField("Condition", Integer.toString(item.getCondition()));
        printField("Status", status(item));
        printField("Serviceable", item.isServiceable() ? "yes" : "no");
        printField("Active loan ID", item.getLoanId() == null ? "(none)" : item.getLoanId());
        switch (item) {
        case Camera camera -> {
            printField("Sensor format", camera.getSensorFormat());
            printField("Lens mount", camera.getLensMount());
            printField("Tripod thread", camera.getTripodThread());
            printField("Battery type", camera.getBatteryType());
            printField("Damage", camera.getDamage().name());
        }
        case Lens lens -> {
            printField("Zoom range", lens.getZoomRange());
            printField("Lens mount", lens.getLensMount());
            printField("Sensor format", lens.getSensorFormat());
            printField("Damage", lens.getDamage().name());
        }
        case Battery battery -> {
            printField("Capacity", battery.getCapacity());
            printField("Battery type", battery.getBatteryType());
            printField("Damage", battery.getDamage().name());
        }
        case Tripod tripod -> {
            printField("Thread", tripod.getThread());
            printField("Damage", tripod.getDamage().name());
        }
        default -> printField("Details", item.getDetails());
        }
        printField("Remarks", item.getRemarks().isEmpty() ? "(none)" : item.getRemarks());
    }

    /** Retains the existing precedence of active loan and serviceability state. */
    private static String status(Equipment item) {
        return item.isLoaned() ? "Loaned" : item.isServiceable() ? "Available" : "Unserviceable";
    }

    /** Pads by terminal columns rather than Java string length so wide names keep their neighbours aligned. */
    private static void printRow(String... values) {
        StringBuilder row = new StringBuilder("|");
        for (int index = 0; index < values.length; index++) {
            row.append(' ').append(cell(values[index], COLUMN_WIDTHS[index])).append(" |");
        }
        System.out.println(row);
    }

    /** Truncates whole graphemes and marks truncation with ASCII dots. Control characters remain visible. */
    private static String cell(String value, int width) {
        List<String> characters = graphemes(printable(value));
        int total = characters.stream().mapToInt(EquipmentView::columns).sum();
        int limit = total <= width ? width : width - 3;
        StringBuilder text = new StringBuilder();
        int used = 0;
        for (String character : characters) {
            int next = columns(character);
            if (used + next > limit) {
                break;
            }
            text.append(character);
            used += next;
        }
        if (total > width) {
            text.append("...");
            used += 3;
        }
        return text.append(" ".repeat(width - used)).toString();
    }

    /** Wraps complete values at the terminal boundary without discarding spaces or cutting graphemes. */
    private static void printField(String label, String value) {
        String prefix = label + ": ";
        String continuation = " ".repeat(prefix.length());
        StringBuilder line = new StringBuilder(prefix);
        int used = prefix.length();
        for (String character : graphemes(printable(value))) {
            int next = columns(character);
            if (used + next > LINE_WIDTH) {
                System.out.println(line);
                line = new StringBuilder(continuation);
                used = continuation.length();
            }
            line.append(character);
            used += next;
        }
        System.out.println(line);
    }

    /** Makes tabs and control characters explicit so stored text cannot add rows or shift columns. */
    private static String printable(String value) {
        StringBuilder text = new StringBuilder();
        value.codePoints().forEach(character -> {
            switch (character) {
            case '\t' -> text.append("\\t");
            case '\r' -> text.append("\\r");
            case '\n' -> text.append("\\n");
            default -> {
                if (Character.isISOControl(character) || character == 0x2028 || character == 0x2029) {
                    text.append(String.format(Locale.ROOT, "\\u%04x", character));
                } else {
                    text.appendCodePoint(character);
                }
            }
            }
        });
        return text.toString();
    }

    /** Keeps combining marks and joined emoji with the character they belong to. */
    private static List<String> graphemes(String text) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        List<String> characters = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; end = iterator.next()) {
            characters.add(text.substring(start, end));
            start = end;
        }
        return characters;
    }

    /** Counts common terminal-wide CJK and emoji graphemes as two columns; combining marks add no width. */
    private static int columns(String character) {
        if (character.indexOf('\uFE0F') >= 0 || character.indexOf('\u20E3') >= 0) {
            return 2;
        }
        return character.codePoints().map(codePoint -> {
            int type = Character.getType(codePoint);
            if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
                    || type == Character.FORMAT) {
                return 0;
            }
            return isWide(codePoint) ? 2 : 1;
        }).max().orElse(0);
    }

    /** Covers the wide character ranges used by terminal CJK text and common emoji. */
    private static boolean isWide(int character) {
        return character >= 0x1100 && (character <= 0x115F || character == 0x2329 || character == 0x232A
                || character >= 0x2E80 && character <= 0xA4CF && character != 0x303F
                || character >= 0xAC00 && character <= 0xD7A3 || character >= 0xF900 && character <= 0xFAFF
                || character >= 0xFE10 && character <= 0xFE19 || character >= 0xFE30 && character <= 0xFE6F
                || character >= 0xFF00 && character <= 0xFF60 || character >= 0xFFE0 && character <= 0xFFE6
                || character >= 0x1F000 && character <= 0x1FAFF || character >= 0x20000 && character <= 0x3FFFD);
    }
}
