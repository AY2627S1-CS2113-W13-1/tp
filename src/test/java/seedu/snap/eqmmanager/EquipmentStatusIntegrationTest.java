package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.equipment.EquipmentStatus;

/** Exercises status commands through the application and verifies cross-feature availability rules. */
class EquipmentStatusIntegrationTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-10-27T10:00:00Z"), ZoneOffset.UTC);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
    }

    @Test
    void statusCommands_markDisplayAndPersistPhysicalState() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/5 sensor/Full frame");
        output.reset();

        manager.executeInput("mark id/001 status/missing");
        manager.executeInput("status-history id/001");
        manager.executeInput("list");

        String printed = printed();
        assertTrue(printed.contains("marked missing successfully"));
        assertTrue(printed.contains("Current status: missing"));
        assertTrue(printed.contains("MISSING"));
        assertTrue(printed.contains("Missing"));

        EqmManager restarted = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        output.reset();
        restarted.executeInput("loan id/001 b/002 due/28-10-26");
        assertTrue(printed().contains("marked missing and cannot be loaned"));
        EquipmentHandler inventory = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        inventory.loadFromStorage();
        assertEquals(EquipmentStatus.MISSING, inventory.getEquipmentList().getFirst().getStatus());
        assertTrue(inventory.getEquipmentList().getFirst().getStatusHistory().size() == 1);
    }

    @Test
    void statusCommands_activeLoan_rejectsMarkingWithoutChangingFiles() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("loan id/001 b/002 due/28-10-26");
        String equipmentBefore = Files.readString(temporaryDirectory.resolve("equipment.txt"));
        output.reset();

        manager.executeInput("mark id/001 status/lost");

        assertTrue(printed().contains("active loan"));
        assertEquals(equipmentBefore, Files.readString(temporaryDirectory.resolve("equipment.txt")));
        assertFalse(Files.readString(temporaryDirectory.resolve("equipment.txt")).contains("status/lost"));
    }

    @Test
    void statusHistory_reusedItemId_startsWithNewPhysicalItem() throws Exception {
        EqmManager manager = new EqmManager(temporaryDirectory, FIXED_CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/5");
        manager.executeInput("mark id/001 status/missing");
        manager.executeInput("delete item/001");
        manager.executeInput("add i/Replacement id/001 type/camera cond/5");
        output.reset();

        manager.executeInput("status-history id/001");

        assertTrue(printed().contains("No status changes recorded."));
        assertFalse(printed().contains("MISSING"));
        EquipmentHandler inventory = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"));
        inventory.loadFromStorage();
        assertEquals(EquipmentStatus.PRESENT, inventory.getEquipmentList().getFirst().getStatus());
    }

    /** Returns captured output without depending on table spacing. */
    private String printed() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
