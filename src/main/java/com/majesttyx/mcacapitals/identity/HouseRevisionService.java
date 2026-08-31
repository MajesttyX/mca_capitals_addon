package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.data.HouseRevisionSavedData;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class HouseRevisionService {

    private HouseRevisionService() {
    }

    public static boolean recordAndApply(
            ServerLevel level,
            UUID founderId,
            String houseName,
            String houseWords
    ) {
        if (level == null || founderId == null) {
            return false;
        }

        String normalizedHouseName = normalize(houseName);
        String normalizedHouseWords = normalize(houseWords);
        if (normalizedHouseName.isBlank()) {
            return false;
        }

        HouseRevisionSavedData.HouseRevisionRecord revision =
                HouseRevisionSavedData.get(level).put(
                        founderId,
                        normalizedHouseName,
                        normalizedHouseWords,
                        level.getGameTime()
                );

        if (revision == null) {
            return false;
        }

        boolean changed = false;
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }

        for (ServerLevel serverLevel : server.getAllLevels()) {
            for (Entity entity : serverLevel.getEntities().getAll()) {
                if (entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
                    continue;
                }

                VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);
                if (identity == null
                        || !identity.hasFoundedHouse()
                        || !founderId.equals(identity.houseFounderId())) {
                    continue;
                }

                changed |= applyRevision(serverLevel, entity, revision);
            }
        }

        return changed;
    }

    public static boolean reconcileEntity(ServerLevel level, Entity entity) {
        if (level == null
                || entity == null
                || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return false;
        }

        VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);
        if (identity == null
                || !identity.hasFoundedHouse()
                || identity.houseFounderId() == null) {
            return false;
        }

        HouseRevisionSavedData.HouseRevisionRecord revision =
                HouseRevisionSavedData.get(level).get(identity.houseFounderId());

        if (revision == null) {
            return false;
        }

        return applyRevision(level, entity, revision);
    }

    private static boolean applyRevision(
            ServerLevel level,
            Entity entity,
            HouseRevisionSavedData.HouseRevisionRecord revision
    ) {
        if (level == null || entity == null || revision == null) {
            return false;
        }

        VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);
        if (identity == null
                || !identity.hasFoundedHouse()
                || !revision.founderId().equals(identity.houseFounderId())) {
            return false;
        }

        boolean alreadyCurrent =
                safeEquals(revision.houseName(), identity.houseName())
                        && safeEquals(revision.houseName(), identity.currentSurname())
                        && safeEquals(revision.houseWords(), identity.houseWords());

        if (alreadyCurrent) {
            return false;
        }

        boolean changed = VillagerIdentityService.reviseHouse(
                level,
                entity,
                revision.houseName(),
                revision.houseWords()
        );

        if (changed) {
            VillagerIdentitySyncService.syncToNearbyPlayers(level, entity);
        }

        return changed;
    }

    private static boolean safeEquals(String first, String second) {
        return normalize(first).equals(normalize(second));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
