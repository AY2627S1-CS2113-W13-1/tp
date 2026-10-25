package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

// @@author Rohita-29

/** Tests construction of tripods from subtype-specific fields. */
class TripodDetailsParserTest {
    @Test
    void createTripod_allFields_preservesValues()
            throws EquipmentAdditionUnsuccessful {
        Tripod tripod = TripodDetailsParser.createTripod(
                "Studio Tripod",
                "301",
                8,
                "thread/1/4\"-20 damage/NONE",
                "Stable");

        assertEquals("1/4\"-20", tripod.getThread());
        assertEquals(
                Tripod.TripodDamage.NONE,
                tripod.getDamage());
        assertEquals("Stable", tripod.getRemarks());
    }

    @Test
    void createTripod_noFields_usesDefaults()
            throws EquipmentAdditionUnsuccessful {
        Tripod tripod = TripodDetailsParser.createTripod(
                "Tripod",
                "302",
                7,
                "",
                "");

        assertEquals(
                CompatibilityText.UNSPECIFIED,
                tripod.getThread());
        assertEquals(
                Tripod.TripodDamage.NONE,
                tripod.getDamage());
    }

    @Test
    void createTripod_unicodeWhitespace_normalizesValue()
            throws EquipmentAdditionUnsuccessful {
        Tripod tripod = TripodDetailsParser.createTripod(
                "Tripod",
                "303",
                6,
                "thread/\u20031/4\"-20\u2003",
                "");

        assertEquals("1/4\"-20", tripod.getThread());
    }

    @Test
    void createTripod_repeatedField_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> TripodDetailsParser.createTripod(
                        "Tripod",
                        "304",
                        5,
                        "thread/1/4\"-20 thread/3/8\"-16",
                        ""));
    }

    @Test
    void createTripod_inapplicableField_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> TripodDetailsParser.createTripod(
                        "Tripod",
                        "305",
                        5,
                        "mount/Canon RF",
                        ""));
    }

    @Test
    void createTripod_invalidDamage_throwsException() {
        assertThrows(
                EquipmentAdditionUnsuccessful.class,
                () -> TripodDetailsParser.createTripod(
                        "Tripod",
                        "306",
                        5,
                        "damage/CRACKED",
                        ""));
    }
}

// @@author
