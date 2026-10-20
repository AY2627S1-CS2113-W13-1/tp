package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.eqmmanager.EqmManager;
import seedu.snap.exceptions.EquipmentFilteringUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Covers individual and combined filters, invalid fields, availability, and inventory preservation. */
class EquipmentFilterTest {
    private Equipment camera;
    private Equipment tripod;
    private List<Equipment> inventory;

    @BeforeEach
    void setUp() {
        camera = new Equipment("Canon Camera 相机", "001", "camera kit", 1);
        tripod = new Equipment("Tripod", "999", "accessory", 10);
        inventory = List.of(camera, tripod);
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
        assertTrue(EquipmentFilter.parse("filter type/CAMERA KIT").matches(camera, false));
        assertFalse(EquipmentFilter.parse("filter type/camera").matches(camera, false));
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
    void parse_combinedFieldsInAnyOrder_requiresAllToMatch() throws EquipmentFilteringUnsuccessful {
        EquipmentFilter filter = EquipmentFilter.parse(
                "filter status/AVAILABLE cond/1-3 type/camera kit id/001 i/canon");
        assertTrue(filter.matches(camera, false));
        assertFalse(filter.matches(camera, true));
        assertFalse(filter.matches(tripod, false));
    }

    @Test
    void parse_textPrefixes_preservesNames() throws EquipmentFilteringUnsuccessful {
        for (String name : new String[]{"USB id/card reader", "fluid/head tripod", "mini/camera"}) {
            Equipment item = new Equipment(name, "002", "accessory", 2);
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
    void apply_matchingAndEmptyResults_preservesInventoryAndOrder() throws EquipmentFilteringUnsuccessful {
        LoanHandler loans = new LoanHandler();
        List<Equipment> result = EquipmentFilter.parse("filter cond/1-10").apply(inventory, loans);
        assertEquals(inventory, result);
        assertSame(camera, result.getFirst());
        assertThrows(UnsupportedOperationException.class, result::clear);
        assertTrue(EquipmentFilter.parse("filter id/555").apply(inventory, loans).isEmpty());
        assertEquals(List.of(camera, tripod), inventory);
    }

    @Test
    void apply_availabilityTracksReturnedAndDeletedLoans() throws Exception {
        LoanHandler loans = new LoanHandler();
        Equipment item = new Equipment("Lens", "998", "lens", 2);
        List<Equipment> items = List.of(item);
        EquipmentFilter available = EquipmentFilter.parse("filter status/available");
        EquipmentFilter loaned = EquipmentFilter.parse("filter status/loaned");
        Loan loan = loans.addLoan("loan id/998 b/002 issued/01-10-26 due/02-10-26");
        try {
            assertEquals(items, loaned.apply(items, loans));
            assertTrue(available.apply(items, loans).isEmpty());
            loan.setReturned(true);
            assertEquals(items, available.apply(items, loans));
            assertTrue(loaned.apply(items, loans).isEmpty());
        } finally {
            loans.deleteLoan("delete-loan id/998");
        }
        assertEquals(items, available.apply(items, loans));
    }

    @Test
    void parse_nameMatching_doesNotDependOnDefaultLocale() throws EquipmentFilteringUnsuccessful {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            Equipment item = new Equipment("FILM", "002", "camera", 2);
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
            EqmManager manager = new EqmManager();
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
}
