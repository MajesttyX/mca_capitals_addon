package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;

public final class CapitalInterregnumDataAccess {
private CapitalInterregnumDataAccess() {
    }

    public static CapitalInterregnumSavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        CapitalInterregnumSavedData::load,
                        CapitalInterregnumSavedData::new,
                        CapitalInterregnumSavedData.DATA_NAME
                );
    }
    public static CapitalInterregnumRecord getRecord(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return null;
        }

        return get(level).getRecord(capitalId);
    }

    public static boolean begin(
            ServerLevel level,
            CapitalInterregnumRecord record
    ) {
        return level != null
                && record != null
                && get(level).begin(record);
    }
    public static boolean remove(
            ServerLevel level,
            UUID capitalId
    ) {
        return level != null
                && capitalId != null
                && get(level).remove(capitalId);
    }

    public static Map<UUID, CapitalInterregnumRecord>
    getSnapshot(ServerLevel level) {
        if (level == null) {
            return Map.of();
        }

        return get(level).getSnapshot();
    }
}
