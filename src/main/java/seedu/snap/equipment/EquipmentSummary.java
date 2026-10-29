package seedu.snap.equipment;

import java.util.List;

import seedu.snap.loan.Loan;

/** Immutable, read-only view of one equipment item and its associated loan history. */
public final class EquipmentSummary {
    private final Equipment equipment;
    private final List<Loan> loanHistory;

    /** Creates a summary and validates that every supplied loan belongs to the selected physical item. */
    public EquipmentSummary(Equipment equipment, List<Loan> loanHistory) {
        if (equipment == null || loanHistory == null) {
            throw new IllegalArgumentException("A summary requires equipment and loan history.");
        }
        for (Loan loan : loanHistory) {
            if (loan == null || !equipment.getItemId().equals(loan.getItemId())
                    || loan.getEquipmentRecordId() != null
                    && !equipment.getRecordId().equals(loan.getEquipmentRecordId())) {
                throw new IllegalArgumentException("Loan history must belong to the selected equipment.");
            }
        }
        this.equipment = equipment;
        this.loanHistory = List.copyOf(loanHistory);
    }

    /** Returns the selected equipment item. */
    public Equipment getEquipment() {
        return equipment;
    }

    /** Returns an immutable loan-history snapshot. */
    public List<Loan> getLoanHistory() {
        return loanHistory;
    }

    /** Formats the summary for the text user interface without changing any application state. */
    public String format() {
        StringBuilder summary = new StringBuilder();
        appendLine(summary, "Equipment summary for item ID " + equipment.getItemId() + ":");
        appendLine(summary, "IDENTITY");
        appendLine(summary, "  Item ID: " + equipment.getItemId());
        appendLine(summary, "  Record ID: " + equipment.getRecordId());
        appendLine(summary, "  Name: " + equipment.getItemName());
        appendLine(summary, "  Type: " + equipment.getType());
        appendLine(summary, "CURRENT DETAILS");
        appendLine(summary, "  Condition: " + equipment.getCondition());
        appendLine(summary, "  Status: " + displayStatus());
        appendLine(summary, "  Serviceable: " + (equipment.isServiceable() ? "yes" : "no"));
        appendLine(summary, "  Remarks: " + displayOptional(equipment.getRemarks()));
        appendLine(summary, "  Subtype details: " + displayOptional(equipment.getDetails()));
        appendLoanHistory(summary);
        appendLine(summary, "STATUS HISTORY");
        appendLine(summary, "  No status history recorded.");
        appendLine(summary, "MAINTENANCE HISTORY");
        appendLine(summary, "  No maintenance history recorded.");
        appendLine(summary, "STORAGE LOCATION");
        appendLine(summary, "  No storage location recorded.");
        return summary.toString();
    }

    /** Formats the current availability using the same precedence as the equipment list. */
    private String displayStatus() {
        return equipment.isLoaned() ? "Loaned" : equipment.isServiceable() ? "Available" : "Unserviceable";
    }

    /** Appends the loan section while handling an item that has never been loaned. */
    private void appendLoanHistory(StringBuilder summary) {
        appendLine(summary, "LOAN HISTORY");
        if (loanHistory.isEmpty()) {
            appendLine(summary, "  No loan history recorded.");
            return;
        }
        appendLine(summary, "  LOAN ID | BORROWER | ISSUE DATE | DUE DATE | RETURNED");
        for (Loan loan : loanHistory) {
            appendLine(summary, "  " + loan.getLoanId() + " | " + loan.getBorrowerId() + " | "
                    + loan.getIssuedDate().toLocalDate() + " | " + loan.getDueDate().toLocalDate() + " | "
                    + loan.isReturned());
        }
    }

    /** Displays empty optional text consistently without changing the stored value. */
    private static String displayOptional(String value) {
        return value == null || value.isEmpty() ? "(none)" : value;
    }

    /** Appends one platform-independent summary line. */
    private static void appendLine(StringBuilder summary, String line) {
        summary.append(line).append(System.lineSeparator());
    }
}
