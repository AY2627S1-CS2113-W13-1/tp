package seedu.snap.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;

/** Checks bounded summary columns and complete, readable subtype details. */
class EquipmentViewTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @BeforeEach
    void captureOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void inventory_longNameAndRemarks_keepsOnlyBoundedSummaryColumns() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/" + "A".repeat(100) + " id/001 type/camera cond/10 remarks/" + "r".repeat(200));

        handler.viewAllEquipment();

        List<String> lines = printed().lines().toList();
        assertTrue(lines.stream().allMatch(line -> line.length() <= 100));
        List<String> rows = lines.stream().filter(line -> line.startsWith("|")).toList();
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(line -> line.length() == 96));
        assertEquals(List.of("ID", "NAME", "TYPE", "CONDITION", "STATUS"),
                List.of(rows.getFirst().split("\\|", -1)).subList(1, 6).stream().map(String::strip).toList());
        assertTrue(rows.getLast().contains("A".repeat(45) + "..."));
        assertFalse(printed().contains("sensor/"));
        assertFalse(printed().contains("r".repeat(20)));
        assertTrue(printed().contains("show id/<ID>"));
        assertTrue(printed().contains("1 equipment item."));
    }

    @Test
    void inventory_wideAndJoinedUnicodeNames_preservesGraphemesAndColumnWidths() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/" + "相".repeat(30) + " id/001 type/camera cond/1");
        handler.addEquipment("add i/" + "😀".repeat(26) + " id/002 type/lens cond/2");
        handler.addEquipment("add i/" + "e\u0301".repeat(50) + " id/003 type/battery cond/3");
        handler.addEquipment("add i/" + "👩‍👩‍👧‍👦".repeat(8) + " id/004 type/tripod cond/4");

        handler.viewAllEquipment();

        List<String> rows = printed().lines().filter(line -> line.startsWith("| 00")).toList();
        assertEquals(" " + "相".repeat(22) + "...  ", rows.get(0).split("\\|", -1)[2]);
        assertEquals(" " + "😀".repeat(22) + "...  ", rows.get(1).split("\\|", -1)[2]);
        assertEquals(" " + "e\u0301".repeat(45) + "... ", rows.get(2).split("\\|", -1)[2]);
        assertEquals(" " + "👩‍👩‍👧‍👦".repeat(8) + " ".repeat(33), rows.get(3).split("\\|", -1)[2]);
        assertTrue(rows.stream().allMatch(row -> row.split("\\|", -1)[5].strip().equals("Available")));
    }

    @Test
    void inventory_controlCharacters_areVisibleWithoutCreatingExtraRows() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Camera\t\u001b[31m id/001 type/camera cond/1");

        handler.viewAllEquipment();

        assertTrue(printed().contains("Camera\\t\\u001b[31m"));
        assertFalse(printed().contains("\t"));
        assertFalse(printed().contains("\u001b"));
        assertEquals(2, printed().lines().filter(line -> line.startsWith("|")).count());
    }

    @Test
    void inventory_andFilteredResults_useIdenticalTablesInOriginalOrder() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Camera id/002 type/camera cond/1");
        handler.addEquipment("add i/Lens id/001 type/lens cond/2");
        handler.viewAllEquipment();
        List<String> inventoryRows = printed().lines().skip(1).toList();
        output.reset();

        handler.viewEquipment(handler.getEquipmentList());

        assertEquals(inventoryRows, printed().lines().skip(1).toList());
        assertTrue(printed().indexOf("| 002 |") < printed().indexOf("| 001 |"));
        assertTrue(printed().contains("2 equipment items."));
    }

    @Test
    void emptyInventory_exampleIsExecutableAndEmptyFilterHasItsOwnMessage() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.viewAllEquipment();
        String example = printed().lines().filter(line -> line.startsWith("Add an item: "))
                .findFirst().orElseThrow().substring("Add an item: ".length());
        assertTrue(printed().contains("0 equipment items."));
        Equipment added = handler.addEquipment(example);
        assertEquals("001", added.getItemId());
        output.reset();

        handler.viewEquipment(List.of());

        assertTrue(printed().contains("No equipment matches the filters."));
        assertTrue(printed().contains("0 equipment items."));
        assertFalse(printed().contains("Add an item:"));
    }

    @Test
    void details_wrapLongValuesWithoutTruncationAndExposeAllCameraFields() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        String name = "A".repeat(100);
        String details = "S".repeat(100);
        String remarks = "r".repeat(98) + "  " + "R".repeat(100);
        Equipment item = handler.addEquipment("add i/" + name + " id/001 type/camera cond/8 sensor/" + details
                + " mount/RF thread/1-4 battery/LP-E6 damage/OTHER remarks/" + remarks);
        item.setLoanId("007");

        handler.showEquipment("show id/001");

        assertEquals(name, field("Name"));
        assertEquals(details, field("Sensor format"));
        assertEquals(remarks, field("Remarks"));
        assertTrue(printed().lines().allMatch(line -> line.length() <= 100));
        assertTrue(printed().contains("Lens mount: RF"));
        assertTrue(printed().contains("Tripod thread: 1-4"));
        assertTrue(printed().contains("Battery type: LP-E6"));
        assertTrue(printed().contains("Damage: OTHER"));
        assertTrue(printed().contains("Status: Loaned"));
        assertTrue(printed().contains("Active loan ID: 007"));
    }

    @Test
    void details_otherSubtypesExposeCompatibilityAndDefaults() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment("add i/Lens id/001 type/lens cond/2 zoom/24-70 mount/RF sensor/APS-C");
        handler.addEquipment("add i/Battery id/002 type/battery cond/3 capacity/2000mAh battery/LP-E6");
        handler.addEquipment("add i/Tripod id/003 type/tripod cond/4");

        handler.showEquipment("show id/001");
        assertTrue(printed().contains("Zoom range: 24-70"));
        assertTrue(printed().contains("Lens mount: RF"));
        assertTrue(printed().contains("Sensor format: APS-C"));
        output.reset();
        handler.showEquipment("show id/002");
        assertTrue(printed().contains("Capacity: 2000mAh"));
        assertTrue(printed().contains("Battery type: LP-E6"));
        output.reset();
        handler.showEquipment("show id/003");
        assertTrue(printed().contains("Thread: unspecified"));
        assertTrue(printed().contains("Damage: NONE"));
        assertTrue(printed().contains("Remarks: (none)"));
        assertTrue(printed().contains("Active loan ID: (none)"));
    }

    /** Reassembles a wrapped value without changing its internal whitespace. */
    private String field(String label) {
        String prefix = label + ": ";
        StringBuilder value = new StringBuilder();
        boolean reading = false;
        for (String line : printed().lines().toList()) {
            if (line.startsWith(prefix)) {
                value.append(line.substring(prefix.length()));
                reading = true;
            } else if (reading && line.startsWith(" ".repeat(prefix.length()))) {
                value.append(line.substring(prefix.length()));
            } else if (reading) {
                break;
            }
        }
        return value.toString();
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
