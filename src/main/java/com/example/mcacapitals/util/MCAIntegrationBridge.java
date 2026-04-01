package com.example.mcacapitals.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
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

    public static boolean isPlayerFemale(ServerLevel level, ServerPlayer player) {
        return MCAPlayerBridge.isPlayerFemale(level, player);
    }

    public static boolean isAliveMCAVillager(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isAliveMCAVillager(level, entityId);
    }

    public static boolean isAliveMCAVillagerEntity(Entity entity) {
        return MCAEntityBridge.isAliveMCAVillagerEntity(entity);
    }

    public static boolean isFemale(ServerLevel level, UUID entityId) {
        return MCAEntityBridge.isFemale(level, entityId);
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

    public static Set<UUID> getChildren(ServerLevel level, UUID entityId) {
        return MCAFamilyBridge.getChildren(level, entityId);
    }

    public static boolean isChildOf(ServerLevel level, UUID childId, UUID parentId) {
        return MCAFamilyBridge.isChildOf(level, childId, parentId);
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

    public static BlockPos getVillageCenter(ServerLevel level, Integer villageId) {
        return MCAVillageBridge.getVillageCenter(level, villageId);
    }

    public static Set<UUID> getVillageResidents(ServerLevel level, int villageId) {
        return MCAVillageBridge.getVillageResidents(level, villageId);
    }

    public static int getHeartsWithPlayer(ServerLevel level, UUID villagerId, UUID playerId) {
        return MCAEntityBridge.getHeartsWithPlayer(level, villagerId, playerId);
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
                + ", isFemale=" + isFemale(level, entityId)
                + ", ageState=" + getAgeState(level, entityId)
                + ", spouse=" + (getSpouse(level, entityId) == null ? "none" : getSpouse(level, entityId))
                + ", childCount=" + getChildren(level, entityId).size()
                + ", profession=" + describeProfession(level, entityId);
    }
}