package seedu.snap.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.MaintenanceHistoryUnsuccessful;
import seedu.snap.exceptions.MaintenanceInspectionUnsuccessful;
import seedu.snap.exceptions.MaintenanceRecordUnsuccessful;

/** Tests maintenance command validation, equipment identity binding, and persistence boundaries. */
class MaintenanceHandlerTest {
    private static final String ADD_CAMERA = "add i/Camera id/001 type/camera cond/5";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void addMaintenanceRecord_validCommand_bindsToPhysicalEquipment() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        Equipment item = equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment);

        MaintenanceRecord record = handler.addMaintenanceRecord(
                "maintain id/001 date/27-10-26 work/Replaced battery outcome/Completed");

        assertEquals(item.getRecordId(), record.getEquipmentRecordId());
        assertEquals(LocalDate.of(2026, 10, 27), record.getMaintenanceDate());
        assertEquals("Replaced battery", record.getWork());
        assertEquals("Completed", record.getOutcome());
        assertEquals(List.of(record), handler.getMaintenanceRecords());
    }

    @Test
    void addMaintenanceRecord_invalidCommand_preservesRecords() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment);

        String[] invalidCommands = {
            "maintain",
            "maintain id/001 date/27-10-26 work/Repair",
            "maintain id/000 date/27-10-26 work/Repair outcome/Completed",
            "maintain id/001 date/31-02-26 work/Repair outcome/Completed",
            "maintain id/001 date/27-10-26 work/ outcome/Completed",
            "maintain id/001 date/27-10-26 work/Repair outcome/"
        };

        for (String command : invalidCommands) {
            assertThrows(MaintenanceRecordUnsuccessful.class,
                    () -> handler.addMaintenanceRecord(command), command);
        }
        assertTrue(handler.getMaintenanceRecords().isEmpty());
    }

    @Test
    void addMaintenanceRecord_unknownEquipment_rejectsWithoutChangingRecords() {
        MaintenanceHandler handler = new MaintenanceHandler(new EquipmentHandler());

        MaintenanceRecordUnsuccessful exception = assertThrows(MaintenanceRecordUnsuccessful.class,
                () -> handler.addMaintenanceRecord(
                        "maintain id/001 date/27-10-26 work/Repair outcome/Completed"));

        assertEquals("Equipment ID 001 does not exist.", exception.getMessage());
        assertTrue(handler.getMaintenanceRecords().isEmpty());
    }

    @Test
    void addMaintenanceRecord_persistenceReload_preservesPhysicalIdentity() throws Exception {
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path maintenanceFile = temporaryDirectory.resolve("maintenance.txt");
        EquipmentHandler equipment = new EquipmentHandler(equipmentFile);
        Equipment item = equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment, maintenanceFile);

        handler.addMaintenanceRecord("maintain id/001 date/27-10-26 work/Repair outcome/Completed");

        EquipmentHandler reloadedEquipment = new EquipmentHandler(equipmentFile);
        reloadedEquipment.loadFromStorage();
        MaintenanceHandler reloaded = new MaintenanceHandler(reloadedEquipment, maintenanceFile);
        reloaded.loadFromStorage();

        assertEquals(item.getRecordId(), reloaded.getMaintenanceRecords().getFirst().getEquipmentRecordId());
        assertEquals("Repair", reloaded.getMaintenanceRecords().getFirst().getWork());
    }

    @Test
    void addMaintenanceRecord_failedSave_preservesRecordsAndDestination() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(ADD_CAMERA);
        Path destination = temporaryDirectory.resolve("maintenance.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        MaintenanceHandler handler = new MaintenanceHandler(equipment, destination);

        assertThrows(MaintenanceRecordUnsuccessful.class,
                () -> handler.addMaintenanceRecord(
                        "maintain id/001 date/27-10-26 work/Repair outcome/Completed"));

        assertTrue(handler.getMaintenanceRecords().isEmpty());
        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
    }

    @Test
    void getMaintenanceHistory_returnsMatchingRecordsInChronologicalOrder() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        Equipment camera = equipment.addEquipment(ADD_CAMERA);
        equipment.addEquipment("add i/Lens id/002 type/lens cond/5");
        MaintenanceHandler handler = new MaintenanceHandler(equipment);
        handler.addMaintenanceRecord("maintain id/001 date/27-10-26 work/Later outcome/Completed");
        handler.addMaintenanceRecord("maintain id/002 date/25-10-26 work/Other outcome/Completed");
        handler.addMaintenanceRecord("maintain id/001 date/24-10-26 work/Earlier outcome/Completed");

        List<MaintenanceRecord> history = handler.getMaintenanceHistory("history maintenance/id/001");

        assertEquals(2, history.size());
        assertEquals(camera.getRecordId(), history.getFirst().getEquipmentRecordId());
        assertEquals(LocalDate.of(2026, 10, 24), history.getFirst().getMaintenanceDate());
        assertEquals(LocalDate.of(2026, 10, 27), history.getLast().getMaintenanceDate());
        assertFalse(history.stream().anyMatch(record -> record.getWork().equals("Other")));
    }

    @Test
    void getMaintenanceHistory_emptyHistory_returnsEmptyReadOnlyList() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment);

        List<MaintenanceRecord> history = handler.getMaintenanceHistory("history maintenance/id/001");

        assertTrue(history.isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> history.add(new MaintenanceRecord(
                        equipment.getEquipmentById("001").getRecordId(),
                        LocalDate.of(2026, 10, 27), "Repair", "Completed")));
    }

    @Test
    void getMaintenanceHistory_invalidAndUnknownIdentifiers_areRejected() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment);

        for (String command : new String[]{
            "history maintenance",
            "history maintenance/id/000",
            "history maintenance/id/01",
            "history maintenance/id/001 extra",
            "history id/001"}) {
            assertThrows(MaintenanceHistoryUnsuccessful.class,
                    () -> handler.getMaintenanceHistory(command), command);
        }
        MaintenanceHistoryUnsuccessful exception = assertThrows(MaintenanceHistoryUnsuccessful.class,
                () -> handler.getMaintenanceHistory("history maintenance/id/999"));
        assertEquals("Equipment ID 999 does not exist.", exception.getMessage());
    }

    @Test
    void getMaintenanceHistory_reusedItemId_excludesPreviousPhysicalItem() throws Exception {
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path maintenanceFile = temporaryDirectory.resolve("maintenance.txt");
        EquipmentHandler equipment = new EquipmentHandler(equipmentFile);
        Equipment original = equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment, maintenanceFile);
        handler.addMaintenanceRecord("maintain id/001 date/27-10-26 work/Original outcome/Completed");

        equipment.deleteEquipment("delete item/001");
        Equipment replacement = equipment.addEquipment("add i/Replacement id/001 type/camera cond/5");
        assertFalse(original.getRecordId().equals(replacement.getRecordId()));
        assertTrue(handler.getMaintenanceHistory("history maintenance/id/001").isEmpty());

        EquipmentHandler reloadedEquipment = new EquipmentHandler(equipmentFile);
        reloadedEquipment.loadFromStorage();
        MaintenanceHandler reloaded = new MaintenanceHandler(reloadedEquipment, maintenanceFile);
        reloaded.loadFromStorage();

        assertTrue(reloaded.getMaintenanceHistory("history maintenance/id/001").isEmpty());
    }

    @Test
    void getMaintenanceDueEquipment_returnsUnserviceableItemsInIdOrder() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        Equipment first = equipment.addEquipment("add i/First id/002 type/camera cond/5");
        Equipment second = equipment.addEquipment("add i/Second id/001 type/lens cond/6");
        Equipment serviceable = equipment.addEquipment("add i/Serviceable id/003 type/battery cond/4");
        first.applyReturnAssessment(equipment.prepareReturnAssessment(first, false, "NONE", ""));
        second.applyReturnAssessment(equipment.prepareReturnAssessment(second, false, "NONE", ""));
        MaintenanceHandler handler = new MaintenanceHandler(equipment);

        List<Equipment> due = handler.getMaintenanceDueEquipment();

        assertEquals(List.of(second, first), due);
        assertFalse(due.contains(serviceable));
        assertThrows(UnsupportedOperationException.class, () -> due.clear());
    }

    @Test
    void viewMaintenanceDue_emptyAndInvalidCommands_areHandled() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment);

        assertThrows(MaintenanceInspectionUnsuccessful.class,
                () -> handler.viewMaintenanceDue("list maintenance-due extra"));
        handler.viewMaintenanceDue("list maintenance-due");
    }
}
