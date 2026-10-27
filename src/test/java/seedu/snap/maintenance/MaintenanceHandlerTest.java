package seedu.snap.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.MaintenanceRecordUnsuccessful;

/** Tests maintenance command validation, equipment identity binding, and persistence boundaries. */
class MaintenanceHandlerTest {
    private static final String ADD_CAMERA = "add i/Camera id/001 type/camera cond/5";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);

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
    void addMaintenanceRecord_invalidValues_reportValidationFailureNotSaveFailure() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment, null, FIXED_CLOCK);

        MaintenanceRecordUnsuccessful exception = assertThrows(MaintenanceRecordUnsuccessful.class,
                () -> handler.addMaintenanceRecord(
                        "maintain id/001 date/27-10-26 work/ outcome/Completed"));

        assertTrue(exception.getMessage().startsWith("Maintenance work must be"));
        assertFalse(exception.getMessage().startsWith("Could not save"));
        assertTrue(handler.getMaintenanceRecords().isEmpty());
    }

    @Test
    void addMaintenanceRecord_futureDate_isRejected() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment, null, FIXED_CLOCK);

        MaintenanceRecordUnsuccessful exception = assertThrows(MaintenanceRecordUnsuccessful.class,
                () -> handler.addMaintenanceRecord(
                        "maintain id/001 date/28-10-26 work/Repair outcome/Planned"));

        assertEquals("Maintenance date cannot be in the future.", exception.getMessage());
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
        MaintenanceHandler handler = new MaintenanceHandler(equipment, maintenanceFile, FIXED_CLOCK);

        handler.addMaintenanceRecord("maintain id/001 date/27-10-26 work/Repair outcome/Completed");

        EquipmentHandler reloadedEquipment = new EquipmentHandler(equipmentFile);
        reloadedEquipment.loadFromStorage();
        MaintenanceHandler reloaded = new MaintenanceHandler(reloadedEquipment, maintenanceFile, FIXED_CLOCK);
        reloaded.loadFromStorage();

        assertEquals(item.getRecordId(), reloaded.getMaintenanceRecords().getFirst().getEquipmentRecordId());
        assertEquals("Repair", reloaded.getMaintenanceRecords().getFirst().getWork());
    }

    @Test
    void addMaintenanceRecord_reusedVisibleId_keepsHistoriesAttachedToPhysicalItems() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        Equipment original = equipment.addEquipment(ADD_CAMERA);
        MaintenanceHandler handler = new MaintenanceHandler(equipment, null, FIXED_CLOCK);

        handler.addMaintenanceRecord("maintain id/001 date/27-10-26 work/Original repair outcome/Completed");
        equipment.deleteEquipment("delete item/001");
        Equipment replacement = equipment.addEquipment(ADD_CAMERA);
        handler.addMaintenanceRecord("maintain id/001 date/27-10-26 work/Replacement repair outcome/Completed");

        assertNotEquals(original.getRecordId(), replacement.getRecordId());
        assertEquals(original.getRecordId(), handler.getMaintenanceRecords().get(0).getEquipmentRecordId());
        assertEquals(replacement.getRecordId(), handler.getMaintenanceRecords().get(1).getEquipmentRecordId());
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
}
