package com.majesttyx.mcacapitals.house;

import com.majesttyx.mcacapitals.capital.CapitalManager;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.data.PlayerHouseSavedData;
import com.majesttyx.mcacapitals.identity.PlayerHouseIdentityService;
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
        record.setInheritanceMode(PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE);
        record.setHouseNameSetAtGameTime(level.getGameTime());
        record.setHouseNameSetInCapitalId(capitalId);
        record.setHouseNameSetInCapitalName(capitalName);
        data.setDirty();

        PlayerHouseIdentityService.applyPlayerHouseToImmediateFamily(level, player);
        return true;
    }

    public static boolean reviseHouse(ServerLevel level, ServerPlayer player, String houseName, String houseWords) {
        if (level == null || player == null) {
            return false;
        }

        String normalizedHouseName = normalizeHouseName(houseName);
        String normalizedHouseWords = normalizeHouseWords(houseWords);

        if (!isValidHouseName(normalizedHouseName)) {
            return false;
        }

        if (!normalizedHouseWords.isBlank() && !isValidHouseWords(normalizedHouseWords)) {
            return false;
        }

        PlayerHouseSavedData data = PlayerHouseSavedData.get(level);
        PlayerHouseRecord record = data.getOrCreate(player.getUUID());

        if (record.getInheritanceMode() == null) {
            record.setInheritanceMode(PlayerHouseInheritanceMode.PRESERVE_PLAYER_HOUSE);
        }

        record.setHouseName(normalizedHouseName);
        record.setHouseWords(normalizedHouseWords);
        record.setHouseNameSetAtGameTime(level.getGameTime());

        data.setDirty();

        PlayerHouseIdentityService.applyPlayerHouseToImmediateFamily(level, player);
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
                + ", HouseWords=" + blankAsUnset(record.getHouseWords())
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

    public static String normalizeHouseWords(String houseWords) {
        if (houseWords == null) {
            return "";
        }

        return houseWords.trim().replaceAll("\\s+", " ");
    }

    public static boolean isValidHouseName(String houseName) {
        String normalized = normalizeHouseName(houseName);
        if (normalized.length() < 2 || normalized.length() > 20 || normalized.contains("§")) {
            return false;
        }

        return normalized.matches("[A-Za-z][A-Za-z '\\-]*");
    }

    public static boolean isValidHouseWords(String houseWords) {
        String normalized = normalizeHouseWords(houseWords);
        if (normalized.length() < 2 || normalized.length() > 80 || normalized.contains("§")) {
            return false;
        }

        return normalized.matches("[A-Za-z][A-Za-z '\\-,]*");
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
