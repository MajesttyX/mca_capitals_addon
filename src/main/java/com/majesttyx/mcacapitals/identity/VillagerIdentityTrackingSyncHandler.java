package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = MCACapitals.MODID)
public final class VillagerIdentityTrackingSyncHandler {

    private static final int SYNC_INTERVAL_TICKS = 20 * 7;
    private static final double SYNC_RADIUS = 64.0D;

    private VillagerIdentityTrackingSyncHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();
        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        boolean repairedFromBirth = BirthIdentityService.repairFromParentsIfNeeded(level, entity);
        if (!repairedFromBirth) {
            VillagerIdentityService.ensureAssigned(level, entity);
            BirthIdentityService.repairFromParentsIfNeeded(level, entity);
        }

        VillagerIdentitySyncService.syncToNearbyPlayers(level, entity);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        VillagerIdentitySyncService.syncToPlayer(player, event.getTarget());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
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
            boolean repairedFromBirth = BirthIdentityService.repairFromParentsIfNeeded(level, villager);
            if (!repairedFromBirth) {
                VillagerIdentityService.ensureAssigned(level, villager);
                BirthIdentityService.repairFromParentsIfNeeded(level, villager);
            }

            VillagerIdentitySyncService.syncToPlayer(player, villager);
        }
    }
}