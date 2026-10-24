package seedu.snap.equipment;

// @@author Rohita-29

/**
 * Compares compatibility attributes belonging to different equipment types.
 */
public final class EquipmentCompatibility {
    private EquipmentCompatibility() {
    }

    /**
     * Compares two equipment items using their shared compatibility fields.
     *
     * @param first first equipment item
     * @param second second equipment item
     * @return compatibility relationship between the items
     * @throws IllegalArgumentException if either item is null
     */
    public static CompatibilityResult compare(
            Equipment first,
            Equipment second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException(
                    "Equipment being compared must not be null.");
        }

        if (first instanceof Camera camera) {
            return compareCameraWith(camera, second);
        }

        if (second instanceof Camera camera) {
            return compareCameraWith(camera, first);
        }

        return CompatibilityResult.NOT_APPLICABLE;
    }

    /** Compares a camera with an equipment item that may depend on it. */
    private static CompatibilityResult compareCameraWith(
            Camera camera,
            Equipment other) {
        if (other instanceof Lens lens) {
            return compareCameraWithLens(camera, lens);
        }

        if (other instanceof Battery battery) {
            return compareValues(
                    camera.getBatteryType(),
                    battery.getBatteryType());
        }

        if (other instanceof Tripod tripod) {
            return compareValues(
                    camera.getTripodThread(),
                    tripod.getThread());
        }

        return CompatibilityResult.NOT_APPLICABLE;
    }

    /** Compares both shared attributes between a camera and lens. */
    private static CompatibilityResult compareCameraWithLens(
            Camera camera,
            Lens lens) {
        CompatibilityResult mountResult = compareValues(
                camera.getLensMount(),
                lens.getLensMount());
        CompatibilityResult sensorResult = compareValues(
                camera.getSensorFormat(),
                lens.getSensorFormat());

        return combine(mountResult, sensorResult);
    }

    /** Compares two normalized compatibility values without case sensitivity. */
    private static CompatibilityResult compareValues(
            String first,
            String second) {
        String normalizedFirst = CompatibilityText.normalize(first);
        String normalizedSecond = CompatibilityText.normalize(second);

        if (isUnspecified(normalizedFirst)
                || isUnspecified(normalizedSecond)) {
            return CompatibilityResult.UNKNOWN;
        }

        if (normalizedFirst.equalsIgnoreCase(normalizedSecond)) {
            return CompatibilityResult.COMPATIBLE;
        }

        return CompatibilityResult.INCOMPATIBLE;
    }

    /** Combines results when compatibility depends on multiple attributes. */
    private static CompatibilityResult combine(
            CompatibilityResult first,
            CompatibilityResult second) {
        if (first == CompatibilityResult.INCOMPATIBLE
                || second == CompatibilityResult.INCOMPATIBLE) {
            return CompatibilityResult.INCOMPATIBLE;
        }

        if (first == CompatibilityResult.COMPATIBLE
                && second == CompatibilityResult.COMPATIBLE) {
            return CompatibilityResult.COMPATIBLE;
        }

        return CompatibilityResult.UNKNOWN;
    }

    /** Returns whether a compatibility value represents missing information. */
    private static boolean isUnspecified(String value) {
        return CompatibilityText.UNSPECIFIED.equalsIgnoreCase(value);
    }
}

// @@author

