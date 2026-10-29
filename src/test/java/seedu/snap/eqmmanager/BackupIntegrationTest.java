package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises backup creation through the public application command interface. */
class BackupIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        manager = new EqmManager(temporaryDirectory.resolve("data"));
        manager.executeInput("add i/Camera id/001 type/camera cond/4 sensor/APS-C");
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        output.reset();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void backupCommand_validDestination_createsCompleteBackup() throws Exception {
        Path backupDirectory = temporaryDirectory.resolve("backup copy");
        Path dataDirectory = temporaryDirectory.resolve("data");
        String equipmentBefore = Files.readString(dataDirectory.resolve("equipment.txt"));
        String loansBefore = Files.readString(dataDirectory.resolve("loans.txt"));
        String sequenceBefore = Files.readString(dataDirectory.resolve("loan-sequence.txt"));

        manager.executeInput("backup path/" + backupDirectory);

        assertTrue(printed().contains("[OK] Backup created successfully"));
        assertEquals(equipmentBefore, Files.readString(backupDirectory.resolve("equipment.txt")));
        assertEquals(loansBefore, Files.readString(backupDirectory.resolve("loans.txt")));
        assertEquals(sequenceBefore, Files.readString(backupDirectory.resolve("loan-sequence.txt")));
        assertEquals(equipmentBefore, Files.readString(dataDirectory.resolve("equipment.txt")));
        assertEquals(loansBefore, Files.readString(dataDirectory.resolve("loans.txt")));
        assertEquals(sequenceBefore, Files.readString(dataDirectory.resolve("loan-sequence.txt")));
    }

    @Test
    void backupCommand_existingDestination_reportsFailureAndPreservesBackup() throws Exception {
        Path backupDirectory = temporaryDirectory.resolve("existing backup");
        Files.createDirectories(backupDirectory);
        Path marker = backupDirectory.resolve("marker.txt");
        Files.writeString(marker, "original backup");

        manager.executeInput("backup path/" + backupDirectory);

        assertTrue(printed().contains("already exists"));
        assertTrue(printed().startsWith("[ERROR]"));
        assertEquals("original backup", Files.readString(marker));
        assertFalse(Files.exists(backupDirectory.resolve("backup-manifest.txt")));
    }

    @Test
    void backupCommand_invalidInput_reportsFormatAndKeepsApplicationUsable() {
        manager.executeInput("backup");

        assertTrue(printed().contains("Invalid format"));
        output.reset();
        manager.executeInput("list borrower/111");
        assertTrue(printed().contains("Equipment currently held by borrower ID 111:"));
        assertTrue(printed().contains("001"));
        assertTrue(printed().contains("1 loan."));
    }

    /** Returns captured command output without relying on table spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
