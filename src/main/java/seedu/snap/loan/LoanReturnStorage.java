package seedu.snap.loan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;

/** Saves a proposed return across equipment and loan files before changing the live objects. */
final class LoanReturnStorage {
    private LoanReturnStorage() {
        // Utility class; do not instantiate.
    }

    /** Restores every original file if an equipment, loan or sequence save fails. */
    static void save(EquipmentHandler inventory, Equipment current, Equipment assessed,
            Path loanFile, List<Loan> loans, Path sequenceFile, int nextLoanNumber) throws IOException {
        List<LoanStorage.FileSnapshot> snapshots = new ArrayList<>();
        capture(snapshots, inventory.getStorageFile());
        capture(snapshots, loanFile);
        if (loanFile != null) {
            capture(snapshots, sequenceFile);
        }
        try {
            inventory.saveReturnAssessment(current, assessed);
            if (loanFile != null) {
                LoanStorage.save(loanFile, loans, sequenceFile, nextLoanNumber);
            }
        } catch (IOException exception) {
            // Attempt each restoration even if an earlier restoration also fails.
            for (LoanStorage.FileSnapshot snapshot : snapshots) {
                try {
                    snapshot.restore();
                } catch (IOException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            throw exception;
        }
    }

    /** In-memory handlers have no file to capture or restore. */
    private static void capture(List<LoanStorage.FileSnapshot> snapshots, Path file) throws IOException {
        if (file != null) {
            snapshots.add(LoanStorage.FileSnapshot.capture(file));
        }
    }
}
