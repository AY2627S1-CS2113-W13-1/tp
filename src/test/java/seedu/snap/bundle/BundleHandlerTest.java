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
        equipmentHandler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/8 sensor/Full frame"
                + " damage/SHUTTER_FAULT remarks/Handle with care");
        equipmentHandler.addEquipment("eqm-add name/Battery e/E002 type/battery cond/8 capacity/2000 mAh damage/NONE");
        equipmentHandler.addEquipment("eqm-add name/Battery 2 e/E003 type/battery cond/8");
        equipmentHandler.addEquipment("eqm-add name/Tripod e/E004 type/tripod cond/8 thread/Quarter inch damage/NONE");
        equipmentHandler.addEquipment("eqm-add name/Lens e/E005 type/lens cond/8 zoom/24 to 70 mm damage/NONE");
        equipmentHandler.addEquipment("eqm-add name/Lens 2 e/E006 type/lens cond/8");
        equipmentHandler.addEquipment("eqm-add name/Camera 2 e/E007 type/camera cond/8");
        equipmentHandler.addEquipment("eqm-add name/Tripod 2 e/E008 type/tripod cond/8");
        bundleHandler = new BundleHandler(equipmentHandler);
    }

    @Test
    void malformedCommands_rejectWithoutChangingBundles() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new BundleHandler(null));
        for (String input : new String[]{null, "bundle-create", "bundle-create wrong/Kit"}) {
            assertThrows(BundleAdditionUnsuccessful.class, () -> bundleHandler.addBundle(input));
        }
        for (String input : new String[]{null, "bundle-list extra"}) {
            assertThrows(BundleAdditionUnsuccessful.class, () -> bundleHandler.viewAllBundles(input));
        }
        for (String input : new String[]{null, "bundle-add-eqm", "bundle-add-eqm b/abc 001",
            "bundle-add-eqm b/000 001"}) {
            assertThrows(BundleAdditionUnsuccessful.class, () -> bundleHandler.addItemsToBundle(input));
        }
        LoanHandler loans = new LoanHandler(equipmentHandler);
        assertThrows(IllegalArgumentException.class,
                () -> bundleHandler.getBundleLoanHistory("bundle-history b/001", null));
        assertThrows(BundleHistoryUnsuccessful.class,
                () -> bundleHandler.getBundleLoanHistory("bundle-history name/Missing", loans));
        assertThrows(BundleHistoryUnsuccessful.class,
                () -> bundleHandler.getBundleLoanHistory("bundle-history wrong/001", loans));
        assertTrue(bundleHandler.getBundleList().isEmpty());
    }

    @Test
    void addBundle_assignsThreeDigitSessionIds() throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("bundle-create name/Travel Kit");
        Bundle second = bundleHandler.addBundle("bundle-create name/Studio Kit");

        assertEquals("B001", first.getBundleId());
        assertEquals("B002", second.getBundleId());
        assertEquals(2, bundleHandler.getBundleList().size());
    }

    @Test
    void addBundle_invalidName_throwsException() {
        for (String name : new String[]{"", "   ", "\u2003", "A".repeat(101)}) {
            assertThrows(BundleAdditionUnsuccessful.class,
                    () -> bundleHandler.addBundle("bundle-create name/" + name));
            assertTrue(bundleHandler.getBundleList().isEmpty());
        }
    }

    @Test
    void addBundle_duplicateName_throwsException() throws BundleAdditionUnsuccessful {
        bundleHandler.addBundle("bundle-create name/Travel Kit");

        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.addBundle("bundle-create name/Travel Kit"));
        assertEquals(1, bundleHandler.getBundleList().size());
    }

    @Test
    void addBundle_nonDefaultLocale_keepsAsciiIds() throws BundleAdditionUnsuccessful {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));

            Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");

            assertEquals("B001", bundle.getBundleId());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void addItemsToBundle_validComposition_marksBundleComplete()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");

        List<String> messages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002 E005");

        assertEquals(3, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertTrue(bundle.isComplete());
    }

    @Test
    void bundleOperations_lowercaseIds_normaliseStoredValues() throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");

        List<String> messages = bundleHandler.addItemsToBundle("bundle-add-eqm b/b001 e001 e002 e005");

        assertEquals(3, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.contains("B001")));
        assertEquals("B001", bundle.getBundleId());
        assertEquals(List.of("E001", "E002", "E005"),
                bundle.getEquipmentList().stream().map(Equipment::getItemId).toList());
    }

    @Test
    void addItemsToBundle_rejectsCategoryLimitsAndReportsEachResult()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");

        List<String> messages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E007 E004 E008");

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
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        loans.addLoan("loan-add e/E006 u/U001 issued/01-10-26 due/02-10-26");

        List<String> messages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E999 E006 E001 E001");

        assertTrue(messages.get(0).contains("does not exist"));
        assertTrue(messages.get(1).contains("currently loaned"));
        assertTrue(messages.get(2).contains("successfully"));
        assertTrue(messages.get(3).contains("already in this bundle"));
        assertFalse(bundle.isComplete());
    }

    @Test
    void addItemsToBundle_returnedItem_canJoinBundle() throws Exception {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        Loan loan = loans.addLoan("loan-add e/E005 u/U001 issued/01-10-26 due/02-10-26");
        assertTrue(bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E005")
                .getFirst().contains("currently loaned"));
        assertFalse(bundle.isComplete());

        loan.setReturned(true);
        List<String> messages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E005");

        assertEquals(List.of("Equipment ID E005 added to bundle B001 successfully."), messages);
        assertTrue(bundle.isComplete());
        assertFalse(equipmentHandler.getEquipmentById("E005").isLoaned());
    }

    @Test
    void addItemsToBundle_deletedLoan_releasesItemForBundle() throws Exception {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        loans.addLoan("loan-add e/E001 u/U001 issued/01-10-26 due/02-10-26");

        loans.deleteLoan("loan-delete l/L001");
        List<String> messages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001");

        assertTrue(messages.getFirst().contains("successfully"));
        assertEquals(List.of(equipmentHandler.getEquipmentById("E001")), bundle.getEquipmentList());
    }

    @Test
    void addItemsToBundle_allowsBatteryLensAdditionsAfterCompletion()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002 E005");

        List<String> messages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E003 E006");

        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertTrue(bundle.isComplete());
        assertEquals(5, bundle.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_allowsEquipmentInDifferentBundles()
            throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("bundle-create name/First Kit");
        Bundle second = bundleHandler.addBundle("bundle-create name/Second Kit");

        List<String> firstMessages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001");
        List<String> secondMessages = bundleHandler.addItemsToBundle("bundle-add-eqm b/B002 E001");

        assertTrue(firstMessages.getFirst().contains("successfully"));
        assertTrue(secondMessages.getFirst().contains("successfully"));
        assertEquals(1, first.getEquipmentList().size());
        assertEquals(1, second.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_unknownBundle_throwsException() {
        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001"));
    }

    @Test
    void bundleOperations_rejectBareAndWrongTypeIdentifiers() throws Exception {
        bundleHandler.addBundle("bundle-create name/Typed Kit");
        String[] commands = {
            "bundle-add-eqm b/001 E001",
            "bundle-add-eqm b/L001 E001",
            "bundle-add-eqm b/B001 001",
            "bundle-add-eqm b/B001 L001"
        };

        for (String command : commands) {
            assertThrows(BundleAdditionUnsuccessful.class,
                    () -> bundleHandler.addItemsToBundle(command), command);
        }
    }

    @Test
    void removeItemsFromBundle_removesMembersAndReportsEachResult() throws Exception {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002 E005");

        List<String> messages = bundleHandler.removeItemsFromBundle("bundle-remove-eqm b/B001 E002 E999 E001");

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
                () -> bundleHandler.removeItemsFromBundle("bundle-remove-eqm b/B001"));
        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.removeItemsFromBundle("bundle-remove-eqm b/B999 E001"));
    }

    @Test
    void containsEquipmentId_checksAllBundlesWithoutChangingContents() throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("bundle-create name/First Kit");
        Bundle second = bundleHandler.addBundle("bundle-create name/Second Kit");
        assertFalse(bundleHandler.containsEquipmentId("E001"));
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B002 E001 E002 E005");

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
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002 E005");
        Equipment camera = equipmentHandler.getEquipmentById("E001");

        equipmentHandler.updateEquipment("eqm-update e/E001 name/Updated Camera cond/3");

        assertSame(camera, bundle.getEquipmentList().getFirst());
        assertEquals("Updated Camera", bundle.getEquipmentList().getFirst().getItemName());
        assertEquals(3, bundle.getEquipmentList().getFirst().getCondition());
        assertEquals("sensor/Full frame damage/SHUTTER_FAULT", camera.getDetails());
        assertEquals("Handle with care", camera.getRemarks());
        assertTrue(bundle.isComplete());
    }

    @Test
    void viewAllBundles_printsBundleAndContents() throws BundleAdditionUnsuccessful {
        bundleHandler.addBundle("bundle-create name/Travel Kit");
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

        try {
            bundleHandler.viewAllBundles("bundle-list");
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
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Travel Kit");
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002");
        LoanHandler loans = new LoanHandler(equipmentHandler);
        Loan firstLoan = loans.addLoan("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        Loan secondLoan = loans.addLoan("loan-add e/E002 u/U222 issued/03-10-26 due/04-10-26");
        secondLoan.setReturned(true);

        List<Loan> history = bundleHandler.getBundleLoanHistory("bundle-history b/B001", loans);

        assertEquals(List.of(firstLoan, secondLoan), history);
        assertEquals("U111", history.getFirst().getBorrowerId());
        assertTrue(history.get(1).isReturned());
        assertEquals("B001", bundle.getBundleId());
    }

    @Test
    void getBundleLoanHistory_canSelectByNameAndHandlesEmptyHistory() throws Exception {
        bundleHandler.addBundle("bundle-create name/Travel Kit");

        List<Loan> history = bundleHandler.getBundleLoanHistory("bundle-history name/Travel Kit",
                new LoanHandler(equipmentHandler));

        assertTrue(history.isEmpty());
    }

    @Test
    void getBundleLoanHistory_rejectsInvalidAndUnknownSelectors() {
        LoanHandler loans = new LoanHandler(equipmentHandler);
        for (String input : new String[]{"bundle-history b/B000", "bundle-history b/01",
            "bundle-history name/", "eqm-loan-history e/E001"}) {
            assertThrows(BundleHistoryUnsuccessful.class,
                    () -> bundleHandler.getBundleLoanHistory(input, loans));
        }
        assertThrows(BundleHistoryUnsuccessful.class,
                () -> bundleHandler.getBundleLoanHistory("bundle-history b/B001", loans));
    }
}
