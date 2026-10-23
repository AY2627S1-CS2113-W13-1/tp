package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a camera in the equipment inventory. */
public class Camera extends Equipment {
    private static final String TYPE = "camera";
    private static final Logger LOGGER = Logger.getLogger(Camera.class.getName());

    /** Damage states that can be recorded for a camera. */
    public enum CameraDamage {
        NONE,
        SENSOR_DAMAGE,
        SHUTTER_FAULT
    }

    private final String sensorFormat;
    private final CameraDamage damage;

    /**
     * Creates a camera with unspecified sensor format and no recorded damage.
     *
     * @param name Name of the camera.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the camera from 1 to 10.
     */
    public Camera(String name, String id, int condition) {
        this(name, id, condition, "unspecified", CameraDamage.NONE);
    }

    /**
     * Creates a camera with its type-specific details.
     * Callers must validate user input before construction.
     *
     * @param name Name of the camera.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the camera from 1 to 10.
     * @param sensorFormat Sensor format description, from 1 to 100 characters without line breaks.
     * @param damage Damage recorded for the camera.
     */
    public Camera(String name, String id, int condition, String sensorFormat, CameraDamage damage) {
        this(name, id, condition, sensorFormat, damage, "");
    }

    /** Creates a camera with type-specific details and optional remarks. */
    public Camera(String name, String id, int condition, String sensorFormat, CameraDamage damage, String remarks) {
        super(name, id, condition, remarks);
        assert sensorFormat != null && !sensorFormat.isBlank() && sensorFormat.length() <= 100
                && !sensorFormat.matches("(?s).*\\R.*")
                : "Camera sensor format must contain 1 to 100 characters without line breaks.";
        assert damage != null : "Camera damage must be specified.";
        this.sensorFormat = sensorFormat;
        this.damage = damage;
        assert TYPE.equals(getType()) : "Camera type must remain camera.";
        LOGGER.log(Level.INFO, "Created camera with item ID {0}.", id);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getSensorFormat() {
        return sensorFormat;
    }

    public CameraDamage getDamage() {
        return damage;
    }

    @Override
    public String getDetails() {
        return "sensor/" + sensorFormat + " damage/" + damage.name();
    }
}
