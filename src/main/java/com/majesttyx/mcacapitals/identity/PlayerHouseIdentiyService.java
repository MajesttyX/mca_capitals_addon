package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.UUID;

public final class PlayerHouseIdentityService {

    private PlayerHouseIdentityService() {
    }

    public static boolean applyBirthIdentityFromParentIds(ServerLevel level, Entity child, UUID firstParentId, UUID secondParentId) {
        if (level == null || child == null || !MCAIntegrationBridge.isMCAVillagerEntity(child)) {
            return false;
        }

        if (applyPlayerHouseToChild(level, child, firstParentId)) {
            return true;
        }

        if (applyPlayerHouseToChild(level, child, secondParentId)) {
            return true;
        }

        Entity firstParent = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, firstParentId);
        Entity secondParent = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, secondParentId);

        if (firstParent != null && secondParent != null) {
            return BirthIdentityService.applyBirthIdentity(level, child, firstParent, secondParent);
        }

        return false;
    }

    public static boolean repairFromParentsIfNeeded(ServerLevel level, Entity child) {
        if (level == null || child == null || !MCAIntegrationBridge.isMCAVillagerEntity(child)) {
            return false;
        }

        VillagerIdentityData existingIdentity = VillagerIdentityService.getIdentity(child);
        if (existingIdentity != null && "LEGAL_RENAME".equals(existingIdentity.surnameSource())) {
            return false;
        }

        for (UUID parentId : MCAIntegrationBridge.getParents(level, child.getUUID())) {
            if (applyPlayerHouseToChildIfNeeded(level, child, parentId)) {
                return true;
            }
        }

        return false;
    }

    public static boolean applyAdoptionIdentityFromFamilyNodes(Object childNode, Object parentNode) {
        UUID childId = resolveNodeId(childNode);
        UUID parentId = resolveNodeId(parentNode);

        if (childId == null || parentId == null || !isPlayerNode(parentNode)) {
            return false;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }

        for (ServerLevel level : server.getAllLevels()) {
            Entity child = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, childId);
            if (child != null && applyAdoptionIdentity(level, child, parentId)) {
                return true;
            }
        }

        return false;
    }

    public static boolean applyAdoptionIdentity(ServerLevel level, Entity child, UUID playerId) {
        if (level == null || child == null || playerId == null || !MCAIntegrationBridge.isMCAVillagerEntity(child)) {
            return false;
        }

        PlayerHouseRecord record = PlayerHouseService.get(level, playerId);
        if (record == null || !record.hasHouseName()) {
            return false;
        }

        VillagerIdentityData existingIdentity = VillagerIdentityService.getIdentity(child);
        if (existingIdentity != null && "LEGAL_RENAME".equals(existingIdentity.surnameSource())) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, child);
        boolean changed = VillagerIdentityService.assignCurrentSurname(
                level,
                child,
                record.getHouseName(),
                SurnameSource.PLAYER_HOUSE
        );

        if (changed) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
        }

        return changed;
    }

    private static boolean applyPlayerHouseToChild(ServerLevel level, Entity child, UUID playerId) {
        if (level == null || child == null || playerId == null) {
            return false;
        }

        PlayerHouseRecord record = PlayerHouseService.get(level, playerId);
        if (record == null || !record.hasHouseName()) {
            return false;
        }

        VillagerIdentityService.ensureOriginFromCurrentVillage(level, child, null, OriginSource.DISCOVERED);

        boolean birthChanged = VillagerIdentityService.assignBirthSurname(
                level,
                child,
                record.getHouseName(),
                SurnameSource.BIRTH
        );

        boolean currentChanged = VillagerIdentityService.assignCurrentSurname(
                level,
                child,
                record.getHouseName(),
                SurnameSource.BIRTH
        );

        if (birthChanged || currentChanged) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
        }

        return birthChanged || currentChanged;
    }

    private static boolean applyPlayerHouseToChildIfNeeded(ServerLevel level, Entity child, UUID playerId) {
        if (level == null || child == null || playerId == null) {
            return false;
        }

        PlayerHouseRecord record = PlayerHouseService.get(level, playerId);
        if (record == null || !record.hasHouseName()) {
            return false;
        }

        VillagerIdentityData existingIdentity = VillagerIdentityService.getIdentity(child);
        if (existingIdentity != null && "LEGAL_RENAME".equals(existingIdentity.surnameSource())) {
            return false;
        }

        boolean missingSurname = existingIdentity == null || !existingIdentity.hasSurname();
        boolean generatedSurname = existingIdentity != null && "GENERATED".equals(existingIdentity.surnameSource());
        boolean currentWrong = existingIdentity == null || !record.getHouseName().equals(existingIdentity.currentSurname());
        boolean birthWrong = existingIdentity == null
                || existingIdentity.birthSurname() == null
                || existingIdentity.birthSurname().isBlank()
                || "GENERATED".equals(existingIdentity.surnameSource());

        if (!missingSurname && !generatedSurname && !currentWrong && !birthWrong) {
            return false;
        }

        return applyPlayerHouseToChild(level, child, playerId);
    }

    private static UUID resolveNodeId(Object node) {
        Object id = invokeNoArg(node, "id");
        return id instanceof UUID uuid ? uuid : null;
    }

    private static boolean isPlayerNode(Object node) {
        Object value = invokeNoArg(node, "isPlayer");
        return value instanceof Boolean result && result;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }

        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }
}