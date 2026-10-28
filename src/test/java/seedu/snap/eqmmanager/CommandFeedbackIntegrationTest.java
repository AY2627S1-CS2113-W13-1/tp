package seedu.snap.eqmmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.bundle.Bundle;
import seedu.snap.bundle.BundleHandler;
import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;
import seedu.snap.loan.LoanHandler;

/** Checks that labelled outcomes describe committed changes and never report success after a failed save. */
class CommandFeedbackIntegrationTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOutput;
    private EqmManager manager;
    @TempDir
    private Path dataDirectory;

    @BeforeEach
    void setUp() {
        originalOutput = System.out;
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);
        manager = new EqmManager(dataDirectory, clock);
        manager.executeInput("add i/Alpha camera id/001 type/camera cond/4");
        manager.executeInput("add i/Studio lens id/002 type/lens cond/2");
        manager.executeInput("add i/Spare battery id/003 type/battery cond/3");
        output.reset();
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void loan_successShowsAssignedIdItemBorrowerAndDates() throws Exception {
        manager.executeInput("loan id/001 b/222 issued/24-10-26 due/27-10-26");

        assertTrue(printed().contains("[OK] Loan 001 issued."));
        assertTrue(printed().contains("Item: 001 \"Alpha camera\""));
        assertTrue(printed().contains("Borrower: 222 | Issued: 24-10-26 | Due: 27-10-26"));
        assertTrue(printed().contains("Status: Loaned"));
        assertFalse(printed().contains("T00:00"));
        assertEquals("002", field(manager, "loans", LoanHandler.class).getNextLoanId());
        assertTrue(Files.readString(dataDirectory.resolve("loans.txt")).contains("loanId/001 id/001 b/222"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void return_successShowsTheResultingAvailability(boolean serviceable) throws Exception {
        manager.executeInput("loan id/001 b/222 issued/24-10-26 due/27-10-26");
        output.reset();

        manager.executeInput("return loan/001 serviceable/" + (serviceable ? "yes" : "no") + " damage/NONE");

        assertTrue(printed().contains("[OK] Loan 001 returned."));
        assertTrue(printed().contains("Item: 001 \"Alpha camera\""));
        assertTrue(printed().contains("Status: " + (serviceable ? "Available" : "Unserviceable")));
        Equipment item = field(manager, "equipment", EquipmentHandler.class).getEquipmentById("001");
        assertEquals(serviceable, item.isServiceable());
        assertFalse(item.isLoaned());
        assertTrue(field(manager, "loans", LoanHandler.class).getLoanList().getFirst().isReturned());
    }

    @Test
    void bundle_mixedBatchLabelsEveryResultAndExplainsFinalComposition() {
        manager.executeInput("add i/Second camera id/004 type/camera cond/2");
        manager.executeInput("add-bundle name/Review kit");
        assertTrue(printed().contains("Bundle 001 \"Review kit\" created."));
        assertTrue(printed().contains("Incomplete (missing camera, lens, battery)"));
        output.reset();

        manager.executeInput("add-to-bundle id/001 001 999 004 002 003 001");

        assertEquals(3, printed().lines().filter(line -> line.startsWith("[OK]")).count());
        assertEquals(3, printed().lines().filter(line -> line.startsWith("[ERROR]")).count());
        assertTrue(printed().contains("Name: \"Alpha camera\""));
        assertTrue(printed().contains("Name: \"Studio lens\""));
        assertTrue(printed().contains("Name: \"Spare battery\""));
        assertTrue(printed().contains("3 memberships added; 3 failed."));
        assertTrue(printed().contains("Composition: Complete"));
    }

    @Test
    void bundle_failedRemovalSaveReportsOnlyErrorAndRestoresMembers() throws Exception {
        manager.executeInput("add-bundle name/Review kit");
        manager.executeInput("add-to-bundle id/001 001 002 003");
        BundleHandler bundles = field(manager, "bundles", BundleHandler.class);
        bundles.saveToStorage();
        Bundle bundle = bundles.getBundleList().getFirst();
        List<Equipment> before = List.copyOf(bundle.getEquipmentList());
        Path file = dataDirectory.resolve("bundles.txt");
        Files.delete(file);
        Files.createDirectory(file);
        Path sentinel = file.resolve("keep.txt");
        Files.writeString(sentinel, "keep bundle data");
        output.reset();

        manager.executeInput("remove-from-bundle id/001 001");

        assertTrue(printed().contains("[ERROR] Could not save bundle removal:"));
        assertFalse(printed().contains("[OK]"));
        assertEquals(before, bundle.getEquipmentList());
        assertTrue(bundle.isComplete());
        assertEquals("keep bundle data", Files.readString(sentinel));
    }

    @Test
    void deletingReturnedHistoryDoesNotDescribeAReplacementWithTheSameVisibleId() {
        manager.executeInput("loan id/001 b/222 issued/24-10-26 due/27-10-26");
        manager.executeInput("return loan/001 serviceable/yes damage/NONE");
        manager.executeInput("delete item/001");
        manager.executeInput("add i/Replacement id/001 type/camera cond/1");
        output.reset();

        manager.executeInput("delete loan/001");

        assertTrue(printed().contains("[OK] Loan 001 deleted."));
        assertTrue(printed().contains("original equipment is not in the inventory"));
        assertFalse(printed().contains("Replacement"));
        assertFalse(printed().contains("Alpha camera"));
    }

    @Test
    void rejectedLoanUsesErrorLabelAndPreservesFilesAndIdentifiers() throws Exception {
        EquipmentHandler equipment = field(manager, "equipment", EquipmentHandler.class);
        LoanHandler loans = field(manager, "loans", LoanHandler.class);
        String inventory = Files.readString(dataDirectory.resolve("equipment.txt"));

        manager.executeInput("loan id/999 b/222 issued/24-10-26 due/27-10-26");

        assertTrue(printed().startsWith("[ERROR]"));
        assertFalse(printed().contains("[OK]"));
        assertEquals("001", loans.getNextLoanId());
        assertEquals(0, loans.getLoanCount());
        assertFalse(equipment.getEquipmentById("001").isLoaned());
        assertEquals(inventory, Files.readString(dataDirectory.resolve("equipment.txt")));
        assertFalse(Files.exists(dataDirectory.resolve("loans.txt")));
    }

    /** Reads handlers in tests without adding application accessors for test-only purposes. */
    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private String printed() {
        return output.toString(StandardCharsets.UTF_8).replaceAll("\u001b\\[[;\\d]*m", "");
    }
}
