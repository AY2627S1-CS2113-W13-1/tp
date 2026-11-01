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
        camera = handler.addEquipment("add i/Camera id/E001 type/camera cond/1");
        tripod = handler.addEquipment("add i/Tripod id/E999 type/tripod cond/10");
    }

    @Test
    void updateEquipment_oneField_keepsUnspecifiedDetails() throws EquipmentUpdateUnsuccessful {
        assertSame(camera, handler.updateEquipment("update id/E001 i/Film Camera"));
        assertEquals("Film Camera", camera.getItemName());
        assertEquals("camera", camera.getType());
        assertEquals(1, camera.getCondition());
        handler.updateEquipment("update id/E001 type/CAMERA");
        assertEquals("Film Camera", camera.getItemName());
        assertEquals("camera", camera.getType());
        assertEquals(1, camera.getCondition());
        handler.updateEquipment("update id/E001 cond/10");
        assertEquals("Film Camera", camera.getItemName());
        assertEquals("camera", camera.getType());
        assertEquals(10, camera.getCondition());
    }

    @Test
    void updateEquipment_allFieldsInAnyOrder_preservesIdentityOrderAndOtherItems()
            throws EquipmentUpdateUnsuccessful {
        List<Equipment> view = handler.getEquipmentList();
        Equipment result = handler.updateEquipment("update cond/5 i/Updated Camera type/CAMERA id/E001");
        assertSame(camera, result);
        assertEquals("E001", camera.getItemId());
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
        String[] names = {"A", "A".repeat(100), "Camera 相机", "USB id/card reader", "fluid/head tripod"};
        for (String name : names) {
            handler.updateEquipment("update id/E001 i/" + name);
            assertEquals(name, camera.getItemName());
        }
        handler.updateEquipment("update id/E001 type/camera cond/10");
        assertEquals("camera", camera.getType());
        assertEquals(10, camera.getCondition());
        handler.updateEquipment("update id/E999 cond/1");
        assertEquals(1, tripod.getCondition());
    }

    @Test
    void updateEquipment_invalidCommands_preserveEveryFieldAndInventoryOrder() {
        String[] invalid = {
            null, "", "update", "update id/E001", "update i/Name", "update id/E000 i/Name",
            "update id/01 i/Name", "update id/E1000 i/Name", "update id/+01 i/Name",
            "update id/E555 i/Name", "update id/E001 id/E999 i/Name", "update id/E001 i/A i/B",
            "update id/E001 type/a type/b", "update id/E001 cond/1 cond/2", "update id/E001 status/loaned",
            "update id/E001 i/", "update id/E001 i/\u2003", "update id/E001 type/", "update id/E001 type/\u2003",
            "update id/E001 i/" + "A".repeat(101), "update id/E001 i/Changed cond/0",
            "update id/E001 i/Changed cond/11", "update id/E001 cond/-1", "update id/E001 cond/01",
            "update id/E001 cond/1.0", "update id/E001 cond/2147483648", "update id/E001 cond/1-3",
            "update id/E001 cond/1 extra", "update rubbish id/E001 i/Name", "update unknown/value"
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
                () -> handler.updateEquipment("update id/E001 i/Changed cond/11"));
        handler.updateEquipment("update id/E001 i/Replacement cond/5");
        assertEquals("Replacement", camera.getItemName());
        assertEquals(5, camera.getCondition());
    }

    @Test
    void updateEquipment_ambiguousId_preservesBothRecords() throws Exception {
        Equipment duplicate = new Lens("Duplicate", "E001", 3);
        Field field = EquipmentHandler.class.getDeclaredField("inventory");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Equipment> inventory = (ArrayList<Equipment>) field.get(handler);
        inventory.add(duplicate);
        EquipmentUpdateUnsuccessful error = assertThrows(EquipmentUpdateUnsuccessful.class,
                () -> handler.updateEquipment("update id/E001 cond/5"));
        assertEquals("Item ID E001 is not unique.", error.getMessage());
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
                    () -> handler.updateEquipment("update id/E001 i/Changed cond/5 type/" + type));
            assertEquals("Equipment type cannot be changed. Item ID E001 must remain camera.", error.getMessage());
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
                    () -> handler.updateEquipment("update id/E001 i/Camera id/" + suffix), suffix);
            assertEquals("Camera", camera.getItemName());
            assertEquals(List.of(camera, tripod), handler.getEquipmentList());
        }
    }

    @Test
    void executeInput_updateAndList_showsNewDetailsAndRecoversAfterError() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            manager.executeInput("add i/Camera id/E001 type/camera cond/1");
            output.reset();
            manager.executeInput("update id/E001 i/Bad cond/11");
            manager.executeInput("update id/E001 i/Film Camera cond/5");
            manager.executeInput("list");
            manager.executeInput("delete item/E001");
            String text = output.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("Condition must be a whole number from 1 to 10."));
            assertTrue(text.contains("[OK] Equipment E001 updated."));
            assertTrue(text.contains("Film Camera"));
            assertTrue(text.contains("[OK] Equipment E001 \"Film Camera\" deleted."));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void parse_nonbreakingWhitespaceAfterRepeatedId_rejectsNumericField() {
        for (String space : new String[]{"\u00a0", "\u2007", "\u202f", "\u2003", " "}) {
            String command = "update id/E001 i/Changed id/" + space + "999";
            assertThrows(EquipmentUpdateUnsuccessful.class, () -> handler.updateEquipment(command));
            assertEquals("Camera", camera.getItemName());
            assertEquals("camera", camera.getType());
            assertEquals(1, camera.getCondition());
            assertEquals(List.of(camera, tripod), handler.getEquipmentList());
        }
    }
}
