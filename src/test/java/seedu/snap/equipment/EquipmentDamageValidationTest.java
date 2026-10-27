package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/**
 * Tests equipment-specific damage parsing and validation.
 */
class EquipmentDamageValidationTest {
    @TempDir
    private Path temporaryDirectory;

    @ParameterizedTest
    @MethodSource("expandedDamageCommands")
    void parseEquipment_expandedDamageValue_preservesDamage(
            String command,
            String expectedDamage)
            throws EquipmentAdditionUnsuccessful {
        Equipment equipment =
                EquipmentHandler.parseEquipment(command);

        assertTrue(
                equipment.getDetails()
                        .contains("damage/" + expectedDamage));
    }

    private static Stream<Arguments> expandedDamageCommands() {
        return Stream.of(
                Arguments.of(
                        "add i/Camera id/001 type/camera cond/5 "
                                + "damage/WATER_DAMAGE",
                        "WATER_DAMAGE"),
                Arguments.of(
                        "add i/Camera id/001 type/camera cond/5 "
                                + "damage/BODY_CRACK",
                        "BODY_CRACK"),
                Arguments.of(
                        "add i/Camera id/001 type/camera cond/5 "
                                + "damage/MISSING_CAP",
                        "MISSING_CAP"),
                Arguments.of(
                        "add i/Lens id/002 type/lens cond/5 "
                                + "damage/WATER_DAMAGE",
                        "WATER_DAMAGE"),
                Arguments.of(
                        "add i/Lens id/002 type/lens cond/5 "
                                + "damage/CRACKED_GLASS",
                        "CRACKED_GLASS"),
                Arguments.of(
                        "add i/Lens id/002 type/lens cond/5 "
                                + "damage/MISSING_CAP",
                        "MISSING_CAP"),
                Arguments.of(
                        "add i/Battery id/003 type/battery cond/5 "
                                + "damage/WATER_DAMAGE",
                        "WATER_DAMAGE"),
                Arguments.of(
                        "add i/Battery id/003 type/battery cond/5 "
                                + "damage/LEAKING",
                        "LEAKING"),
                Arguments.of(
                        "add i/Tripod id/004 type/tripod cond/5 "
                                + "damage/WATER_DAMAGE",
                        "WATER_DAMAGE"),
                Arguments.of(
                        "add i/Tripod id/004 type/tripod cond/5 "
                                + "damage/MISSING_PART",
                        "MISSING_PART"));
    }

    @ParameterizedTest
    @MethodSource("otherDamageCommandsWithRemarks")
    void parseEquipment_otherDamageWithRemarks_preservesValues(
            String command,
            String expectedType)
            throws EquipmentAdditionUnsuccessful {
        Equipment equipment =
                EquipmentHandler.parseEquipment(command);

        assertEquals(expectedType, equipment.getType());
        assertTrue(equipment.getDetails().contains("damage/OTHER"));
        assertEquals(
                "Damage does not match an existing category",
                equipment.getRemarks());
    }

    private static Stream<Arguments> otherDamageCommandsWithRemarks() {
        String remarks =
                " remarks/Damage does not match an existing category";

        return Stream.of(
                Arguments.of(
                        "add i/Camera id/001 type/camera cond/5 "
                                + "damage/OTHER" + remarks,
                        "camera"),
                Arguments.of(
                        "add i/Lens id/002 type/lens cond/5 "
                                + "damage/OTHER" + remarks,
                        "lens"),
                Arguments.of(
                        "add i/Battery id/003 type/battery cond/5 "
                                + "damage/OTHER" + remarks,
                        "battery"),
                Arguments.of(
                        "add i/Tripod id/004 type/tripod cond/5 "
                                + "damage/OTHER" + remarks,
                        "tripod"));
    }

    @ParameterizedTest
    @MethodSource("otherDamageCommandsWithoutRemarks")
    void parseEquipment_otherDamageWithoutRemarks_throwsException(
            String command,
            String equipmentType) {
        EquipmentAdditionUnsuccessful exception =
                assertThrows(
                        EquipmentAdditionUnsuccessful.class,
                        () -> EquipmentHandler.parseEquipment(command));

        assertEquals(
                "Damage OTHER for " + equipmentType
                        + " requires non-empty remarks.",
                exception.getMessage());
    }

    private static Stream<Arguments> otherDamageCommandsWithoutRemarks() {
        return Stream.of(
                Arguments.of(
                        "add i/Camera id/001 type/camera cond/5 "
                                + "damage/OTHER",
                        "camera"),
                Arguments.of(
                        "add i/Lens id/002 type/lens cond/5 "
                                + "damage/OTHER",
                        "lens"),
                Arguments.of(
                        "add i/Battery id/003 type/battery cond/5 "
                                + "damage/OTHER",
                        "battery"),
                Arguments.of(
                        "add i/Tripod id/004 type/tripod cond/5 "
                                + "damage/OTHER remarks/   ",
                        "tripod"));
    }

    @ParameterizedTest
    @MethodSource("damageFromAnotherSubtype")
    void parseEquipment_damageFromAnotherSubtype_rejects(String command, String type) {
        EquipmentAdditionUnsuccessful exception = assertThrows(EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(command));
        assertTrue(exception.getMessage().contains("Invalid damage for " + type));
    }

    /** Supplies new damage values that belong only to a different equipment subtype. */
    private static Stream<Arguments> damageFromAnotherSubtype() {
        return Stream.of(
                Arguments.of("add i/Camera id/001 type/camera cond/5 damage/CRACKED_GLASS", "camera"),
                Arguments.of("add i/Lens id/002 type/lens cond/5 damage/LEAKING", "lens"),
                Arguments.of("add i/Battery id/003 type/battery cond/5 damage/MISSING_PART", "battery"),
                Arguments.of("add i/Tripod id/004 type/tripod cond/5 damage/BODY_CRACK", "tripod"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"camera", "lens", "battery", "tripod"})
    void parseEquipment_otherRemarksBoundary_acceptsValidLengthAndRejectsTooLong(String type) throws Exception {
        String prefix = "add i/Item id/001 type/" + type + " cond/5 damage/other remarks/";
        String longest = "r".repeat(Equipment.MAX_REMARKS_LENGTH);

        Equipment shortest = EquipmentHandler.parseEquipment(prefix + " r ");
        Equipment maximum = EquipmentHandler.parseEquipment(prefix + longest);
        EquipmentAdditionUnsuccessful exception = assertThrows(EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(prefix + longest + "r"));

        assertEquals("r", shortest.getRemarks());
        assertTrue(shortest.getDetails().contains("damage/OTHER"));
        assertEquals(longest, maximum.getRemarks());
        assertEquals("Remarks must contain at most 200 characters and no line breaks.", exception.getMessage());
    }

    @Test
    void persistence_expandedDamageValues_surviveReload()
            throws Exception {
        Path file =
                temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler firstHandler =
                new EquipmentHandler(file);

        firstHandler.addEquipment(
                "add i/Camera id/001 type/camera cond/5 "
                        + "damage/WATER_DAMAGE");
        firstHandler.addEquipment(
                "add i/Lens id/002 type/lens cond/5 "
                        + "damage/CRACKED_GLASS");
        firstHandler.addEquipment(
                "add i/Battery id/003 type/battery cond/5 "
                        + "damage/LEAKING");
        firstHandler.addEquipment(
                "add i/Tripod id/004 type/tripod cond/5 "
                        + "damage/OTHER remarks/Mounting plate missing");

        EquipmentHandler secondHandler =
                new EquipmentHandler(file);
        secondHandler.loadFromStorage();

        List<Equipment> equipment =
                secondHandler.getEquipmentList();

        assertEquals(4, equipment.size());
        assertTrue(
                equipment.get(0)
                        .getDetails()
                        .contains("damage/WATER_DAMAGE"));
        assertTrue(
                equipment.get(1)
                        .getDetails()
                        .contains("damage/CRACKED_GLASS"));
        assertTrue(
                equipment.get(2)
                        .getDetails()
                        .contains("damage/LEAKING"));
        assertTrue(
                equipment.get(3)
                        .getDetails()
                        .contains("damage/OTHER"));
        assertEquals(
                "Mounting plate missing",
                equipment.get(3).getRemarks());
    }
}

// @@author
