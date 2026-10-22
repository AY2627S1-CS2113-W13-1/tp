package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.EquipmentDeletionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;

/** Exercises independent inventories, full capacity, and long loan/return/delete sequences. */
class EquipmentAvailabilityStressTest {
    @Test
    void addLoan_independentInventories_doNotShareLoanState() throws Exception {
        EquipmentHandler firstInventory = new EquipmentHandler();
        EquipmentHandler secondInventory = new EquipmentHandler();
        Equipment first = firstInventory.addEquipment("add i/Camera id/001 type/camera cond/1");
        Equipment second = secondInventory.addEquipment("add i/Other id/001 type/camera cond/1");
        LoanHandler firstLoans = new LoanHandler(firstInventory);
        LoanHandler secondLoans = new LoanHandler(secondInventory);
        firstLoans.addLoan(command("001", "01-10-26"));
        assertFalse(second.isLoaned());
        secondLoans.addLoan(command("001", "01-10-26"));
        firstLoans.deleteLoan("delete-loan id/001");
        assertFalse(first.isLoaned());
        assertTrue(second.isLoaned());
    }

    @Test
    void setReturned_deletedAndReusedEquipment_doesNotReviveOldAssociation() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        inventory.addEquipment("add i/Camera id/001 type/camera cond/1");
        LoanHandler handler = new LoanHandler(inventory);
        Loan old = handler.addLoan(command("001", "01-10-26"));
        old.setReturned(true);
        inventory.deleteEquipment("delete item/001");
        Equipment replacement = inventory.addEquipment("add i/Replacement id/001 type/camera cond/1");
        assertThrows(IllegalStateException.class, () -> old.setReturned(false));
        assertFalse(replacement.isLoaned());
        assertFalse(handler.hasActiveLoanForItem("001"));
        handler.addLoan(command("001", "02-10-26"));
        assertTrue(replacement.isLoaned());
    }

    @Test
    void loanLifecycle_all999Ids_preservesEveryEquipmentStatus() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        LoanHandler handler = new LoanHandler(inventory);
        for (int id = 1; id <= 999; id++) {
            String formatted = String.format(java.util.Locale.ROOT, "%03d", id);
            inventory.addEquipment("add i/Camera id/" + formatted + " type/camera cond/1");
            handler.addLoan(command(formatted, "01-10-26"));
        }
        assertEquals(999, handler.numLoans);
        for (Equipment item : inventory.getEquipmentList()) {
            assertTrue(item.isLoaned());
            assertThrows(LoanAdditionUnsuccessful.class,
                    () -> handler.addLoan(command(item.getItemId(), "02-10-26")));
        }
        for (int i = 0; i < 999; i++) {
            Equipment item = inventory.getEquipmentList().get(i);
            if (i % 2 == 0) {
                handler.loans.get(i).setReturned(true);
            }
        }
        for (int i = 0; i < 999; i++) {
            Equipment item = inventory.getEquipmentList().get(i);
            assertEquals(i % 2 != 0, item.isLoaned());
            handler.deleteLoan("delete-loan id/" + item.getItemId());
            assertFalse(item.isLoaned());
        }
        assertEquals(0, handler.numLoans);
    }

    @Test
    void loanLifecycle_seededOperations_matchIndependentActiveLoanModel() throws Exception {
        EquipmentHandler inventory = new EquipmentHandler();
        ArrayList<Equipment> items = new ArrayList<>();
        for (int id = 1; id <= 40; id++) {
            String formatted = String.format(java.util.Locale.ROOT, "%03d", id);
            items.add(inventory.addEquipment("add i/Camera id/" + formatted + " type/camera cond/1"));
        }
        LoanHandler handler = new LoanHandler(inventory);
        Loan[] active = new Loan[items.size()];
        Random random = new Random(2113);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-uu");
        for (int step = 0; step < 5_000; step++) {
            int index = random.nextInt(items.size());
            Equipment item = items.get(index);
            String date = LocalDate.of(2026, 1, 1).plusDays(step).format(format);
            switch (random.nextInt(4)) {
            case 0:
                if (active[index] == null) {
                    active[index] = handler.addLoan(command(item.getItemId(), date));
                } else {
                    assertThrows(LoanAdditionUnsuccessful.class,
                            () -> handler.addLoan(command(item.getItemId(), date)));
                }
                break;
            case 1:
                if (active[index] != null) {
                    active[index].setReturned(true);
                    active[index] = null;
                }
                break;
            case 2:
                if (active[index] != null) {
                    String issued = active[index].getIssuedDate().format(format);
                    handler.deleteLoan("delete-loan id/" + item.getItemId() + " issued/" + issued);
                    active[index] = null;
                }
                break;
            default:
                if (active[index] != null) {
                    assertThrows(EquipmentDeletionUnsuccessful.class,
                            () -> inventory.deleteEquipment("delete item/" + item.getItemId()));
                }
                break;
            }
            for (int i = 0; i < items.size(); i++) {
                assertEquals(active[i] != null, items.get(i).isLoaned(), "step " + step + ", item " + i);
                assertEquals(active[i] != null, handler.hasActiveLoanForItem(items.get(i).getItemId()));
                assertSame(items.get(i), inventory.getEquipmentList().get(i));
            }
            assertEquals(handler.loans.size(), handler.numLoans);
        }
        assertEquals(List.copyOf(items), inventory.getEquipmentList());
    }

    /** Uses identical issue/due dates to avoid introducing unrelated date validity into lifecycle tests. */
    private String command(String id, String date) {
        return "loan id/" + id + " b/002 issued/" + date + " due/" + date;
    }
}
