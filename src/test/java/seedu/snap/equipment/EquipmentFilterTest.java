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
        // @@author yarnieee
        camera = new Camera("Canon Camera 相机", "E001", 1);
        tripod = new Tripod("Tripod", "E999", 10);
        // @@author
    }

    @Test
    void parse_idAndName_selectsMatchingEquipment() throws EquipmentFilteringUnsuccessful {
        // @@author yarnieee
        for (String input : new String[]{"eqm-filter e/E001", "eqm-filter name/cAmErA", "eqm-filter name/相机"}) {
            // @@author
            EquipmentFilter filter = EquipmentFilter.parse(input);
            assertTrue(filter.matches(camera, false), input);
            assertFalse(filter.matches(tripod, false), input);
        }
    }

    // @@author yarnieee
    @Test
    void parse_lowercaseId_selectsCanonicalEquipment() throws EquipmentFilteringUnsuccessful {
        EquipmentFilter filter = EquipmentFilter.parse("eqm-filter e/e001");

        // @@author
        assertTrue(filter.matches(camera, false));
        assertFalse(filter.matches(tripod, false));
    }

    @Test
    // @@author yarnieee
    void parse_idAfterNameField_keepsBothFields() throws EquipmentFilteringUnsuccessful {
        EquipmentFilter filter = EquipmentFilter.parse("eqm-filter name/Camera e/E001");

        // @@author
        assertTrue(filter.matches(camera, false));
        assertFalse(filter.matches(tripod, false));
    }

    @Test
    void parse_type_usesFullCaseInsensitiveMatch() throws EquipmentFilteringUnsuccessful {
        // @@author yarnieee
        assertTrue(EquipmentFilter.parse("eqm-filter type/CAMERA").matches(camera, false));
        assertFalse(EquipmentFilter.parse("eqm-filter type/cam").matches(camera, false));
        assertFalse(EquipmentFilter.parse("eqm-filter type/camera kit").matches(camera, false));
        // @@author
    }

    @Test
    void parse_conditionRange_includesBothBoundaries() throws EquipmentFilteringUnsuccessful {
        // @@author yarnieee
        EquipmentFilter filter = EquipmentFilter.parse("eqm-filter cond/1-10");
        // @@author
        assertTrue(filter.matches(camera, false));
        assertTrue(filter.matches(tripod, false));
        // @@author yarnieee
        assertFalse(EquipmentFilter.parse("eqm-filter cond/2-9").matches(camera, false));
        assertFalse(EquipmentFilter.parse("eqm-filter cond/2-9").matches(tripod, false));
        assertTrue(EquipmentFilter.parse("eqm-filter cond/10").matches(tripod, false));
        assertTrue(EquipmentFilter.parse("eqm-filter cond/10-10").matches(tripod, false));
        // @@author
    }

    @Test
    void apply_multipleMatches_returnsReadOnlySnapshotInInventoryOrder() throws EquipmentFilteringUnsuccessful {
        ArrayList<Equipment> inventory = new ArrayList<>(List.of(tripod, camera));
        LoanHandler loans = new LoanHandler(new EquipmentHandler());
        // @@author yarnieee
        List<Equipment> result = EquipmentFilter.parse("eqm-filter cond/1-10").apply(inventory, loans);
        // @@author
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
                // @@author yarnieee
                "eqm-filter status/AVAILABLE cond/1-3 type/camera e/E001 name/canon");
        // @@author
        assertTrue(filter.matches(camera, false));
        assertFalse(filter.matches(camera, true));
        assertFalse(filter.matches(tripod, false));
    }

    @Test
    void parse_textPrefixes_preservesNames() throws EquipmentFilteringUnsuccessful {
        for (String name : new String[]{"USB id/card reader", "fluid/head tripod", "mini/camera"}) {
            // @@author yarnieee
            Equipment item = new Tripod(name, "E002", 2);
            assertTrue(EquipmentFilter.parse("eqm-filter name/" + name).matches(item, false));
            // @@author
        }
    }

    @Test
    void parse_invalidCommands_rejectsEachExplicitCase() {
        String[] invalid = {
            // @@author yarnieee
            null, "", "eqm-filter", "eqm-filter ", "eqm-filter unknown/value", "eqm-filter rubbish e/E001",
            "eqm-filter e/E001 e/E002", "eqm-filter name/Camera name/Tripod", "eqm-filter type/a type/b",
            "eqm-filter cond/1 cond/2", "eqm-filter status/available status/loaned",
            "eqm-filter name/", "eqm-filter name/\u2003", "eqm-filter type/", "eqm-filter type/\u2003",
            "eqm-filter status/",
            "eqm-filter e/E000", "eqm-filter e/01", "eqm-filter e/E1000", "eqm-filter e/+01", "eqm-filter e/E001 extra",
            "eqm-filter cond/0", "eqm-filter cond/11", "eqm-filter cond/01", "eqm-filter cond/1.0",
            "eqm-filter cond/9-2",
            "eqm-filter cond/1-11", "eqm-filter cond/-1", "eqm-filter cond/1-", "eqm-filter cond/1-3-5",
            "eqm-filter cond/2147483648", "eqm-filter cond/1 extra", "eqm-filter status/yes",
            "eqm-filter name/" + "a".repeat(101)
            // @@author
        };
        for (String input : invalid) {
            assertThrows(EquipmentFilteringUnsuccessful.class, () -> EquipmentFilter.parse(input), input);
        }
    }

    @Test
    void parse_reservedIdSyntaxInsideName_rejectsMalformedAndDuplicateIds() {
        for (String suffix : new String[]{"", " ", "001", "+01", "-01", " 999"}) {
            assertThrows(EquipmentFilteringUnsuccessful.class,
                    // @@author yarnieee
                    () -> EquipmentFilter.parse("eqm-filter e/E001 name/Camera e/" + suffix), suffix);
            // @@author
        }
    }

    @Test
    void executeInput_emptyInventory_filtersWithoutCreatingEquipment() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EqmManager manager = new EqmManager(temporaryDataDirectory);
            // @@author yarnieee
            manager.executeInput("eqm-filter cond/1-10");
            // @@author
            assertEquals(String.join(System.lineSeparator(), "Matching equipment", "No equipment matches the filters.",
                    "0 equipment items.", ""),
                    output.toString(StandardCharsets.UTF_8));
            // @@author yarnieee
            manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
            // @@author
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-filter e/E001");
            // @@author
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
            // @@author yarnieee
            manager.executeInput("eqm-add name/First Camera e/E999 type/camera cond/1");
            manager.executeInput("eqm-add name/Excluded Tripod e/E002 type/tripod cond/5");
            manager.executeInput("eqm-add name/Second Camera e/E001 type/camera cond/10");
            // @@author
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-filter type/camera cond/1-10");
            // @@author
            String filtered = output.toString(StandardCharsets.UTF_8);
            assertTrue(filtered.contains("First Camera"));
            assertTrue(filtered.contains("Second Camera"));
            assertTrue(filtered.indexOf("First Camera") < filtered.indexOf("Second Camera"));
            assertFalse(filtered.contains("Excluded Tripod"));
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-list");
            // @@author
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
            // @@author yarnieee
            manager.executeInput("eqm-add name/Lens e/E998 type/lens cond/2");
            // @@author
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-filter e/E998 status/available");
            // @@author
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            // @@author yarnieee
            manager.executeInput("loan-add e/E998 u/U002 issued/01-10-26 due/02-10-26");
            // @@author
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-filter e/E998 status/available");
            // @@author
            assertEquals(String.join(System.lineSeparator(), "Matching equipment", "No equipment matches the filters.",
                    "0 equipment items.", ""),
                    output.toString(StandardCharsets.UTF_8));
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-filter e/E998 status/loaned");
            // @@author
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            // @@author yarnieee
            manager.executeInput("loan-delete l/L001");
            // @@author
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-filter e/E998 status/available");
            // @@author
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Lens"));
            // @@author yarnieee
            manager.executeInput("eqm-delete e/E998");
            // @@author
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void parse_nameMatching_doesNotDependOnDefaultLocale() throws EquipmentFilteringUnsuccessful {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            // @@author yarnieee
            Equipment item = new Camera("FILM", "E002", 2);
            assertTrue(EquipmentFilter.parse("eqm-filter name/film").matches(item, false));
            // @@author
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
            // @@author yarnieee
            manager.executeInput("eqm-add name/Camera e/E001 type/camera cond/1");
            // @@author
            output.reset();
            // @@author yarnieee
            manager.executeInput("eqm-filter cond/9-1");
            manager.executeInput("eqm-filter e/E999");
            manager.executeInput("eqm-filter e/E001");
            manager.executeInput("eqm-delete e/E001");
            // @@author
            String text = output.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("The minimum condition must not exceed the maximum."));
            assertTrue(text.contains("No equipment matches the filters."));
            assertTrue(text.contains("Camera"));
            // @@author yarnieee
            assertTrue(text.contains("[OK] Equipment E001 \"Camera\" deleted."));
            // @@author
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void parse_nonbreakingWhitespaceAfterRepeatedId_rejectsNumericField() {
        for (String space : new String[]{"\u00a0", "\u2007", "\u202f", "\u2003", " "}) {
            // @@author yarnieee
            String command = "eqm-filter e/E001 name/Camera e/" + space + "999";
            // @@author
            assertThrows(EquipmentFilteringUnsuccessful.class, () -> EquipmentFilter.parse(command));
        }
    }
}
