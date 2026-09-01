package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalRefugeeDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRefugeeRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CapitalResidentScanner {
    private static final Map<String, Long> CACHED_GAME_TIME_BY_DIMENSION =
            new HashMap<>();

    private static final Map<String, Map<UUID, Set<UUID>>>
            CACHED_RESIDENTS_BY_DIMENSION =
            new HashMap<>();

    private CapitalResidentScanner() {
    }

    public static Set<UUID> scanResidents(
            ServerLevel level,
            UUID capitalId
    ) {
        if (level == null || capitalId == null) {
            return new HashSet<>();
        }

        String dimensionKey =
                level.dimension()
                        .location()
                        .toString();

        long gameTime =
                level.getGameTime();

        Long cachedGameTime =
                CACHED_GAME_TIME_BY_DIMENSION.get(
                        dimensionKey
                );

        if (cachedGameTime == null
                || cachedGameTime.longValue() != gameTime) {
            CACHED_GAME_TIME_BY_DIMENSION.put(
                    dimensionKey,
                    gameTime
            );

            CACHED_RESIDENTS_BY_DIMENSION.put(
                    dimensionKey,
                    new HashMap<>()
            );
        }

        Map<UUID, Set<UUID>> cachedResidents =
                CACHED_RESIDENTS_BY_DIMENSION
                        .computeIfAbsent(
                                dimensionKey,
                                key -> new HashMap<>()
                        );

        Set<UUID> existing =
                cachedResidents.get(capitalId);

        if (existing != null) {
            return existing;
        }

        Set<UUID> scanned =
                scanResidentsUncached(
                        level,
                        capitalId
                );

        cachedResidents.put(
                capitalId,
                scanned
        );

        return scanned;
    }

    public static void clearCache(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        String dimensionKey =
                level.dimension()
                        .location()
                        .toString();

        CACHED_GAME_TIME_BY_DIMENSION.remove(
                dimensionKey
        );

        CACHED_RESIDENTS_BY_DIMENSION.remove(
                dimensionKey
        );
    }

    public static void clearAllCaches() {
        CACHED_GAME_TIME_BY_DIMENSION.clear();
        CACHED_RESIDENTS_BY_DIMENSION.clear();
    }

    private static Set<UUID> scanResidentsUncached(
            ServerLevel level,
            UUID capitalId
    ) {
        Set<UUID> residents =
                new HashSet<>();

        CapitalRecord capital =
                CapitalManager.getCapital(
                        capitalId
                );

        if (capital != null
                && capital.getVillageId() != null
                && CapitalManager.isCapitalInLevel(capital, level)) {
            residents.addAll(
                    MCAIntegrationBridge
                            .getVillageResidents(
                                    level,
                                    capital.getVillageId()
                            )
            );

            residents.removeIf(
                    residentId ->
                            isAwaitingAsylum(
                                    level,
                                    residentId
                            )
            );

            return residents;
        }

        Iterable<Entity> allEntities =
                level.getEntities()
                        .getAll();

        for (Entity entity : allEntities) {
            if (entity == null
                    || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
                continue;
            }

            UUID entityId = entity.getUUID();

            if (!isAwaitingAsylum(
                    level,
                    entityId
            )) {
                residents.add(entityId);
            }
        }

        return residents;
    }

    private static boolean isAwaitingAsylum(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRefugeeRecord record =
                CapitalRefugeeDataAccess.getRecord(
                        level,
                        villagerId
                );

        return record != null
                && record.isAwaitingAsylum();
    }
}
