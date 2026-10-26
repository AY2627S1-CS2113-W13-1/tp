package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.EquipmentStatusUnsuccessful;

/** Tests marking and querying status history before application-level command dispatch. */
class EquipmentStatusHandlerTest {
    private static final Instant FIXED_TIME = Instant.parse("2026-10-27T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void markStatus_validTransitions_appendChronologicalHistory() throws Exception {
        EquipmentHandler handler = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"), FIXED_CLOCK);
        handler.addEquipment("add i/Camera id/001 type/camera cond/5");

        Equipment markedMissing = handler.markStatus("mark id/001 status/missing");
        Equipment markedLost = handler.markStatus("mark id/001 status/lost");
        List<EquipmentStatusChange> history = handler.getStatusHistory("status-history id/001");

        assertEquals(markedMissing, markedLost);
        assertEquals(EquipmentStatus.LOST, markedLost.getStatus());
        assertEquals(List.of(EquipmentStatus.MISSING, EquipmentStatus.LOST),
                history.stream().map(EquipmentStatusChange::status).toList());
        assertTrue(history.getFirst().changedAt().isBefore(history.getLast().changedAt()));
        assertFalse(markedLost.isAvailableForLoan());
    }

    @Test
    void markStatus_invalidOrRepeatedInput_preservesEquipmentAndFile() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file, FIXED_CLOCK);
        Equipment equipment = handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        String saved = Files.readString(file);

        for (String command : new String[]{"mark", "mark id/000 status/missing", "mark id/999 status/lost",
            "mark id/001 status/present", "mark id/001 status/missing extra"}) {
            assertThrows(EquipmentStatusUnsuccessful.class, () -> handler.markStatus(command));
            assertEquals(EquipmentStatus.PRESENT, equipment.getStatus());
            assertTrue(equipment.getStatusHistory().isEmpty());
            assertEquals(saved, Files.readString(file));
        }
    }

    @Test
    void statusHistory_invalidQueryAndReadOnlyQuery_preserveSavedFile() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file, FIXED_CLOCK);
        handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        handler.markStatus("mark id/001 status/missing");
        String saved = Files.readString(file);

        assertThrows(EquipmentStatusUnsuccessful.class,
                () -> handler.getStatusHistory("status-history id/999"));
        assertThrows(EquipmentStatusUnsuccessful.class,
                () -> handler.getStatusHistory("status-history id/01"));
        assertEquals(saved, Files.readString(file));
    }

    @Test
    void normalUpdate_preservesStatusAndHistory() throws Exception {
        EquipmentHandler handler = new EquipmentHandler(temporaryDirectory.resolve("equipment.txt"), FIXED_CLOCK);
        handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        handler.markStatus("mark id/001 status/missing");

        handler.updateEquipment("update id/001 i/Workshop camera cond/6");

        Equipment updated = handler.getEquipmentList().getFirst();
        assertEquals("Workshop camera", updated.getItemName());
        assertEquals(6, updated.getCondition());
        assertEquals(EquipmentStatus.MISSING, updated.getStatus());
        assertEquals(1, updated.getStatusHistory().size());
    }

    @Test
    void markStatus_activeLoan_isRejectedWithoutChangingStatus() throws Exception {
        EquipmentHandler handler = new EquipmentHandler(null, FIXED_CLOCK);
        Equipment equipment = handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        equipment.setLoanId("001");

        EquipmentStatusUnsuccessful error = assertThrows(EquipmentStatusUnsuccessful.class,
                () -> handler.markStatus("mark id/001 status/missing"));

        assertTrue(error.getMessage().contains("active loan"));
        assertEquals(EquipmentStatus.PRESENT, equipment.getStatus());
        assertTrue(equipment.getStatusHistory().isEmpty());
    }

    @Test
    void markStatus_saveFailure_preservesLiveStateAndAllowsRetry() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file, FIXED_CLOCK);
        Equipment equipment = handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        Files.delete(file);
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        EquipmentStatusUnsuccessful error = assertThrows(EquipmentStatusUnsuccessful.class,
                () -> handler.markStatus("mark id/001 status/missing"));

        assertTrue(error.getMessage().startsWith("Could not save equipment status change:"));
        assertEquals(EquipmentStatus.PRESENT, equipment.getStatus());
        assertTrue(equipment.getStatusHistory().isEmpty());
        assertEquals("keep", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(file);
        handler.markStatus("mark id/001 status/missing");
        assertEquals(EquipmentStatus.MISSING, equipment.getStatus());
    }
}
