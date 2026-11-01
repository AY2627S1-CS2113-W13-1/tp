package seedu.snap.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests maintenance persistence, validation diagnostics, and failure-safe replacement. */
class MaintenanceStorageTest {
    private static final String FIRST_EQUIPMENT_ID = UUID.randomUUID().toString();
    private static final String SECOND_EQUIPMENT_ID = UUID.randomUUID().toString();

    @TempDir
    private Path temporaryDirectory;

    @Test
    void saveAndLoad_roundTrip_preservesAllRecordFields() throws Exception {
        Path file = temporaryDirectory.resolve("maintenance.txt");
        List<MaintenanceRecord> expected = List.of(
                new MaintenanceRecord(FIRST_EQUIPMENT_ID, LocalDate.of(2026, 10, 27),
                        "Replaced the battery", "Completed"),
                new MaintenanceRecord(SECOND_EQUIPMENT_ID, LocalDate.of(2026, 11, 2),
                        "Cleaned and inspected the lens", "Passed inspection"));

        MaintenanceStorage.save(file, expected);

        assertEquals(expected.stream().map(MaintenanceRecord::getEquipmentRecordId).toList(),
                MaintenanceStorage.load(file).stream().map(MaintenanceRecord::getEquipmentRecordId).toList());
        assertEquals(expected.stream().map(MaintenanceRecord::getMaintenanceDate).toList(),
                MaintenanceStorage.load(file).stream().map(MaintenanceRecord::getMaintenanceDate).toList());
        assertEquals(expected.stream().map(MaintenanceRecord::getWork).toList(),
                MaintenanceStorage.load(file).stream().map(MaintenanceRecord::getWork).toList());
        assertEquals(expected.stream().map(MaintenanceRecord::getOutcome).toList(),
                MaintenanceStorage.load(file).stream().map(MaintenanceRecord::getOutcome).toList());
    }

    @Test
    void load_missingFile_returnsEmptyListWithoutCreatingFile() throws IOException {
        Path file = temporaryDirectory.resolve("missing-maintenance.txt");

        assertTrue(MaintenanceStorage.load(file).isEmpty());
        assertFalse(Files.exists(file));
    }

    @Test
    void load_blankLines_areIgnored() throws IOException {
        Path file = temporaryDirectory.resolve("maintenance.txt");
        Files.writeString(file, "\n  \nmaintenance\tequipment/" + FIRST_EQUIPMENT_ID
                + "\tdate/27-10-26\twork/Repair\toutcome/Completed\n");

        assertEquals(1, MaintenanceStorage.load(file).size());
    }

    @Test
    void load_malformedRecord_reportsTheLineNumber() throws IOException {
        Path file = temporaryDirectory.resolve("maintenance.txt");
        Files.writeString(file, "maintenance\tequipment/" + FIRST_EQUIPMENT_ID
                + "\tdate/27-10-26\twork/Repair\toutcome/Completed\n"
                + "not a maintenance record\n");

        IOException exception = assertThrows(IOException.class, () -> MaintenanceStorage.load(file));

        assertTrue(exception.getMessage().contains("line 2"));
    }

    @Test
    void load_invalidDate_rejectsTheSavedRecord() throws IOException {
        Path file = temporaryDirectory.resolve("maintenance.txt");
        Files.writeString(file, "maintenance\tequipment/" + FIRST_EQUIPMENT_ID
                + "\tdate/31-02-26\twork/Repair\toutcome/Completed\n");

        IOException exception = assertThrows(IOException.class, () -> MaintenanceStorage.load(file));

        assertTrue(exception.getMessage().contains("Maintenance date must use DD-MM-YY"));
    }

    @Test
    void load_wrongMarkerOrEmptyField_rejectsTheSavedRecord() throws IOException {
        Path file = temporaryDirectory.resolve("maintenance.txt");
        String validFields = "equipment/" + FIRST_EQUIPMENT_ID
                + "\tdate/27-10-26\twork/Repair\toutcome/Completed";

        Files.writeString(file, "wrong\t" + validFields + "\n");
        assertThrows(IOException.class, () -> MaintenanceStorage.load(file));

        Files.writeString(file, "maintenance\tequipment/" + FIRST_EQUIPMENT_ID
                + "\tdate/27-10-26\twork/\toutcome/Completed\n");
        assertThrows(IOException.class, () -> MaintenanceStorage.load(file));

        Files.writeString(file, "maintenance\twrong/" + FIRST_EQUIPMENT_ID
                + "\tdate/27-10-26\twork/Repair\toutcome/Completed\n");
        assertThrows(IOException.class, () -> MaintenanceStorage.load(file));
    }

    @Test
    void save_failedReplacement_preservesDestinationAndCleansTemporaryFile() throws IOException {
        Path destination = temporaryDirectory.resolve("maintenance.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        MaintenanceRecord record = new MaintenanceRecord(FIRST_EQUIPMENT_ID, LocalDate.of(2026, 10, 27),
                "Repair", "Completed");

        assertThrows(IOException.class, () -> MaintenanceStorage.save(destination, List.of(record)));

        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("maintenance-")));
        }
    }

    @Test
    void save_nullRecord_throwsAssertionError() throws IOException {
        Path file = temporaryDirectory.resolve("maintenance.txt");

        assertThrows(AssertionError.class,
                () -> MaintenanceStorage.save(file, Collections.singletonList(null)));
    }
}
