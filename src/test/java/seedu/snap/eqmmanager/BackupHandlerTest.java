package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BackupUnsuccessful;
import seedu.snap.loan.LoanHandler;

/** Verifies backup format creation, destination validation and non-destructive behavior. */
class BackupHandlerTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void createBackup_populatedData_copiesAllFilesAndLeavesLiveDataUnchanged() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        loans.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        List<Path> liveFiles = List.of(dataDirectory.resolve("equipment.txt"), dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        List<byte[]> liveContents = liveFiles.stream().map(this::readBytes).toList();
        FileTime marker = FileTime.fromMillis(1000);
        liveFiles.forEach(file -> setModifiedTime(file, marker));
        Path destination = temporaryDirectory.resolve("backup copy");
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);

        Path created = handler.createBackup("backup path/" + destination);

        assertEquals(destination.toAbsolutePath().normalize(), created);
        assertArrayEquals(liveContents.get(0), readBytes(created.resolve(BackupHandler.EQUIPMENT_FILE_NAME)));
        assertArrayEquals(liveContents.get(1), readBytes(created.resolve(BackupHandler.LOAN_FILE_NAME)));
        assertArrayEquals(liveContents.get(2), readBytes(created.resolve(BackupHandler.SEQUENCE_FILE_NAME)));
        assertTrue(Files.readString(created.resolve(BackupHandler.MANIFEST_FILE_NAME))
                .startsWith("backupVersion/1"));
        for (int i = 0; i < liveFiles.size(); i++) {
            assertArrayEquals(liveContents.get(i), readBytes(liveFiles.get(i)));
            assertEquals(marker, Files.getLastModifiedTime(liveFiles.get(i)));
        }
    }

    @Test
    void createBackup_emptyData_createsValidEmptyRecordsAndInitialSequence() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        Path destination = temporaryDirectory.resolve("empty-backup");

        new BackupHandler(dataDirectory, equipment, loans).createBackup("backup path/" + destination);

        assertTrue(Files.exists(destination.resolve(BackupHandler.EQUIPMENT_FILE_NAME)));
        assertTrue(Files.exists(destination.resolve(BackupHandler.LOAN_FILE_NAME)));
        assertEquals("", Files.readString(destination.resolve(BackupHandler.EQUIPMENT_FILE_NAME)));
        assertEquals("", Files.readString(destination.resolve(BackupHandler.LOAN_FILE_NAME)));
        assertEquals("nextLoanId/001" + System.lineSeparator(),
                Files.readString(destination.resolve(BackupHandler.SEQUENCE_FILE_NAME)));
        assertFalse(Files.exists(dataDirectory));
    }

    @Test
    void createBackup_existingDestination_doesNotOverwriteExistingData() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        Path destination = temporaryDirectory.resolve("existing-backup");
        Files.createDirectories(destination);
        Path marker = destination.resolve("keep.txt");
        Files.writeString(marker, "keep this backup");

        BackupUnsuccessful exception = assertThrows(BackupUnsuccessful.class,
                () -> new BackupHandler(dataDirectory, equipment, loans)
                        .createBackup("backup path/" + destination));

        assertTrue(exception.getMessage().contains("already exists"));
        assertEquals("keep this backup", Files.readString(marker));
        assertFalse(Files.exists(destination.resolve(BackupHandler.MANIFEST_FILE_NAME)));
    }

    @Test
    void createBackup_invalidDestinationOrLiveData_rejectsBeforeWriting() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        Path parentFile = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(parentFile, "not a directory");
        Path insideLiveData = dataDirectory.resolve("backup");

        assertThrows(BackupUnsuccessful.class,
                () -> new BackupHandler(dataDirectory, equipment, loans)
                        .createBackup("backup path/" + parentFile.resolve("backup")));
        assertThrows(BackupUnsuccessful.class,
                () -> new BackupHandler(dataDirectory, equipment, loans)
                        .createBackup("backup path/" + insideLiveData));
        Files.createDirectories(dataDirectory.resolve(BackupHandler.EQUIPMENT_FILE_NAME));
        assertThrows(BackupUnsuccessful.class,
                () -> new BackupHandler(dataDirectory, equipment, loans)
                        .createBackup("backup path/" + temporaryDirectory.resolve("failed-backup")));
        assertFalse(Files.exists(temporaryDirectory.resolve("failed-backup")));
    }

    @Test
    void createBackup_invalidCommand_reportsRequiredFormat() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);

        for (String command : new String[]{null, "", "backup", "backup path/", "backup file/test",
                "backup path/test\nsecond"}) {
            assertThrows(BackupUnsuccessful.class, () -> handler.createBackup(command), command);
        }
    }

    private byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private void setModifiedTime(Path path, FileTime time) {
        try {
            Files.setLastModifiedTime(path, time);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
