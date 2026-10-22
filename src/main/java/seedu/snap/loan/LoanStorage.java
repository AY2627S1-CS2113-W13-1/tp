package seedu.snap.loan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import seedu.snap.exceptions.LoanAdditionUnsuccessful;

/** Loads and saves loan records in a readable UTF-8 text file. */
final class LoanStorage {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu");

    private LoanStorage() {
        // Utility class; do not instantiate.
    }

    /**
     * Loads loan records from each non-blank line in the supplied file.
     * The optional returned field is accepted for compatibility with older files;
     * newly saved records always include it.
     *
     * @param file file containing saved loans
     * @return parsed loan records
     * @throws IOException if the file cannot be read or contains an invalid record
     */
    static List<Loan> load(Path file) throws IOException {
        List<Loan> loaded = new ArrayList<>();
        if (Files.notExists(file)) {
            return loaded;
        }

        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            try {
                String[] fields = line.trim().split("\\s+");
                if (fields.length != 5 && fields.length != 6) {
                    throw new IllegalArgumentException("Expected five loan fields.");
                }

                StringBuilder command = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    if (i > 0) {
                        command.append(' ');
                    }
                    command.append(fields[i]);
                }
                Loan loan = LoanHandler.parseLoanRecord(command.toString());
                if (fields.length == 6) {
                    if (!fields[5].startsWith("returned/")) {
                        throw new IllegalArgumentException("Expected returned/true or returned/false.");
                    }
                    String returned = fields[5].substring("returned/".length());
                    if (!returned.equals("true") && !returned.equals("false")) {
                        throw new IllegalArgumentException("Expected returned/true or returned/false.");
                    }
                    loan.setReturned(Boolean.parseBoolean(returned));
                }
                loaded.add(loan);
            } catch (LoanAdditionUnsuccessful | IllegalArgumentException exception) {
                throw new IOException("Invalid loan record on line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }
        return loaded;
    }

    /** Saves records through a temporary file before replacing the destination. */
    static void save(Path file, List<Loan> loans) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "loans-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (Loan loan : loans) {
                assert loan != null : "Loan being saved must not be null";
                lines.add(toStorageLine(loan));
            }
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** Converts a loan into a readable, reloadable text record. */
    private static String toStorageLine(Loan loan) {
        return "loan id/" + loan.getItemId()
                + " b/" + loan.getBorrowerId()
                + " issued/" + DATE_FORMATTER.format(loan.getIssuedDate())
                + " due/" + DATE_FORMATTER.format(loan.getDueDate())
                + " returned/" + loan.isReturned();
    }
}
