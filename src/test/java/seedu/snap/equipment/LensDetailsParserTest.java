package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests construction of lenses from subtype-specific fields. */
class LensDetailsParserTest {
    @Test
    void createLens_allFields_createsLensWithNormalizedValues()
            throws EquipmentAdditionUnsuccessful {
        Lens lens = LensDetailsParser.createLens(
                "Standard Zoom",
                "101",
                8,
                "zoom/\u200324-70 mm\u2003 mount/Canon RF "
                        + "sensor/Full frame damage/NONE",
                "Studio lens");

        assertEquals("24-70 mm", lens.getZoomRange());
        assertEquals("Canon RF", lens.getLensMount());
        assertEquals("Full frame", lens.getSensorFormat());
        assertEquals(Lens.LensDamage.NONE, lens.getDamage());
        assertEquals("Studio lens", lens.getRemarks());
    }

    @Test
    void createLens_noFields_usesDefaults()
            throws EquipmentAdditionUnsuccessful {
        Lens lens = LensDetailsParser.createLens(
                "Prime Lens",
                "102",
                9,
                "",
                "");

        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getZoomRange());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getLensMount());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getSensorFormat());
        assertEquals(Lens.LensDamage.NONE, lens.getDamage());
    }

    @Test
    void createLens_partialFields_storesSuppliedValues()
            throws EquipmentAdditionUnsuccessful {
        Lens lens = LensDetailsParser.createLens(
                "Portrait Lens",
                "103",
                7,
                "mount/Nikon Z sensor/Full frame",
                "");

        assertEquals("Nikon Z", lens.getLensMount());
        assertEquals("Full frame", lens.getSensorFormat());
        assertEquals(
                CompatibilityText.UNSPECIFIED,
                lens.getZoomRange());
    }

    @Test
    void createLens_wrongFieldOrder_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> LensDetailsParser.createLens(
                        "Lens",
                        "104",
                        5,
                        "sensor/APS-C mount/Canon RF",
                        ""));
    }

    @Test
    void createLens_repeatedField_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> LensDetailsParser.createLens(
                        "Lens",
                        "105",
                        5,
                        "mount/Canon RF mount/Nikon Z",
                        ""));
    }

    @Test
    void createLens_inapplicableField_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> LensDetailsParser.createLens(
                        "Lens",
                        "106",
                        5,
                        "capacity/2000mAh",
                        ""));
    }

    @Test
    void createLens_invalidDamage_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> LensDetailsParser.createLens(
                        "Lens",
                        "107",
                        5,
                        "damage/CRACKED",
                        ""));
    }
}

// @@author
