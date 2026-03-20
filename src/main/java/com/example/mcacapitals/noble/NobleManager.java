package com.example.mcacapitals.noble;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NobleManager {

    private static final Map<UUID, NobleRecord> nobles = new HashMap<>();

    private NobleManager() {
    }

    public static Map<UUID, NobleRecord> getAll() {
        return nobles;
    }

    public static NobleRecord get(UUID villagerId) {
        return nobles.get(villagerId);
    }

    public static NobleRecord getOrCreate(UUID villagerId) {
        return nobles.computeIfAbsent(villagerId, id -> new NobleRecord(id, NobleTitle.COMMONER));
    }

    public static void setTitle(UUID villagerId, NobleTitle title) {
        NobleRecord record = getOrCreate(villagerId);
        record.setDirectTitle(title);
    }

    public static NobleTitle getTitle(UUID villagerId) {
        NobleRecord record = nobles.get(villagerId);
        return record != null ? record.getDirectTitle() : NobleTitle.COMMONER;
    }

    public static void clearAll() {
        nobles.clear();
    }
}