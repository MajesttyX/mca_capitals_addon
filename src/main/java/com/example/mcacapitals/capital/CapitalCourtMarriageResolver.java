package com.example.mcacapitals.capital;

import com.example.mcacapitals.util.MCAIntegrationBridge;
import com.example.mcacapitals.util.MCARelationshipBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashSet;
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

        ServerPlayer livePlayerSpouse = findActualPlayerSpouse(level, personId);
        if (livePlayerSpouse != null) {
            return livePlayerSpouse.getUUID();
        }

        UUID spouse = MCAIntegrationBridge.getSpouse(level, personId);
        if (spouse != null) {
            return spouse;
        }

        Entity personEntity = MCAIntegrationBridge.getEntityByUuid(level, personId);

        if (personEntity instanceof ServerPlayer playerPerson) {
            for (Entity entity : level.getAllEntities()) {
                if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
                    continue;
                }

                if (MCARelationshipBridge.isActuallyMarried(playerPerson, entity)) {
                    return entity.getUUID();
                }
            }

            return null;
        }

        return null;
    }

    static ServerPlayer findActualPlayerSpouse(ServerLevel level, UUID villagerOrPlayerId) {
        if (level == null || villagerOrPlayerId == null || level.getServer() == null) {
            return null;
        }

        Entity entity = MCAIntegrationBridge.getEntityByUuid(level, villagerOrPlayerId);
        if (entity instanceof ServerPlayer playerEntity) {
            return playerEntity;
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (MCARelationshipBridge.isActuallyMarriedToPlayer(player, villagerOrPlayerId)) {
                return player;
            }
        }

        return null;
    }

    static boolean isValidMarriedConsort(ServerLevel level, UUID sovereignId, UUID spouseId) {
        if (level == null || sovereignId == null || spouseId == null) {
            return false;
        }

        ServerPlayer playerSpouseFromSovereign = findActualPlayerSpouse(level, sovereignId);
        if (playerSpouseFromSovereign != null && playerSpouseFromSovereign.getUUID().equals(spouseId)) {
            return true;
        }

        Entity sovereignEntity = MCAIntegrationBridge.getEntityByUuid(level, sovereignId);
        if (sovereignEntity instanceof ServerPlayer playerSovereign) {
            return MCARelationshipBridge.isActuallyMarriedToPlayer(playerSovereign, spouseId);
        }

        Entity spouseEntity = MCAIntegrationBridge.getEntityByUuid(level, spouseId);
        if (spouseEntity instanceof ServerPlayer playerSpouse) {
            return MCARelationshipBridge.isActuallyMarriedToPlayer(playerSpouse, sovereignId);
        }

        return true;
    }

    static void addMarriageDerivedTitles(
            ServerLevel level,
            Set<UUID> residents,
            CapitalRecord capital,
            Set<UUID> dukes,
            Map<UUID, Boolean> dukeFemale,
            Set<UUID> lords,
            Map<UUID, Boolean> lordFemale,
            Set<UUID> knights,
            Map<UUID, Boolean> knightFemale
    ) {
        for (UUID dukeId : new LinkedHashSet<>(dukes)) {
            addSpouse(level, residents, capital, dukeId, dukes, dukeFemale);
        }

        for (UUID lordId : new LinkedHashSet<>(lords)) {
            addSpouse(level, residents, capital, lordId, lords, lordFemale);
        }
    }

    private static void addSpouse(
            ServerLevel level,
            Set<UUID> residents,
            CapitalRecord capital,
            UUID sourceId,
            Set<UUID> targetSet,
            Map<UUID, Boolean> femaleMap
    ) {
        UUID spouse = findActualSpouse(level, sourceId);
        if (!CapitalCourtBuilder.isValidRelationshipPerson(level, spouse)) {
            return;
        }
        if (!isValidMarriedConsort(level, sourceId, spouse)) {
            return;
        }
        if (residents != null && !residents.contains(spouse)) {
            return;
        }
        if (spouse.equals(capital.getSovereign())
                || spouse.equals(capital.getConsort())
                || spouse.equals(capital.getDowager())
                || spouse.equals(capital.getCommander())
                || spouse.equals(capital.getHeir())) {
            return;
        }

        targetSet.add(spouse);
        femaleMap.put(spouse, MCAIntegrationBridge.isFemale(level, spouse));
    }
}