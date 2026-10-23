package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.exceptions.EquipmentFilteringUnsuccessful;
import seedu.snap.loan.LoanHandler;

/** Covers individual and combined filters, invalid fields, availability, and inventory preservation. */
class EquipmentFilterTest {
    @TempDir
    private Path temporaryDataDirectory;

    private Equipment camera;
    private Equipment tripod;

    @BeforeEach
    void setUp() {
        camera = new Camera("Canon Camera 相机", "001", 1);
        tripod = new Tripod("Tripod", "999", 10);
    }

    @Test
    void parse_idAndName_selectsMatchingEquipment() throws EquipmentFilteringUnsuccessful {
        for (String input : new String[]{"filter id/001", "filter i/cAmErA", "filter i/相机"}) {
            EquipmentFilter filter = EquipmentFilter.parse(input);
            assertTrue(filter.matches(camera, false), input);
            assertFalse(filter.matches(tripod, false), input);
        }
    }

    @Test
    void parse_type_usesFullCaseInsensitiveMatch() throws EquipmentFilteringUnsuccessful {
        assertTrue(EquipmentFilter.parse("filter type/CAMERA").matches(camera, false));
        assertFalse(EquipmentFilter.parse("filter type/cam").matches(camera, false));
        assertFalse(EquipmentFilter.parse("filter type/camera kit").matches(camera, false));
    }

    @Test
    void parse_conditionRange_includesBothBoundaries() throws EquipmentFilteringUnsuccessful {
        EquipmentFilter filter = EquipmentFilter.parse("filter cond/1-10");
        assertTrue(filter.matches(camera, false));
        assertTrue(filter.matches(tripod, false));
        assertFalse(EquipmentFilter.parse("filter cond/2-9").matches(camera, false));
        assertFalse(EquipmentFilter.parse("filter cond/2-9").matches(tripod, false));
        assertTrue(EquipmentFilter.parse("filter cond/10").matches(tripod, false));
        assertTrue(EquipmentFilter.parse("filter cond/10-10").matches(tripod, false));
    }

    @Test
    void apply_multipleMatches_returnsReadOnlySnapshotInInventoryOrder() throws EquipmentFilteringUnsuccessful {
        ArrayList<Equipment> inventory = new ArrayList<>(List.of(tripod, camera));
        LoanHandler loans = new LoanHandler(new EquipmentHandler());
        List<Equipment> result = EquipmentFilter.parse("filter cond/1-10").apply(inventory, loans);
        assertEquals(List.of(tripod, camera), result);
        assertSame(tripod, result.getFirst());
        assertSame(camera, result.getLast());
        assertThrows(UnsupportedOperationException.class, result::clear);
        assertThrows(UnsupportedOperationException.class, () -> result.add(camera));
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, camera));
        assertEquals(List.of(tripod, camera), inventory);
        inventory.clear();
        assertEquals(List.of(tripod, camera), result);
    }

    @Test
    void parse_combinedFieldsInAnyOrder_requiresAllToMatch() throws EquipmentFilteringUnsuccessful {
        EquipmentFilter filter = EquipmentFilter.parse(
                "filter status/AVAILABLE cond/1-3 type/camera id/001 i/canon");
        assertTrue(filter.matches(camera, false));
        assertFalse(filter.matches(camera, true));
        assertFalse(filter.matches(tripod, false));
    }

    @Test
    void parse_textPrefixes_preservesNames() throws EquipmentFilteringUnsuccessful {
        for (String name : new String[]{"USB id/card reader", "fluid/head tripod", "mini/camera"}) {
            Equipment item = new Tripod(name, "002", 2);
            assertTrue(EquipmentFilter.parse("filter i/" + name).matches(item, false));
        }
    }

    @Test
    void parse_invalidCommands_rejectsEachExplicitCase() {
        String[] invalid = {
            null, "", "filter", "filter ", "filter unknown/value", "filter rubbish id/001",
            "filter id/001 id/002", "filter i/Camera i/Tripod", "filter type/a type/b",
            "filter cond/1 cond/2", "filter status/available status/loaned",
            "filter i/", "filter i/\u2003", "filter type/", "filter type/\u2003", "filter status/",
            "filter id/000", "filter id/01", "filter id/1000", "filter id/+01", "filter id/001 extra",
            "filter cond/0", "filter cond/11", "filter cond/01", "filter cond/1.0", "filter cond/9-2",
            "filter cond/1-11", "filter cond/-1", "filter cond/1-", "filter cond/1-3-5",
            "filter cond/2147483648", "filter cond/1 extra", "filter status/yes",
            "filter i/" + "a".repeat(101)
        };
        for (String input : invalid) {
            assertThrows(EquipmentFilteringUnsuccessful.class, () -> EquipmentFilter.parse(input), input);
        }
    }

    @Test
    void parse_reservedIdSyntaxInsideName_rejectsMalformedAndDuplicateIds() {
        for (String suffix : new String[]{"", " ", "001", "+01", "-01", " 999"}) {
            assertThrows(EquipmentFilteringUnsuccessful.class,
                    () -> EquipmentFilter.parse("filter id/001 i/Camera id/" + suffix), suffix);
        }
    }

    @Test
    void executeInput_emptyInventory_filtersWithoutCreatingEquipment() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            manager.executeInput("filter cond/1-10");
            assertEquals("No equipment matches the filters." + System.lineSeparator(),
                    output.toString(StandardCharsets.UTF_8));
            manager.executeInput("add i/Camera id/001 type/camera cond/1");
            output.reset();
            manager.executeInput("filter id/001");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Camera"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void executeInput_multipleMatches_preservesInsertionOrderAndExcludedItems() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            manager.executeInput("add i/First Camera id/999 type/camera cond/1");
            manager.executeInput("add i/Excluded Tripod id/002 type/tripod cond/5");
            manager.executeInput("add i/Second Camera id/001 type/camera cond/10");
            output.reset();
            manager.executeInput("filter type/camera cond/1-10");
            String filtered = output.toString(StandardCharsets.UTF_8);
            assertTrue(filtered.contains("First Camera"));
            assertTrue(filtered.contains("Second Camera"));
            assertTrue(filtered.indexOf("First Camera") < filtered.indexOf("Second Camera"));
            assertFalse(filtered.contains("Excluded Tripod"));
            output.reset();
            manager.executeInput("list");
            String all = output.toString(StandardCharsets.UTF_8);
            assertTrue(all.indexOf("First Camera") < all.indexOf("Excluded Tripod"));
            assertTrue(all.indexOf("Excluded Tripod") < all.indexOf("Second Camera"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void executeInput_availabilityChangesAfterLoanDeletion() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EqmManager manager = new EqmManager(temporaryDataDirectory);
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            manager.executeInput("add i/Lens id/998 type/lens cond/2");
            output.reset();
            manager.executeInput("filter id/998 status/available");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            manager.executeInput("loan id/998 b/002 issued/01-10-26 due/02-10-26");
            output.reset();
            manager.executeInput("filter id/998 status/available");
            assertEquals("No equipment matches the filters." + System.lineSeparator(),
                    output.toString(StandardCharsets.UTF_8));
            output.reset();
            manager.executeInput("filter id/998 status/loaned");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            manager.executeInput("delete loan/001");
            output.reset();
            manager.executeInput("filter id/998 status/available");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            manager.executeInput("delete item/998");
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void parse_nameMatching_doesNotDependOnDefaultLocale() throws EquipmentFilteringUnsuccessful {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            Equipment item = new Camera("FILM", "002", 2);
            assertTrue(EquipmentFilter.parse("filter i/film").matches(item, false));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void executeInput_invalidFilter_recoversAndLeavesInventoryAvailable() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            manager.executeInput("add i/Camera id/001 type/camera cond/1");
            output.reset();
            manager.executeInput("filter cond/9-1");
            manager.executeInput("filter id/999");
            manager.executeInput("filter id/001");
            manager.executeInput("delete item/001");
            String text = output.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("The minimum condition must not exceed the maximum."));
            assertTrue(text.contains("No equipment matches the filters."));
            assertTrue(text.contains("Camera"));
            assertTrue(text.contains("Equipment with item ID 001 deleted successfully."));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void parse_nonbreakingWhitespaceAfterRepeatedId_rejectsNumericField() {
        for (String space : new String[]{"\u00a0", "\u2007", "\u202f", "\u2003", " "}) {
            String command = "filter id/001 i/Camera id/" + space + "999";
            assertThrows(EquipmentFilteringUnsuccessful.class, () -> EquipmentFilter.parse(command));
        }
    }
}
