package seedu.snap.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import seedu.snap.loan.Loan;

/** Shares readable loan dates, states and result counts across lists and histories. */
public final class LoanView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-uu", Locale.ROOT);
    private static final String BORDER = "+---------+---------+-------------+----------+----------+----------+";

    private LoanView() {
        // Utility class; do not instantiate.
    }

    /** Displays records in the order selected by the handler, without sorting or changing them. */
    public static void print(List<Loan> records, LocalDate referenceDate, String heading, String emptyMessage) {
        Objects.requireNonNull(records, "Loan records must not be null");
        Objects.requireNonNull(referenceDate, "Reference date must not be null");
        ConsoleOutput.heading(heading);
        if (records.isEmpty()) {
            System.out.println(ConsoleOutput.inline(emptyMessage));
        } else {
            System.out.println(BORDER);
            System.out.printf("| %-7s | %-7s | %-11s | %-8s | %-8s | %-8s |%n",
                    "LOAN ID", "ITEM ID", "BORROWER ID", "ISSUED", "DUE", "STATE");
            System.out.println(BORDER);
            for (Loan loan : records) {
                System.out.printf("| %-7s | %-7s | %-11s | %-8s | %-8s | %-8s |%n",
                        loan.getLoanId(), loan.getItemId(), loan.getBorrowerId(),
                        date(loan.getIssuedDate()), date(loan.getDueDate()), state(loan, referenceDate));
            }
            System.out.println(BORDER);
        }
        System.out.println(records.size() + (records.size() == 1 ? " loan." : " loans."));
    }

    /** Formats user-facing dates without exposing storage timestamps or changing the stored value. */
    public static String date(LocalDateTime value) {
        return DATE.format(value);
    }

    /** Reuses the model's date-only overdue rule, with returned history taking precedence. */
    public static String state(Loan loan, LocalDate referenceDate) {
        return loan.isReturned() ? "Returned" : loan.isOverdue(referenceDate) ? "Overdue" : "Active";
    }
}
