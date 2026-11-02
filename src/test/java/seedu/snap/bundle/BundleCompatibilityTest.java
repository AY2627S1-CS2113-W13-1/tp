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
        equipmentHandler.addEquipment("eqm-add name/Camera e/E001 type/camera cond/1 sensor/APS-C"
                + " mount/Canon RF thread/Quarter inch battery/LP-E17");
        equipmentHandler.addEquipment("eqm-add name/Compatible Lens e/E002 type/lens cond/1 zoom/18-45 mm"
                + " mount/canon rf sensor/aps-c");
        equipmentHandler.addEquipment("eqm-add name/Compatible Battery e/E003 type/battery cond/1"
                + " capacity/1000 mAh battery/lp-e17");
        equipmentHandler.addEquipment("eqm-add name/Compatible Tripod e/E004 type/tripod cond/1"
                + " thread/quarter inch");
        equipmentHandler.addEquipment("eqm-add name/Wrong Mount Lens e/E005 type/lens cond/1"
                + " mount/Nikon Z sensor/APS-C");
        equipmentHandler.addEquipment("eqm-add name/Wrong Sensor Lens e/E006 type/lens cond/1"
                + " mount/Canon RF sensor/Full frame");
        equipmentHandler.addEquipment("eqm-add name/Wrong Battery e/E007 type/battery cond/1"
                + " battery/NP-FZ100");
        equipmentHandler.addEquipment("eqm-add name/Wrong Tripod e/E008 type/tripod cond/1"
                + " thread/Three eighth inch");
        equipmentHandler.addEquipment("eqm-add name/Unknown Lens e/E009 type/lens cond/1");
        bundleHandler = new BundleHandler(equipmentHandler);
    }

    @Test
    void addItemsToBundle_compatibleEquipment_addsEveryItem()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Compatible Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E002 E003 E004");

        assertEquals(4, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertEquals(List.of("E001", "E002", "E003", "E004"), equipmentIds(bundle));
        assertTrue(bundle.isComplete());
    }

    private static Stream<Arguments> incompatibleEquipmentIds() {
        return Stream.of(
                Arguments.of("E005"),
                Arguments.of("E006"),
                Arguments.of("E007"),
                Arguments.of("E008"));
    }

    @ParameterizedTest
    @MethodSource("incompatibleEquipmentIds")
    void addItemsToBundle_incompatibleEquipment_rejectsCandidate(String candidateId)
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Incompatible Kit");
        bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001");

        List<String> messages =
                bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 " + candidateId);

        assertEquals(List.of(
                "Equipment ID " + candidateId
                        + " was not added: equipment is incompatible with equipment ID E001."),
                messages);
        assertEquals(List.of("E001"), equipmentIds(bundle));
        assertFalse(bundle.isComplete());
    }

    @Test
    void addItemsToBundle_incompatibleItemInSameCommand_reportsEachResult()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Mixed Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E005 E003");

        assertTrue(messages.get(0).contains("successfully"));
        assertEquals(
                "Equipment ID E005 was not added: equipment is incompatible with equipment ID E001.",
                messages.get(1));
        assertTrue(messages.get(2).contains("successfully"));
        assertEquals(List.of("E001", "E003"), equipmentIds(bundle));
    }

    @Test
    void addItemsToBundle_cameraAddedAfterIncompatibleLens_rejectsCamera()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Reverse Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E005 E001");

        assertTrue(messages.get(0).contains("successfully"));
        assertEquals(
                "Equipment ID E001 was not added: equipment is incompatible with equipment ID E005.",
                messages.get(1));
        assertEquals(List.of("E005"), equipmentIds(bundle));
    }

    @Test
    void addItemsToBundle_unknownCompatibility_allowsAddition()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Unknown Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E001 E009");

        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertEquals(List.of("E001", "E009"), equipmentIds(bundle));
    }

    @Test
    void addItemsToBundle_unrelatedEquipmentTypes_allowsAddition()
            throws BundleAdditionUnsuccessful {
        Bundle bundle = bundleHandler.addBundle("bundle-create name/Camera-free Kit");

        List<String> messages =
                bundleHandler.addItemsToBundle("bundle-add-eqm b/B001 E002 E003");

        assertTrue(messages.stream().allMatch(message -> message.contains("successfully")));
        assertEquals(List.of("E002", "E003"), equipmentIds(bundle));
    }

    /** Returns the equipment IDs in their bundle insertion order. */
    private List<String> equipmentIds(Bundle bundle) {
        return bundle.getEquipmentList().stream()
                .map(Equipment::getItemId)
                .toList();
    }
}

// @@author
