package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.MCACapitals;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = MCACapitals.MODID)
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

        repairIdentityInheritance(level, entity);
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
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