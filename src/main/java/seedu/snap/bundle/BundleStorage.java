package seedu.snap.bundle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import seedu.snap.equipment.Equipment;

/** Loads and saves bundle definitions and their physical equipment references. */
final class BundleStorage {
    private static final String RECORD_PREFIX = "bundle";
    private static final String ID_PREFIX = "id/";
    private static final String NAME_PREFIX = "name64/";
    private static final String ITEMS_PREFIX = "items/";
    private static final String ITEM_SEPARATOR = "~";

    private BundleStorage() {
        // Utility class; do not instantiate.
    }

    /**
     * Loads one bundle record per non-blank line.
     *
     * @param file file containing saved bundles
     * @return parsed bundle definitions
     * @throws IOException if a record is malformed or the file cannot be read
     */
    static List<StoredBundle> load(Path file) throws IOException {
        if (Files.notExists(file)) {
            return List.of();
        }

        List<StoredBundle> loaded = new ArrayList<>();
        Set<String> bundleIds = new HashSet<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            try {
                StoredBundle bundle = parseLine(line);
                if (!bundleIds.add(bundle.bundleId())) {
                    throw new IllegalArgumentException("Duplicate bundle ID " + bundle.bundleId() + ".");
                }
                loaded.add(bundle);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid bundle record on line " + lineNumber + ": "
                        + exception.getMessage(), exception);
            }
        }
        return List.copyOf(loaded);
    }

    /** Saves bundle records through a temporary file before replacing the destination. */
    static void save(Path file, List<Bundle> bundles) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "bundles-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (Bundle bundle : bundles) {
                assert bundle != null : "Saved bundles must not contain null items";
                lines.add(toStorageLine(bundle));
            }
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** Converts a bundle to a single-line record with a URL-safe encoded name. */
    private static String toStorageLine(Bundle bundle) throws IOException {
        if (!isValidBundleId(bundle.getBundleId())) {
            throw new IOException("Bundle ID must be from 001 to 999.");
        }
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(bundle.getBundleName().getBytes(StandardCharsets.UTF_8));
        String items = bundle.getEquipmentList().stream()
                .map(BundleStorage::toStoredItem)
                .collect(Collectors.joining(","));
        return RECORD_PREFIX + " " + ID_PREFIX + bundle.getBundleId() + " "
                + NAME_PREFIX + encodedName + " " + ITEMS_PREFIX + items;
    }

    /** Converts an equipment reference into an ID-and-identity pair. */
    private static String toStoredItem(Equipment item) {
        assert item != null : "Saved bundle equipment must not be null";
        assert isValidRecordId(item.getRecordId()) : "Saved equipment must have a canonical record ID";
        return item.getItemId() + ITEM_SEPARATOR + item.getRecordId();
    }

    /** Parses and validates one persisted bundle record. */
    private static StoredBundle parseLine(String line) {
        String[] fields = line.split("\\s+", -1);
        if (fields.length != 4 || !RECORD_PREFIX.equals(fields[0])) {
            throw new IllegalArgumentException("Expected: bundle id/NNN name64/[NAME] items/[ITEMS].");
        }

        String bundleId = parseBundleId(fields[1]);
        String name = decodeName(fields[2]);
        if (!fields[3].startsWith(ITEMS_PREFIX)) {
            throw new IllegalArgumentException("Expected items/ equipment references.");
        }

        String itemText = fields[3].substring(ITEMS_PREFIX.length());
        List<StoredItem> items = itemText.isEmpty() ? List.of() : parseItems(itemText);
        return new StoredBundle(bundleId, name, items);
    }

    /** Parses item ID and permanent record ID pairs while rejecting duplicates. */
    private static List<StoredItem> parseItems(String itemText) {
        List<StoredItem> items = new ArrayList<>();
        Set<String> itemIds = new HashSet<>();
        Set<String> recordIds = new HashSet<>();
        for (String itemTextPart : itemText.split(",", -1)) {
            String[] identity = itemTextPart.split(ITEM_SEPARATOR, -1);
            if (identity.length != 2 || !isValidItemId(identity[0]) || !isValidRecordId(identity[1])) {
                throw new IllegalArgumentException("Bundle equipment references must use ID~record ID.");
            }
            if (!itemIds.add(identity[0])) {
                throw new IllegalArgumentException("Duplicate equipment ID " + identity[0] + " in bundle.");
            }
            if (!recordIds.add(identity[1])) {
                throw new IllegalArgumentException("Duplicate equipment record ID " + identity[1] + " in bundle.");
            }
            items.add(new StoredItem(identity[0], identity[1]));
        }
        return List.copyOf(items);
    }

    /** Decodes and validates the stored bundle name. */
    private static String decodeName(String field) {
        if (!field.startsWith(NAME_PREFIX)) {
            throw new IllegalArgumentException("Expected name64/ bundle name.");
        }
        try {
            String name = new String(Base64.getUrlDecoder().decode(field.substring(NAME_PREFIX.length())),
                    StandardCharsets.UTF_8);
            if (name.isBlank() || name.length() > 100) {
                throw new IllegalArgumentException("Bundle name must contain 1 to 100 characters.");
            }
            return name;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Bundle name is not valid Base64 text.", exception);
        }
    }

    private static String parseBundleId(String field) {
        if (!field.startsWith(ID_PREFIX)) {
            throw new IllegalArgumentException("Expected id/ bundle ID.");
        }
        String id = field.substring(ID_PREFIX.length());
        if (!isValidBundleId(id)) {
            throw new IllegalArgumentException("Bundle ID must be from 001 to 999.");
        }
        return id;
    }

    private static boolean isValidBundleId(String id) {
        return id != null && id.matches("[0-9]{3}") && !id.equals("000");
    }

    private static boolean isValidItemId(String id) {
        return id != null && id.matches("[0-9]{3}") && !id.equals("000");
    }

    private static boolean isValidRecordId(String id) {
        try {
            return UUID.fromString(id).toString().equals(id);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** Parsed bundle metadata before it is linked to the current equipment objects. */
    record StoredBundle(String bundleId, String bundleName, List<StoredItem> items) { }

    /** Parsed reference to one physical equipment record. */
    record StoredItem(String itemId, String recordId) { }
}
