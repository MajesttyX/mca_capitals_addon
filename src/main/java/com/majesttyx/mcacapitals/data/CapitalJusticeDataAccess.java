package com.majesttyx.mcacapitals.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Set;
import java.util.UUID;

public final class CapitalJusticeDataAccess {

    private static final SavedData.Factory<CapitalJusticeSavedData> FACTORY =
            new SavedData.Factory<>(CapitalJusticeSavedData::new, CapitalJusticeSavedData::load, null);

    private CapitalJusticeDataAccess() {
    }

    public static CapitalJusticeSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, CapitalJusticeSavedData.DATA_NAME);
    }

    public static long getLastAccusationDay(ServerLevel level, UUID capitalId, UUID playerId) {
        return get(level).getLastAccusationDay(capitalId, playerId);
    }

    public static void setLastAccusationDay(ServerLevel level, UUID capitalId, UUID playerId, long day) {
        get(level).setLastAccusationDay(capitalId, playerId, day);
    }

    public static long getLastExileScanDay(ServerLevel level, UUID capitalId) {
        return get(level).getLastExileScanDay(capitalId);
    }

    public static void setLastExileScanDay(ServerLevel level, UUID capitalId, long day) {
        get(level).setLastExileScanDay(capitalId, day);
    }

    public static boolean hasDiscoveredExile(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).hasDiscoveredExile(capitalId, targetId);
    }

    public static Set<UUID> getDiscoveredExiles(ServerLevel level, UUID capitalId) {
        return get(level).getDiscoveredExiles(capitalId);
    }

    public static void markDiscoveredExile(ServerLevel level, UUID capitalId, UUID targetId) {
        get(level).markDiscoveredExile(capitalId, targetId);
    }

    public static boolean clearDiscoveredExile(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).clearDiscoveredExile(capitalId, targetId);
    }

    public static boolean hasArrestWarrant(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).hasArrestWarrant(capitalId, targetId);
    }

    public static Set<UUID> getArrestWarrants(ServerLevel level, UUID capitalId) {
        return get(level).getArrestWarrants(capitalId);
    }

    public static boolean issueArrestWarrant(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).issueArrestWarrant(capitalId, targetId, level.getGameTime());
    }

    public static boolean clearArrestWarrant(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).clearArrestWarrant(capitalId, targetId);
    }

    public static long getArrestWarrantIssuedGameTime(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).getArrestWarrantIssuedGameTime(capitalId, targetId);
    }

    public static boolean isDetainedPrisoner(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).isDetainedPrisoner(capitalId, targetId);
    }

    public static Set<UUID> getDetainedPrisoners(ServerLevel level, UUID capitalId) {
        return get(level).getDetainedPrisoners(capitalId);
    }

    public static boolean markDetainedPrisoner(ServerLevel level, UUID capitalId, UUID targetId, long day) {
        return get(level).markDetainedPrisoner(capitalId, targetId, day);
    }

    public static boolean clearDetainedPrisoner(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).clearDetainedPrisoner(capitalId, targetId);
    }

    public static long getDetentionStartDay(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).getDetentionStartDay(capitalId, targetId);
    }

    public static boolean clearJusticeCase(ServerLevel level, UUID capitalId, UUID targetId) {
        return get(level).clearJusticeCase(capitalId, targetId);
    }
}