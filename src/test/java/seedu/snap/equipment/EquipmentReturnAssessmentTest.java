package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.bundle.Bundle;
import seedu.snap.bundle.BundleHandler;
import seedu.snap.loan.LoanHandler;

/** Verifies return assessments preserve equipment references, subtype details, and stored serviceability. */
class EquipmentReturnAssessmentTest {
    @TempDir
    private Path temporaryDirectory;

    /** Supplies a distinct attribute and every supported damage value for each equipment subtype. */
    private static Stream<Arguments> supportedDamageValues() {
        return Stream.of(
                Arguments.of("camera", "sensor/Full frame", "NONE"),
                Arguments.of("camera", "sensor/Full frame", "SENSOR_DAMAGE"),
                Arguments.of("camera", "sensor/Full frame", "SHUTTER_FAULT"),
                Arguments.of("lens", "zoom/24 to 70 mm", "NONE"),
                Arguments.of("lens", "zoom/24 to 70 mm", "SCRATCHED_GLASS"),
                Arguments.of("lens", "zoom/24 to 70 mm", "AUTOFOCUS_FAULT"),
                Arguments.of("battery", "capacity/2000 mAh", "NONE"),
                Arguments.of("battery", "capacity/2000 mAh", "SWELLING"),
                Arguments.of("battery", "capacity/2000 mAh", "CONTACT_DAMAGE"),
                Arguments.of("tripod", "thread/Quarter inch", "NONE"),
                Arguments.of("tripod", "thread/Quarter inch", "BENT_LEG"),
                Arguments.of("tripod", "thread/Quarter inch", "HEAD_FAULT"));
    }

    /** Supplies equipment containing every compatibility field affected by a return. */
    private static Stream<Arguments> compatibilityAttributes() {
        return Stream.of(
                Arguments.of(
                        "camera",
                        "add i/Camera id/001 type/camera cond/7 "
                                + "sensor/Full frame mount/Canon RF "
                                + "thread/Quarter inch battery/LP-E17",
                        "SHUTTER_FAULT",
                        "sensor/Full frame mount/Canon RF "
                                + "thread/Quarter inch battery/LP-E17 "
                                + "damage/SHUTTER_FAULT"),
                Arguments.of(
                        "lens",
                        "add i/Lens id/001 type/lens cond/7 "
                                + "zoom/24-70 mm mount/Canon RF "
                                + "sensor/Full frame",
                        "SCRATCHED_GLASS",
                        "zoom/24-70 mm mount/Canon RF "
                                + "sensor/Full frame "
                                + "damage/SCRATCHED_GLASS"),
                Arguments.of(
                        "battery",
                        "add i/Battery id/001 type/battery cond/7 "
                                + "capacity/2130 mAh battery/LP-E17",
                        "CONTACT_DAMAGE",
                        "capacity/2130 mAh battery/LP-E17 "
                                + "damage/CONTACT_DAMAGE"));
    }

    @ParameterizedTest
    @MethodSource("compatibilityAttributes")
    void assessment_compatibilityAttributes_survivePrepareApplyAndRestart(
            String type,
            String command,
            String damage,
            String expectedDetails)
            throws Exception {
        Path file = temporaryDirectory.resolve(
                type + "-compatibility.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(command);

        Equipment assessed = handler.prepareReturnAssessment(
                original,
                false,
                damage,
                "Checked after return");

        assertEquals(expectedDetails, assessed.getDetails());

        handler.saveReturnAssessment(original, assessed);
        original.applyReturnAssessment(assessed);

        assertEquals(expectedDetails, original.getDetails());
        assertFalse(original.isServiceable());

        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        Equipment restored =
                restarted.getEquipmentList().getFirst();

        assertEquals(expectedDetails, restored.getDetails());
        assertEquals("Checked after return", restored.getRemarks());
        assertFalse(restored.isServiceable());
    }

    @ParameterizedTest
    @MethodSource("supportedDamageValues")
    void assessment_allSubtypeDamageValues_preserveReferencesAndAttributes(String type, String attribute, String damage)
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Original id/001 type/" + type + " cond/7 "
                + attribute + " remarks/Before return");
        BundleHandler bundles = new BundleHandler(handler);
        Bundle bundle = bundles.addBundle("add-bundle name/Session kit");
        bundles.addItemsToBundle("add-to-bundle id/001 001");
        original.setLoanId("123");

        Equipment assessed = handler.prepareReturnAssessment(original, false, damage, "Return assessment");

        assertNotSame(original, assessed);
        assertEquals(original.getClass(), assessed.getClass());
        assertEquals(attribute + " damage/" + damage, assessed.getDetails());
        assertTrue(original.isServiceable());
        assertEquals("Before return", original.getRemarks());
        handler.saveReturnAssessment(original, assessed);
        assertTrue(original.isServiceable());
        assertSame(original, handler.getEquipmentList().getFirst());

        original.applyReturnAssessment(assessed);

        assertSame(original, bundle.getEquipmentList().getFirst());
        assertFalse(original.isServiceable());
        assertTrue(original.isLoaned());
        assertEquals("123", original.getLoanId());
        assertEquals(attribute + " damage/" + damage, original.getDetails());
        assertEquals("Return assessment", original.getRemarks());
        assertSame(original, handler.updateEquipment("update id/001 i/Updated cond/8"));
        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        Equipment restored = restarted.getEquipmentList().getFirst();
        assertEquals(original.getClass(), restored.getClass());
        assertFalse(restored.isServiceable());
        assertEquals(attribute + " damage/" + damage, restored.getDetails());
        assertEquals("Return assessment", restored.getRemarks());
        assertEquals("Updated", restored.getItemName());
        assertEquals(8, restored.getCondition());
    }

    /** Supplies damage belonging to a different subtype; OTHER is not defined by any current enum. */
    private static Stream<Arguments> invalidDamageValues() {
        return Stream.of(
                Arguments.of("camera", "SCRATCHED_GLASS"),
                Arguments.of("lens", "SWELLING"),
                Arguments.of("battery", "BENT_LEG"),
                Arguments.of("tripod", "SENSOR_DAMAGE"),
                Arguments.of("camera", "OTHER"),
                Arguments.of("lens", "OTHER"),
                Arguments.of("battery", "OTHER"),
                Arguments.of("tripod", "OTHER"));
    }

    @ParameterizedTest
    @MethodSource("invalidDamageValues")
    void assessment_invalidDamage_rejectsWithoutChangingEquipment(String type, String damage) throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment original = handler.addEquipment("add i/Original id/001 type/" + type + " cond/7");
        String details = original.getDetails();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> handler.prepareReturnAssessment(original, false, damage, "Not committed"));

        assertTrue(exception.getMessage().contains("Invalid damage for " + type));
        assertEquals(details, original.getDetails());
        assertEquals("", original.getRemarks());
        assertTrue(original.isServiceable());
    }

    @Test
    void assessment_remarksBoundary_rejectsLongOrMultilineValues() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/7");
        String longest = "r".repeat(Equipment.MAX_REMARKS_LENGTH);

        assertEquals(longest, handler.prepareReturnAssessment(original, true, "none", longest).getRemarks());
        assertEquals(longest, handler.prepareReturnAssessment(original, true, "NONE", "  " + longest + "  ")
                .getRemarks());
        for (String remarks : new String[]{longest + "r", "two\nlines", "two\rlines", "two\u2028lines", "\nvalid"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> handler.prepareReturnAssessment(original, true, "NONE", remarks));
        }
        assertThrows(IllegalArgumentException.class,
                () -> handler.prepareReturnAssessment(original, true, "NONE", null));
        assertTrue(original.isServiceable());
        assertEquals("", original.getRemarks());
    }

    @Test
    void assessment_remarksWithCommandPrefixes_roundTripWithoutChangingStructuralFields() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/7 sensor/Full frame");
        String remarks = "Recheck i/Spare id/002 type/lens cond/1 remarks/Label serviceable/no damage/OTHER";
        Equipment assessed = handler.prepareReturnAssessment(original, false, "SHUTTER_FAULT", remarks);

        handler.saveReturnAssessment(original, assessed);
        original.applyReturnAssessment(assessed);
        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        Equipment restored = restarted.getEquipmentList().getFirst();

        assertEquals(remarks, restored.getRemarks());
        assertEquals("Camera", restored.getItemName());
        assertEquals("001", restored.getItemId());
        assertEquals("camera", restored.getType());
        assertEquals(7, restored.getCondition());
        assertEquals("sensor/Full frame damage/SHUTTER_FAULT", restored.getDetails());
        assertFalse(restored.isServiceable());
    }

    @Test
    void applyAssessment_differentIdentityOrAttribute_rejectsWithoutMutation() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Camera original = (Camera) handler.addEquipment(
                "add i/Camera id/001 type/camera cond/7 sensor/Full frame");
        List<Equipment> invalid = List.of(
                new Lens("Camera", "001", 7),
                new Camera("Camera", "002", 7, "Full frame", Camera.CameraDamage.SENSOR_DAMAGE),
                new Camera("Camera", "001", 7, "Crop sensor", Camera.CameraDamage.SENSOR_DAMAGE),
                new Camera("Different name", "001", 7, "Full frame", Camera.CameraDamage.SENSOR_DAMAGE));

        for (Equipment assessed : invalid) {
            assertThrows(IllegalArgumentException.class, () -> original.applyReturnAssessment(assessed));
            assertEquals(Camera.CameraDamage.NONE, original.getDamage());
            assertEquals("Full frame", original.getSensorFormat());
            assertTrue(original.isServiceable());
        }
    }

    @Test
    void storage_legacyAndExplicitServiceability_restoreFlagsAndFreeText() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.write(file, List.of(
                "add i/Legacy serviceable/no id/001 type/camera cond/7 remarks/serviceable/no",
                "serviceable/yes\tadd i/Ready id/002 type/lens cond/7 zoom/serviceable/no",
                "serviceable/no\tadd i/Repair id/003 type/battery cond/7 remarks/check serviceable/yes"));
        List<Equipment> loaded = EquipmentStorage.load(file);

        assertTrue(loaded.get(0).isServiceable());
        assertTrue(loaded.get(1).isServiceable());
        assertFalse(loaded.get(2).isServiceable());
        EquipmentStorage.save(file, loaded);
        List<Equipment> restarted = EquipmentStorage.load(file);
        assertEquals("Legacy serviceable/no", restarted.get(0).getItemName());
        assertEquals("serviceable/no", restarted.get(0).getRemarks());
        assertEquals("zoom/serviceable/no damage/NONE", restarted.get(1).getDetails());
        assertFalse(restarted.get(2).isServiceable());
        assertEquals("check serviceable/yes", restarted.get(2).getRemarks());
    }

    @Test
    void storage_invalidServiceability_reportsRecordAndPreservesInventory() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/7");
        for (String prefix : new String[]{"serviceable/maybe\t", "serviceable/no "}) {
            Files.writeString(file, prefix + "add i/Camera id/001 type/camera cond/7");

            IOException exception = assertThrows(IOException.class, handler::loadFromStorage);

            assertTrue(exception.getMessage().contains("line 1"));
            assertSame(original, handler.getEquipmentList().getFirst());
            assertTrue(original.isServiceable());
        }
    }

    @Test
    void saveAssessment_failedReplacement_preservesOriginalEquipmentAndExistingData() throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/7 remarks/Original");
        Equipment assessed = handler.prepareReturnAssessment(original, false, "SENSOR_DAMAGE", "After return");
        Files.delete(file);
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep equipment data");

        assertThrows(IOException.class, () -> handler.saveReturnAssessment(original, assessed));

        assertTrue(original.isServiceable());
        assertEquals("Original", original.getRemarks());
        assertEquals(Camera.CameraDamage.NONE, ((Camera) original).getDamage());
        assertSame(original, handler.getEquipmentList().getFirst());
        assertEquals("keep equipment data", Files.readString(sentinel));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("equipment-")));
        }
    }

    @Test
    void unserviceableEquipment_listingAndFilters_doNotReportAvailableOrLoaned() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/7");
        original.applyReturnAssessment(handler.prepareReturnAssessment(original, false, "SENSOR_DAMAGE", "Repair"));
        LoanHandler loans = new LoanHandler(handler);

        assertTrue(EquipmentFilter.parse("filter status/available").apply(handler.getEquipmentList(), loans).isEmpty());
        assertTrue(EquipmentFilter.parse("filter status/loaned").apply(handler.getEquipmentList(), loans).isEmpty());
        assertEquals(List.of(original), EquipmentFilter.parse("filter id/001")
                .apply(handler.getEquipmentList(), loans));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        try (PrintStream replacement = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            handler.viewAllEquipment();
        } finally {
            System.setOut(previous);
        }
        String listing = output.toString(StandardCharsets.UTF_8);
        assertTrue(listing.contains("Unserviceable"));
        assertFalse(listing.contains("Available"));
        assertTrue(EquipmentFilter.parse("filter status/loaned").matches(original, true));
    }

    @Test
    void unserviceableEquipment_bundleAddition_rejectsAndLeavesBundleEmpty() throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment original = handler.addEquipment("add i/Camera id/001 type/camera cond/7");
        original.applyReturnAssessment(handler.prepareReturnAssessment(original, false, "SENSOR_DAMAGE", "Repair"));
        BundleHandler bundles = new BundleHandler(handler);
        Bundle bundle = bundles.addBundle("add-bundle name/Session kit");

        List<String> messages = bundles.addItemsToBundle("add-to-bundle id/001 001");

        assertEquals(List.of("Equipment ID 001 was not added: equipment is unserviceable."), messages);
        assertTrue(bundle.getEquipmentList().isEmpty());
    }
}
