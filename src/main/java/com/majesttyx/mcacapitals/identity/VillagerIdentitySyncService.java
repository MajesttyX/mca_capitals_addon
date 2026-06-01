package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.SyncVillagerIdentityPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class VillagerIdentitySyncService {

    private static final double NEARBY_SYNC_RADIUS = 64.0D;

    private VillagerIdentitySyncService() {
    }

    public static void syncToPlayer(ServerPlayer player, Entity entity) {
        if (player == null || entity == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        boolean repairedFromBirth = BirthIdentityService.repairFromParentsIfNeeded(level, entity);
        if (!repairedFromBirth) {
            VillagerIdentityService.ensureAssigned(level, entity);
            BirthIdentityService.repairFromParentsIfNeeded(level, entity);
        }

        SyncVillagerIdentityPacket packet = createPacket(level, entity);
        if (packet == null) {
            return;
        }

        ModNetwork.sendToPlayer(player, packet);
    }

    public static void syncToNearbyPlayers(ServerLevel level, Entity entity) {
        if (level == null || entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return;
        }

        boolean repairedFromBirth = BirthIdentityService.repairFromParentsIfNeeded(level, entity);
        if (!repairedFromBirth) {
            VillagerIdentityService.ensureAssigned(level, entity);
            BirthIdentityService.repairFromParentsIfNeeded(level, entity);
        }

        SyncVillagerIdentityPacket packet = createPacket(level, entity);
        if (packet == null) {
            return;
        }

        double maxDistanceSqr = NEARBY_SYNC_RADIUS * NEARBY_SYNC_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player == null || player.distanceToSqr(entity) > maxDistanceSqr) {
                continue;
            }

            ModNetwork.sendToPlayer(player, packet);
        }
    }

    public static SyncVillagerIdentityPacket createPacket(ServerLevel level, Entity entity) {
        if (level == null || entity == null || !MCAIntegrationBridge.isMCAVillagerEntity(entity)) {
            return null;
        }

        UUID villagerId = entity.getUUID();
        VillagerIdentityData identity = VillagerIdentityService.getIdentity(entity);

        String title = CapitalTitleResolver.getDisplayTitleForEntity(level, villagerId);
        String royalGuardOrderLine = resolveRoyalGuardOrderLine(level, villagerId);

        boolean hasIdentity = identity != null && (identity.hasOrigin() || identity.hasSurname() || identity.hasFoundedHouse());
        boolean hasTitle = title != null && !title.isBlank() && !"None".equals(title) && !"Commoner".equals(title);

        if (!hasIdentity && !hasTitle) {
            return null;
        }

        return new SyncVillagerIdentityPacket(
                villagerId,
                identity == null ? "" : identity.originVillageName(),
                identity == null ? "" : identity.originSource(),
                identity == null ? "" : identity.currentSurname(),
                title,
                royalGuardOrderLine,
                identity != null && identity.hasFoundedHouse(),
                identity == null ? "" : identity.houseName(),
                identity == null ? "" : identity.houseWords(),
                identity == null ? "" : identity.houseWordsPersonality()
        );
    }

    private static String resolveRoyalGuardOrderLine(ServerLevel level, UUID villagerId) {
        CapitalRecord capital = CapitalTitleResolver.findCapitalForEntity(level, villagerId);
        if (capital == null || !capital.isRoyalGuard(villagerId)) {
            return "";
        }

        return capital.isSovereignFemale() ? "Of the Queensguard" : "Of the Kingsguard";
    }
}