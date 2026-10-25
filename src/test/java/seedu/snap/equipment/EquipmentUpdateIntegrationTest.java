package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.EquipmentUpdateUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Verifies updates preserve each equipment subclass, its stored details, and its loan references. */
class EquipmentUpdateIntegrationTest {
    @TempDir
    private Path temporaryDirectory;

    /** Supplies non-default details and damage for every supported equipment subclass. */
    private static Stream<String> equipmentCommands() {
        return Stream.of(
                "add i/Original id/001 type/camera cond/1 sensor/full frame damage/SENSOR_DAMAGE",
                "add i/Original id/001 type/lens cond/1 zoom/24-70mm damage/SCRATCHED_GLASS",
                "add i/Original id/001 type/battery cond/1 capacity/2000mAh damage/SWELLING",
                "add i/Original id/001 type/tripod cond/1 thread/1-4 inch damage/BENT_LEG");
    }

    @ParameterizedTest
    @MethodSource("equipmentCommands")
    void updateEquipment_activeLoan_preservesSubclassDetailsAndReferences(String command) throws Exception {
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        EquipmentHandler handler = new EquipmentHandler(equipmentFile);
        Equipment original = handler.addEquipment(command + " remarks/Serial label checked");
        Equipment other = handler.addEquipment("add i/Other id/999 type/lens cond/10");
        String details = original.getDetails();
        LoanHandler loans = new LoanHandler(handler, loanFile);
        Loan active = loans.addLoan("loan id/001 b/002 issued/01-10-26 due/02-10-26");
        assertEquals("001", active.getLoanId());
        assertEquals("001", original.getLoanId());

        Equipment updated = handler.updateEquipment("update id/001 i/Updated cond/5 type/"
                + original.getType().toUpperCase(Locale.ROOT));

        assertSame(original, updated);
        assertEquals(List.of(original, other), handler.getEquipmentList());
        assertEquals("Updated", original.getItemName());
        assertEquals(5, original.getCondition());
        assertEquals(details, original.getDetails());
        assertEquals("Serial label checked", original.getRemarks());
        assertEquals(active.getLoanId(), original.getLoanId());
        assertTrue(original.isLoaned());
        assertSame(original, EquipmentFilter.parse("filter id/001 status/loaned")
                .apply(handler.getEquipmentList(), loans).getFirst());
        assertThrows(EquipmentDeletionUnsuccessful.class, () -> handler.deleteEquipment("delete item/001"));

        EquipmentHandler restarted = new EquipmentHandler(equipmentFile);
        restarted.loadFromStorage();
        LoanHandler restartedLoans = new LoanHandler(restarted, loanFile);
        restartedLoans.loadFromStorage();
        Equipment restored = restarted.getEquipmentList().getFirst();
        assertEquals(original.getClass(), restored.getClass());
        assertEquals(details, restored.getDetails());
        assertEquals("Serial label checked", restored.getRemarks());
        assertEquals(active.getLoanId(), restored.getLoanId());
        assertEquals(active.getLoanId(), restartedLoans.getLoanList().getFirst().getLoanId());
        assertEquals("Updated", restored.getItemName());
        assertEquals(5, restored.getCondition());
        assertTrue(restored.isLoaned());
        assertFalse(Files.readString(equipmentFile).contains("loanId/"));

        active.setReturned(true);
        assertFalse(original.isLoaned());
        assertSame(original, EquipmentFilter.parse("filter id/001 status/available")
                .apply(handler.getEquipmentList(), loans).getFirst());
        assertSame(original, handler.deleteEquipment("delete item/001"));
    }

    @ParameterizedTest
    @MethodSource("equipmentCommands")
    void updateEquipment_failedSave_preservesDetailsAndActiveLoan(String command) throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(command);
        String details = original.getDetails();
        LoanHandler loans = new LoanHandler(handler);
        Loan active = loans.addLoan("loan id/001 b/002 issued/01-10-26 due/02-10-26");
        Files.delete(file);
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep");

        EquipmentUpdateUnsuccessful error = assertThrows(EquipmentUpdateUnsuccessful.class,
                () -> handler.updateEquipment("update id/001 i/Rejected cond/5"));

        assertTrue(error.getMessage().startsWith("Could not save equipment update:"));
        assertSame(original, handler.getEquipmentList().getFirst());
        assertEquals("Original", original.getItemName());
        assertEquals(1, original.getCondition());
        assertEquals(details, original.getDetails());
        assertTrue(original.isLoaned());
        assertSame(active, loans.getLoanList().getFirst());
        assertEquals("keep", Files.readString(sentinel));
        Files.delete(sentinel);
        Files.delete(file);
        assertSame(original, handler.updateEquipment("update id/001 cond/5"));
        assertEquals(details, original.getDetails());
        assertTrue(Files.readString(file).contains(details));
    }

    @ParameterizedTest
    @MethodSource("equipmentCommands")
    void updateEquipment_differentType_preservesSavedRecordsAndAllDetails(String command) throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(command);
        String saved = Files.readString(file);
        String details = original.getDetails();
        for (String type : new String[]{"camera", "lens", "battery", "tripod", "unsupported"}) {
            if (type.equals(original.getType())) {
                continue;
            }
            EquipmentUpdateUnsuccessful error = assertThrows(EquipmentUpdateUnsuccessful.class,
                    () -> handler.updateEquipment("update id/001 i/Rejected cond/5 type/" + type));
            assertTrue(error.getMessage().startsWith("Equipment type cannot be changed."));
            assertEquals(saved, Files.readString(file));
            assertSame(original, handler.getEquipmentList().getFirst());
            assertEquals("Original", original.getItemName());
            assertEquals(1, original.getCondition());
            assertEquals(details, original.getDetails());
        }
    }
}
