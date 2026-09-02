package com.majesttyx.mcacapitals.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MCAIntegrationBridge {

    private MCAIntegrationBridge() {
    }

    public static Entity findLoadedEntityByUuid(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.findLoadedEntityByUuid(level, entityId);
    }

    public static Entity getEntityByUuid(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.getEntityByUuid(level, entityId);
    }

    public static Entity findLoadedMCAVillagerByUuid(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.findLoadedMCAVillagerByUuid(level, entityId);
    }

    public static boolean isLoadedAndAlive(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isLoadedAndAlive(level, entityId);
    }

    public static boolean isMCAVillager(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isMCAVillager(level, entityId);
    }

    public static boolean isMCAVillagerEntity(Object entity) {
        return MCAEntityBridge.isMCAVillagerEntity(entity);
    }

    public static boolean isFamilyNodeDeceased(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.isFamilyNodeDeceased(level, entityId);
    }

    public static Optional<Integer> getLastSeenVillageId(ServerLevel level, ServerPlayer player) {
        return MCAPlayerBridge.getLastSeenVillageId(level, player);
    }

    public static boolean isPlayerInVillage(ServerLevel level, ServerPlayer player, Integer villageId) {
        return MCAPlayerBridge.isPlayerInVillage(level, player, villageId);
    }

    public static String getPlayerDialogueName(ServerPlayer player) {
        return MCAPlayerBridge.getDialogueName(player);
    }

    public static boolean forceVillageResidency(
            ServerLevel level,
            UUID villagerId,
            int villageId
    ) {
        return MCAVillageResidencyBridge.forceVillageResidency(
                level,
                villagerId,
                villageId
        );
    }

    public static boolean isPlayerFemale(ServerLevel level, ServerPlayer player) {
        return MCAPlayerBridge.isPlayerFemale(level, player);
    }

    public static boolean isAliveMCAVillager(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isAliveMCAVillager(level, entityId);
    }

    public static boolean isAliveMCAVillagerEntity(Entity entity) {
        return MCAEntityBridge.isAliveMCAVillagerEntity(entity);
    }

    public static Optional<Boolean> getFemaleIfKnown(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return Optional.empty();
        }

        if (level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entityId);
            if (player != null) {
                return Optional.of(MCAPlayerBridge.isPlayerFemale(level, player));
            }
        }

        Entity entity = MCAEntityBridge.findLoadedEntityByUuid(level, entityId);
        if (MCAEntityBridge.isMCAVillagerEntity(entity)) {
            return Optional.of(MCAEntityBridge.isFemale(level, entityId));
        }

        return MCAFamilyBridge.getFemaleIfKnown(level, entityId);
    }

    public static boolean isFemale(ServerLevel level, UUID entityId) {
        return getFemaleIfKnown(level, entityId).orElse(false);
    }

    public static String getAgeState(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.getAgeState(level, entityId);
    }

    public static boolean isTeenOrAdultVillager(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isTeenOrAdultVillager(level, entityId);
    }

    public static boolean hasPersistentFamilyNode(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.hasPersistentFamilyNode(level, entityId);
    }

    public static boolean hasFamilyNode(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.hasFamilyNode(level, entityId);
    }

    public static UUID getSpouse(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.getSpouse(level, entityId);
    }

    public static boolean isPlayerFamilyNode(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.isPlayerFamilyNode(level, entityId);
    }

    public static boolean isPlayerIdentity(ServerLevel level, UUID entityId) {
        if (level == null || entityId == null) {
            return false;
        }

        if (level.getServer() != null
                && level.getServer().getPlayerList().getPlayer(entityId) != null) {
            return true;
        }

        return MCAFamilyBridge.isPlayerFamilyNode(level, entityId);
    }

    public static String getFamilyNodeName(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.getFamilyNodeName(level, entityId);
    }

    public static boolean isPersistentlyMarried(ServerLevel level, UUID firstId, UUID secondId) {
        return MCAFamilyBridge.isPersistentlyMarried(level, firstId, secondId);
    }

    public static Set<UUID> getChildren(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.getChildren(level, entityId);
    }

    public static UUID getFather(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.getFather(level, entityId);
    }

    public static UUID getMother(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.getMother(level, entityId);
    }

    public static Set<UUID> getParents(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.getParents(level, entityId);
    }

    public static boolean isChildOf(ServerLevel level, UUID childId, UUID parentId) {
        return MCAFamilyBridge.isChildOf(level, childId, parentId);
    }

    public static boolean areSiblings(ServerLevel level, UUID firstId, UUID secondId) {
        return MCAFamilyBridge.areSiblings(level, firstId, secondId);
    }

    public static boolean isGrandparentOf(ServerLevel level, UUID possibleGrandparent, UUID possibleGrandchild) {
        return MCAFamilyBridge.isGrandparentOf(level, possibleGrandparent, possibleGrandchild);
    }

    public static boolean isAuntOrUncleOf(ServerLevel level, UUID possibleAuntOrUncle, UUID possibleNieceOrNephew) {
        return MCAFamilyBridge.isAuntOrUncleOf(level, possibleAuntOrUncle, possibleNieceOrNephew);
    }

    public static boolean areCloselyRelatedForMarriage(ServerLevel level, UUID firstId, UUID secondId) {
        return MCAFamilyBridge.areCloselyRelatedForMarriage(level, firstId, secondId);
    }

    public static boolean isMCAGuard(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isMCAGuard(level, entityId);
    }

    public static boolean isMCAFootGuard(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isMCAFootGuard(level, entityId);
    }

    public static boolean isMasterProfessionVillager(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isMasterProfessionVillager(level, entityId);
    }

    public static boolean isMasterClericVillager(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isMasterClericVillager(level, entityId);
    }

    public static boolean isAliveAdultOrChildVillager(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isAliveAdultOrChildVillager(level, entityId);
    }

    public static String describeProfession(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.describeProfession(level, entityId);
    }

    public static Set<Integer> getVillageIdsAtOrAbovePopulation(ServerLevel level, int requiredPopulation) {
        return MCAVillageBridge.getVillageIdsAtOrAbovePopulation(level, requiredPopulation);
    }

    public static Set<Integer> getAllVillageIds(ServerLevel level) {
        return MCAVillageBridge.getAllVillageIds(level);
    }

    public static Integer getVillageIdForResident(ServerLevel level, UUID entityId) {
        return MCAVillageBridge.getVillageIdForResident(level, entityId);
    }

    public static boolean hasVillage(ServerLevel level, int villageId) {
        return MCAVillageBridge.hasVillage(level, villageId);
    }

    public static boolean isVillage(ServerLevel level, int villageId) {
        return MCAVillageBridge.isVillage(level, villageId);
    }

    public static int getVillagePopulation(ServerLevel level, int villageId) {
        return MCAVillageBridge.getVillagePopulation(level, villageId);
    }

    public static String getVillageName(ServerLevel level, Integer villageId) {
        return MCAVillageBridge.getVillageName(level, villageId);
    }

    public static Component getVillageNameComponent(ServerLevel level, Integer villageId) {
        return MCAVillageBridge.getVillageNameComponent(level, villageId);
    }

    public static BlockPos getVillageCenter(ServerLevel level, Integer villageId) {
        return MCAVillageBridge.getVillageCenter(level, villageId);
    }

    public static Set<UUID> getVillageResidents(ServerLevel level, int villageId) {
        return MCAVillageBridge.getVillageResidents(level, villageId);
    }

    public static Map<UUID, String> getVillageResidentNames(ServerLevel level, int villageId) {
        return MCAVillageBridge.getVillageResidentNames(level, villageId);
    }

    public static int countBuildingsOfType(ServerLevel level, Integer villageId, String buildingType) {
        return MCAVillageBridge.countBuildingsOfType(level, villageId, buildingType);
    }

    public static List<AABB> getBuildingBoundsOfType(ServerLevel level, Integer villageId, String buildingType) {
        return MCAVillageBridge.getBuildingBoundsOfType(level, villageId, buildingType);
    }

    public static List<BlockPos> getBuildingCentersOfType(ServerLevel level, Integer villageId, String buildingType) {
        return MCAVillageBridge.getBuildingCentersOfType(level, villageId, buildingType);
    }

    public static int getHeartsWithPlayer(ServerLevel level, UUID villagerId, UUID playerId) {
        return MCAEntityBridge.getHeartsWithPlayer(level, villagerId, playerId);
    }

    public static void captureLoadedResidentStates(ServerLevel level, java.util.Collection<UUID> residentIds) {
        MCAEntityBridge.captureLoadedResidentStates(level, residentIds);
    }

    public static boolean adjustHearts(ServerLevel level, UUID villagerId, UUID playerId, int delta) {
        return MCASocialBridge.adjustHearts(level, villagerId, playerId, delta);
    }

    public static String getClothes(ServerLevel level, UUID entityId) {
        return MCAClothingBridge.getClothes(level, entityId);
    }

    public static boolean setClothes(ServerLevel level, UUID entityId, String clothesId) {
        return MCAClothingBridge.setClothes(level, entityId, clothesId);
    }

    public static void randomizeClothes(ServerLevel level, UUID entityId) {
        MCAClothingBridge.randomizeClothes(level, entityId);
    }

    public static boolean clothingExists(String clothesId) {
        return MCAClothingBridge.clothingExists(clothesId);
    }

    public static List<Entity> getNearbyMCAVillagers(ServerLevel level, AABB area) {
        return MCAEntityBridge.getNearbyMCAVillagers(level, area);
    }

    public static void addEffect(Entity entity, MobEffectInstance effect) {
        MCAEntityBridge.addEffect(entity, effect);
    }

    public static boolean moveTo(Entity entity, double x, double y, double z, double speed) {
        return MCAEntityBridge.moveTo(entity, x, y, z, speed);
    }

    public static boolean stopInteracting(Entity villagerEntity) {
        return MCASocialBridge.stopInteracting(villagerEntity);
    }

    public static String describeEntity(ServerLevel level, UUID entityId) {
        return "isMCA=" + isMCAVillager(level, entityId)
                + ", hasFamilyNode=" + hasFamilyNode(level, entityId)
                + ", isGuard=" + isMCAGuard(level, entityId)
                + ", isFootGuard=" + isMCAFootGuard(level, entityId)
                + ", isMaster=" + isMasterProfessionVillager(level, entityId)
                + ", isMasterCleric=" + isMasterClericVillager(level, entityId)
                + ", isFemale=" + isFemale(level, entityId)
                + ", ageState=" + getAgeState(level, entityId)
                + ", spouse=" + (getSpouse(level, entityId) == null ? "none" : getSpouse(level, entityId))
                + ", childCount=" + getChildren(level, entityId).size()
                + ", profession=" + describeProfession(level, entityId);
    }
}