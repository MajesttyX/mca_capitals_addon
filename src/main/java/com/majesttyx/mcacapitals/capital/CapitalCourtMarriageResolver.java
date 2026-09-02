package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import com.majesttyx.mcacapitals.util.MCAPersistentPersonBridge;
import com.majesttyx.mcacapitals.util.MCARelationshipBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CapitalCourtMarriageResolver {

    private CapitalCourtMarriageResolver() {
    }

    static UUID findActualSpouse(ServerLevel level, UUID personId) {
        if (level == null || personId == null) {
            return null;
        }

        UUID recordedSpouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (recordedSpouse != null
                && MCAPersistentPersonBridge.hasCurrentMarriage(level, personId, recordedSpouse)) {
            if (MCAPersistentPersonBridge.isKnownVillager(level, recordedSpouse)) {
                if (!MCAIntegrationBridge.isFamilyNodeDeceased(level, recordedSpouse)) {
                    return recordedSpouse;
                }
                return null;
            }

            if (MCAPersistentPersonBridge.isKnownPlayer(level, recordedSpouse)) {
                return recordedSpouse;
            }
        }

        ServerPlayer playerSpouse = findActualPlayerSpouse(level, personId);
        return playerSpouse != null ? playerSpouse.getUUID() : null;
    }

    static UUID findActualVillagerSpouse(ServerLevel level, UUID personId) {
        if (level == null || personId == null) {
            return null;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (spouse == null
                || !MCAPersistentPersonBridge.hasCurrentMarriage(level, personId, spouse)
                || !MCAPersistentPersonBridge.isKnownVillager(level, spouse)
                || MCAIntegrationBridge.isFamilyNodeDeceased(level, spouse)) {
            return null;
        }

        Entity loaded = MCAIntegrationBridge.getEntityByUuid(level, spouse);
        if (loaded != null
                && (!MCAIntegrationBridge.isMCAVillagerEntity(loaded)
                || !loaded.isAlive()
                || loaded.isRemoved())) {
            return null;
        }

        return spouse;
    }

    static ServerPlayer findActualPlayerSpouse(ServerLevel level, UUID personId) {
        if (level == null || personId == null || level.getServer() == null) {
            return null;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, personId);

        if (entity instanceof ServerPlayer playerPerson) {
            for (Entity candidate : level.getAllEntities()) {
                if (!MCAIntegrationBridge.isMCAVillagerEntity(candidate)) {
                    continue;
                }
                if (MCARelationshipBridge.isActuallyMarried(playerPerson, candidate)) {
                    return null;
                }
            }
            return null;
        }

        UUID recordedSpouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (recordedSpouse != null
                && MCAPersistentPersonBridge.hasCurrentMarriage(level, personId, recordedSpouse)) {
            if (MCAPersistentPersonBridge.isKnownVillager(level, recordedSpouse)) {
                return null;
            }
            if (MCAPersistentPersonBridge.isKnownPlayer(level, recordedSpouse)) {
                return level.getServer().getPlayerList().getPlayer(recordedSpouse);
            }
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (MCARelationshipBridge.isActuallyMarriedToPlayer(player, personId)) {
                return player;
            }
        }

        return null;
    }

    static String resolveSpouseName(ServerLevel level, UUID personId) {
        if (level == null || personId == null) {
            return "Unknown";
        }

        UUID spouseId = findActualSpouse(level, personId);
        if (spouseId == null) {
            return "Unknown";
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, spouseId);
        if (entity != null) {
            return entity.getName().getString();
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(spouseId);
        if (player != null) {
            return player.getGameProfile().getName();
        }

        return "Unknown";
    }

    static boolean isValidMarriedConsort(ServerLevel level, UUID sourceId, UUID spouseId) {
        if (level == null || sourceId == null || spouseId == null) {
            return false;
        }

        UUID actualSpouse = findActualSpouse(level, sourceId);
        return spouseId.equals(actualSpouse);
    }

    static void collectMarriageDukeSources(
            ServerLevel level,
            Set<UUID> residents,
            CapitalRecord capital,
            Set<UUID> directDukes,
            Map<UUID, UUID> marriageDukeSources,
            Map<UUID, Boolean> marriageDukeFemale
    ) {
        for (UUID dukeId : directDukes) {
            if (dukeId == null) {
                continue;
            }

            UUID spouse = findActualVillagerSpouse(level, dukeId);
            if (spouse == null) {
                continue;
            }
            if (!isValidMarriedConsort(level, dukeId, spouse)) {
                continue;
            }
            if (residents != null && !residents.contains(spouse)) {
                continue;
            }
            if (spouse.equals(capital.getSovereign())
                    || spouse.equals(capital.getConsort())
                    || spouse.equals(capital.getDowager())
                    || spouse.equals(capital.getCommander())
                    || spouse.equals(capital.getHeir())) {
                continue;
            }
            if (directDukes.contains(spouse)) {
                continue;
            }

            marriageDukeSources.put(spouse, dukeId);
            marriageDukeFemale.put(spouse, MCAIntegrationBridge.isFemale(level, spouse));
        }
    }
}
