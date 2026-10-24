package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests construction of batteries from subtype-specific fields. */
class BatteryDetailsParserTest {
    @Test
    void createBattery_allFields_preservesValues()
            throws EquipmentAdditionUnsuccessful {
        Battery battery = BatteryDetailsParser.createBattery(
                "Camera Battery",
                "201",
                8,
                "capacity/2000 mAh battery/Li-ion damage/NONE",
                "Charged");

        assertEquals("2000 mAh", battery.getCapacity());
        assertEquals("Li-ion", battery.getBatteryType());
        assertEquals(
                Battery.BatteryDamage.NONE,
                battery.getDamage());
        assertEquals("Charged", battery.getRemarks());
    }

    @Test
    void createBattery_noFields_usesDefaults()
            throws EquipmentAdditionUnsuccessful {
        Battery battery = BatteryDetailsParser.createBattery(
                "Battery",
                "202",
                7,
                "",
                "");

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
    void createBattery_partialFields_preservesSuppliedValue()
            throws EquipmentAdditionUnsuccessful {
        Battery battery = BatteryDetailsParser.createBattery(
                "Battery",
                "203",
                6,
                "battery/AA",
                "");

        assertEquals("AA", battery.getBatteryType());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                battery.getCapacity());
    }

    @Test
    void createBattery_wrongFieldOrder_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> BatteryDetailsParser.createBattery(
                        "Battery",
                        "204",
                        5,
                        "battery/AA capacity/2000 mAh",
                        ""));
    }

    @Test
    void createBattery_repeatedField_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> BatteryDetailsParser.createBattery(
                        "Battery",
                        "205",
                        5,
                        "battery/AA battery/AAA",
                        ""));
    }

    @Test
    void createBattery_inapplicableField_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> BatteryDetailsParser.createBattery(
                        "Battery",
                        "206",
                        5,
                        "mount/Canon RF",
                        ""));
    }

    @Test
    void createBattery_invalidDamage_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> BatteryDetailsParser.createBattery(
                        "Battery",
                        "207",
                        5,
                        "damage/CRACKED",
                        ""));
    }
}

// @@author
