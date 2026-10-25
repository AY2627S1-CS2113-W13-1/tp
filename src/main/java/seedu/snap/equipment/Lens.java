package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a lens in the equipment inventory. */
public class Lens extends Equipment {
    private static final String TYPE = "lens";
    private static final Logger LOGGER =
            Logger.getLogger(Lens.class.getName());

    /** Damage states that can be recorded for a lens. */
    public enum LensDamage {
        NONE,
        SCRATCHED_GLASS,
        AUTOFOCUS_FAULT
    }

    private final String zoomRange;
    private LensDamage damage;

    // @@author Rohita-29

    private final String lensMount;
    private final String sensorFormat;

    // @@author

    /**
     * Creates a lens with unspecified details and no recorded damage.
     *
     * @param name name of the lens
     * @param id three-digit equipment ID
     * @param condition condition of the lens from 1 to 10
     */
    public Lens(String name, String id, int condition) {
        this(
                name,
                id,
                condition,
                CompatibilityText.UNSPECIFIED,
                LensDamage.NONE);
    }

    /**
     * Creates a lens with its existing type-specific details.
     *
     * @param name name of the lens
     * @param id three-digit equipment ID
     * @param condition condition of the lens from 1 to 10
     * @param zoomRange zoom range description
     * @param damage damage recorded for the lens
     */
    public Lens(
            String name,
            String id,
            int condition,
            String zoomRange,
            LensDamage damage) {
        this(name, id, condition, zoomRange, damage, "");
    }

    // @@author Rohita-29

    /**
     * Creates a lens with type-specific details and optional remarks.
     *
     * @param name name of the lens
     * @param id three-digit equipment ID
     * @param condition condition of the lens from 1 to 10
     * @param zoomRange zoom range description
     * @param damage damage recorded for the lens
     * @param remarks optional equipment remarks
     */
    public Lens(
            String name,
            String id,
            int condition,
            String zoomRange,
            LensDamage damage,
            String remarks) {
        this(
                name,
                id,
                condition,
                zoomRange,
                damage,
                remarks,
                CompatibilityText.UNSPECIFIED,
                CompatibilityText.UNSPECIFIED);
    }

    /**
     * Creates a lens with all compatibility attributes.
     *
     * @param name name of the lens
     * @param id three-digit equipment ID
     * @param condition condition of the lens from 1 to 10
     * @param zoomRange zoom range supported by the lens
     * @param damage damage recorded for the lens
     * @param remarks optional equipment remarks
     * @param lensMount lens mount used by the lens
     * @param sensorFormat sensor format supported by the lens
     */
    public Lens(
            String name,
            String id,
            int condition,
            String zoomRange,
            LensDamage damage,
            String remarks,
            String lensMount,
            String sensorFormat) {
        super(name, id, condition, remarks);
        assert zoomRange != null
                && !zoomRange.isBlank()
                && zoomRange.length() <= CompatibilityText.MAX_LENGTH
                && !zoomRange.matches("(?s).*\\R.*")
                : "Lens zoom range must contain 1 to 100 "
                + "characters without line breaks.";
        assert damage != null : "Lens damage must be specified.";

        this.zoomRange = zoomRange;
        this.damage = damage;
        this.lensMount = CompatibilityText.normalize(lensMount);
        this.sensorFormat = CompatibilityText.normalize(sensorFormat);

        assert TYPE.equals(getType())
                : "Lens type must remain lens.";
        LOGGER.log(
                Level.INFO,
                "Created lens with item ID {0}.",
                id);
    }

    // @@author

    @Override
    public String getType() {
        return TYPE;
    }

    public String getZoomRange() {
        return zoomRange;
    }

    public LensDamage getDamage() {
        return damage;
    }

    // @@author Rohita-29

    /** Returns the lens mount used by this lens. */
    public String getLensMount() {
        return lensMount;
    }

    /** Returns the sensor format supported by this lens. */
    public String getSensorFormat() {
        return sensorFormat;
    }

    @Override
    protected void applyDamageAssessment(Equipment assessed) {
        Lens lens = (Lens) assessed;
        if (!zoomRange.equals(lens.zoomRange) || lens.damage == null) {
            throw new IllegalArgumentException("A return assessment cannot change the lens zoom range.");
        }
        damage = lens.damage;
    }

    @Override
    public String getDetails() {
        StringBuilder details = new StringBuilder("zoom/")
                .append(zoomRange);

        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.MOUNT,
                lensMount);
        EquipmentDetailFormatter.appendIfSpecified(
                details,
                EquipmentDetailField.SENSOR,
                sensorFormat);

        return details.append(' ')
                .append(EquipmentDetailField.DAMAGE.getMarker())
                .append(damage.name())
                .toString();
    }

    // @@author
}
