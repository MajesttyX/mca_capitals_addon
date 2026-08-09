package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class PlayerCapitalAllegianceDataAccess {
private PlayerCapitalAllegianceDataAccess() {
    }

    public static PlayerCapitalAllegianceSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PlayerCapitalAllegianceSavedData::load,
                        PlayerCapitalAllegianceSavedData::new,
                        PlayerCapitalAllegianceSavedData.DATA_NAME
                );
    }
    public static UUID getDeclaredCapitalId(ServerLevel level, UUID playerId) {
        return level == null || playerId == null
                ? null
                : get(level).getDeclaredCapitalId(playerId);
    }

    public static long getLastChangeDay(ServerLevel level, UUID playerId) {
        return level == null || playerId == null
                ? 0L
                : get(level).getLastChangeDay(playerId);
    }
    public static void setDeclaration(
            ServerLevel level,
            UUID playerId,
            UUID capitalId,
            long changeDay
    ) {
        if (level != null && playerId != null && capitalId != null) {
            get(level).setDeclaration(playerId, capitalId, changeDay);
        }
    }
    public static boolean clearDeclaration(ServerLevel level, UUID playerId) {
        return level != null
                && playerId != null
                && get(level).clearDeclaration(playerId);
    }
}
