package com.majesttyx.mcacapitals.capital;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CapitalManager {

    private static final Map<UUID, CapitalRecord> CAPITALS = new LinkedHashMap<>();

    private CapitalManager() {
    }

    public static void clear() {
        CAPITALS.clear();
    }

    public static void clearAll() {
        CAPITALS.clear();
    }

    public static boolean isEmpty() {
        return CAPITALS.isEmpty();
    }

    public static void putCapital(CapitalRecord capital) {
        if (capital == null || capital.getCapitalId() == null) {
            return;
        }
        CAPITALS.put(capital.getCapitalId(), capital);
    }

    public static CapitalRecord getCapital(UUID capitalId) {
        if (capitalId == null) {
            return null;
        }
        return CAPITALS.get(capitalId);
    }

    public static void removeCapital(UUID capitalId) {
        if (capitalId == null) {
            return;
        }
        CAPITALS.remove(capitalId);
    }

    public static Map<UUID, CapitalRecord> getAllCapitals() {
        return Collections.unmodifiableMap(CAPITALS);
    }

    public static Map<UUID, CapitalRecord> getAllCapitalsSnapshot() {
        return new LinkedHashMap<>(CAPITALS);
    }

    public static Collection<CapitalRecord> getAllCapitalRecords() {
        return Collections.unmodifiableCollection(CAPITALS.values());
    }

    public static boolean hasCapitalForVillageId(ServerLevel level, Integer villageId) {
        return getCapitalByVillageId(level, villageId) != null;
    }

    public static CapitalRecord getCapitalForVillage(ServerLevel level, Integer villageId) {
        return getCapitalByVillageId(level, villageId);
    }

    public static CapitalRecord getCapitalByVillageId(ServerLevel level, Integer villageId) {
        if (level == null || villageId == null) {
            return null;
        }

        String dimensionId = getDimensionId(level);
        for (CapitalRecord capital : CAPITALS.values()) {
            if (villageId.equals(capital.getVillageId()) && matchesDimension(capital, dimensionId)) {
                return capital;
            }
        }

        return null;
    }

    /**
     * Legacy lookup retained only for compatibility with old internal callers. If the same MCA
     * village id exists in more than one dimension, this deliberately returns null rather than
     * selecting the wrong capital.
     */
    public static CapitalRecord getCapitalByVillageId(Integer villageId) {
        if (villageId == null) {
            return null;
        }

        CapitalRecord found = null;
        for (CapitalRecord capital : CAPITALS.values()) {
            if (!villageId.equals(capital.getVillageId())) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = capital;
        }
        return found;
    }

    public static boolean isCapitalInLevel(CapitalRecord capital, ServerLevel level) {
        if (capital == null || level == null) {
            return false;
        }
        return matchesDimension(capital, getDimensionId(level));
    }

    public static String getDimensionId(ServerLevel level) {
        return level == null ? null : level.dimension().location().toString();
    }

    public static ServerLevel getCapitalLevel(MinecraftServer server, CapitalRecord capital) {
        if (server == null || capital == null) {
            return null;
        }

        String dimensionId = capital.getVillageDimensionId();
        if (dimensionId == null || dimensionId.isBlank()) {
            return null;
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (dimensionId.equals(getDimensionId(level))) {
                return level;
            }
        }
        return null;
    }

    public static ServerLevel resolveCapitalLevel(ServerLevel contextLevel, CapitalRecord capital) {
        if (contextLevel == null) {
            return null;
        }
        ServerLevel resolved = getCapitalLevel(contextLevel.getServer(), capital);
        return resolved == null ? contextLevel : resolved;
    }

    private static boolean matchesDimension(CapitalRecord capital, String dimensionId) {
        String capitalDimension = capital == null ? null : capital.getVillageDimensionId();
        return capitalDimension == null || capitalDimension.isBlank() || capitalDimension.equals(dimensionId);
    }

    public static CapitalRecord getCapitalBySovereign(UUID sovereignId) {
        if (sovereignId == null) {
            return null;
        }

        for (CapitalRecord capital : CAPITALS.values()) {
            if (sovereignId.equals(capital.getSovereign())
                    || sovereignId.equals(capital.getPlayerSovereignId())) {
                return capital;
            }
        }

        return null;
    }

    public static CapitalRecord getCapitalForResident(UUID residentId) {
        if (residentId == null) {
            return null;
        }

        for (CapitalRecord capital : CAPITALS.values()) {
            if (belongsToCapital(capital, residentId)) {
                return capital;
            }
        }

        return null;
    }

    private static boolean belongsToCapital(CapitalRecord capital, UUID residentId) {
        if (capital == null || residentId == null) {
            return false;
        }

        return residentId.equals(capital.getSovereign())
                || residentId.equals(capital.getConsort())
                || residentId.equals(capital.getDowager())
                || residentId.equals(capital.getHeir())
                || residentId.equals(capital.getCommander())
                || residentId.equals(capital.getPlayerSovereignId())
                || residentId.equals(capital.getPlayerConsortId())
                || residentId.equals(CapitalAmbassadorService.getCachedAmbassador(capital))
                || capital.isRoyalChild(residentId)
                || capital.isDisinheritedRoyalChild(residentId)
                || capital.isLegitimizedRoyalChild(residentId)
                || capital.isRoyalHouseholdMember(residentId)
                || capital.isDuke(residentId)
                || capital.isLord(residentId)
                || capital.isKnight(residentId)
                || capital.isRoyalGuard(residentId)
                || capital.isDisgracedRoyalGuard(residentId);
    }
}
