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
        Equipment camera = inventory.addEquipment("add i/Camera id/001 type/camera cond/4");
        LoanHandler handler = new LoanHandler(inventory, loanFile);
        Loan original = handler.addLoan("loan id/001 b/111 issued/01-10-26 due/02-10-26");
        String validLoans = Files.readString(loanFile);
        String invalid = "loan loanId/001 id/001 b/111 issued/01-10-26 due/02-10-26 " + metadata + "\n";
        String equipmentBefore = Files.readString(equipmentFile);
        Path sequenceFile = temporaryDirectory.resolve("loan-sequence.txt");
        String sequenceBefore = Files.readString(sequenceFile);
        Files.writeString(loanFile, invalid);

        IOException error = assertThrows(IOException.class, handler::loadFromStorage);

        assertTrue(error.getMessage().contains("line 1"));
        assertEquals(List.of(original), handler.getLoanList());
        assertSame(original, handler.getLoanHistory("history id/001").getFirst());
        assertEquals(original.getLoanId(), camera.getLoanId());
        assertTrue(camera.isLoaned());
        assertEquals("002", handler.getNextLoanId());
        assertEquals(invalid, Files.readString(loanFile));
        assertEquals(equipmentBefore, Files.readString(equipmentFile));
        assertEquals(sequenceBefore, Files.readString(sequenceFile));

        Files.writeString(loanFile, validLoans);
        handler.loadFromStorage();

        assertEquals(camera.getRecordId(), handler.getLoanHistory("history id/001").getFirst()
                .getEquipmentRecordId());
        assertEquals("001", camera.getLoanId());
    }
}
