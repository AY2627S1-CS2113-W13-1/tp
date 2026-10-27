package seedu.snap.loan;

import java.util.Locale;
import java.util.Objects;

import seedu.snap.equipment.Battery;
import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.Lens;
import seedu.snap.equipment.Tripod;

// @@author Rohita-29

/**
 * Preserves an equipment condition assessment at a specific point in a loan.
 */
public final class EquipmentConditionSnapshot {
    static final int MAX_TYPE_LENGTH = 100;
    static final int MAX_DAMAGE_LENGTH = 100;
    static final int MAX_REMARKS_LENGTH = 200;

    private final String equipmentType;
    private final int condition;
    private final String damage;
    private final String remarks;

    /**
     * Creates an immutable equipment condition snapshot.
     *
     * @param equipmentType equipment subclass type
     * @param condition assessed numeric condition
     * @param damage damage state at the time of assessment
     * @param remarks remarks at the time of assessment
     * @throws IllegalArgumentException if any value is invalid
     */
    public EquipmentConditionSnapshot(
            String equipmentType,
            int condition,
            String damage,
            String remarks) {
        String normalizedType =
                normalizeRequired(
                        equipmentType,
                        "Equipment type",
                        MAX_TYPE_LENGTH)
                        .toLowerCase(Locale.ROOT);
        String normalizedDamage =
                normalizeRequired(
                        damage,
                        "Damage",
                        MAX_DAMAGE_LENGTH)
                        .toUpperCase(Locale.ROOT);
        validateTypeAndDamage(normalizedType, normalizedDamage);

        this.equipmentType = normalizedType;
        this.condition = validateCondition(condition);
        this.damage = normalizedDamage;
        this.remarks = normalizeRemarks(remarks);
    }

    /**
     * Captures the current condition information from supported equipment.
     *
     * @param equipment equipment being issued
     * @return immutable snapshot of its current condition
     * @throws IllegalArgumentException if the equipment is null or unsupported
     */
    public static EquipmentConditionSnapshot fromEquipment(
            Equipment equipment) {
        if (equipment == null) {
            throw new IllegalArgumentException(
                    "Equipment must not be null.");
        }

        String damage = getDamageName(equipment);
        return new EquipmentConditionSnapshot(
                equipment.getType(),
                equipment.getCondition(),
                damage,
                equipment.getRemarks());
    }

    /** Returns the captured equipment type. */
    public String getEquipmentType() {
        return equipmentType;
    }

    /** Returns the captured numeric condition. */
    public int getCondition() {
        return condition;
    }

    /** Returns the captured damage state. */
    public String getDamage() {
        return damage;
    }

    /** Returns the captured equipment remarks. */
    public String getRemarks() {
        return remarks;
    }

    /** Returns the damage enum name belonging to the equipment subclass. */
    private static String getDamageName(Equipment equipment) {
        if (equipment instanceof Camera camera) {
            return camera.getDamage().name();
        }
        if (equipment instanceof Lens lens) {
            return lens.getDamage().name();
        }
        if (equipment instanceof Battery battery) {
            return battery.getDamage().name();
        }
        if (equipment instanceof Tripod tripod) {
            return tripod.getDamage().name();
        }

        throw new IllegalArgumentException(
                "Unsupported equipment type: "
                        + equipment.getClass().getSimpleName()
                        + ".");
    }

    /** Validates the equipment type and its corresponding damage enum. */
    private static void validateTypeAndDamage(
            String equipmentType, String damage) {
        switch (equipmentType) {
        case "camera" -> validateDamage(
                Camera.CameraDamage.class, damage, equipmentType);
        case "lens" -> validateDamage(
                Lens.LensDamage.class, damage, equipmentType);
        case "battery" -> validateDamage(
                Battery.BatteryDamage.class, damage, equipmentType);
        case "tripod" -> validateDamage(
                Tripod.TripodDamage.class, damage, equipmentType);
        default -> throw new IllegalArgumentException(
                "Unsupported equipment type: " + equipmentType + ".");
        }
    }

    /** Rejects damage values that are not defined for the equipment type. */
    private static <T extends Enum<T>> void validateDamage(
            Class<T> damageClass, String damage, String equipmentType) {
        try {
            Enum.valueOf(damageClass, damage);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid damage " + damage
                            + " for equipment type " + equipmentType + ".",
                    exception);
        }
    }

    /** Validates the supported equipment condition range. */
    private static int validateCondition(int condition) {
        if (condition < 1 || condition > 10) {
            throw new IllegalArgumentException(
                    "Condition must be from 1 to 10.");
        }
        return condition;
    }

    /** Normalizes and validates a required single-line field. */
    private static String normalizeRequired(
            String value,
            String fieldName,
            int maximumLength) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null.");
        }

        String normalized = value.strip();
        if (normalized.isEmpty()
                || normalized.length() > maximumLength
                || normalized.matches("(?s).*\\R.*")) {
            throw new IllegalArgumentException(
                    fieldName + " must contain 1 to "
                            + maximumLength
                            + " characters on one line.");
        }

        return normalized;
    }

    /** Normalizes optional single-line remarks. */
    private static String normalizeRemarks(String remarks) {
        if (remarks == null) {
            throw new IllegalArgumentException(
                    "Remarks must not be null.");
        }

        String normalized = remarks.strip();
        if (normalized.length() > MAX_REMARKS_LENGTH
                || normalized.matches("(?s).*\\R.*")) {
            throw new IllegalArgumentException(
                    "Remarks must contain at most 200 "
                            + "characters on one line.");
        }

        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EquipmentConditionSnapshot snapshot)) {
            return false;
        }
        return condition == snapshot.condition
                && equipmentType.equals(snapshot.equipmentType)
                && damage.equals(snapshot.damage)
                && remarks.equals(snapshot.remarks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                equipmentType,
                condition,
                damage,
                remarks);
    }
}

// @@author
