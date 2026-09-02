package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
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

        UUID spouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (spouse != null
                && CapitalCourtBuilder.isValidRelationshipPerson(level, spouse)
                && MCAIntegrationBridge.isPersistentlyMarried(level, personId, spouse)) {
            return spouse;
        }

        UUID villagerSpouse = findActualVillagerSpouse(level, personId);
        if (villagerSpouse != null) {
            return villagerSpouse;
        }

        UUID playerSpouse = findActualPlayerSpouseId(level, personId);
        if (playerSpouse != null) {
            return playerSpouse;
        }

        return null;
    }

    static UUID findActualVillagerSpouse(ServerLevel level, UUID personId) {
        if (level == null || personId == null) {
            return null;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (spouse == null || MCAIntegrationBridge.isPlayerFamilyNode(level, spouse)) {
            return null;
        }

        if (!CapitalCourtBuilder.isValidRelationshipPerson(level, spouse)) {
            return null;
        }

        return MCAIntegrationBridge.isPersistentlyMarried(level, personId, spouse)
                ? spouse
                : null;
    }

    static UUID findActualPlayerSpouseId(ServerLevel level, UUID personId) {
        if (level == null || personId == null || level.getServer() == null) {
            return null;
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (spouse != null
                && MCAIntegrationBridge.isPlayerFamilyNode(level, spouse)
                && CapitalCourtBuilder.isValidRelationshipPerson(level, spouse)
                && MCAIntegrationBridge.isPersistentlyMarried(level, personId, spouse)) {
            return spouse;
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            if (playerId.equals(spouse)
                    && MCAIntegrationBridge.isPersistentlyMarried(level, personId, playerId)) {
                return playerId;
            }
            if (MCARelationshipBridge.isActuallyMarriedToPlayer(player, personId)) {
                return playerId;
            }
        }

        return null;
    }

    static ServerPlayer findActualPlayerSpouse(ServerLevel level, UUID personId) {
        UUID spouseId = findActualPlayerSpouseId(level, personId);
        if (spouseId == null || level == null || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(spouseId);
    }

    static String resolveSpouseName(ServerLevel level, UUID personId) {
        if (level == null || personId == null) {
            return "Unknown";
        }

        UUID spouse = findActualSpouse(level, personId);
        if (spouse == null) {
            return "Unknown";
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, spouse);
        if (entity != null) {
            return entity.getName().getString();
        }

        if (level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(spouse);
            if (player != null) {
                return player.getGameProfile().getName();
            }
        }

        String persistentName = MCAIntegrationBridge.getFamilyNodeName(level, spouse);
        return persistentName == null || persistentName.isBlank() ? "Unknown" : persistentName;
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
