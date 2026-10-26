package seedu.snap.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.MaintenanceSummaryUnsuccessful;

/** Tests maintenance summary command validation and stable display output. */
class MaintenanceSummaryViewTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @BeforeEach
    void redirectOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void viewMaintenanceSummary_validCommand_displaysStableSummary() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        Equipment overdue = equipment.addEquipment("add i/Overdue id/002 type/camera cond/5");
        equipment.addEquipment("add i/Frequent id/001 type/lens cond/5");
        overdue.applyReturnAssessment(equipment.prepareReturnAssessment(overdue, false, "NONE", ""));
        MaintenanceHandler handler = new MaintenanceHandler(equipment);
        handler.addMaintenanceRecord("maintain id/001 date/25-10-26 work/Repair A outcome/Completed");
        handler.addMaintenanceRecord("maintain id/001 date/26-10-26 work/Repair B outcome/Completed");

        handler.viewMaintenanceSummary("summary maintenance");

        String printed = output.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Maintenance summary:"));
        assertTrue(printed.contains("OVERDUE"));
        assertTrue(printed.contains("Frequent"));
        assertTrue(printed.contains("YES"));
        assertTrue(printed.contains("2026-10-26"));
        assertTrue(printed.indexOf("001") < printed.indexOf("002"));
    }

    @Test
    void viewMaintenanceSummary_emptySummary_displaysClearMessage() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment("add i/Camera id/001 type/camera cond/5");
        MaintenanceHandler handler = new MaintenanceHandler(equipment);

        handler.viewMaintenanceSummary("summary maintenance");

        assertEquals("No overdue maintenance or frequent repair records found." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void viewMaintenanceSummary_invalidCommands_areRejected() {
        MaintenanceHandler handler = new MaintenanceHandler(new EquipmentHandler());

        for (String command : new String[]{"summary", "summary maintenance extra", "summary maintenance\n"}) {
            assertThrows(MaintenanceSummaryUnsuccessful.class,
                    () -> handler.viewMaintenanceSummary(command), command);
        }
    }
}
