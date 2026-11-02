package seedu.snap.loan;

import seedu.snap.equipment.Equipment;

/** Immutable condition and serviceability recorded for one completed loan. */
public record LoanReturnAssessment(EquipmentConditionSnapshot condition, boolean serviceable) {
    /** Rejects a missing assessment rather than inventing historical condition information. */
    public LoanReturnAssessment {
        if (condition == null) {
            throw new IllegalArgumentException("Return condition must not be null.");
        }
    }

    /** Copies the validated return proposal without retaining a reference to mutable equipment. */
    static LoanReturnAssessment fromEquipment(Equipment assessed) {
        return new LoanReturnAssessment(EquipmentConditionSnapshot.fromEquipment(assessed),
                assessed.isServiceable());
    }
}
