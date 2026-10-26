package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests persistent equipment states without involving command parsing or file storage. */
class EquipmentStatusTest {
    @Test
    void newEquipment_hasPresentStatusAndNoHistory() {
        Equipment camera = new Camera("Camera", "001", 5);

        assertEquals(EquipmentStatus.PRESENT, camera.getStatus());
        assertTrue(camera.getStatusHistory().isEmpty());
        assertEquals("available", camera.getStatusLabel());
        assertTrue(camera.isAvailableForLoan());
        assertFalse(camera.isMissingOrLost());
    }

    @Test
    void restoredStatusState_preservesChronologicalImmutableHistory() {
        Equipment camera = new Camera("Camera", "001", 5);
        List<EquipmentStatusChange> history = List.of(
                new EquipmentStatusChange(EquipmentStatus.MISSING, Instant.parse("2026-10-27T10:00:00Z")),
                new EquipmentStatusChange(EquipmentStatus.LOST, Instant.parse("2026-10-27T11:00:00Z")));

        camera.restoreStatusState(EquipmentStatus.LOST, history);

        assertEquals(EquipmentStatus.LOST, camera.getStatus());
        assertEquals(history, camera.getStatusHistory());
        assertEquals("lost", camera.getStatusLabel());
        assertFalse(camera.isAvailableForLoan());
        assertThrows(UnsupportedOperationException.class,
                () -> camera.getStatusHistory().add(history.getFirst()));
    }

    @Test
    void restoredStatusState_rejectsInvalidHistory() {
        Equipment camera = new Camera("Camera", "001", 5);
        EquipmentStatusChange first = new EquipmentStatusChange(
                EquipmentStatus.MISSING, Instant.parse("2026-10-27T10:00:00Z"));
        EquipmentStatusChange second = new EquipmentStatusChange(
                EquipmentStatus.LOST, Instant.parse("2026-10-27T09:00:00Z"));

        assertThrows(IllegalArgumentException.class,
                () -> camera.restoreStatusState(EquipmentStatus.LOST, List.of(first, second)));
        assertEquals(EquipmentStatus.PRESENT, camera.getStatus());
        assertTrue(camera.getStatusHistory().isEmpty());
    }

    @Test
    void statusValueParsing_isCaseInsensitiveAndRejectsUnknownValues() {
        assertEquals(EquipmentStatus.MISSING, EquipmentStatus.fromValue(" Missing "));
        assertEquals(EquipmentStatus.LOST, EquipmentStatus.fromValue("LOST"));
        assertThrows(IllegalArgumentException.class, () -> EquipmentStatus.fromValue("available"));
    }
}
