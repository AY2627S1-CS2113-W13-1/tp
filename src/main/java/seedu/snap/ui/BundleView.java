package seedu.snap.ui;

import java.util.List;

import seedu.snap.bundle.Bundle;
import seedu.snap.equipment.Equipment;

/** Displays bundle composition and contents using existing model requirements. */
public final class BundleView {
    private BundleView() {
        // Utility class; do not instantiate.
    }

    /** Explains which required equipment types are absent without introducing compatibility rules. */
    public static String composition(Bundle bundle) {
        if (bundle.isComplete()) {
            return "Complete";
        }
        List<String> missing = bundle.getMissingEquipmentTypes();
        return missing.isEmpty() ? "Incomplete (requirements not met)"
                : "Incomplete (missing " + String.join(", ", missing) + ")";
    }

    /** Displays every bundle and member in their existing order, with counts and persistence guidance. */
    public static void print(List<Bundle> bundles) {
        ConsoleOutput.heading("Bundles");
        if (bundles.isEmpty()) {
            System.out.println("No bundles recorded. Create one with: add-bundle name/Camera kit");
        }
        for (Bundle bundle : bundles) {
            System.out.println("Bundle " + bundle.getBundleId() + ": "
                    + ConsoleOutput.inline(bundle.getBundleName()));
            System.out.println("  Composition: " + composition(bundle));
            List<Equipment> items = bundle.getEquipmentList();
            System.out.println("  " + items.size() + (items.size() == 1 ? " equipment item:" : " equipment items:"));
            for (Equipment item : items) {
                System.out.println("    " + item.getItemId() + " " + ConsoleOutput.inline(item.getItemName())
                        + " (" + item.getType() + ")");
            }
            System.out.println();
        }
        System.out.println(bundles.size() + (bundles.size() == 1 ? " bundle." : " bundles."));
        persistenceNote();
    }

    /** Describes current persistence, which supersedes the original session-only bundle limitation. */
    public static void persistenceNote() {
        System.out.println("Save new bundles and additions with bye; saved bundles are restored at startup.");
        System.out.println("Successful membership removals are saved immediately.");
    }
}
