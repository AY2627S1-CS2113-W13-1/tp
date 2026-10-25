package seedu.snap.equipment;

// @@author Rohita-29

/**
 * Describes the compatibility relationship between two equipment items.
 */
public enum CompatibilityResult {
    /** Every required known attribute matches. */
    COMPATIBLE,

    /** At least one required known attribute conflicts. */
    INCOMPATIBLE,

    /** Compatibility cannot be determined because information is missing. */
    UNKNOWN,

    /** The two equipment types do not have a compatibility relationship. */
    NOT_APPLICABLE
}

// @@author
