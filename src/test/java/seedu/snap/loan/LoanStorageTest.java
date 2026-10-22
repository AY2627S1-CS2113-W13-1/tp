package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests loan storage validation and failure-safe file replacement. */
class LoanStorageTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void load_malformedRecord_reportsTheLineNumber() throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, "loan id/001 b/002 issued/01-10-26 due/02-10-26\nnot a loan record\n");

        IOException exception = assertThrows(IOException.class, () -> LoanStorage.load(file));

        assertTrue(exception.getMessage().contains("line 2"));
    }

    @Test
    void load_invalidReturnedStatus_rejectsTheSavedFile() throws IOException {
        Path file = temporaryDirectory.resolve("loans.txt");
        Files.writeString(file, "loan id/001 b/002 issued/01-10-26 due/02-10-26 returned/maybe\n");

        IOException exception = assertThrows(IOException.class, () -> LoanStorage.load(file));

        assertTrue(exception.getMessage().contains("returned/true or returned/false"));
    }

    @Test
    void save_failedReplacement_preservesDestinationAndCleansTemporaryFile() throws IOException {
        Path destination = temporaryDirectory.resolve("loans.txt");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("keep.txt"), "keep this record");
        Loan loan = new Loan("001", "002", LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 2, 0, 0));

        assertThrows(IOException.class, () -> LoanStorage.save(destination, List.of(loan)));

        assertEquals("keep this record", Files.readString(destination.resolve("keep.txt")));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("loans-")));
        }
    }
}
