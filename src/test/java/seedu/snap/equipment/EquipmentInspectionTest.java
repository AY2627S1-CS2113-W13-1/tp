package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests detached inspection proposals, persistence, and preservation of equipment state. */
class EquipmentInspectionTest {
    private static final String ADD_CAMERA =
            "add i/Camera id/001 type/camera cond/5 sensor/Full frame damage/SHUTTER_FAULT "
                    + "remarks/Handle with care";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void prepareInspectionAssessment_preservesIdentityAndDetails() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment camera = handler.addEquipment(ADD_CAMERA);

        Equipment proposal = handler.prepareInspectionAssessment(camera, true);

        assertEquals(camera.getRecordId(), proposal.getRecordId());
        assertEquals(camera.getItemId(), proposal.getItemId());
        assertEquals(camera.getDetails(), proposal.getDetails());
        assertEquals(camera.getRemarks(), proposal.getRemarks());
        assertTrue(proposal.isServiceable());
        assertFalse(camera.isLoaned());
        assertTrue(camera.isServiceable());
    }

    @Test
    void prepareInspectionAssessment_activeLoan_isRejected() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment camera = handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        camera.setLoanId("001");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> handler.prepareInspectionAssessment(camera, true));

        assertEquals("Equipment with an active loan cannot be inspected.", exception.getMessage());
        assertTrue(camera.isLoaned());
    }

    @Test
    void saveInspectionAssessment_persistsResultBeforeLiveStateChanges() throws Exception {
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(equipmentFile);
        Equipment camera = handler.addEquipment("add i/Camera id/001 type/camera cond/5");
        Equipment proposal = handler.prepareInspectionAssessment(camera, false);

        handler.saveInspectionAssessment(camera, proposal);

        assertTrue(camera.isServiceable());
        camera.applyReturnAssessment(proposal);
        EquipmentHandler reloaded = new EquipmentHandler(equipmentFile);
        reloaded.loadFromStorage();
        assertFalse(reloaded.getEquipmentById("001").isServiceable());
    }

    @Test
    void saveInspectionAssessment_failedSave_preservesLiveStateAndFile() throws Exception {
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler initial = new EquipmentHandler(equipmentFile);
        Equipment camera = initial.addEquipment("add i/Camera id/001 type/camera cond/5");
        Files.delete(equipmentFile);
        Files.createDirectory(equipmentFile);
        Files.writeString(equipmentFile.resolve("keep.txt"), "keep equipment");
        Equipment proposal = initial.prepareInspectionAssessment(camera, false);

        assertThrows(java.io.IOException.class,
                () -> initial.saveInspectionAssessment(camera, proposal));
        assertTrue(camera.isServiceable());
        assertTrue(Files.exists(equipmentFile.resolve("keep.txt")));
        assertEquals("keep equipment", Files.readString(equipmentFile.resolve("keep.txt")));
    }
}
