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
            // @@author yarnieee
            manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
            // @@author
            for (String space : new String[]{" ", "\t", "\u2003", "\u00a0", "\u2007", "\u202f"}) {
                output.reset();
                // @@author yarnieee
                manager.executeInput(space + "eqm-update" + space + "e/E001" + space + "cond/5" + space);
                assertTrue(output.toString(StandardCharsets.UTF_8).contains("[OK] Equipment E001 updated."));
                // @@author
            }
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-delete e/E001");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("[OK] Equipment E001 \"Camera\" deleted."));
            // @@author
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void updateEquipment_unicodeSeparators_acceptsEachWhitespaceCharacter() throws Exception {
        for (String separator : new String[]{" ", "\t", "\u2003", "\u00a0", "\u2007", "\u202f"}) {
            EquipmentHandler handler = new EquipmentHandler();
            // @@author yarnieee
            Equipment item = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
            handler.updateEquipment("eqm-update" + separator + "e/E001" + separator + "name/Film Camera");
            // @@author
            assertEquals("Film Camera", item.getItemName());
        }
    }

    @Test
    void updateEquipment_visuallyBlankValues_rejectsWithoutChangingRecord() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment item = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        // @@author
        for (String blank : new String[]{"\u00a0", "\u2007", "\u202f", " \u00a0\t\u2003"}) {
            // @@author yarnieee
            for (String field : new String[]{"name/", "type/"}) {
                // @@author
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        // @@author yarnieee
                        () -> handler.updateEquipment("eqm-update e/E001 " + field + blank));
                // @@author
                assertEquals("Camera", item.getItemName());
                assertEquals("camera", item.getType());
            }
        }
    }

    @Test
    void updateEquipment_everyValidId_updatesOnlyTheTarget() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        for (int id = 1; id <= 999; id++) {
            // @@author yarnieee
            String formatted = String.format(java.util.Locale.ROOT, "E%03d", id);
            handler.addEquipment("eqm-add name/Original e/" + formatted + " type/camera cond/1");
            // @@author
        }
        for (int id = 999; id >= 1; id--) {
            // @@author yarnieee
            String formatted = String.format(java.util.Locale.ROOT, "E%03d", id);
            // @@author
            Equipment target = handler.getEquipmentList().get(id - 1);
            // @@author yarnieee
            handler.updateEquipment("eqm-update e/" + formatted + " name/Changed " + formatted + " cond/10");
            // @@author
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
        // @@author yarnieee
        Equipment item = handler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1");
        String[] tokens = {"e/E001", "e/E000", "e/E999", "name/New", "type/lens", "cond/10", "cond/11",
            "cond/2147483648", "status/available", "name/", "type/", "\u00a0", "x", "e/card"};
        // @@author
        Random random = new Random(2627);
        for (int trial = 0; trial < 4_000; trial++) {
            String name = item.getItemName();
            String type = item.getType();
            int condition = item.getCondition();
            // @@author yarnieee
            StringBuilder input = new StringBuilder("eqm-update ");
            // @@author
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
            // @@author yarnieee
            assertEquals("E001", item.getItemId());
            // @@author
            assertEquals(List.of(item), handler.getEquipmentList());
        }
    }

    @Test
    void updateEquipment_seededMixedOperations_preservesModelAndAllOtherRecords() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        // @@author yarnieee
        Equipment first = handler.addEquipment("eqm-add name/Original e/E001 type/camera cond/1");
        Equipment other = handler.addEquipment("eqm-add name/Other e/E999 type/lens cond/10");
        // @@author
        String expectedName = "Original";
        String expectedType = "camera";
        int expectedCondition = 1;
        Random random = new Random(2113);
        for (int step = 0; step < 5_000; step++) {
            switch (random.nextInt(5)) {
            case 0:
                expectedName = "Camera " + step;
                // @@author yarnieee
                handler.updateEquipment("eqm-update e/E001 name/" + expectedName);
                // @@author
                break;
            case 1:
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        // @@author yarnieee
                        () -> handler.updateEquipment("eqm-update name/Rejected type/lens e/E001"));
                // @@author
                break;
            case 2:
                expectedCondition = random.nextInt(10) + 1;
                // @@author yarnieee
                handler.updateEquipment("eqm-update e/E001 cond/" + expectedCondition);
                // @@author
                break;
            case 3:
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        // @@author yarnieee
                        () -> handler.updateEquipment("eqm-update e/E001 name/Rejected cond/2147483648"));
                // @@author
                break;
            case 4:
                assertThrows(EquipmentUpdateUnsuccessful.class,
                        // @@author yarnieee
                        () -> handler.updateEquipment("eqm-update e/E555 name/Missing"));
                // @@author
                break;
            default:
                throw new AssertionError("Unexpected test operation");
            }
            assertEquals(expectedName, first.getItemName(), "step " + step);
            assertEquals(expectedType, first.getType(), "step " + step);
            assertEquals(expectedCondition, first.getCondition(), "step " + step);
            assertEquals(List.of(first, other), handler.getEquipmentList());
            assertSame(first, handler.getEquipmentList().getFirst());
            // @@author yarnieee
            assertEquals("E001", first.getItemId());
            // @@author
            assertEquals("Other", other.getItemName());
            assertEquals("lens", other.getType());
            assertEquals(10, other.getCondition());
        }
    }
}
