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
        for (String input : new String[]{"eqm-filter e/001", "eqm-filter name/cAmErA", "eqm-filter name/相机"}) {
            EquipmentFilter filter = EquipmentFilter.parse(input);
            assertTrue(filter.matches(camera, false), input);
            assertFalse(filter.matches(tripod, false), input);
        }
    }

    @Test
    void parse_type_usesFullCaseInsensitiveMatch() throws EquipmentFilteringUnsuccessful {
        assertTrue(EquipmentFilter.parse("eqm-filter type/CAMERA").matches(camera, false));
        assertFalse(EquipmentFilter.parse("eqm-filter type/cam").matches(camera, false));
        assertFalse(EquipmentFilter.parse("eqm-filter type/camera kit").matches(camera, false));
    }

    @Test
    void parse_conditionRange_includesBothBoundaries() throws EquipmentFilteringUnsuccessful {
        EquipmentFilter filter = EquipmentFilter.parse("eqm-filter cond/1-10");
        assertTrue(filter.matches(camera, false));
        assertTrue(filter.matches(tripod, false));
        assertFalse(EquipmentFilter.parse("eqm-filter cond/2-9").matches(camera, false));
        assertFalse(EquipmentFilter.parse("eqm-filter cond/2-9").matches(tripod, false));
        assertTrue(EquipmentFilter.parse("eqm-filter cond/10").matches(tripod, false));
        assertTrue(EquipmentFilter.parse("eqm-filter cond/10-10").matches(tripod, false));
    }

    @Test
    void apply_multipleMatches_returnsReadOnlySnapshotInInventoryOrder() throws EquipmentFilteringUnsuccessful {
        ArrayList<Equipment> inventory = new ArrayList<>(List.of(tripod, camera));
        LoanHandler loans = new LoanHandler(new EquipmentHandler());
        List<Equipment> result = EquipmentFilter.parse("eqm-filter cond/1-10").apply(inventory, loans);
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
                "eqm-filter status/AVAILABLE cond/1-3 type/camera e/001 name/canon");
        assertTrue(filter.matches(camera, false));
        assertFalse(filter.matches(camera, true));
        assertFalse(filter.matches(tripod, false));
    }

    @Test
    void parse_textPrefixes_preservesNames() throws EquipmentFilteringUnsuccessful {
        for (String name : new String[]{"USB e/card reader", "fluid/head tripod", "mini/camera"}) {
            Equipment item = new Tripod(name, "002", 2);
            assertTrue(EquipmentFilter.parse("eqm-filter name/" + name).matches(item, false));
        }
    }

    @Test
    void parse_invalidCommands_rejectsEachExplicitCase() {
        String[] invalid = {
            null, "", "eqm-filter", "eqm-filter ", "eqm-filter unknown/value", "eqm-filter rubbish e/001",
            "eqm-filter e/001 e/002", "eqm-filter name/Camera name/Tripod", "eqm-filter type/a type/b",
            "eqm-filter cond/1 cond/2", "eqm-filter status/available status/loaned",
            "eqm-filter name/", "eqm-filter name/\u2003", "eqm-filter type/", "eqm-filter type/\u2003",
            "eqm-filter status/", "eqm-filter e/000", "eqm-filter e/01", "eqm-filter e/1000",
            "eqm-filter e/+01", "eqm-filter e/001 extra", "eqm-filter cond/0", "eqm-filter cond/11",
            "eqm-filter cond/01", "eqm-filter cond/1.0", "eqm-filter cond/9-2",
            "eqm-filter cond/1-11", "eqm-filter cond/-1", "eqm-filter cond/1-", "eqm-filter cond/1-3-5",
            "eqm-filter cond/2147483648", "eqm-filter cond/1 extra", "eqm-filter status/yes",
            "eqm-filter name/" + "a".repeat(101)
        };
        for (String input : invalid) {
            assertThrows(EquipmentFilteringUnsuccessful.class, () -> EquipmentFilter.parse(input), input);
        }
    }

    @Test
    void parse_reservedIdSyntaxInsideName_rejectsMalformedAndDuplicateIds() {
        for (String suffix : new String[]{"", " ", "001", "+01", "-01", " 999"}) {
            assertThrows(EquipmentFilteringUnsuccessful.class,
                    () -> EquipmentFilter.parse("eqm-filter e/001 name/Camera e/" + suffix), suffix);
        }
    }

    @Test
    void executeInput_emptyInventory_filtersWithoutCreatingEquipment() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            manager.executeInput("eqm-filter cond/1-10");
            assertEquals(String.join(System.lineSeparator(), "Matching equipment", "No equipment matches the filters.",
                    "0 equipment items.", ""),
                    output.toString(StandardCharsets.UTF_8));
            manager.executeInput("eqm-add name/Camera e/001 type/camera cond/1");
            output.reset();
            manager.executeInput("eqm-filter e/001");
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
            manager.executeInput("eqm-add name/First Camera e/999 type/camera cond/1");
            manager.executeInput("eqm-add name/Excluded Tripod e/002 type/tripod cond/5");
            manager.executeInput("eqm-add name/Second Camera e/001 type/camera cond/10");
            output.reset();
            manager.executeInput("eqm-filter type/camera cond/1-10");
            String filtered = output.toString(StandardCharsets.UTF_8);
            assertTrue(filtered.contains("First Camera"));
            assertTrue(filtered.contains("Second Camera"));
            assertTrue(filtered.indexOf("First Camera") < filtered.indexOf("Second Camera"));
            assertFalse(filtered.contains("Excluded Tripod"));
            output.reset();
            manager.executeInput("eqm-list");
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
            manager.executeInput("eqm-add name/Lens e/998 type/lens cond/2");
            output.reset();
            manager.executeInput("eqm-filter e/998 status/available");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            manager.executeInput("loan-add e/998 u/002 issued/01-10-26 due/02-10-26");
            output.reset();
            manager.executeInput("eqm-filter e/998 status/available");
            assertEquals(String.join(System.lineSeparator(), "Matching equipment", "No equipment matches the filters.",
                    "0 equipment items.", ""),
                    output.toString(StandardCharsets.UTF_8));
            output.reset();
            manager.executeInput("eqm-filter e/998 status/loaned");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            manager.executeInput("loan-delete l/001");
            output.reset();
            manager.executeInput("eqm-filter e/998 status/available");
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            manager.executeInput("eqm-delete e/998");
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
            assertTrue(EquipmentFilter.parse("eqm-filter name/film").matches(item, false));
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
            manager.executeInput("eqm-add name/Camera e/001 type/camera cond/1");
            output.reset();
            manager.executeInput("eqm-filter cond/9-1");
            manager.executeInput("eqm-filter e/999");
            manager.executeInput("eqm-filter e/001");
            manager.executeInput("eqm-delete e/001");
            String text = output.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("The minimum condition must not exceed the maximum."));
            assertTrue(text.contains("No equipment matches the filters."));
            assertTrue(text.contains("Camera"));
            assertTrue(text.contains("[OK] Equipment 001 \"Camera\" deleted."));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void parse_nonbreakingWhitespaceAfterRepeatedId_rejectsNumericField() {
        for (String space : new String[]{"\u00a0", "\u2007", "\u202f", "\u2003", " "}) {
            String command = "eqm-filter e/001 name/Camera e/" + space + "999";
            assertThrows(EquipmentFilteringUnsuccessful.class, () -> EquipmentFilter.parse(command));
        }
    }
}
