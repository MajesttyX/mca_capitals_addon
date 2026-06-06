package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.house.PlayerHouseRecord;
import com.majesttyx.mcacapitals.house.PlayerHouseService;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.Set;
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

        boolean changed = applyPlayerHouseIdentityToVillager(level, child, playerId, record, SurnameSource.PLAYER_HOUSE, false);

        if (changed) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
        }

        return changed;
    }

    public static void applyPlayerHouseToImmediateFamily(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) {
            return;
        }

        PlayerHouseRecord record = PlayerHouseService.get(level, player.getUUID());
        if (record == null || !record.hasHouseName()) {
            return;
        }

        UUID spouseId = MCAIntegrationBridge.getSpouse(level, player.getUUID());
        Entity spouse = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, spouseId);
        if (spouse != null && MCAIntegrationBridge.isMCAVillagerEntity(spouse)) {
            if (applyPlayerHouseIdentityToVillager(level, spouse, player.getUUID(), record, SurnameSource.MARRIAGE, false)) {
                VillagerIdentitySyncService.syncToNearbyPlayers(level, spouse);
            }
        }

        Set<UUID> childIds = MCAIntegrationBridge.getChildren(level, player.getUUID());
        for (UUID childId : childIds) {
            Entity child = MCAIntegrationBridge.findLoadedMCAVillagerByUuid(level, childId);
            if (child == null || !MCAIntegrationBridge.isMCAVillagerEntity(child)) {
                continue;
            }

            if (applyPlayerHouseIdentityToVillager(level, child, player.getUUID(), record, SurnameSource.BIRTH, true)) {
                VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
            }
        }
    }

    public static boolean applyPlayerHouseIdentityToVillager(
            ServerLevel level,
            Entity villager,
            UUID playerId,
            PlayerHouseRecord record,
            SurnameSource surnameSource,
            boolean setBirthSurname
    ) {
        if (level == null || villager == null || playerId == null || record == null || !record.hasHouseName()) {
            return false;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(villager)) {
            return false;
        }

        VillagerIdentityService.ensureAssigned(level, villager);

        boolean changed = false;

        if (setBirthSurname) {
            changed |= VillagerIdentityService.ensureOriginFromCurrentVillage(level, villager, null, OriginSource.BIRTH);
            changed |= VillagerIdentityService.assignBirthSurname(level, villager, record.getHouseName(), surnameSource);
        }

        changed |= VillagerIdentityService.assignCurrentSurname(level, villager, record.getHouseName(), surnameSource);

        changed |= VillagerIdentityService.foundHouse(
                level,
                villager,
                record.getHouseName(),
                record.getHouseWords(),
                "PLAYER",
                playerId,
                resolvePlayerName(level, playerId),
                record.getHouseNameSetInCapitalId(),
                record.getHouseNameSetInCapitalName()
        );

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

        boolean changed = applyPlayerHouseIdentityToVillager(level, child, playerId, record, SurnameSource.BIRTH, true);

        if (changed) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, child);
        }

        return changed;
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
                || "GENERATED".equals(existingIdentity.surnameSource())
                || !record.getHouseName().equals(existingIdentity.birthSurname());

        boolean houseWrong = existingIdentity == null
                || !existingIdentity.hasFoundedHouse()
                || !record.getHouseName().equals(existingIdentity.houseName())
                || !safeEquals(record.getHouseWords(), existingIdentity.houseWords());

        if (!missingSurname && !generatedSurname && !currentWrong && !birthWrong && !houseWrong) {
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

    private static String resolvePlayerName(ServerLevel level, UUID playerId) {
        if (level == null || playerId == null || level.getServer() == null) {
            return "";
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            return player.getName().getString();
        }

        return "";
    }

    private static boolean safeEquals(String first, String second) {
        String a = first == null ? "" : first;
        String b = second == null ? "" : second;
        return a.equals(b);
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