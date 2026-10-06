package seedu.duke;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Command {
    private static final List<String[]> equipmentList = new ArrayList<>();

    protected static void addEquipment(String[] item) {
        equipmentList.add(item);
    }

    protected static List<String[]> getEquipmentList() {
        return Collections.unmodifiableList(equipmentList);
    }
}
