package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests Battery compatibility parsing, display and persistence. */
class BatteryCompatibilityIntegrationTest {
    private static final String FULL_BATTERY_COMMAND =
            "add i/Camera Battery id/201 type/battery cond/8 "
                    + "capacity/2000 mAh battery/Li-ion "
                    + "damage/NONE remarks/Charged";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void parseEquipment_allBatteryCompatibilityFields_preservesValues()
            throws EquipmentAdditionUnsuccessful {
        Battery battery = assertInstanceOf(
                Battery.class,
                EquipmentHandler.parseEquipment(
                        FULL_BATTERY_COMMAND));

        assertFullCompatibility(battery);
        assertEquals("Charged", battery.getRemarks());
    }

    @Test
    void parseEquipment_omittedCompatibilityFields_usesDefaults()
            throws EquipmentAdditionUnsuccessful {
        Battery battery = assertInstanceOf(
                Battery.class,
                EquipmentHandler.parseEquipment(
                        "add i/Battery id/202 "
                                + "type/battery cond/7"));

        assertEquals(
                CompatibilityText.UNSPECIFIED,
                battery.getCapacity());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                battery.getBatteryType());
        assertEquals(
                Battery.BatteryDamage.NONE,
                battery.getDamage());
    }

    @Test
    void parseEquipment_partialCompatibilityFields_preservesValue()
            throws EquipmentAdditionUnsuccessful {
        Battery battery = assertInstanceOf(
                Battery.class,
                EquipmentHandler.parseEquipment(
                        "add i/AA Battery id/203 "
                                + "type/battery cond/6 battery/AA"));

        assertEquals(
                CompatibilityText.UNSPECIFIED,
                battery.getCapacity());
        assertEquals("AA", battery.getBatteryType());
        assertEquals(
                "capacity/unspecified battery/AA damage/NONE",
                battery.getDetails());
    }

    @Test
    void parseEquipment_invalidBatteryFields_throwException() {
        String repeatedType =
                "add i/Battery id/204 type/battery cond/5 "
                        + "battery/AA battery/AAA";
        String wrongOrder =
                "add i/Battery id/205 type/battery cond/5 "
                        + "battery/AA capacity/2000 mAh";
        String mountField =
                "add i/Battery id/206 type/battery cond/5 "
                        + "mount/Canon RF";

        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(repeatedType));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(wrongOrder));
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> EquipmentHandler.parseEquipment(mountField));
    }

    @Test
    void persistence_fullBatteryCompatibility_survivesReload()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler originalHandler =
                new EquipmentHandler(file);
        originalHandler.addEquipment(FULL_BATTERY_COMMAND);

        String savedRecord = Files.readString(file);
        assertTrue(savedRecord.contains("capacity/2000 mAh"));
        assertTrue(savedRecord.contains("battery/Li-ion"));

        EquipmentHandler reloadedHandler =
                new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();

        Battery reloadedBattery = assertInstanceOf(
                Battery.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertFullCompatibility(reloadedBattery);
        assertEquals("Charged", reloadedBattery.getRemarks());
    }

    @Test
    void persistence_legacyBatteryRecord_usesCompatibilityDefault()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        Files.writeString(
                file,
                "add i/Legacy Battery id/201 type/battery cond/5 "
                        + "capacity/2000 mAh damage/NONE"
                        + System.lineSeparator());

        EquipmentHandler handler = new EquipmentHandler(file);
        handler.loadFromStorage();

        Battery battery = assertInstanceOf(
                Battery.class,
                handler.getEquipmentList().getFirst());

        assertEquals("2000 mAh", battery.getCapacity());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                battery.getBatteryType());
    }

    @Test
    void updateEquipment_batteryCompatibility_preservesValuesAndStorage()
            throws Exception {
        Path file = temporaryDirectory.resolve("equipment.txt");
        EquipmentHandler handler = new EquipmentHandler(file);
        handler.addEquipment(FULL_BATTERY_COMMAND);

        Battery updatedBattery = assertInstanceOf(
                Battery.class,
                handler.updateEquipment("update id/201 cond/9"));

        assertEquals(9, updatedBattery.getCondition());
        assertFullCompatibility(updatedBattery);

        EquipmentHandler reloadedHandler =
                new EquipmentHandler(file);
        reloadedHandler.loadFromStorage();

        Battery reloadedBattery = assertInstanceOf(
                Battery.class,
                reloadedHandler.getEquipmentList().getFirst());

        assertEquals(9, reloadedBattery.getCondition());
        assertFullCompatibility(reloadedBattery);
    }

    @Test
    void showEquipment_batteryCompatibility_displaysValues()
            throws Exception {
        EquipmentHandler handler = new EquipmentHandler();
        handler.addEquipment(FULL_BATTERY_COMMAND);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(
                    new PrintStream(
                            output,
                            true,
                            StandardCharsets.UTF_8));
            handler.showEquipment("show id/201");
        } finally {
            System.setOut(originalOutput);
        }

        String displayed =
                output.toString(StandardCharsets.UTF_8);
        assertTrue(displayed.contains("Capacity: 2000 mAh"));
        assertTrue(displayed.contains("Battery type: Li-ion"));
    }

    /** Checks every explicit compatibility field on a Battery. */
    private void assertFullCompatibility(Battery battery) {
        assertEquals("2000 mAh", battery.getCapacity());
        assertEquals("Li-ion", battery.getBatteryType());
        assertEquals(
                Battery.BatteryDamage.NONE,
                battery.getDamage());
    }
}

// @@author
