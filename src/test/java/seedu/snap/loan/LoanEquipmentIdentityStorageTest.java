package seedu.snap.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import seedu.snap.equipment.Equipment;
import seedu.snap.equipment.EquipmentHandler;

/** Verifies malformed permanent identities cannot replace valid live loan associations. */
class LoanEquipmentIdentityStorageTest {
    @TempDir
    private Path temporaryDirectory;

    @ParameterizedTest
    @ValueSource(strings = {"equipment/not-a-uuid", "equipment/1-1-1-1-1", "equipment/",
        "equipment/E21C9254-AB91-4624-82EB-9B4423E5A795",
        "equipment/e21c9254-ab91-4624-82eb-9b4423e5a795 equipment/e21c9254-ab91-4624-82eb-9b4423e5a795",
        "equipment/e21c9254-ab91-4624-82eb-9b4423e5a795 returned/true", "returned/true returned/false",
        "returned/maybe", "unrecognised/value"})
    void load_invalidIdentityMetadata_keepsLiveLoanAndSavedBytes(String metadata) throws Exception {
        Path equipmentFile = temporaryDirectory.resolve("equipment.txt");
        Path loanFile = temporaryDirectory.resolve("loans.txt");
        EquipmentHandler inventory = new EquipmentHandler(equipmentFile);
        // @@author yarnieee
        Equipment camera = inventory.addEquipment("eqm-add name/Camera e/E001 type/camera cond/4");
        // @@author
        LoanHandler handler = new LoanHandler(inventory, loanFile);
        // @@author yarnieee
        Loan original = handler.addLoan("loan-add e/E001 u/U111 issued/01-10-26 due/02-10-26");
        // @@author
        String validLoans = Files.readString(loanFile);
        // @@author yarnieee
        String invalid = "loan loanId/L001 id/E001 b/U111 issued/01-10-26 due/02-10-26 " + metadata + "\n";
        // @@author
        String equipmentBefore = Files.readString(equipmentFile);
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        String sequenceBefore = Files.readString(sequenceFile);
        Files.writeString(loanFile, invalid);

        IOException error = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(error.getMessage().contains("line 1"));
        assertEquals(List.of(original), handler.getLoanList());
        // @@author yarnieee
        assertSame(original, handler.getLoanHistory("eqm-loan-history e/E001").getFirst());
        // @@author
        assertEquals(original.getLoanId(), camera.getLoanId());
        assertTrue(camera.isLoaned());
        // @@author yarnieee
        assertEquals("L002", handler.getNextLoanId());
        // @@author
        assertEquals(invalid, Files.readString(loanFile));
        assertEquals(equipmentBefore, Files.readString(equipmentFile));
        assertEquals(sequenceBefore, Files.readString(sequenceFile));

        Files.writeString(loanFile, validLoans);
        handler.loadFromStorage();

        // @@author yarnieee
        assertEquals(camera.getRecordId(), handler.getLoanHistory("eqm-loan-history e/E001").getFirst()
                // @@author
                .getEquipmentRecordId());
        // @@author yarnieee
        assertEquals("L001", camera.getLoanId());
        // @@author
    }
}
