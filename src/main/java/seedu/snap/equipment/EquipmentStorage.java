package seedu.snap.equipment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.exceptions.EquipmentAdditionUnsuccessful;

/** Loads and saves equipment records in a readable UTF-8 text file. */
final class EquipmentStorage {
    private static final Logger logger = Logger.getLogger(EquipmentStorage.class.getName());
    private EquipmentStorage() {
        // Utility class; do not instantiate.
    }

    /**
     * Loads one add command per non-blank line from the supplied file.
     * A missing file represents an empty inventory.
     *
     * @param file file containing saved equipment
     * @return parsed equipment records
     * @throws IOException if the file cannot be read or contains an invalid record
     */
    static List<Equipment> load(Path file) throws IOException {
        List<Equipment> loaded = new ArrayList<>();
        if (Files.notExists(file)) {
            return loaded;
        }

        Set<String> ids = new HashSet<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            try {
                int loanIdMarker = line.lastIndexOf(" loanId/");
                int legacyStateMarker = line.lastIndexOf(" loaned/");
                int stateMarker = Math.max(loanIdMarker, legacyStateMarker);
                String equipmentRecord = stateMarker < 0 ? line : line.substring(0, stateMarker);
                Equipment equipment = EquipmentHandler.parseEquipment(equipmentRecord);
                if (loanIdMarker >= 0) {
                    String loanId = line.substring(loanIdMarker + " loanId/".length());
                    if (!loanId.equals("none") && !isValidLoanId(loanId)) {
                        throw new IllegalArgumentException("Expected loanId/NNN or loanId/none.");
                    }
                    equipment.setLoanId(loanId.equals("none") ? null : loanId);
                } else if (legacyStateMarker >= 0) {
                    String state = line.substring(legacyStateMarker + " loaned/".length());
                    if (!state.equals("true") && !state.equals("false")) {
                        throw new IllegalArgumentException("Expected loaned/true or loaned/false.");
                    }
                    if (Boolean.parseBoolean(state)) {
                        // Legacy records do not identify which loan owns the item. The loan file is
                        // authoritative and LoanHandler reconciles this state after both files load.
                        logger.log(Level.WARNING, "Legacy equipment record for item ID {0} says loaned=true; "
                                + "waiting for an active loan ID during reconciliation.", equipment.getItemId());
                    }
                }
                if (!ids.add(equipment.getItemId())) {
                    throw new IllegalArgumentException("Duplicate equipment ID " + equipment.getItemId() + ".");
                }
                loaded.add(equipment);
            } catch (EquipmentAdditionUnsuccessful | IllegalArgumentException exception) {
                throw new IOException("Invalid equipment record on line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }
        return loaded;
    }

    /**
     * Saves records through a temporary file before replacing the destination.
     * This preserves the previous file if writing the new contents fails.
     *
     * @param file destination file
     * @param equipment records to save
     * @throws IOException if the directory or file cannot be written
     */
    static void save(Path file, List<Equipment> equipment) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "equipment-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (Equipment item : equipment) {
                assert item != null : "Equipment being saved must not be null";
                lines.add(toStorageLine(item));
            }
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** Converts an equipment record to the documented add-command format. */
    private static String toStorageLine(Equipment equipment) {
        return "add i/" + equipment.getItemName()
                + " id/" + equipment.getItemId()
                + " type/" + equipment.getType()
                + " cond/" + equipment.getCondition()
                + " loanId/" + (equipment.getLoanId() == null ? "none" : equipment.getLoanId());
    }

    /** Returns whether an equipment record contains a usable persisted loan identifier. */
    private static boolean isValidLoanId(String loanId) {
        return loanId.matches("[0-9]{3}") && !loanId.equals("000");
    }
}
