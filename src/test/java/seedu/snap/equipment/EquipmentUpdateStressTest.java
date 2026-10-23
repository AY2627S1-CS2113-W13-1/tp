package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;

/** Exercises update parsing and verifies thousands of commands against independent expected values. */
class EquipmentUpdateStressTest {
    @TempDir
    private Path temporaryDataDirectory;

    @Test
    void executeInput_unicodeWhitespace_routesToUpdateAndKeepsInventoryUsable() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            manager.executeInput("add i/Camera id/001 type/camera cond/1");
            for (String space : new String[]{" ", "\t", "\u2003", "\u00a0", "\u2007", "\u202f"}) {
                output.reset();
                manager.executeInput(space + "update" + space + "id/001" + space + "cond/5" + space);
                assertTrue(output.toString(StandardCharsets.UTF_8).contains("updated successfully."));
            }
            output.reset();
            manager.executeInput("delete item/001");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("deleted successfully."));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void updateEquipment_unicodeSeparators_acceptsEachWhitespaceCharacter() throws Exception {
        for (String separator : new String[]{" ", "\t", "\u2003", "\u00a0", "\u2007", "\u202f"}) {
            EquipmentHandler handler = new EquipmentHandler();
            Equipment item = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
            handler.updateEquipment("update" + separator + "id/001" + separator + "i/Film Camera");
            assertEquals("Film Camera", item.getItemName());
        }
    }

    @Test
    void updateEquipment_visuallyBlankValues_rejectsWithoutChangingRecord() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment item = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        for (String blank : new String[]{"\u00a0", "\u2007", "\u202f", " \u00a0\t\u2003"}) {
            for (String field : new String[]{"i/", "type/"}) {
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        () -> handler.updateEquipment("update id/001 " + field + blank));
                assertEquals("Camera", item.getItemName());
                assertEquals("camera", item.getType());
            }
        }
    }

    @Test
    void updateEquipment_everyValidId_updatesOnlyTheTarget() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        for (int id = 1; id <= 999; id++) {
            String formatted = String.format(java.util.Locale.ROOT, "%03d", id);
            handler.addEquipment("add i/Original id/" + formatted + " type/camera cond/1");
        }
        for (int id = 999; id >= 1; id--) {
            String formatted = String.format(java.util.Locale.ROOT, "%03d", id);
            Equipment target = handler.getEquipmentList().get(id - 1);
            handler.updateEquipment("update id/" + formatted + " i/Changed " + formatted + " cond/10");
            assertSame(target, handler.getEquipmentList().get(id - 1));
            assertEquals("Changed " + formatted, target.getItemName());
            assertEquals(formatted, target.getItemId());
            assertEquals(10, target.getCondition());
            if (id > 1) {
                assertEquals("Original", handler.getEquipmentList().get(id - 2).getItemName());
                assertEquals(1, handler.getEquipmentList().get(id - 2).getCondition());
            }
        }
        assertEquals(999, handler.getEquipmentList().size());
    }

    @Test
    void updateEquipment_seededMalformedTokens_neverPartiallyAppliesRejectedInput() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment item = handler.addEquipment("add i/Camera id/001 type/camera cond/1");
        String[] tokens = {"id/001", "id/000", "id/999", "i/New", "type/lens", "cond/10", "cond/11",
            "cond/2147483648", "status/available", "i/", "type/", "\u00a0", "x", "id/card"};
        Random random = new Random(2627);
        for (int trial = 0; trial < 4_000; trial++) {
            String name = item.getItemName();
            String type = item.getType();
            int condition = item.getCondition();
            StringBuilder input = new StringBuilder("update ");
            for (int token = 0; token < 1 + random.nextInt(10); token++) {
                input.append(tokens[random.nextInt(tokens.length)]).append(' ');
            }
            try {
                handler.updateEquipment(input.toString());
            } catch (EquipmentUpdateUnsuccessful expected) {
                assertEquals(name, item.getItemName());
                assertEquals(type, item.getType());
                assertEquals(condition, item.getCondition());
            }
            assertEquals("001", item.getItemId());
            assertEquals(List.of(item), handler.getEquipmentList());
        }
    }

    @Test
    void updateEquipment_seededMixedOperations_preservesModelAndAllOtherRecords() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment first = handler.addEquipment("add i/Original id/001 type/camera cond/1");
        Equipment other = handler.addEquipment("add i/Other id/999 type/lens cond/10");
        String expectedName = "Original";
        String expectedType = "camera";
        int expectedCondition = 1;
        Random random = new Random(2113);
        for (int step = 0; step < 5_000; step++) {
            switch (random.nextInt(5)) {
            case 0:
                expectedName = "Camera " + step;
                handler.updateEquipment("update id/001 i/" + expectedName);
                break;
            case 1:
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        () -> handler.updateEquipment("update i/Rejected type/lens id/001"));
                break;
            case 2:
                expectedCondition = random.nextInt(10) + 1;
                handler.updateEquipment("update id/001 cond/" + expectedCondition);
                break;
            case 3:
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        () -> handler.updateEquipment("update id/001 i/Rejected cond/2147483648"));
                break;
            case 4:
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        () -> handler.updateEquipment("update id/555 i/Missing"));
                break;
            default:
                throw new AssertionError("Unexpected test operation");
            }
            assertEquals(expectedName, first.getItemName(), "step " + step);
            assertEquals(expectedType, first.getType(), "step " + step);
            assertEquals(expectedCondition, first.getCondition(), "step " + step);
            assertEquals(List.of(first, other), handler.getEquipmentList());
            assertSame(first, handler.getEquipmentList().getFirst());
            assertEquals("001", first.getItemId());
            assertEquals("Other", other.getItemName());
            assertEquals("lens", other.getType());
            assertEquals(10, other.getCondition());
        }
    }
}
