package seedu.snap.eqmmanager;

// @@author Rohita-29

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests bundle compatibility validation through the application command interface. */
class BundleCompatibilityCommandTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void redirectOutput() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void addToBundle_incompatibleAndCompatibleCandidates_reportsAndStoresEachResult() {
        EqmManager manager = new EqmManager(temporaryDirectory);
        manager.executeInput("add i/Camera id/001 type/camera cond/1 sensor/APS-C"
                + " mount/Canon RF battery/LP-E17");
        manager.executeInput("add i/Incompatible Lens id/002 type/lens cond/1"
                + " mount/Nikon Z sensor/APS-C");
        manager.executeInput("add i/Compatible Battery id/003 type/battery cond/1"
                + " battery/lp-e17");
        manager.executeInput("add-bundle name/Camera Kit");
        manager.executeInput("add-to-bundle id/001 001");
        output.reset();

        manager.executeInput("add-to-bundle id/001 002 003");

        String expected =
                "Equipment ID 002 was not added: equipment is incompatible with equipment ID 001."
                        + System.lineSeparator()
                        + "Equipment ID 003 added to bundle 001 successfully."
                        + System.lineSeparator();
        assertEquals(expected, output.toString(StandardCharsets.UTF_8));

        output.reset();
        manager.executeInput("list bundles");
        String listing = output.toString(StandardCharsets.UTF_8);

        assertTrue(listing.contains("Camera"));
        assertTrue(listing.contains("Compatible Battery"));
        assertFalse(listing.contains("Incompatible Lens"));
    }
}

// @@author
