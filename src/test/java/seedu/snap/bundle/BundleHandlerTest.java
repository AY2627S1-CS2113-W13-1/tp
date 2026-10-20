package seedu.snap.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Tests bundle command parsing, validation, partial additions, and listing. */
class BundleHandlerTest {
    private EquipmentHandler equipmentHandler;
    private BundleHandler bundleHandler;

    @BeforeEach
    void setUp() throws EquipmentAdditionUnsuccessful {
        equipmentHandler = new EquipmentHandler();
        equipmentHandler.addEquipment("add i/Camera id/001 type/camera cond/8");
        equipmentHandler.addEquipment("add i/Battery id/002 type/battery cond/8");
        equipmentHandler.addEquipment("add i/Battery 2 id/003 type/battery cond/8");
        equipmentHandler.addEquipment("add i/Tripod id/004 type/tripod cond/8");
        equipmentHandler.addEquipment("add i/Lens id/005 type/lens cond/8");
        equipmentHandler.addEquipment("add i/Lens 2 id/006 type/lens cond/8");
        equipmentHandler.addEquipment("add i/Camera 2 id/007 type/camera cond/8");
        equipmentHandler.addEquipment("add i/Tripod 2 id/008 type/tripod cond/8");
        bundleHandler = new BundleHandler(equipmentHandler);
    }

    @Test
    void addBundle_assignsThreeDigitSessionIds() throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("add-bundle name/Travel Kit");
        Bundle second = bundleHandler.addBundle("add-bundle name/Studio Kit");

        assertEquals("001", first.getBundleId());
        assertEquals("002", second.getBundleId());
        assertEquals(2, bundleHandler.getBundleList().size());
    }

    @Test
    void addBundle_invalidName_throwsException() {
        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.addBundle("add-bundle name/"));
    }

    @Test
    void addItemsToBundle_validComposition_marksBundleComplete()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/001 001 002 005");

        assertEquals(3, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertTrue(bundle.isComplete());
    }

    @Test
    void addItemsToBundle_rejectsCategoryLimitsAndReportsEachResult()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/001 001 007 004 008");

        assertEquals(4, messages.size());
        assertTrue(messages.get(0).contains("successfully"));
        assertTrue(messages.get(1).contains("only one camera"));
        assertTrue(messages.get(2).contains("successfully"));
        assertTrue(messages.get(3).contains("at most one tripod"));
        assertEquals(2, bundle.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_rejectsUnknownLoanedAndDuplicateItems()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        equipmentHandler.getEquipmentById("006").markLoaned();

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/001 999 006 001 001");

        assertTrue(messages.get(0).contains("does not exist"));
        assertTrue(messages.get(1).contains("currently loaned"));
        assertTrue(messages.get(2).contains("successfully"));
        assertTrue(messages.get(3).contains("already in this bundle"));
        assertFalse(bundle.isComplete());
    }

    @Test
    void addItemsToBundle_allowsBatteryLensAdditionsAfterCompletion()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/001 001 002 005");

        List<String> messages = bundleHandler.addItemsToBundle("add-to-bundle id/001 003 006");

        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertTrue(bundle.isComplete());
        assertEquals(5, bundle.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_allowsEquipmentInDifferentBundles()
            throws BundleAdditionUnsuccessful {
        Bundle first = bundleHandler.addBundle("add-bundle name/First Kit");
        Bundle second = bundleHandler.addBundle("add-bundle name/Second Kit");

        List<String> firstMessages = bundleHandler.addItemsToBundle("add-to-bundle id/001 001");
        List<String> secondMessages = bundleHandler.addItemsToBundle("add-to-bundle id/002 001");

        assertTrue(firstMessages.getFirst().contains("successfully"));
        assertTrue(secondMessages.getFirst().contains("successfully"));
        assertEquals(1, first.getEquipmentList().size());
        assertEquals(1, second.getEquipmentList().size());
    }

    @Test
    void addItemsToBundle_unknownBundle_throwsException() {
        assertThrows(BundleAdditionUnsuccessful.class,
                () -> bundleHandler.addItemsToBundle("add-to-bundle id/001 001"));
    }

    @Test
    void viewAllBundles_printsBundleAndContents() throws BundleAdditionUnsuccessful {
        bundleHandler.addBundle("add-bundle name/Travel Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/001 001 002");
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
        assertTrue(listing.contains("001"));
        assertTrue(listing.contains("camera"));
        assertTrue(listing.contains("battery"));
    }
}
