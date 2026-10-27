package seedu.snap.bundle;

// @@author Rohita-29

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.exceptions.BundleAdditionUnsuccessful;
import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Tests compatibility validation when equipment is added to bundles. */
class BundleCompatibilityTest {
    private EquipmentHandler equipmentHandler;
    private BundleHandler bundleHandler;

    @BeforeEach
    void setUp() throws EquipmentAdditionUnsuccessful {
        equipmentHandler = new EquipmentHandler();
        equipmentHandler.addEquipment("add i/Camera id/001 type/camera cond/1 sensor/APS-C"
                + " mount/Canon RF thread/Quarter inch battery/LP-E17");
        equipmentHandler.addEquipment("add i/Compatible Lens id/002 type/lens cond/1 zoom/18-45 mm"
                + " mount/canon rf sensor/aps-c");
        equipmentHandler.addEquipment("add i/Compatible Battery id/003 type/battery cond/1"
                + " capacity/1000 mAh battery/lp-e17");
        equipmentHandler.addEquipment("add i/Compatible Tripod id/004 type/tripod cond/1"
                + " thread/quarter inch");
        equipmentHandler.addEquipment("add i/Wrong Mount Lens id/005 type/lens cond/1"
                + " mount/Nikon Z sensor/APS-C");
        equipmentHandler.addEquipment("add i/Wrong Sensor Lens id/006 type/lens cond/1"
                + " mount/Canon RF sensor/Full frame");
        equipmentHandler.addEquipment("add i/Wrong Battery id/007 type/battery cond/1"
                + " battery/NP-FZ100");
        equipmentHandler.addEquipment("add i/Wrong Tripod id/008 type/tripod cond/1"
                + " thread/Three eighth inch");
        equipmentHandler.addEquipment("add i/Unknown Lens id/009 type/lens cond/1");
        bundleHandler = new BundleHandler(equipmentHandler);
    }

    @Test
    void addItemsToBundle_compatibleEquipment_addsEveryItem()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Compatible Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("add-to-bundle id/001 001 002 003 004");

        assertEquals(4, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertEquals(List.of("001", "002", "003", "004"), equipmentIds(bundle));
        assertTrue(bundle.isComplete());
    }

    private static Stream<Arguments> incompatibleEquipmentIds() {
        return Stream.of(
                Arguments.of("005"),
                Arguments.of("006"),
                Arguments.of("007"),
                Arguments.of("008"));
    }

    @ParameterizedTest
    @MethodSource("incompatibleEquipmentIds")
    void addItemsToBundle_incompatibleEquipment_rejectsCandidate(String candidateId)
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Incompatible Kit");
        bundleHandler.addItemsToBundle("add-to-bundle id/001 001");

        List<String> messages =
                bundleHandler.addItemsToBundle("add-to-bundle id/001 " + candidateId);

        assertEquals(List.of(
                "Equipment ID " + candidateId
                        + " was not added: equipment is incompatible with equipment ID 001."),
                messages);
        assertEquals(List.of("001"), equipmentIds(bundle));
        assertFalse(bundle.isComplete());
    }

    @Test
    void addItemsToBundle_incompatibleItemInSameCommand_reportsEachResult()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Mixed Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("add-to-bundle id/001 001 005 003");

        assertTrue(messages.get(0).contains("successfully"));
        assertEquals(
                "Equipment ID 005 was not added: equipment is incompatible with equipment ID 001.",
                messages.get(1));
        assertTrue(messages.get(2).contains("successfully"));
        assertEquals(List.of("001", "003"), equipmentIds(bundle));
    }

    @Test
    void addItemsToBundle_cameraAddedAfterIncompatibleLens_rejectsCamera()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Reverse Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("add-to-bundle id/001 005 001");

        assertTrue(messages.get(0).contains("successfully"));
        assertEquals(
                "Equipment ID 001 was not added: equipment is incompatible with equipment ID 005.",
                messages.get(1));
        assertEquals(List.of("005"), equipmentIds(bundle));
    }

    @Test
    void addItemsToBundle_unknownCompatibility_allowsAddition()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Unknown Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("add-to-bundle id/001 001 009");

        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertEquals(List.of("001", "009"), equipmentIds(bundle));
    }

    @Test
    void addItemsToBundle_unrelatedEquipmentTypes_allowsAddition()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("add-bundle name/Camera-free Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("add-to-bundle id/001 002 003");

        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertEquals(List.of("002", "003"), equipmentIds(bundle));
    }

    /** Returns the equipment IDs in their bundle insertion order. */
    private List<String> equipmentIds(Bundle bundle) {
        return bundle.getEquipmentList().stream()
                .map(Equipment::getItemId)
                .toList();
    }
}

// @@author
