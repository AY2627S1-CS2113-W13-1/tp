package seedu.snap.bundle;

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

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;
import seedu.snap.exceptions.BundleHistoryUnsuccessful;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;
import seedu.snap.exceptions.LoanAdditionUnsuccessful;
import seedu.snap.loan.Loan;
import seedu.snap.loan.LoanHandler;

/** Tests bundle command parsing, validation, partial additions, and listing. */
class BundleHandlerTest {
    private EquipmentHandler equipmentHandler;
    private BundleHandler bundleHandler;

    @BeforeEach
    void setUp() throws EquipmentAdditionUnsuccessful {
        equipmentHandler = new EquipmentHandler();
        equipmentHandler.addEquipment("add i/Camera id/E001 type/camera cond/8 sensor/Full frame"
                + " damage/SHUTTER_FAULT remarks/Handle with care");
        equipmentHandler.addEquipment("add i/Battery id/E002 type/battery cond/8 capacity/2000 mAh damage/NONE");
        equipmentHandler.addEquipment("add i/Battery 2 id/E003 type/battery cond/8");
        equipmentHandler.addEquipment("add i/Tripod id/E004 type/tripod cond/8 thread/Quarter inch damage/NONE");
        equipmentHandler.addEquipment("add i/Lens id/E005 type/lens cond/8 zoom/24 to 70 mm damage/NONE");
        equipmentHandler.addEquipment("add i/Lens 2 id/E006 type/lens cond/8");
        equipmentHandler.addEquipment("add i/Camera 2 id/E007 type/camera cond/8");
        equipmentHandler.addEquipment("add i/Tripod 2 id/E008 type/tripod cond/8");
        bundleHandler = new BundleHandler(equipmentHandler);
    }

    @Test
    void addBundle_assignsThreeDigitSessionIds() throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("add-bundle name/Travel Kit");
        Bundle second = bundleHandler.addBundle("add-bundle name/Studio Kit");

        assertEquals("B001", first.getBundleId());
        assertEquals("B002", second.getBundleId());
        assertEquals(2, bundleHandler.getBundleList().size());
    }

    @Test
    void addBundle_invalidName_throwsException() {
        for (String name : new String[]{"", "   ", "\u2003", "A".repeat(101)}) {
            assertThrows(BundleAdditionUnsuccessful.class,
                    () -> bundleHandler.addBundle("add-bundle name/" + name));
            assertTrue(bundleHandler.getBundleList().isEmpty());
        }
    }

    @Test
    void addBundle_duplicateName_throwsException() throws BundleAdditionUnsuccessful {
        bundleHandler.addBundle("add-bundle name/Travel Kit");

        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.addBundle("add-bundle name/Travel Kit"));
        assertEquals(1, bundleHandler.getBundleList().size());
    }

    @Test
    void addBundle_nonDefaultLocale_keepsAsciiIds() throws BundleAdditionUnsuccessful {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));

            Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");

            assertEquals("B001", bundle.getBundleId());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void addItemsToBundle_validComposition_marksBundleComplete()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E002 E005");

        assertEquals(3, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertTrue(bundle.isComplete());
    }

    @Test
    void addItemsToBundle_rejectsCategoryLimitsAndReportsEachResult()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E007 E004 E008");

        assertEquals(4, messages.size());
        assertTrue(messages.get(0).contains("successfully"));
        assertTrue(messages.get(1).contains("only one camera"));
        assertTrue(messages.get(2).contains("successfully"));
        assertTrue(messages.get(3).contains("at most one tripod"));
        assertEquals(2, bundle.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_rejectsUnknownLoanedAndDuplicateItems()
            throws BundleAdditionUnsuccessful, LoanAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        loans.addLoan("loan id/E006 b/U001 issued/01-10-26 due/02-10-26");

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/B001 E999 E006 E001 E001");

        assertTrue(messages.get(0).contains("does not exist"));
        assertTrue(messages.get(1).contains("currently loaned"));
        assertTrue(messages.get(2).contains("successfully"));
        assertTrue(messages.get(3).contains("already in this bundle"));
        assertFalse(bundle.isComplete());
    }

    @Test
    void addItemsToBundle_returnedItem_canJoinBundle() throws Exception {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E002");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        Loan loan = loans.addLoan("loan id/E005 b/U001 issued/01-10-26 due/02-10-26");
        assertTrue(bundleHandler.addItemsToBundle("add-to-bundle id/B001 E005")
                .getFirst().contains("currently loaned"));
        assertFalse(bundle.isComplete());

        loan.setReturned(true);
        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/B001 E005");

        assertEquals(List.of("Equipment ID E005 added to bundle B001 successfully."), messages);
        assertTrue(bundle.isComplete());
        assertFalse(equipmentHandler.getEquipmentById("E005").isLoaned());
    }

    @Test
    void addItemsToBundle_deletedLoan_releasesItemForBundle() throws Exception {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        loans.addLoan("loan id/E001 b/U001 issued/01-10-26 due/02-10-26");

        loans.deleteLoan("delete loan/L001");
        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001");

        assertTrue(messages.getFirst().contains("successfully"));
        assertEquals(List.of(equipmentHandler.getEquipmentById("E001")), bundle.getEquipmentList());
    }

    @Test
    void addItemsToBundle_allowsBatteryLensAdditionsAfterCompletion()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E002 E005");

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/B001 E003 E006");

        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertTrue(bundle.isComplete());
        assertEquals(5, bundle.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_allowsEquipmentInDifferentBundles()
            throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("add-bundle name/First Kit");
        Bundle second = bundleHandler.addBundle("add-bundle name/Second Kit");

        List<String> firstMessages = bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001");
        List<String> secondMessages = bundleHandler.addItemsToBundle("add-to-bundle id/B002 E001");

        assertTrue(firstMessages.getFirst().contains("successfully"));
        assertTrue(secondMessages.getFirst().contains("successfully"));
        assertEquals(1, first.getEquipmentList().size());
        assertEquals(1, second.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_unknownBundle_throwsException() {
        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001"));
    }

    @Test
    void bundleOperations_rejectBareAndWrongTypeIdentifiers() throws Exception {
        bundleHandler.addBundle("add-bundle name/Typed Kit");
        String[] commands = {
            "add-to-bundle id/001 E001",
            "add-to-bundle id/L001 E001",
            "add-to-bundle id/B001 001",
            "add-to-bundle id/B001 L001"
        };

        for (String command : commands) {
            assertThrows(BundleAdditionUnsuccessful.class,
                    () -> bundleHandler.addItemsToBundle(command), command);
        }
    }

    @Test
    void removeItemsFromBundle_removesMembersAndReportsEachResult() throws Exception {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E002 E005");

        List<String> messages = bundleHandler.removeItemsFromBundle("remove-from-bundle id/B001 E002 E999 E001");

        assertEquals(3, messages.size());
        assertTrue(messages.get(0).contains("removed from bundle B001 successfully"));
        assertTrue(messages.get(1).contains("not in bundle B001"));
        assertTrue(messages.get(2).contains("removed from bundle B001 successfully"));
        assertEquals(List.of(equipmentHandler.getEquipmentById("E005")), bundle.getEquipmentList());
        assertFalse(bundleHandler.containsEquipmentId("E001"));
        assertFalse(bundleHandler.containsEquipmentId("E002"));
    }

    @Test
    void removeItemsFromBundle_invalidCommandOrUnknownBundle_throwsException() {
        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.removeItemsFromBundle("remove-from-bundle id/B001"));
        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.removeItemsFromBundle("remove-from-bundle id/B999 E001"));
    }

    @Test
    void containsEquipmentId_checksAllBundlesWithoutChangingContents() throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("add-bundle name/First Kit");
        Bundle second = bundleHandler.addBundle("add-bundle name/Second Kit");
        assertFalse(bundleHandler.containsEquipmentId("E001"));
        bundleHandler.addItemsToBundle("add-to-bundle id/B002 E001 E002 E005");

        assertTrue(bundleHandler.containsEquipmentId("E001"));
        assertTrue(bundleHandler.containsEquipmentId("E002"));
        assertTrue(bundleHandler.containsEquipmentId("E005"));
        assertFalse(bundleHandler.containsEquipmentId("E003"));
        assertFalse(bundleHandler.containsEquipmentId("E999"));
        assertTrue(first.getEquipmentList().isEmpty());
        assertEquals(3, second.getEquipmentList().size());
        assertTrue(second.isComplete());
    }

    @Test
    void updateEquipment_bundledItem_preservesReferenceDetailsAndCompletion() throws Exception {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E002 E005");
        Equipment camera = equipmentHandler.getEquipmentById("E001");

        equipmentHandler.updateEquipment("update id/E001 i/Updated Camera cond/3");

        assertSame(camera, bundle.getEquipmentList().getFirst());
        assertEquals("Updated Camera", bundle.getEquipmentList().getFirst().getItemName());
        assertEquals(3, bundle.getEquipmentList().getFirst().getCondition());
        assertEquals("sensor/Full frame damage/SHUTTER_FAULT", camera.getDetails());
        assertEquals("Handle with care", camera.getRemarks());
        assertTrue(bundle.isComplete());
    }

    @Test
    void viewAllBundles_printsBundleAndContents() throws BundleAdditionUnsuccessful {
        bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E002");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        try {
            bundleHandler.viewAllBundles("list bundles");
        } finally {
            System.setOut(originalOutput);
        }

        String listing = output.toString(StandardCharsets.UTF_8);
        assertTrue(listing.contains("Travel Kit"));
        assertTrue(listing.contains("E001"));
        assertTrue(listing.contains("camera"));
        assertTrue(listing.contains("battery"));
    }

    @Test
    void getBundleLoanHistory_combinesActiveAndReturnedLoansInChronologicalOrder() throws Exception {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/B001 E001 E002");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        Loan firstLoan = loans.addLoan("loan id/E001 b/U111 issued/01-10-26 due/02-10-26");
        Loan secondLoan = loans.addLoan("loan id/E002 b/U222 issued/03-10-26 due/04-10-26");
        secondLoan.setReturned(true);

        List<Loan> history = bundleHandler.getBundleLoanHistory("history bundle/id/B001", loans);

        assertEquals(List.of(firstLoan, secondLoan), history);
        assertEquals("U111", history.getFirst().getBorrowerId());
        assertTrue(history.get(1).isReturned());
        assertEquals("B001", bundle.getBundleId());
    }

    @Test
    void getBundleLoanHistory_canSelectByNameAndHandlesEmptyHistory() throws Exception {
        bundleHandler.addBundle("add-bundle name/Travel Kit");

        List<Loan> history = bundleHandler.getBundleLoanHistory("history bundle/name/Travel Kit",
                new LoanHandler(equipmentHandler));

        assertTrue(history.isEmpty());
    }

    @Test
    void getBundleLoanHistory_rejectsInvalidAndUnknownSelectors() {
        LoanHandler loans = new LoanHandler(equipmentHandler);
        for (String input : new String[]{"history bundle/id/B000", "history bundle/id/01",
            "history bundle/name/", "history id/E001"}) {
            assertThrows(BundleHistoryUnsuccessful.class,
                    () -> bundleHandler.getBundleLoanHistory(input, loans));
        }
        assertThrows(BundleHistoryUnsuccessful.class,
                () -> bundleHandler.getBundleLoanHistory("history bundle/id/B001", loans));
    }
}
