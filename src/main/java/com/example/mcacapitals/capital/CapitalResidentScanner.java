package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalResidentScanner {

    private static final Map<String, Long> CACHED_GAME_TIME_BY_DIMENSION = new HashMap<>();
    private static final Map<String, Map<UUID, Set<UUID>>> CACHED_RESIDENTS_BY_DIMENSION = new HashMap<>();

    private CapitalResidentScanner() {
    }

    public static Set<UUID> scanResidents(ServerLevel level, UUID capitalId) {
        if (level == null || capitalId == null) {
            return new HashSet<>();
        }

        String dimensionKey = level.dimension().location().toString();
        long gameTime = level.getGameTime();

        Long cachedGameTime = CACHED_GAME_TIME_BY_DIMENSION.get(dimensionKey);
        if (cachedGameTime == null || cachedGameTime.longValue() != gameTime) {
            CACHED_GAME_TIME_BY_DIMENSION.put(dimensionKey, gameTime);
            CACHED_RESIDENTS_BY_DIMENSION.put(dimensionKey, new HashMap<>());
        }

        Map<UUID, Set<UUID>> cachedResidents = CACHED_RESIDENTS_BY_DIMENSION.computeIfAbsent(dimensionKey, key -> new HashMap<>());
        Set<UUID> existing = cachedResidents.get(capitalId);
        if (existing != null) {
            return existing;
        }

        Set<UUID> scanned = scanResidentsUncached(level, capitalId);
        cachedResidents.put(capitalId, scanned);
        return scanned;
    }

    public static void clearCache(ServerLevel level) {
        if (level == null) {
            return;
        }

        String dimensionKey = level.dimension().location().toString();
        CACHED_GAME_TIME_BY_DIMENSION.remove(dimensionKey);
        CACHED_RESIDENTS_BY_DIMENSION.remove(dimensionKey);
    }

    public static void clearAllCaches() {
        CACHED_GAME_TIME_BY_DIMENSION.clear();
        CACHED_RESIDENTS_BY_DIMENSION.clear();
    }

    private static Set<UUID> scanResidentsUncached(ServerLevel level, UUID capitalId) {
        Set<UUID> residents = new HashSet<>();

        CapitalRecord capital = CapitalManager.getCapital(capitalId);
        if (capital != null && capital.getVillageId() != null) {
            residents.addAll(MCAIntegrationBridge.getVillageResidents(level, capital.getVillageId()));
            return residents;
        }

        Iterable<Entity> allEntities = level.getEntities().getAll();
        for (Entity entity : allEntities) {
            UUID entityId = entity.getUUID();
            if (MCAIntegrationBridge.isMCAVillager(level, entityId)) {
                residents.add(entityId);
            }
        }

        return residents;
    }
}