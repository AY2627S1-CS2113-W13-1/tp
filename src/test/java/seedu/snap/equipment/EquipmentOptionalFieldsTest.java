package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Verifies independently omitted equipment fields use documented defaults and preserve validated values. */
class EquipmentOptionalFieldsTest {
    private static final String[] WHITESPACE = {" ", "\t", "\u00a0", "\u2007", "\u202f", "\u2003"};
    private static final String REMARKS = "Serial label checked; keep with case.";

    @TempDir
    private Path temporaryDirectory;

    /** Describes the distinct field, value, and damage accepted by one equipment subtype. */
    private record EquipmentType(String type, String marker, String value, String damage) {
    }

    /** Supplies every supported equipment subtype with a non-default attribute and damage value. */
    private static Stream<EquipmentType> equipmentTypes() {
        return Stream.of(
                new EquipmentType("camera", "sensor/", "Full frame", "SENSOR_DAMAGE"),
                new EquipmentType("lens", "zoom/", "24 to 70 mm", "SCRATCHED_GLASS"),
                new EquipmentType("battery", "capacity/", "2000 mAh", "SWELLING"),
                new EquipmentType("tripod", "thread/", "Quarter inch", "BENT_LEG"));
    }

    /** Supplies all eight combinations of omitted or supplied attribute, damage, and remarks fields. */
    private static Stream<Arguments> omissionCombinations() {
        return equipmentTypes().flatMap(type -> IntStream.range(0, 8)
                .mapToObj(mask -> Arguments.of(type, (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0)));
    }

    @ParameterizedTest
    @MethodSource("omissionCombinations")
    void addEquipment_optionalCombinations_useDefaultsOnlyForOmittedFields(EquipmentType type,
            boolean attributeSupplied, boolean damageSupplied, boolean remarksSupplied) throws Exception {
        String command = command(type, "001");
        if (attributeSupplied) {
            command += " " + type.marker() + type.value();
        }
        if (damageSupplied) {
            command += " damage/" + type.damage().toLowerCase(Locale.ROOT);
        }
        if (remarksSupplied) {
            command += " remarks/" + REMARKS;
        }

        Equipment item = new EquipmentHandler().addEquipment(command);

        assertEquals(type.type(), item.getType());
        assertEquals(type.marker() + (attributeSupplied ? type.value() : "unspecified")
                + " damage/" + (damageSupplied ? type.damage() : "NONE"), item.getDetails());
        assertEquals(remarksSupplied ? REMARKS : "", item.getRemarks());
        assertEquals("001", item.getItemId());
        assertEquals(7, item.getCondition());
    }

    @ParameterizedTest
    @MethodSource("equipmentTypes")
    void addEquipment_optionalUnicodeEdges_trimValuesAndPreserveInternalWhitespace(EquipmentType type)
            throws Exception {
        for (String whitespace : WHITESPACE) {
            String value = "First" + whitespace + "second";
            Equipment item = new EquipmentHandler().addEquipment(command(type, "001") + " " + type.marker()
                    + whitespace + value + whitespace + " damage/" + whitespace + type.damage() + whitespace
                    + " remarks/" + whitespace + REMARKS + whitespace);

            assertEquals(type.marker() + value + " damage/" + type.damage(), item.getDetails());
            assertEquals(REMARKS, item.getRemarks());
        }
    }

    @ParameterizedTest
    @MethodSource("equipmentTypes")
    void addEquipment_blankOptionalValues_rejectInsteadOfUsingOmissionDefaults(EquipmentType type)
            throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        Equipment original = handler.addEquipment(command(type, "001"));
        for (String whitespace : WHITESPACE) {
            String[] invalidTails = {
                type.marker(), type.marker() + whitespace,
                type.marker() + whitespace + " damage/NONE",
                "damage/", "damage/" + whitespace,
                "damage/" + whitespace + " remarks/" + REMARKS
            };
            for (String tail : invalidTails) {
                assertThrows(EquipmentAdditionUnsuccessful.class,
                        () -> handler.addEquipment(command(type, "002") + " " + tail));
                assertEquals(List.of(original), handler.getEquipmentList());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("equipmentTypes")
    void addEquipment_emptySuppliedRemarks_matchOmittedRemarks(EquipmentType type) throws Exception {
        for (String whitespace : WHITESPACE) {
            Equipment item = new EquipmentHandler().addEquipment(command(type, "001") + " remarks/" + whitespace);

            assertEquals("", item.getRemarks());
            assertEquals(type.marker() + "unspecified damage/NONE", item.getDetails());
        }
    }

    @ParameterizedTest
    @MethodSource("equipmentTypes")
    void addEquipment_repeatedReorderedOrWrongTypeFields_preserveInventoryAndSavedData(EquipmentType type)
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(command(type, "001"));
        String saved = Files.readString(file);
        String wrongMarker = type.type().equals("camera") ? "zoom/" : "sensor/";
        String[] invalidTails = {
            type.marker() + type.value() + " " + type.marker() + "Repeated",
            "damage/NONE damage/" + type.damage(),
            "damage/NONE " + type.marker() + type.value(),
            wrongMarker + "Wrong field",
            type.marker() + type.value() + " " + wrongMarker + "Wrong field",
            type.marker() + type.value() + " damage/OTHER"
        };
        for (String tail : invalidTails) {
            assertThrows(EquipmentAdditionUnsuccessful.class,
                    () -> handler.addEquipment(command(type, "002") + " " + tail));
            assertEquals(List.of(original), handler.getEquipmentList());
            assertEquals(saved, Files.readString(file));
        }
    }

    @ParameterizedTest
    @MethodSource("equipmentTypes")
    void optionalDefaults_restart_preservesSubtypeAndEmptyRemarks(EquipmentType type) throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(command(type, "001"));

        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        Equipment restored = restarted.getEquipmentList().getFirst();

        assertEquals(original.getClass(), restored.getClass());
        assertEquals(type.marker() + "unspecified damage/NONE", restored.getDetails());
        assertEquals("", restored.getRemarks());
    }

    @ParameterizedTest
    @MethodSource("equipmentTypes")
    void optionalValues_returnAndNormalUpdate_preserveAssessmentAfterRestart(EquipmentType type) throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        Equipment original = handler.addEquipment(command(type, "001") + " " + type.marker() + type.value()
                + " damage/" + type.damage() + " remarks/" + REMARKS);
        Equipment assessed = handler.prepareReturnAssessment(original, false, type.damage(), REMARKS);
        handler.saveReturnAssessment(original, assessed);
        original.applyReturnAssessment(assessed);

        assertSame(original, handler.updateEquipment("update id/001 i/Updated cond/8"));
        EquipmentHandler restarted = new EquipmentHandler(file);
        restarted.loadFromStorage();
        Equipment restored = restarted.getEquipmentList().getFirst();

        assertEquals(original.getClass(), restored.getClass());
        assertEquals(type.marker() + type.value() + " damage/" + type.damage(), restored.getDetails());
        assertEquals(REMARKS, restored.getRemarks());
        assertFalse(restored.isServiceable());
        assertEquals("Updated", restored.getItemName());
        assertEquals(8, restored.getCondition());
    }

    @Test
    void addEquipment_requiredFieldsRemainRequired() {
        EquipmentHandler handler = new EquipmentHandler();
        String[] commands = {
            "add id/001 type/camera cond/7", "add i/Camera type/camera cond/7",
            "add i/Camera id/001 cond/7", "add i/Camera id/001 type/camera"
        };
        for (String command : commands) {
            assertThrows(EquipmentAdditionUnsuccessful.class, () -> handler.addEquipment(command));
        }
        assertEquals(List.of(), handler.getEquipmentList());
    }

    /** Builds only the required command fields, leaving optional additions explicit in each test. */
    private static String command(EquipmentType type, String id) {
        return "add i/Equipment id/" + id + " type/" + type.type() + " cond/7";
    }
}
