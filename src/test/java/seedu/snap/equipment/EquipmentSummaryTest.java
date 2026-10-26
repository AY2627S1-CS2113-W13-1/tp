package seedu.snap.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.snap.loan.Loan;

/** Tests summary formatting, optional sections and physical-item loan-history validation. */
class EquipmentSummaryTest {
    @Test
    void format_completeCurrentDetailsAndEmptyOptionalSections() {
        Equipment camera = new Camera("Camera", "001", 5, "Full frame", Camera.CameraDamage.NONE,
                "Handle with care");

        String summary = new EquipmentSummary(camera, List.of()).format();

        assertTrue(summary.contains("Equipment summary for item ID 001:"));
        assertTrue(summary.contains("Record ID: " + camera.getRecordId()));
        assertTrue(summary.contains("Type: camera"));
        assertTrue(summary.contains("Condition: 5"));
        assertTrue(summary.contains("Status: Available"));
        assertTrue(summary.contains("Remarks: Handle with care"));
        assertTrue(summary.contains("Subtype details: sensor/Full frame damage/NONE"));
        assertTrue(summary.contains("No loan history recorded."));
        assertTrue(summary.contains("No status history recorded."));
        assertTrue(summary.contains("No maintenance history recorded."));
        assertTrue(summary.contains("No storage location recorded."));
    }

    @Test
    void format_loanHistoryIncludesCurrentAndReturnedRecords() {
        Equipment camera = new Camera("Camera", "001", 5);
        Loan returned = new Loan("001", camera.getItemId(), "111",
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0),
                camera.getRecordId());
        returned.setReturned(true);
        Loan active = new Loan("002", camera.getItemId(), "222",
                LocalDateTime.of(2026, 10, 3, 0, 0), LocalDateTime.of(2026, 10, 4, 0, 0),
                camera.getRecordId());

        String summary = new EquipmentSummary(camera, List.of(returned, active)).format();

        assertTrue(summary.contains("001 | 111 | 2026-10-01 | 2026-10-02 | true"));
        assertTrue(summary.contains("002 | 222 | 2026-10-03 | 2026-10-04 | false"));
        assertTrue(summary.indexOf("001 | 111") < summary.indexOf("002 | 222"));
    }

    @Test
    void constructor_rejectsLoanHistoryForAnotherPhysicalItem() {
        Equipment camera = new Camera("Camera", "001", 5);
        Equipment replacement = new Camera("Camera", "001", 5);
        Loan unrelated = new Loan("001", replacement.getItemId(), "111",
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 2, 0, 0),
                replacement.getRecordId());

        assertThrows(IllegalArgumentException.class, () -> new EquipmentSummary(camera, List.of(unrelated)));
        assertEquals("001", camera.getItemId());
    }
}
