package seedu.snap.maintenance;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents one completed maintenance activity for a physical equipment item.
 *
 * <p>The permanent equipment record ID is stored instead of relying only on the
 * visible item ID. This keeps the record attached to the same physical item if
 * the visible item ID is reused later.</p>
 */
public final class MaintenanceRecord {
    /** Maximum length of the maintenance work description. */
    public static final int MAX_WORK_LENGTH = 200;
    /** Maximum length of the maintenance outcome. */
    public static final int MAX_OUTCOME_LENGTH = 100;

    private final String equipmentRecordId;
    private final LocalDate maintenanceDate;
    private final String work;
    private final String outcome;

    /**
     * Creates a validated maintenance record.
     *
     * @param equipmentRecordId permanent identity of the physical equipment
     * @param maintenanceDate date on which the maintenance was performed
     * @param work description of the maintenance work
     * @param outcome result of the maintenance work
     */
    public MaintenanceRecord(String equipmentRecordId, LocalDate maintenanceDate,
            String work, String outcome) {
        validateEquipmentRecordId(equipmentRecordId);
        if (maintenanceDate == null) {
            throw new IllegalArgumentException("Maintenance date must not be null.");
        }
        String normalisedWork = normaliseText(work, "Maintenance work", MAX_WORK_LENGTH);
        String normalisedOutcome = normaliseText(outcome, "Maintenance outcome", MAX_OUTCOME_LENGTH);

        this.equipmentRecordId = equipmentRecordId;
        this.maintenanceDate = maintenanceDate;
        this.work = normalisedWork;
        this.outcome = normalisedOutcome;
        assert hasValidState() : "Maintenance records must contain valid values";
    }

    /** Returns the permanent identity of the physical equipment item. */
    public String getEquipmentRecordId() {
        return equipmentRecordId;
    }

    /** Returns the date on which the maintenance was performed. */
    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    /** Returns the maintenance work description. */
    public String getWork() {
        return work;
    }

    /** Returns the maintenance outcome. */
    public String getOutcome() {
        return outcome;
    }

    /** Checks whether a value is a canonical permanent equipment identity. */
    public static boolean isValidEquipmentRecordId(String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** Rejects malformed permanent identities before storing a record. */
    private static void validateEquipmentRecordId(String value) {
        if (!isValidEquipmentRecordId(value)) {
            throw new IllegalArgumentException("Equipment record identity must be a canonical UUID.");
        }
    }

    /** Normalises and validates free text stored in the tab-separated persistence format. */
    private static String normaliseText(String value, String fieldName, int maximumLength) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null.");
        }
        String normalised = value.strip();
        if (normalised.isEmpty() || normalised.length() > maximumLength
                || normalised.matches("(?s).*\\R.*") || normalised.contains("\t")) {
            throw new IllegalArgumentException(fieldName + " must be a non-empty single-line value of at most "
                    + maximumLength + " characters.");
        }
        return normalised;
    }

    /** Checks the immutable state invariant used by assertions in this class. */
    private boolean hasValidState() {
        return isValidEquipmentRecordId(equipmentRecordId)
                && maintenanceDate != null
                && !work.isEmpty() && work.length() <= MAX_WORK_LENGTH
                && !outcome.isEmpty() && outcome.length() <= MAX_OUTCOME_LENGTH
                && !work.matches("(?s).*\\R.*") && !outcome.matches("(?s).*\\R.*")
                && !work.contains("\t") && !outcome.contains("\t");
    }
}
