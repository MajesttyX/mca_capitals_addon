package com.majesttyx.mcacapitals.house;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.PlayerHouseSavedData;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class PlayerHouseService {

    private PlayerHouseService() {
    }

    public static boolean hasHouse(ServerLevel level, UUID playerId) {
        PlayerHouseRecord record = get(level, playerId);
        return record != null && record.hasHouseName();
    }

    public static PlayerHouseRecord get(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null) {
            return null;
        }
        return PlayerHouseSavedData.get(level).get(playerId);
    }

    public static boolean setHouse(ServerLevel level, ServerPlayer player, UUID capitalId, String houseName, PlayerHouseInheritanceMode mode) {
        if (level == null || player == null || capitalId == null) {
            return false;
        }

        String normalized = normalizeHouseName(houseName);
        if (!isValidHouseName(normalized)) {
            return false;
        }

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        String capitalName = "";
        if (capital != null && capital.getVillageId() != null) {
            capitalName = MCAIntegrationBridge.getVillageName(level, capital.getVillageId());
        }

        PlayerHouseSavedData data = PlayerHouseSavedData.get(level);
        PlayerHouseRecord record = data.getOrCreate(player.getUUID());
        record.setHouseName(normalized);
        record.setInheritanceMode(mode == null ? PlayerHouseInheritanceMode.FOLLOW_CAPITAL_LAW : mode);
        record.setHouseNameSetAtGameTime(level.getGameTime());
        record.setHouseNameSetInCapitalId(capitalId);
        record.setHouseNameSetInCapitalName(capitalName);
        data.setDirty();
        return true;
    }

    public static void clear(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null) {
            return;
        }
        PlayerHouseSavedData.get(level).remove(playerId);
    }

    public static String describe(ServerLevel level, UUID playerId) {
        PlayerHouseRecord record = get(level, playerId);
        if (record == null || !record.hasHouseName()) {
            return "PlayerHouse=unset";
        }

        return "PlayerHouse=" + record.getHouseName()
                + ", InheritanceMode=" + record.getInheritanceMode().name()
                + ", SetAtGameTime=" + record.getHouseNameSetAtGameTime()
                + ", SetInCapitalId=" + (record.getHouseNameSetInCapitalId() == null ? "unset" : record.getHouseNameSetInCapitalId())
                + ", SetInCapitalName=" + blankAsUnset(record.getHouseNameSetInCapitalName());
    }

    public static String normalizeHouseName(String houseName) {
        if (houseName == null) {
            return "";
        }
        return houseName.trim().replaceAll("\\s+", " ");
    }

    public static boolean isValidHouseName(String houseName) {
        String normalized = normalizeHouseName(houseName);
        if (normalized.length() < 2 || normalized.length() > 20 || normalized.contains("§")) {
            return false;
        }

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isLetter(c) || c == ' ' || c == '-' || c == '\'') {
                continue;
            }
            return false;
        }

        return true;
    }

    public static PlayerHouseInheritanceMode parseMode(String value) {
        try {
            return PlayerHouseInheritanceMode.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String blankAsUnset(String value) {
        return value == null || value.isBlank() ? "unset" : value;
    }
}