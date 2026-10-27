package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BackupUnsuccessful;
import seedu.snap.loan.Loan;
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

    @Test
    void restoreBackup_validBackup_replacesLiveRecordsAndReloadsHandlers() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        Loan original = loans.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);
        Path backup = temporaryDirectory.resolve("valid-backup");
        handler.createBackup("backup path/" + backup);
        loans.returnLoan("return loan/" + original.getLoanId()
                + " serviceable/yes damage/NONE remarks/");
        equipment.addEquipment("add i/Lens id/002 type/lens cond/5 zoom/24-70mm");

        Path restored = handler.restoreBackup("restore path/" + backup);

        assertEquals(backup.toAbsolutePath().normalize(), restored);
        assertEquals(1, equipment.getEquipmentList().size());
        assertEquals("Camera", equipment.getEquipmentList().getFirst().getItemName());
        assertTrue(equipment.getEquipmentList().getFirst().isLoaned());
        assertEquals(1, loans.getLoanCount());
        assertFalse(loans.getLoanList().getFirst().isReturned());
        assertEquals("002", loans.getNextLoanId());
    }

    @Test
    void restoreBackup_tamperedOrIncompleteBackup_leavesLiveRecordsUnchanged() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        loans.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);
        Path backup = temporaryDirectory.resolve("tampered-backup");
        handler.createBackup("backup path/" + backup);
        List<Path> liveFiles = List.of(dataDirectory.resolve("equipment.txt"), dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        List<byte[]> liveContents = liveFiles.stream().map(this::readBytes).toList();
        Files.writeString(backup.resolve(BackupHandler.LOAN_FILE_NAME), "not a loan record\n");

        BackupUnsuccessful exception = assertThrows(BackupUnsuccessful.class,
                () -> handler.restoreBackup("restore path/" + backup));

        assertTrue(exception.getMessage().contains("manifest"));
        for (int i = 0; i < liveFiles.size(); i++) {
            assertArrayEquals(liveContents.get(i), readBytes(liveFiles.get(i)));
        }
        assertEquals(1, loans.getLoanCount());
        assertTrue(equipment.getEquipmentById("001").isLoaned());
    }

    @Test
    void restoreBackup_malformedRecordWithMatchingManifest_rejectsBeforeReplacingLiveData() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);
        Path backup = temporaryDirectory.resolve("malformed-backup");
        handler.createBackup("backup path/" + backup);
        Files.writeString(backup.resolve(BackupHandler.EQUIPMENT_FILE_NAME), "not an equipment record\n");
        refreshManifest(backup);

        BackupUnsuccessful exception = assertThrows(BackupUnsuccessful.class,
                () -> handler.restoreBackup("restore path/" + backup));

        assertTrue(exception.getMessage().contains("Invalid equipment record"));
        assertFalse(Files.exists(dataDirectory));
    }

    @Test
    void restoreBackup_duplicateEquipmentWithMatchingManifest_rejectsBeforeReplacingLiveData() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);
        Path backup = temporaryDirectory.resolve("duplicate-backup");
        handler.createBackup("backup path/" + backup);
        String record = Files.readString(backup.resolve(BackupHandler.EQUIPMENT_FILE_NAME));
        Files.writeString(backup.resolve(BackupHandler.EQUIPMENT_FILE_NAME),
                record + System.lineSeparator() + record);
        refreshManifest(backup);

        BackupUnsuccessful exception = assertThrows(BackupUnsuccessful.class,
                () -> handler.restoreBackup("restore path/" + backup));

        assertTrue(exception.getMessage().contains("Duplicate equipment ID"));
        assertEquals(1, equipment.getEquipmentList().size());
        assertFalse(equipment.getEquipmentById("001").isLoaned());
    }

    @Test
    void restoreBackup_inconsistentActiveLoanWithMatchingManifest_rejectsBeforeReplacingLiveData() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);
        Path backup = temporaryDirectory.resolve("inconsistent-backup");
        handler.createBackup("backup path/" + backup);
        Files.writeString(backup.resolve(BackupHandler.LOAN_FILE_NAME),
                "loan loanId/001 id/999 b/111 issued/01-10-26 due/02-10-26 returned/false\n");
        refreshManifest(backup);

        BackupUnsuccessful exception = assertThrows(BackupUnsuccessful.class,
                () -> handler.restoreBackup("restore path/" + backup));

        assertTrue(exception.getMessage().contains("Invalid active loan"));
        assertFalse(Files.exists(dataDirectory.resolve(BackupHandler.LOAN_FILE_NAME)));
        assertFalse(equipment.getEquipmentById("001").isLoaned());
    }

    @Test
    void restoreBackup_invalidSource_reportsErrorWithoutChangingLiveData() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);
        Path missing = temporaryDirectory.resolve("missing-backup");

        BackupUnsuccessful exception = assertThrows(BackupUnsuccessful.class,
                () -> handler.restoreBackup("restore path/" + missing));

        assertTrue(exception.getMessage().contains("not an existing directory"));
        assertFalse(Files.exists(dataDirectory));
    }

    @Test
    void restoreBackup_failureAfterFileReplacement_rollsBackFilesAndInMemoryState() throws Exception {
        Path dataDirectory = temporaryDirectory.resolve("data");
        EquipmentHandler equipment = new EquipmentHandler(dataDirectory.resolve("equipment.txt"));
        equipment.addEquipment("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        LoanHandler loans = new LoanHandler(equipment, dataDirectory.resolve("loans.txt"),
                dataDirectory.resolve("loan-sequence.txt"));
        Loan original = loans.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        BackupHandler handler = new BackupHandler(dataDirectory, equipment, loans);
        Path backup = temporaryDirectory.resolve("rollback-backup");
        handler.createBackup("backup path/" + backup);

        loans.returnLoan("return loan/" + original.getLoanId() + " serviceable/yes damage/NONE remarks/");
        equipment.addEquipment("add i/Lens id/002 type/lens cond/5 zoom/24-70mm");
        List<Path> liveFiles = List.of(dataDirectory.resolve(BackupHandler.EQUIPMENT_FILE_NAME),
                dataDirectory.resolve(BackupHandler.LOAN_FILE_NAME),
                dataDirectory.resolve(BackupHandler.SEQUENCE_FILE_NAME));
        List<byte[]> liveContents = liveFiles.stream().map(this::readBytes).toList();

        BackupHandler failingHandler = new BackupHandler(dataDirectory, equipment, loans, null,
                () -> {
                    throw new IOException("forced restore failure");
                });

        BackupUnsuccessful exception = assertThrows(BackupUnsuccessful.class,
                () -> failingHandler.restoreBackup("restore path/" + backup));

        assertTrue(exception.getMessage().contains("forced restore failure"));
        for (int i = 0; i < liveFiles.size(); i++) {
            assertArrayEquals(liveContents.get(i), readBytes(liveFiles.get(i)));
        }
        assertEquals(2, equipment.getEquipmentList().size());
        assertTrue(loans.getLoanList().getFirst().isReturned());
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

    private void refreshManifest(Path backup) throws Exception {
        String manifest = String.join(System.lineSeparator(),
                "backupVersion/1",
                "equipmentSha256/" + checksum(backup.resolve(BackupHandler.EQUIPMENT_FILE_NAME)),
                "loansSha256/" + checksum(backup.resolve(BackupHandler.LOAN_FILE_NAME)),
                "loanSequenceSha256/" + checksum(backup.resolve(BackupHandler.SEQUENCE_FILE_NAME)))
                + System.lineSeparator();
        Files.writeString(backup.resolve(BackupHandler.MANIFEST_FILE_NAME), manifest);
    }

    private String checksum(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
