package seedu.snap.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import seedu.snap.exceptions.EquipmentFilteringUnsuccessful;
import seedu.snap.loan.LoanHandler;

/** Selects equipment matching all supplied filters without changing the stored items. */
public final class EquipmentFilter {
    private static final Logger logger = Logger.getLogger(EquipmentFilter.class.getName());
    private final Map<String, String> fields;
    private int minimumCondition = 1;
    private int maximumCondition = 10;

    private EquipmentFilter(Map<String, String> fields) {
        this.fields = fields;
    }

    /** Parses optional ID, name, type, condition/range, and availability filters in any order. */
    public static EquipmentFilter parse(String input) throws EquipmentFilteringUnsuccessful {
        try {
            Map<String, String> fields = EquipmentFields.parse(input, "filter", "id", "i", "type", "cond", "status");
            EquipmentFilter filter = new EquipmentFilter(fields);
            filter.validate();
            return filter;
        } catch (IllegalArgumentException e) {
            logger.log(Level.FINE, "Equipment filter rejected: {0}", e.getMessage());
            throw new EquipmentFilteringUnsuccessful(e.getMessage());
        }
    }

    /** Validates scalar values before any inventory is inspected. */
    private void validate() {
        String id = fields.get("id");
        if (id != null && (!id.matches("[0-9]{3}") || id.equals("000"))) {
            throw new IllegalArgumentException("Item ID must be from 001 to 999.");
        }
        String name = fields.get("i");
        if (name != null && name.length() > 100) {
            throw new IllegalArgumentException("Item name must contain 1 to 100 characters.");
        }
        String condition = fields.get("cond");
        if (condition != null) {
            if (!condition.matches("(10|[1-9])(-(10|[1-9]))?")) {
                throw new IllegalArgumentException("Condition must be from 1 to 10 or an inclusive range, e.g. 1-3.");
            }
            String[] bounds = condition.split("-");
            minimumCondition = Integer.parseInt(bounds[0]);
            maximumCondition = Integer.parseInt(bounds[bounds.length - 1]);
            if (minimumCondition > maximumCondition) {
                throw new IllegalArgumentException("The minimum condition must not exceed the maximum.");
            }
        }
        String status = fields.get("status");
        if (status != null && !status.equalsIgnoreCase("available") && !status.equalsIgnoreCase("loaned")) {
            throw new IllegalArgumentException("Status must be available or loaned.");
        }
    }

    /** Returns matching items in inventory order; the returned list cannot change the inventory. */
    public List<Equipment> apply(List<Equipment> inventory, LoanHandler loans) {
        ArrayList<Equipment> result = new ArrayList<>();
        for (Equipment item : inventory) {
            if (matches(item, loans.hasActiveLoanForItem(item.getItemId()))) {
                result.add(item);
            }
        }
        assert result.size() <= inventory.size() : "Filtering cannot produce extra equipment";
        logger.log(Level.FINE, "Equipment filter matched {0} item(s)", result.size());
        return Collections.unmodifiableList(result);
    }

    /** Applies every supplied criterion; availability comes from the current active loan records. */
    boolean matches(Equipment item, boolean isLoaned) {
        return (!fields.containsKey("id") || fields.get("id").equals(item.getItemId()))
                && (!fields.containsKey("i") || item.getItemName().toLowerCase(Locale.ROOT)
                        .contains(fields.get("i").toLowerCase(Locale.ROOT)))
                && (!fields.containsKey("type") || fields.get("type").equalsIgnoreCase(item.getType()))
                && item.getCondition() >= minimumCondition && item.getCondition() <= maximumCondition
                && (!fields.containsKey("status")
                        || fields.get("status").equalsIgnoreCase(isLoaned ? "loaned" : "available"));
    }
}
