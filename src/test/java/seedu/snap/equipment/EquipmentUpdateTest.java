package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;

/** Tests partial updates, input boundaries, rejected commands, and preservation of item identity. */
class EquipmentUpdateTest {
    @TempDir
    private Path temporaryDataDirectory;

    private EquipmentHandler handler;
    private Equipment camera;
    private Equipment tripod;

    @BeforeEach
    void setUp() throws EquipmentAdditionUnsuccessful {
        handler = new EquipmentHandler();
        // @@author yarnieee
        camera = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        tripod = handler.addEquipment("eqm-add name/Tripod e/E999 type/tripod cond/10");
        // @@author
    }

    @Test
    void updateEquipment_oneField_keepsUnspecifiedDetails() throws EquipmentUpdateUnsuccessful {
        // @@author yarnieee
        assertSame(camera, handler.updateEquipment("eqm-update e/E001 name/Film Camera"));
        // @@author
        assertEquals("Film Camera", camera.getItemName());
        assertEquals("camera", camera.getType());
        assertEquals(1, camera.getCondition());
        // @@author yarnieee
        handler.updateEquipment("eqm-update e/E001 type/CAMERA");
        // @@author
        assertEquals("Film Camera", camera.getItemName());
        assertEquals("camera", camera.getType());
        assertEquals(1, camera.getCondition());
        // @@author yarnieee
        handler.updateEquipment("eqm-update e/E001 cond/10");
        // @@author
        assertEquals("Film Camera", camera.getItemName());
        assertEquals("camera", camera.getType());
        assertEquals(10, camera.getCondition());
    }

    @Test
    void updateEquipment_allFieldsInAnyOrder_preservesIdentityOrderAndOtherItems()
            throws EquipmentUpdateUnsuccessful {
        List<Equipment> view = handler.getEquipmentList();
        // @@author yarnieee
        Equipment result = handler.updateEquipment("eqm-update cond/5 name/Updated Camera type/CAMERA e/E001");
        // @@author
        assertSame(camera, result);
        // @@author yarnieee
        assertEquals("E001", camera.getItemId());
        // @@author
        assertEquals("Updated Camera", camera.getItemName());
        assertEquals("camera", camera.getType());
        assertEquals(5, camera.getCondition());
        assertEquals(List.of(camera, tripod), view);
        assertSame(camera, view.getFirst());
        assertSame(tripod, view.getLast());
        assertEquals("Tripod", tripod.getItemName());
        assertEquals(10, tripod.getCondition());
    }

    @Test
    void updateEquipment_nameBoundariesUnicodeAndPrefixText_acceptsValidValues()
            throws EquipmentUpdateUnsuccessful {
        // @@author yarnieee
        String[] names = {"A", "A".repeat(100), "Camera 相机", "USB e/card reader", "flue/head tripod"};
        for (String name : names) {
            handler.updateEquipment("eqm-update e/E001 name/" + name);
            // @@author
            assertEquals(name, camera.getItemName());
        }
        // @@author yarnieee
        handler.updateEquipment("eqm-update e/E001 type/camera cond/10");
        // @@author
        assertEquals("camera", camera.getType());
        assertEquals(10, camera.getCondition());
        // @@author yarnieee
        handler.updateEquipment("eqm-update e/E999 cond/1");
        // @@author
        assertEquals(1, tripod.getCondition());
    }

    @Test
    void updateEquipment_invalidCommands_preserveEveryFieldAndInventoryOrder() {
        String[] invalid = {
            // @@author yarnieee
            null, "", "eqm-update", "eqm-update e/E001", "eqm-update name/Name", "eqm-update e/E000 name/Name",
            "eqm-update e/01 name/Name", "eqm-update e/E1000 name/Name", "eqm-update e/+01 name/Name",
            "eqm-update e/E555 name/Name", "eqm-update e/E001 e/E999 name/Name", "eqm-update e/E001 name/A name/B",
            "eqm-update e/E001 type/a type/b", "eqm-update e/E001 cond/1 cond/2", "eqm-update e/E001 status/loaned",
            "eqm-update e/E001 name/", "eqm-update e/E001 name/\u2003", "eqm-update e/E001 type/",
            "eqm-update e/E001 type/\u2003",
            "eqm-update e/E001 name/" + "A".repeat(101), "eqm-update e/E001 name/Changed cond/0",
            "eqm-update e/E001 name/Changed cond/11", "eqm-update e/E001 cond/-1", "eqm-update e/E001 cond/01",
            "eqm-update e/E001 cond/1.0", "eqm-update e/E001 cond/2147483648", "eqm-update e/E001 cond/1-3",
            "eqm-update e/E001 cond/1 extra", "update rubbish e/E001 name/Name", "update unknown/value"
            // @@author
        };
        for (String input : invalid) {
            assertThrows(EquipmentUpdateUnsuccessful.class, () -> handler.updateEquipment(input), input);
            assertSame(camera, handler.getEquipmentList().getFirst());
            assertEquals(List.of(camera, tripod), handler.getEquipmentList(), input);
            assertEquals("Camera", camera.getItemName(), input);
            assertEquals("camera", camera.getType(), input);
            assertEquals(1, camera.getCondition(), input);
            assertEquals("Tripod", tripod.getItemName(), input);
            assertEquals(10, tripod.getCondition(), input);
        }
    }

    @Test
    void updateEquipment_validAfterRejection_recovers() throws EquipmentUpdateUnsuccessful {
        assertThrows(EquipmentUpdateUnsuccessful.class,
                // @@author yarnieee
                () -> handler.updateEquipment("eqm-update e/E001 name/Changed cond/11"));
        handler.updateEquipment("eqm-update e/E001 name/Replacement cond/5");
        // @@author
        assertEquals("Replacement", camera.getItemName());
        assertEquals(5, camera.getCondition());
    }

    @Test
    void updateEquipment_ambiguousId_preservesBothRecords() throws Exception {
        // @@author yarnieee
        Equipment duplicate = new Lens("Duplicate", "E001", 3);
        // @@author
        Field field = EquipmentHandler.class.getDeclaredField("inventory");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Equipment> inventory = (ArrayList<Equipment>) field.get(handler);
        inventory.add(duplicate);
        EquipmentUpdateUnsuccessful error = assertThrows(EquipmentUpdateUnsuccessful.class,
                // @@author yarnieee
                () -> handler.updateEquipment("eqm-update e/E001 cond/5"));
        assertEquals("Item ID E001 is not unique.", error.getMessage());
        // @@author
        assertEquals(1, camera.getCondition());
        assertEquals(3, duplicate.getCondition());
        assertEquals(List.of(camera, tripod, duplicate), handler.getEquipmentList());
    }

    @Test
    void updateDetails_invalidNames_preserveAllOriginalDetails() {
        for (String name : new String[]{null, "", "\u2003", "A".repeat(101)}) {
            assertThrows(AssertionError.class, () -> camera.updateDetails(name, 5));
            assertEquals("Camera", camera.getItemName());
            assertEquals("camera", camera.getType());
            assertEquals(1, camera.getCondition());
        }
    }

    @Test
    void updateEquipment_differentTypes_preserveAllOriginalDetails() {
        for (String type : new String[]{"lens", "battery", "tripod", "camera kit"}) {
            EquipmentUpdateUnsuccessful error = assertThrows(EquipmentUpdateUnsuccessful.class,
                    // @@author yarnieee
                    () -> handler.updateEquipment("eqm-update e/E001 name/Changed cond/5 type/" + type));
            assertEquals("Equipment type cannot be changed. Item ID E001 must remain camera.", error.getMessage());
            // @@author
            assertEquals("Camera", camera.getItemName());
            assertEquals("camera", camera.getType());
            assertEquals(1, camera.getCondition());
        }
    }

    @Test
    void updateDetails_conditionOutsideEitherBoundary_preservesAllOriginalDetails() {
        for (int condition : new int[]{Integer.MIN_VALUE, 0, 11, Integer.MAX_VALUE}) {
            assertThrows(AssertionError.class, () -> camera.updateDetails("Changed", condition));
            assertEquals("Camera", camera.getItemName());
            assertEquals("camera", camera.getType());
            assertEquals(1, camera.getCondition());
        }
    }

    @Test
    void updateEquipment_reservedIdSyntaxInsideName_cannotHideDuplicateOrEmptyId() {
        for (String suffix : new String[]{"", " ", "001", "+01", "-01", " 999"}) {
            assertThrows(EquipmentUpdateUnsuccessful.class,
                    // @@author yarnieee
                    () -> handler.updateEquipment("eqm-update e/E001 name/Camera e/" + suffix), suffix);
            // @@author
            assertEquals("Camera", camera.getItemName());
            assertEquals(List.of(camera, tripod), handler.getEquipmentList());
        }
    }

    // @@author yarnieee
    @Test
    void updateEquipment_idAfterNameField_updatesTheRequestedItem() throws EquipmentUpdateUnsuccessful {
        handler.updateEquipment("eqm-update name/Film Camera e/E001");

        // @@author
        assertEquals("Film Camera", camera.getItemName());
        // @@author yarnieee
        assertEquals("E001", camera.getItemId());
    }

    // @@author
    @Test
    void executeInput_updateAndList_showsNewDetailsAndRecoversAfterError() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            // @@author yarnieee
            manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
            // @@author
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-update e/E001 name/Bad cond/11");
            manager.executeInput("eqm-update e/E001 name/Film Camera cond/5");
            manager.executeInput("eqm-list");
            manager.executeInput("eqm-delete e/E001");
            // @@author
            String text = output.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("Condition must be a whole number from 1 to 10."));
            // @@author yarnieee
            assertTrue(text.contains("[OK] Equipment E001 updated."));
            // @@author
            assertTrue(text.contains("Film Camera"));
            // @@author yarnieee
            assertTrue(text.contains("[OK] Equipment E001 \"Film Camera\" deleted."));
            // @@author
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void parse_nonbreakingWhitespaceAfterRepeatedId_rejectsNumericField() {
        for (String space : new String[]{"\u00a0", "\u2007", "\u202f", "\u2003", " "}) {
            // @@author yarnieee
            String command = "eqm-update e/E001 name/Changed e/" + space + "999";
            // @@author
            assertThrows(EquipmentUpdateUnsuccessful.class, () -> handler.updateEquipment(command));
            assertEquals("Camera", camera.getItemName());
            assertEquals("camera", camera.getType());
            assertEquals(1, camera.getCondition());
            assertEquals(List.of(camera, tripod), handler.getEquipmentList());
        }
    }
}
