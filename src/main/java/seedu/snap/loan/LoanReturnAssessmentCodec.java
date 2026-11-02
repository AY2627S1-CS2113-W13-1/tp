package seedu.snap.loan;

/** Encodes the optional per-loan return assessment without changing older issue-snapshot fields. */
final class LoanReturnAssessmentCodec {
    static final int FIELD_COUNT = EquipmentConditionSnapshotCodec.FIELD_COUNT + 1;
    private static final String SERVICEABLE_PREFIX = "returnServiceable/";

    private LoanReturnAssessmentCodec() {
        // Utility class; do not instantiate.
    }

    /** Saves condition values and an explicit yes/no serviceability assessment as five storage tokens. */
    static String encode(LoanReturnAssessment assessment) {
        return EquipmentConditionSnapshotCodec.encode(assessment.condition(), "return")
                + " " + SERVICEABLE_PREFIX + (assessment.serviceable() ? "yes" : "no");
    }

    /** Rejects incomplete assessments and unsupported serviceability values in saved records. */
    static LoanReturnAssessment decode(String[] fields, int startIndex) {
        if (fields == null || startIndex < 0 || startIndex + FIELD_COUNT > fields.length) {
            throw new IllegalArgumentException("Incomplete return assessment.");
        }
        EquipmentConditionSnapshot condition = EquipmentConditionSnapshotCodec.decode(fields, startIndex, "return");
        String serviceability = fields[startIndex + FIELD_COUNT - 1];
        if (!serviceability.equals(SERVICEABLE_PREFIX + "yes")
                && !serviceability.equals(SERVICEABLE_PREFIX + "no")) {
            throw new IllegalArgumentException("Expected returnServiceable/yes or returnServiceable/no.");
        }
        return new LoanReturnAssessment(condition, serviceability.equals(SERVICEABLE_PREFIX + "yes"));
    }
}
