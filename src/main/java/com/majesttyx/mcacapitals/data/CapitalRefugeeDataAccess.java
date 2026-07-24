package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CapitalRefugeeDataAccess {

    private static final SavedData.Factory<
            CapitalRefugeeSavedData
            > FACTORY =
            new SavedData.Factory<>(
                    CapitalRefugeeSavedData::new,
                    CapitalRefugeeSavedData::load,
                    null
            );

    private CapitalRefugeeDataAccess() {
    }

    public static CapitalRefugeeSavedData get(
            ServerLevel level
    ) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        FACTORY,
                        CapitalRefugeeSavedData.DATA_NAME
                );
    }

    public static CapitalRefugeeRecord getRecord(
            ServerLevel level,
            UUID refugeeId
    ) {
        if (level == null || refugeeId == null) {
            return null;
        }

        return get(level).getRecord(refugeeId);
    }

    public static CapitalRefugeeRecord markExiled(
            ServerLevel level,
            UUID refugeeId,
            UUID originCapitalId,
            int originVillageId,
            String originCapitalName
    ) {
        if (level == null) {
            return null;
        }

        return get(level).markExiled(
                refugeeId,
                originCapitalId,
                originVillageId,
                originCapitalName,
                level.getGameTime()
        );
    }

    public static boolean grantAsylum(
            ServerLevel level,
            UUID refugeeId,
            UUID asylumCapitalId
    ) {
        return level != null
                && get(level).grantAsylum(
                refugeeId,
                asylumCapitalId,
                level.getGameTime()
        );
    }

    public static boolean clearAsylum(
            ServerLevel level,
            UUID refugeeId
    ) {
        return level != null
                && get(level).clearAsylum(
                refugeeId
        );
    }

    public static List<CapitalRefugeeRecord>
    getAwaitingAsylum(
            ServerLevel level
    ) {
        if (level == null) {
            return List.of();
        }

        return get(level).getAwaitingAsylum();
    }

    public static List<CapitalRefugeeRecord> getAsylees(
            ServerLevel level,
            UUID asylumCapitalId
    ) {
        if (level == null) {
            return List.of();
        }

        return get(level).getAsylees(
                asylumCapitalId
        );
    }

    public static Map<UUID, CapitalRefugeeRecord>
    getSnapshot(
            ServerLevel level
    ) {
        if (level == null) {
            return Map.of();
        }

        return get(level).getSnapshot();
    }

    public static boolean removeCapital(
            ServerLevel level,
            UUID capitalId
    ) {
        return level != null
                && get(level).removeCapital(
                capitalId
        );
    }
}