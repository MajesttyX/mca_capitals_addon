package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class VillagerIdentityTrackingSyncHandler {

    private static final int SYNC_INTERVAL_TICKS = 20 * 7;
    private static final double SYNC_RADIUS = 64.0D;

    private VillagerIdentityTrackingSyncHandler() {
    }

    public static void onEntityJoinLevel(Entity entity, ServerLevel level) {
        if (entity == null || level == null) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        repairIdentityInheritance(level, entity);
        VillagerIdentitySyncService.syncToNearbyPlayers(level, entity);
    }

    public static void onStartTracking(Entity target, ServerPlayer player) {
        if (target == null || player == null) {
            return;
        }

        VillagerIdentitySyncService.syncToPlayer(player, target);
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (player.tickCount % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        List<Entity> nearbyVillagers = level.getEntities(
                player,
                player.getBoundingBox().inflate(SYNC_RADIUS),
                MCAIntegrationBridge::isMCAVillagerEntity
        );

        for (Entity villager : nearbyVillagers) {
            repairIdentityInheritance(level, villager);
            VillagerIdentitySyncService.syncToPlayer(player, villager);
        }
    }

    private static void repairIdentityInheritance(ServerLevel level, Entity entity) {
        boolean repairedFromPlayerHouse = PlayerHouseIdentityService.repairFromParentsIfNeeded(level, entity);
        boolean repairedFromBirth = repairedFromPlayerHouse || BirthIdentityService.repairFromParentsIfNeeded(level, entity);

        if (!repairedFromBirth) {
            VillagerIdentityService.ensureAssigned(level, entity);
            if (!PlayerHouseIdentityService.repairFromParentsIfNeeded(level, entity)) {
                BirthIdentityService.repairFromParentsIfNeeded(level, entity);
            }
        }
    }
}