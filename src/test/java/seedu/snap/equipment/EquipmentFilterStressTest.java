package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.exceptions.EquipmentFilteringUnsuccessful;

/** Checks filter grammar, all field orders, large inputs, and a seeded independent matching model. */
class EquipmentFilterStressTest {
    @TempDir
    private Path temporaryDataDirectory;

    @Test
    void parse_unicodeSeparatorsAndBlankValues_handlesEachExplicitCase() throws Exception {
        // @@author yarnieee
        Equipment camera = new Camera("Camera", "E001", 1);
        // @@author
        for (String space : new String[]{" ", "\t", "\u2003", "\u00a0", "\u2007", "\u202f"}) {
            // @@author yarnieee
            EquipmentFilter filter = EquipmentFilter.parse("eqm-filter" + space + "e/E001" + space + "cond/1-3");
            // @@author
            assertTrue(filter.matches(camera, false));
        }
        for (String blank : new String[]{"\u00a0", "\u2007", "\u202f", "\t\u00a0\u2003"}) {
            // @@author yarnieee
            for (String field : new String[]{"name/", "type/"}) {
                // @@author
                assertThrows(EquipmentFilteringUnsuccessful.class,
                        // @@author yarnieee
                        () -> EquipmentFilter.parse("eqm-filter " + field + blank));
                // @@author
            }
        }
    }

    @Test
    void parse_all120FieldOrders_keepTheSameMeaning() throws Exception {
        // @@author yarnieee
        checkPermutations(new String[]{"e/E001", "name/camera", "type/camera", "cond/1-3", "status/available"}, 0);
        // @@author
    }

    /** Swaps fields recursively so every ordering is tested without a large handwritten input list. */
    private void checkPermutations(String[] fields, int position) throws Exception {
        if (position == fields.length) {
            // @@author yarnieee
            EquipmentFilter filter = EquipmentFilter.parse("eqm-filter " + String.join(" ", fields));
            assertTrue(filter.matches(new Camera("Camera", "E001", 2), false));
            assertFalse(filter.matches(new Camera("Camera", "E001", 2), true));
            assertFalse(filter.matches(new Camera("Camera", "E001", 4), false));
            // @@author
            return;
        }
        for (int i = position; i < fields.length; i++) {
            String saved = fields[position];
            fields[position] = fields[i];
            fields[i] = saved;
            checkPermutations(fields, position + 1);
            fields[i] = fields[position];
            fields[position] = saved;
        }
    }

    @Test
    void matches_manyQueriesAcrossFullInventory_matchesIndependentExpectedResults() throws Exception {
        List<Equipment> items = new ArrayList<>();
        for (int id = 1; id <= 999; id++) {
            // @@author yarnieee
            String formatted = String.format(Locale.ROOT, "E%03d", id);
            // @@author
            int condition = (id - 1) % 10 + 1;
            items.add(id % 2 == 0 ? new Camera("Camera " + formatted, formatted, condition)
                    : new Lens("Camera " + formatted, formatted, condition));
        }
        Random random = new Random(2113);
        for (int query = 0; query < 200; query++) {
            int minimum = random.nextInt(10) + 1;
            int maximum = minimum + random.nextInt(11 - minimum);
            boolean hasType = random.nextBoolean();
            boolean hasName = random.nextBoolean();
            boolean hasStatus = random.nextBoolean();
            String type = random.nextBoolean() ? "camera" : "lens";
            String fragment = Integer.toString(random.nextInt(10));
            boolean requiresLoaned = random.nextBoolean();
            List<String> fields = new ArrayList<>();
            fields.add("cond/" + minimum + "-" + maximum);
            if (hasType) {
                fields.add("type/" + type.toUpperCase(Locale.ROOT));
            }
            if (hasName) {
                // @@author yarnieee
                fields.add("name/" + fragment);
                // @@author
            }
            if (hasStatus) {
                fields.add("status/" + (requiresLoaned ? "loaned" : "available"));
            }
            Collections.shuffle(fields, random);
            // @@author yarnieee
            EquipmentFilter filter = EquipmentFilter.parse("eqm-filter " + String.join(" ", fields));
            // @@author
            for (int index = 0; index < items.size(); index++) {
                Equipment item = items.get(index);
                boolean isLoaned = index % 3 == 0;
                boolean expected = item.getCondition() >= minimum && item.getCondition() <= maximum;
                expected &= !hasType || type.equals(item.getType());
                expected &= !hasName || item.getItemName().contains(fragment);
                expected &= !hasStatus || isLoaned == requiresLoaned;
                assertEquals(expected, filter.matches(item, isLoaned), "query " + query + ", item " + index);
            }
        }
    }

    @Test
    void parse_seededTokenMutations_neverThrowsUnexpectedExceptions() throws Exception {
        Random random = new Random(2026);
        // @@author yarnieee
        String[] tokens = {"e/E001", "e/E000", "name/Camera", "type/camera", "cond/1-3", "status/available",
            "cond/2147483648", "x", " ", "\u00a0", "name/", "\t", "\u0000", "e/card"};
        Equipment item = new Camera("Camera", "E001", 1);
        // @@author
        for (int trial = 0; trial < 8_000; trial++) {
            // @@author yarnieee
            StringBuilder command = new StringBuilder("eqm-filter ");
            // @@author
            for (int token = 0; token < 1 + random.nextInt(12); token++) {
                command.append(tokens[random.nextInt(tokens.length)]).append(' ');
            }
            try {
                EquipmentFilter filter = EquipmentFilter.parse(command.toString());
                filter.matches(item, random.nextBoolean());
            } catch (EquipmentFilteringUnsuccessful expected) {
                assertFalse(expected.getMessage().isBlank());
            }
        }
    }

    @Test
    void parse_largeValuesAndRepeatedPrefixes_rejectsCleanly() {
        // @@author yarnieee
        for (String input : new String[]{"eqm-filter name/" + "A".repeat(100_000),
            "eqm-filter " + "e/E001 ".repeat(10_000), "eqm-filter cond/" + "9".repeat(100_000)}) {
            // @@author
            assertThrows(EquipmentFilteringUnsuccessful.class, () -> EquipmentFilter.parse(input));
        }
    }

    @Test
    void executeInput_tabsAndUnicodeWhitespace_routeToFilter() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            // @@author yarnieee
            manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
            // @@author
            for (String space : new String[]{" ", "\t", "\u2003", "\u00a0"}) {
                output.reset();
                // @@author yarnieee
                manager.executeInput(space + "eqm-filter" + space + "e/E001" + space);
                // @@author
                assertTrue(output.toString(StandardCharsets.UTF_8).contains("Camera"));
            }
        } finally {
            System.setOut(original);
        }
    }
}
