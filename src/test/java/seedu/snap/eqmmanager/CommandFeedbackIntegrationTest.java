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
        // @@author yarnieee
        manager.executeInput("eqm-add name/Alpha camera e/E001 type/camera cond/4");
        manager.executeInput("eqm-add name/Studio lens e/E002 type/lens cond/2");
        manager.executeInput("eqm-add name/Spare battery e/E003 type/battery cond/3");
        // @@author
        output.reset();
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOutput);
    }

    @Test
    void loan_successShowsAssignedIdItemBorrowerAndDates() throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U222 issued/24-10-26 due/27-10-26");

        assertTrue(printed().contains("[OK] Loan L001 issued."));
        assertTrue(printed().contains("Item: E001 \"Alpha camera\""));
        assertTrue(printed().contains("Borrower: U222 | Issued: 24-10-26 | Due: 27-10-26"));
        // @@author
        assertTrue(printed().contains("Status: Loaned"));
        assertFalse(printed().contains("T00:00"));
        // @@author yarnieee
        assertEquals("L002", field(manager, "loans", LoanHandler.class).getNextLoanId());
        assertTrue(Files.readString(dataDirectory.resolve("loans.txt")).contains("loanId/L001 id/E001 b/U222"));
        // @@author
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void return_successShowsTheResultingAvailability(boolean serviceable) throws Exception {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U222 issued/24-10-26 due/27-10-26");
        // @@author
        output.reset();

        // @@author yarnieee
        manager.executeInput("loan-return l/L001 serviceable/" + (serviceable ? "yes" : "no") + " damage/NONE");

        assertTrue(printed().contains("[OK] Loan L001 returned."));
        assertTrue(printed().contains("Item: E001 \"Alpha camera\""));
        // @@author
        assertTrue(printed().contains("Status: " + (serviceable ? "Available" : "Unserviceable")));
        // @@author yarnieee
        Equipment item = field(manager, "equipment", EquipmentHandler.class).getEquipmentById("E001");
        // @@author
        assertEquals(serviceable, item.isServiceable());
        assertFalse(item.isLoaned());
        assertTrue(field(manager, "loans", LoanHandler.class).getLoanList().getFirst().isReturned());
    }

    @Test
    void bundle_mixedBatchLabelsEveryResultAndExplainsFinalComposition() {
        // @@author yarnieee
        manager.executeInput("eqm-add name/Second camera e/E004 type/camera cond/2");
        manager.executeInput("bundle-create name/Review kit");
        assertTrue(printed().contains("Bundle B001 \"Review kit\" created."));
        // @@author
        assertTrue(printed().contains("Incomplete (missing camera, lens, battery)"));
        output.reset();

        // @@author yarnieee
        manager.executeInput("bundle-add-eqm b/B001 E001 E999 E004 E002 E003 E001");
        // @@author

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
        // @@author yarnieee
        manager.executeInput("bundle-create name/Review kit");
        manager.executeInput("bundle-add-eqm b/B001 E001 E002 E003");
        // @@author
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

        // @@author yarnieee
        manager.executeInput("bundle-remove-eqm b/B001 E001");
        // @@author

        assertTrue(printed().contains("[ERROR] Could not save bundle removal:"));
        assertFalse(printed().contains("[OK]"));
        assertEquals(before, bundle.getEquipmentList());
        assertTrue(bundle.isComplete());
        assertEquals("keep bundle data", Files.readString(sentinel));
    }

    @Test
    void deletingReturnedHistoryDoesNotDescribeAReplacementWithTheSameVisibleId() {
        // @@author yarnieee
        manager.executeInput("loan-add e/E001 u/U222 issued/24-10-26 due/27-10-26");
        manager.executeInput("loan-return l/L001 serviceable/yes damage/NONE");
        manager.executeInput("eqm-delete e/E001");
        manager.executeInput("eqm-add name/Replacement e/E001 type/camera cond/1");
        // @@author
        output.reset();

        // @@author yarnieee
        manager.executeInput("loan-delete l/L001");

        assertTrue(printed().contains("[OK] Loan L001 deleted."));
        // @@author
        assertTrue(printed().contains("original equipment is not in the inventory"));
        assertFalse(printed().contains("Replacement"));
        assertFalse(printed().contains("Alpha camera"));
    }

    @Test
    void rejectedLoanUsesErrorLabelAndPreservesFilesAndIdentifiers() throws Exception {
        EquipmentHandler equipment = field(manager, "equipment", EquipmentHandler.class);
        LoanHandler loans = field(manager, "loans", LoanHandler.class);
        String inventory = Files.readString(dataDirectory.resolve("equipment.txt"));

        // @@author yarnieee
        manager.executeInput("loan-add e/E999 u/U222 issued/24-10-26 due/27-10-26");
        // @@author

        assertTrue(printed().startsWith("[ERROR]"));
        assertFalse(printed().contains("[OK]"));
        // @@author yarnieee
        assertEquals("L001", loans.getNextLoanId());
        // @@author
        assertEquals(0, loans.getLoanCount());
        // @@author yarnieee
        assertFalse(equipment.getEquipmentById("E001").isLoaned());
        // @@author
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
