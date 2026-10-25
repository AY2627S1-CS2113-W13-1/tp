package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.snap.equipment.Camera;
import seedu.snap.equipment.Camera.CameraDamage;

// @@author Rohita-29

/** Tests persistence of issue-time equipment condition snapshots. */
class LoanConditionSnapshotStorageTest {
    private static final LocalDateTime ISSUE_DATE =
            LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime DUE_DATE =
            LocalDateTime.of(2026, 10, 20, 0, 0);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void saveAndLoad_snapshotWithSpecialCharacters_restoresSnapshot()
            throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Camera camera =
                new Camera(
                        "Test Camera",
                        "001",
                        6,
                        "Full frame",
                        CameraDamage.SENSOR_DAMAGE,
                        "Lens + mount / checked ✓");
        Loan original =
                new Loan(
                        "001",
                        camera,
                        "101",
                        ISSUE_DATE,
                        DUE_DATE);

        LoanStorage.save(file, List.of(original));
        Loan loaded = LoanStorage.load(file).getFirst();

        assertEquals(
                original.getIssueConditionSnapshot(),
                loaded.getIssueConditionSnapshot());
        assertTrue(
                Files.readString(file)
                        .contains("issueType/camera"));
        assertTrue(
                Files.readString(file)
                        .contains("issueCondition/6"));
    }

    @Test
    void load_legacyRecordWithoutSnapshot_keepsSnapshotEmpty()
            throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(
                file,
                "loan loanId/001 id/001 b/101 "
                        + "issued/01-10-26 due/20-10-26 "
                        + "returned/false\n");

        Loan loaded = LoanStorage.load(file).getFirst();

        assertNull(loaded.getIssueConditionSnapshot());
    }

    @Test
    void load_invalidSnapshotCondition_rejectsRecord()
            throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(
                file,
                "loan loanId/001 id/001 b/101 "
                        + "issued/01-10-26 due/20-10-26 "
                        + "returned/false "
                        + "issueType/camera "
                        + "issueCondition/not-a-number "
                        + "issueDamage/NONE "
                        + "issueRemarks/\n");

        IOException exception =
                assertThrows(
                        IOException.class,
                        () -> LoanStorage.load(file));

        assertTrue(exception.getMessage().contains("line 1"));
    }

    @Test
    void load_incompleteSnapshot_rejectsRecord()
            throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(
                file,
                "loan loanId/001 id/001 b/101 "
                        + "issued/01-10-26 due/20-10-26 "
                        + "returned/false "
                        + "issueType/camera "
                        + "issueCondition/6\n");

        IOException exception =
                assertThrows(
                        IOException.class,
                        () -> LoanStorage.load(file));

        assertTrue(exception.getMessage().contains("line 1"));
    }
}

// @@author
