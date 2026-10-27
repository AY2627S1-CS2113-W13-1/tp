package seedu.snap.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests maintenance-record validation and immutable values. */
class MaintenanceRecordTest {
    private static final String EQUIPMENT_RECORD_ID = UUID.randomUUID().toString();
    private static final LocalDate DATE = LocalDate.of(2026, 10, 27);

    @Test
    void constructor_validValues_preservesRecordFields() {
        MaintenanceRecord record = new MaintenanceRecord(EQUIPMENT_RECORD_ID, DATE,
                "Replaced the battery", "Completed");

        assertEquals(EQUIPMENT_RECORD_ID, record.getEquipmentRecordId());
        assertEquals(DATE, record.getMaintenanceDate());
        assertEquals("Replaced the battery", record.getWork());
        assertEquals("Completed", record.getOutcome());
    }

    @Test
    void constructor_trimsOuterWhitespaceWithoutChangingWorkText() {
        MaintenanceRecord record = new MaintenanceRecord(EQUIPMENT_RECORD_ID, DATE,
                "  Cleaned the sensor  ", "  Passed  ");

        assertEquals("Cleaned the sensor", record.getWork());
        assertEquals("Passed", record.getOutcome());
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void constructor_invalidValues_throwsIllegalArgumentException(String recordId, LocalDate date,
            String work, String outcome) {
        assertThrows(IllegalArgumentException.class,
                () -> new MaintenanceRecord(recordId, date, work, outcome));
    }

    private static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of("not-a-uuid", DATE, "Work", "Outcome"),
                Arguments.of(EQUIPMENT_RECORD_ID, null, "Work", "Outcome"),
                Arguments.of(EQUIPMENT_RECORD_ID, DATE, "", "Outcome"),
                Arguments.of(EQUIPMENT_RECORD_ID, DATE, "Work", ""),
                Arguments.of(EQUIPMENT_RECORD_ID, DATE, "Work\ncontinued", "Outcome"),
                Arguments.of(EQUIPMENT_RECORD_ID, DATE, "Work\tcontinued", "Outcome"),
                Arguments.of(EQUIPMENT_RECORD_ID, DATE, "W".repeat(MaintenanceRecord.MAX_WORK_LENGTH + 1), "Outcome"),
                Arguments.of(EQUIPMENT_RECORD_ID, DATE, "Work", "O".repeat(
                        MaintenanceRecord.MAX_OUTCOME_LENGTH + 1)));
    }

    @Test
    void isValidEquipmentRecordId_acceptsOnlyCanonicalUuid() {
        assertTrue(MaintenanceRecord.isValidEquipmentRecordId(EQUIPMENT_RECORD_ID));
        assertFalse(MaintenanceRecord.isValidEquipmentRecordId(EQUIPMENT_RECORD_ID.toUpperCase()));
        assertFalse(MaintenanceRecord.isValidEquipmentRecordId(null));
        assertFalse(MaintenanceRecord.isValidEquipmentRecordId("123"));
    }
}
