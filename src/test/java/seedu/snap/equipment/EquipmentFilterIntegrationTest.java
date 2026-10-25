package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Checks filtering alongside equipment subclasses, stored details, and active-loan safeguards. */
class EquipmentFilterIntegrationTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void apply_allSubclasses_preservesObjectsDetailsAndSavedRecords() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler equipment = new EquipmentHandler(file);
        equipment.addEquipment("add i/Camera id/001 type/camera cond/1 sensor/full frame damage/SENSOR_DAMAGE");
        equipment.addEquipment("add i/Lens id/002 type/lens cond/2 zoom/24-70mm damage/SCRATCHED_GLASS");
        equipment.addEquipment("add i/Battery id/003 type/battery cond/3 capacity/2000mAh damage/SWELLING");
        equipment.addEquipment("add i/Tripod id/004 type/tripod cond/4 thread/1-4 inch damage/BENT_LEG");
        List<Equipment> original = List.copyOf(equipment.getEquipmentList());
        String saved = Files.readString(file);
        LoanHandler loans = new LoanHandler(equipment);

        List<Equipment> matches = EquipmentFilter.parse("filter status/available cond/1-4")
                .apply(equipment.getEquipmentList(), loans);

        assertEquals(original, matches);
        for (int index = 0; index < original.size(); index++) {
            assertSame(original.get(index), matches.get(index));
        }
        assertInstanceOf(Camera.class, matches.get(0));
        assertInstanceOf(Lens.class, matches.get(1));
        assertInstanceOf(Battery.class, matches.get(2));
        assertInstanceOf(Tripod.class, matches.get(3));
        assertEquals(original, equipment.getEquipmentList());
        assertEquals(saved, Files.readString(file));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            equipment.viewEquipment(matches);
        } finally {
            System.setOut(originalOutput);
        }
        String table = output.toString(StandardCharsets.UTF_8);
        assertTrue(table.contains("STATUS"));
        assertTrue(table.contains("DETAILS"));
        assertEquals(4, table.lines().filter(line -> line.contains("Available")).count());
        for (Equipment item : original) {
            assertTrue(table.contains(item.getDetails()));
        }

        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        for (int index = 0; index < original.size(); index++) {
            Equipment restored = restarted.getEquipmentList().get(index);
            assertEquals(original.get(index).getClass(), restored.getClass());
            assertEquals(original.get(index).getDetails(), restored.getDetails());
        }
    }

    @Test
    void apply_returnedHistoryAndActiveLoan_preservesAvailabilityAndDeletionChecks() throws Exception {
        EquipmentHandler equipment = new EquipmentHandler();
        Equipment camera = equipment.addEquipment("add i/Camera id/001 type/camera cond/1 sensor/full frame");
        LoanHandler loans = new LoanHandler(equipment);
        Loan returned = loans.addLoan("loan id/001 b/002 issued/01-10-26 due/02-10-26");
        returned.setReturned(true);
        EquipmentFilter available = EquipmentFilter.parse("filter status/available");
        EquipmentFilter loaned = EquipmentFilter.parse("filter status/loaned");
        assertEquals(List.of(camera), available.apply(equipment.getEquipmentList(), loans));

        Loan active = loans.addLoan("loan id/001 b/003 issued/03-10-26 due/04-10-26");
        loans.deleteLoan("delete loan/001");

        assertTrue(available.apply(equipment.getEquipmentList(), loans).isEmpty());
        assertSame(camera, loaned.apply(equipment.getEquipmentList(), loans).getFirst());
        assertTrue(camera.isLoaned());
        assertEquals(List.of(active), loans.getLoanList());
        assertThrows(EquipmentDeletionUnsuccessful.class, () -> equipment.deleteEquipment("delete item/001"));
        assertThrows(LoanAdditionUnsuccessful.class,
                () -> loans.addLoan("loan id/001 b/004 issued/05-10-26 due/06-10-26"));
        assertEquals("sensor/full frame damage/NONE", camera.getDetails());

        active.setReturned(true);
        assertFalse(camera.isLoaned());
        assertTrue(loaned.apply(equipment.getEquipmentList(), loans).isEmpty());
        assertSame(camera, available.apply(equipment.getEquipmentList(), loans).getFirst());
        assertSame(camera, equipment.deleteEquipment("delete item/001"));
    }
}
