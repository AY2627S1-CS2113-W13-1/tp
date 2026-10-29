package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks list-command routing without allowing malformed queries to display another record type. */
class ListCommandIntegrationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);
    private static final List<String> STATE_QUERIES = List.of(
            "list", "list loans", "list bundles", "show id/001", "history id/001", "view loan/001");
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;

    @TempDir
    private Path dataDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        manager = new EqmManager(dataDirectory, CLOCK);
        manager.executeInput("add i/Camera id/001 type/camera cond/1");
        manager.executeInput("add i/Lens id/002 type/lens cond/2");
        manager.executeInput("add i/Battery id/003 type/battery cond/3");
        manager.executeInput("add i/Tripod id/004 type/tripod cond/4");
        manager.executeInput("add-bundle name/Test kit");
        manager.executeInput("add-to-bundle id/001 001 002 003");
        manager.executeInput("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("loan id/002 b/222 issued/01-10-26 due/31-10-26");
        manager.executeInput("loan id/003 b/111 issued/01-10-26 due/02-10-26");
        manager.executeInput("return loan/003 serviceable/yes damage/NONE");
        manager.executeInput("maintain id/001 date/01-10-26 work/Checked camera outcome/Completed");
        manager.executeInput("bye");
        manager = new EqmManager(dataDirectory, CLOCK);
        output.reset();
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @ParameterizedTest
    @ValueSource(strings = {"list overude", "list loans extra", "list bundles extra",
        "list overdue extra", "list unknown", "list loans b/111"})
    void invalidList_reportsErrorWithoutChangingRecordsFilesOrIdentifiers(String command) throws Exception {
        Map<String, String> recordsBefore = visibleRecords();
        Map<Path, byte[]> contentsBefore = new LinkedHashMap<>();
        Map<Path, FileTime> modifiedBefore = new LinkedHashMap<>();
        for (String name : List.of("equipment.txt", "loans.txt", "loan-sequence.txt",
                "bundles.txt", "maintenance.txt")) {
            Path file = dataDirectory.resolve(name);
            contentsBefore.put(file, Files.readAllBytes(file));
            modifiedBefore.put(file, Files.getLastModifiedTime(file));
        }

        String result = execute(command);

        assertEquals("[ERROR] Command not recognised. Type help for available commands."
                + System.lineSeparator(), result);
        assertEquals(recordsBefore, visibleRecords());
        for (Path file : contentsBefore.keySet()) {
            assertArrayEquals(contentsBefore.get(file), Files.readAllBytes(file), file.toString());
            assertEquals(modifiedBefore.get(file), Files.getLastModifiedTime(file), file.toString());
        }
        assertTrue(execute("loan id/004 b/333 due/31-10-26").contains("[OK] Loan 004 issued."));
        assertTrue(execute("add-bundle name/Next kit").contains("[OK] Bundle 002 \"Next kit\" created."));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "list | Equipment | 4 equipment items.",
        "list loans | Loans | 3 loans.",
        "list overdue | Overdue loans | 1 loan.",
        "list bundles | Bundles | 1 bundle.",
        "list borrower/111 | Equipment currently held by borrower ID 111: | 1 loan.",
        "list member/111 | Equipment currently held by borrower ID 111: | 1 loan."
    })
    void validList_selectsTheRequestedRecords(String command, String heading, String count) {
        String result = execute(command);

        assertTrue(result.startsWith(heading + System.lineSeparator()), result);
        assertTrue(result.contains(count), result);
        assertFalse(result.contains("[ERROR]"), result);
        if (command.equals("list overdue") || command.contains("/111")) {
            assertTrue(result.contains("Overdue"), result);
            assertFalse(result.contains("Returned"), result);
            assertFalse(result.contains("222"), result);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"  list  ", "\tlist\t", "\u2003list\u00a0"})
    void plainList_surroundingWhitespaceStillAccepted(String command) {
        assertEquals(execute("list"), execute(command));
    }

    /** Captures observable records without exposing application internals just for this test. */
    private Map<String, String> visibleRecords() {
        Map<String, String> records = new LinkedHashMap<>();
        for (String query : STATE_QUERIES) {
            records.put(query, execute(query));
        }
        return records;
    }

    /** Executes one command and returns only its plain response. */
    private String execute(String command) {
        output.reset();
        manager.executeInput(command);
        return output.toString(StandardCharsets.UTF_8).replaceAll("\u001b\\[[;\\d]*m", "");
    }
}
