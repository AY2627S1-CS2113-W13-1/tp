package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests command routing and output for equipment deletion. */
class EqmManagerTest {
    private static final String ADD_CAMERA = "add i/Canon EOS R10 id/001 type/camera cond/1";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;

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
    void deleteEquipment_validCommand_printsSuccessMessage() {
        EqmManager manager = new EqmManager();
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("delete item/001");

        assertEquals("Equipment with item ID 001 deleted successfully." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteEquipment_invalidCommands_printExpectedErrors() {
        EqmManager manager = new EqmManager();
        manager.executeInput(ADD_CAMERA);
        output.reset();
        String expectedMessage = "Invalid delete syntax. ITEM ID must be from 001 to 999.";
        String[] invalidCommands = {
            "delete",
            "delete loan/001",
            "delete item/1",
            "delete item/000",
            "delete item/1000",
            "delete item/abc",
            "delete item/001 extra"
        };

        for (String command : invalidCommands) {
            manager.executeInput(command);
            assertEquals(expectedMessage + System.lineSeparator(),
                    output.toString(StandardCharsets.UTF_8), command);
            output.reset();
        }
    }

    @Test
    void deleteEquipment_unknownId_printsExpectedError() {
        EqmManager manager = new EqmManager();
        manager.executeInput(ADD_CAMERA);
        output.reset();

        manager.executeInput("delete item/999");

        assertEquals("Equipment ID 999 does not exist." + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void bundleCommands_addAndListBundle() {
        EqmManager manager = new EqmManager();
        manager.executeInput("add i/Canon EOS R10 id/001 type/camera cond/1");
        manager.executeInput("add i/Canon Battery id/002 type/battery cond/1");
        manager.executeInput("add-bundle name/Travel Kit");
        output.reset();

        manager.executeInput("add-to-bundle id/001 001 002");
        manager.executeInput("list bundles");

        String listing = output.toString(StandardCharsets.UTF_8);
        assertEquals(true, listing.contains("Equipment ID 001 added to bundle 001 successfully."));
        assertEquals(true, listing.contains("Travel Kit"));
        assertEquals(true, listing.contains("camera"));
        assertEquals(true, listing.contains("battery"));
    }
}
