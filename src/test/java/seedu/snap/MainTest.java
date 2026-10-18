package seedu.snap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** Tests application startup and exit through the main entry point. */
class MainTest {
    @Test
    void main_byeCommand_startsAndExits() {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream("bye\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(capturedOutput);

            Main.main(new String[0]);

            assertEquals("Greeting message." + System.lineSeparator()
                    + ">>> Goodbye message." + System.lineSeparator(),
                    output.toString(StandardCharsets.UTF_8));
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
    }
}
