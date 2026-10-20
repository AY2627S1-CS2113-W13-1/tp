package seedu.snap.equipment;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a camera in the equipment inventory. */
public class Camera extends Equipment {
    private static final String TYPE = "camera";
    private static final Logger LOGGER = Logger.getLogger(Camera.class.getName());

    /**
     * Creates a camera.
     *
     * @param name Name of the camera.
     * @param id Three-digit equipment ID.
     * @param condition Condition of the camera from 1 to 10.
     */
    public Camera(String name, String id, int condition) {
        super(name, id, condition);
        assert TYPE.equals(getType()) : "Camera type must remain camera.";
        LOGGER.log(Level.INFO, "Created camera with item ID {0}.", id);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
