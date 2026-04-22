package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.MCARelationshipBridge;
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

        UUID villagerSpouse = findActualVillagerSpouse(level, personId);
        if (villagerSpouse != null) {
            return villagerSpouse;
        }

        ServerPlayer playerSpouse = findActualPlayerSpouse(level, personId);
        if (playerSpouse != null) {
            return playerSpouse.getUUID();
        }

        return null;
    }

    static UUID findActualVillagerSpouse(ServerLevel level, UUID personId) {
        if (level == null || personId == null) {
            return null;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (spouse == null) {
            return null;
        }

        if (!MCAIntegrationBridge.isMCAVillager(level, spouse)) {
            return null;
        }

        if (!CapitalCourtBuilder.isValidRelationshipPerson(level, spouse)) {
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

        UUID villagerSpouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (villagerSpouse != null && MCAIntegrationBridge.isMCAVillager(level, villagerSpouse)) {
            return null;
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

        UUID villagerSpouse = findActualVillagerSpouse(level, personId);
        if (villagerSpouse != null) {
            Entity entity = MCAIntegrationBridge.getEntityByUuid(level, villagerSpouse);
            if (entity != null) {
                return entity.getName().getString();
            }
            return "Unknown";
        }

        ServerPlayer playerSpouse = findActualPlayerSpouse(level, personId);
        if (playerSpouse != null) {
            return playerSpouse.getGameProfile().getName();
        }

        return "Unknown";
    }

    static boolean isValidMarriedConsort(ServerLevel level, UUID sourceId, UUID spouseId) {
        if (level == null || sourceId == null || spouseId == null) {
            return false;
        }

        UUID villagerSpouse = findActualVillagerSpouse(level, sourceId);
        if (villagerSpouse != null) {
            return villagerSpouse.equals(spouseId);
        }

        ServerPlayer playerSpouse = findActualPlayerSpouse(level, sourceId);
        if (playerSpouse != null) {
            return playerSpouse.getUUID().equals(spouseId);
        }

        return false;
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