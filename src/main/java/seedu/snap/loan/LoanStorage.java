package seedu.snap.loan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.exceptions.LoanAdditionUnsuccessful;

/** Loads and saves loan records in a readable UTF-8 text file. */
final class LoanStorage {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-uu");
    private static final Logger logger = Logger.getLogger(LoanStorage.class.getName());
    private static final int FIRST_LOAN_NUMBER = 1;
    private static final int MAX_LOAN_NUMBER = 999;

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
        return load(file, null).loans();
    }

    /** Loads loans and the next monotonic identifier from their persistence files. */
    static LoadedLoans load(Path file, Path sequenceFile) throws IOException {
        List<LoanRecord> records = new ArrayList<>();
        if (Files.notExists(file)) {
            return new LoadedLoans(List.of(), readNextLoanNumber(sequenceFile, FIRST_LOAN_NUMBER), false);
        }

        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            try {
                records.add(parseRecord(line.trim()));
            } catch (LoanAdditionUnsuccessful | IllegalArgumentException exception) {
                throw new IOException("Invalid loan record on line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }

        Set<String> explicitLoanIds = new HashSet<>();
        for (LoanRecord record : records) {
            if (record.loanId() != null && !explicitLoanIds.add(record.loanId())) {
                throw new IOException("Duplicate loan ID " + record.loanId() + ".");
            }
        }

        int migrationNumber = readNextLoanNumber(sequenceFile, FIRST_LOAN_NUMBER);
        List<Loan> loaded = new ArrayList<>();
        Set<String> allLoanIds = new HashSet<>();
        for (LoanRecord record : records) {
            String loanId = record.loanId();
            if (loanId == null) {
                while (migrationNumber <= MAX_LOAN_NUMBER
                        && explicitLoanIds.contains(formatLoanId(migrationNumber))) {
                    migrationNumber++;
                }
                if (migrationNumber > MAX_LOAN_NUMBER) {
                    throw new IOException("Legacy loan records exceed the 999 loan ID limit.");
                }
                loanId = formatLoanId(migrationNumber++);
                logger.log(Level.INFO, "Assigned migrated loan record the ID {0}.", loanId);
            }
            if (!allLoanIds.add(loanId)) {
                throw new IOException("Duplicate loan ID " + loanId + ".");
            }
            Loan loan = new Loan(loanId, record.itemId(), record.borrowerId(),
                    record.issuedDate(), record.dueDate());
            loan.setReturned(record.returned());
            loaded.add(loan);
        }

        int minimumNextNumber = FIRST_LOAN_NUMBER;
        for (Loan loan : loaded) {
            minimumNextNumber = Math.max(minimumNextNumber, Integer.parseInt(loan.getLoanId()) + 1);
        }
        int nextLoanNumber = readNextLoanNumber(sequenceFile, minimumNextNumber);
        if (nextLoanNumber < minimumNextNumber) {
            logger.log(Level.WARNING, "Loan sequence was behind the loaded records; advancing to {0}.",
                    minimumNextNumber);
            nextLoanNumber = minimumNextNumber;
        }
        boolean migrated = records.stream().anyMatch(record -> record.loanId() == null);
        return new LoadedLoans(loaded, nextLoanNumber, migrated);
    }

    /** Saves records through a temporary file before replacing the destination. */
    static void save(Path file, List<Loan> loans) throws IOException {
        save(file, loans, null, FIRST_LOAN_NUMBER);
    }

    /** Saves loan records and their next-ID sequence, rolling back both files if either replacement fails. */
    static void save(Path file, List<Loan> loans, Path sequenceFile, int nextLoanNumber) throws IOException {
        if (nextLoanNumber < FIRST_LOAN_NUMBER || nextLoanNumber > MAX_LOAN_NUMBER + 1) {
            throw new IOException("Next loan ID is outside the supported range.");
        }
        FileSnapshot loanSnapshot = FileSnapshot.capture(file);
        FileSnapshot sequenceSnapshot = sequenceFile == null ? null : FileSnapshot.capture(sequenceFile);
        try {
            saveLoanFile(file, loans);
            if (sequenceFile != null) {
                saveSequenceFile(sequenceFile, nextLoanNumber);
            }
        } catch (IOException exception) {
            try {
                loanSnapshot.restore();
                if (sequenceSnapshot != null) {
                    sequenceSnapshot.restore();
                }
            } catch (IOException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    /** Saves only the loan records to a temporary replacement file. */
    private static void saveLoanFile(Path file, List<Loan> loans) throws IOException {
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

    /** Saves the monotonic next-ID value through a temporary replacement file. */
    private static void saveSequenceFile(Path file, int nextLoanNumber) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "loan-sequence-", ".tmp");
        try {
            Files.writeString(temporaryFile, "nextLoanId/" + formatSequenceNumber(nextLoanNumber)
                    + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** Converts a loan into a readable, reloadable text record. */
    private static String toStorageLine(Loan loan) throws IOException {
        if (loan.getLoanId() == null) {
            throw new IOException("Saved loans must have a loan ID.");
        }
        return "loan loanId/" + loan.getLoanId()
                + " id/" + loan.getItemId()
                + " b/" + loan.getBorrowerId()
                + " issued/" + DATE_FORMATTER.format(loan.getIssuedDate())
                + " due/" + DATE_FORMATTER.format(loan.getDueDate())
                + " returned/" + loan.isReturned();
    }

    /** Parses either the new loan-ID record format or the legacy format without a loan ID. */
    private static LoanRecord parseRecord(String line) throws LoanAdditionUnsuccessful {
        String[] fields = line.split("\\s+");
        boolean hasLoanId = fields.length >= 2 && fields[1].startsWith("loanId/");
        int expectedMinimumFields = hasLoanId ? 6 : 5;
        int expectedMaximumFields = hasLoanId ? 7 : 6;
        if (fields.length < expectedMinimumFields || fields.length > expectedMaximumFields
                || !fields[0].equals("loan")) {
            throw new IllegalArgumentException("Expected loanId/NNN id/NNN b/NNN issued/DD-MM-YY due/DD-MM-YY.");
        }

        int offset = hasLoanId ? 1 : 0;
        String loanId = hasLoanId ? parseLoanIdField(fields[1]) : null;
        String itemId = parseField(fields[1 + offset], "id/");
        String borrowerId = parseField(fields[2 + offset], "b/");
        String issuedText = parseField(fields[3 + offset], "issued/");
        String dueText = parseField(fields[4 + offset], "due/");
        Loan parsed = LoanHandler.parseLoanRecord("loan id/" + itemId + " b/" + borrowerId
                + " issued/" + issuedText + " due/" + dueText);
        boolean returned = false;
        if (fields.length == expectedMaximumFields) {
            String returnedField = fields[5 + offset];
            if (!returnedField.startsWith("returned/")) {
                throw new IllegalArgumentException("Expected returned/true or returned/false.");
            }
            String returnedValue = returnedField.substring("returned/".length());
            if (!returnedValue.equals("true") && !returnedValue.equals("false")) {
                throw new IllegalArgumentException("Expected returned/true or returned/false.");
            }
            returned = Boolean.parseBoolean(returnedValue);
        }
        return new LoanRecord(loanId, parsed.getItemId(), parsed.getBorrowerId(),
                parsed.getIssuedDate(), parsed.getDueDate(), returned);
    }

    private static String parseLoanIdField(String field) {
        String loanId = parseField(field, "loanId/");
        if (!isValidLoanId(loanId)) {
            throw new IllegalArgumentException("Loan ID must be from 001 to 999.");
        }
        return loanId;
    }

    private static String parseField(String field, String prefix) {
        if (!field.startsWith(prefix)) {
            throw new IllegalArgumentException("Expected " + prefix + " value.");
        }
        return field.substring(prefix.length());
    }

    private static int readNextLoanNumber(Path sequenceFile, int defaultValue) throws IOException {
        if (sequenceFile == null || Files.notExists(sequenceFile)) {
            return defaultValue;
        }
        List<String> lines = Files.readAllLines(sequenceFile, StandardCharsets.UTF_8);
        if (lines.size() != 1 || !lines.getFirst().startsWith("nextLoanId/")) {
            throw new IOException("Invalid loan sequence file.");
        }
        String value = lines.getFirst().substring("nextLoanId/".length());
        try {
            int next = Integer.parseInt(value);
            if (next < FIRST_LOAN_NUMBER || next > MAX_LOAN_NUMBER + 1) {
                throw new NumberFormatException();
            }
            return next;
        } catch (NumberFormatException exception) {
            throw new IOException("Loan sequence must contain nextLoanId/001 through nextLoanId/1000.",
                    exception);
        }
    }

    private static String formatLoanId(int loanNumber) {
        return String.format(Locale.ROOT, "%03d", loanNumber);
    }

    private static String formatSequenceNumber(int loanNumber) {
        return String.format(Locale.ROOT, "%03d", loanNumber);
    }

    private static boolean isValidLoanId(String loanId) {
        return loanId.matches("[0-9]{3}") && !loanId.equals("000");
    }

    /** Result of loading records and their next monotonic identifier. */
    record LoadedLoans(List<Loan> loans, int nextLoanNumber, boolean migrated) {
    }

    private record LoanRecord(String loanId, String itemId, String borrowerId,
            java.time.LocalDateTime issuedDate, java.time.LocalDateTime dueDate, boolean returned) {
    }

    /** Captures a file before a two-file replacement so failed saves can be rolled back. */
    private record FileSnapshot(Path path, boolean existed, boolean regularFile, byte[] contents) {
        static FileSnapshot capture(Path path) throws IOException {
            boolean exists = Files.exists(path);
            boolean regular = exists && Files.isRegularFile(path);
            byte[] bytes = regular ? Files.readAllBytes(path) : new byte[0];
            return new FileSnapshot(path, exists, regular, bytes);
        }

        void restore() throws IOException {
            if (existed && regularFile) {
                Files.write(path, contents);
            } else if (!existed && Files.exists(path) && Files.isRegularFile(path)) {
                Files.delete(path);
            }
        }
    }
}
