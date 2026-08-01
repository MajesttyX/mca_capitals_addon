package com.majesttyx.mcacapitals.identity;

import com.majesttyx.mcacapitals.capital.CapitalAsylumService;
import com.majesttyx.mcacapitals.capital.CapitalRecord;
import com.majesttyx.mcacapitals.capital.CapitalTitleResolver;
import com.majesttyx.mcacapitals.config.MCACapitalsConfig;
import com.majesttyx.mcacapitals.network.ModNetwork;
import com.majesttyx.mcacapitals.network.SyncVillagerIdentityPacket;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class VillagerIdentitySyncService {

    private static final double NEARBY_SYNC_RADIUS =
            64.0D;

    private VillagerIdentitySyncService() {
    }

    public static void syncToPlayer(
            ServerPlayer player,
            Entity entity
    ) {
        if (player == null
                || entity == null
                || !(player.level()
                instanceof ServerLevel level)) {
            return;
        }

        if (!MCAIntegrationBridge
                .isMCAVillagerEntity(entity)) {
            return;
        }

        repairIdentityInheritance(
                level,
                entity
        );

        SyncVillagerIdentityPacket packet =
                createPacket(
                        level,
                        entity
                );

        if (packet == null) {
            return;
        }

        ModNetwork.sendToPlayer(
                player,
                packet
        );
    }

    public static void syncToNearbyPlayers(
            ServerLevel level,
            Entity entity
    ) {
        if (level == null
                || entity == null
                || !MCAIntegrationBridge
                .isMCAVillagerEntity(entity)) {
            return;
        }

        repairIdentityInheritance(
                level,
                entity
        );

        SyncVillagerIdentityPacket packet =
                createPacket(
                        level,
                        entity
                );

        if (packet == null) {
            return;
        }

        double maxDistanceSqr =
                NEARBY_SYNC_RADIUS
                        * NEARBY_SYNC_RADIUS;

        for (ServerPlayer player :
                level.players()) {
            if (player == null
                    || player.distanceToSqr(entity)
                    > maxDistanceSqr) {
                continue;
            }

            ModNetwork.sendToPlayer(
                    player,
                    packet
            );
        }
    }

    public static SyncVillagerIdentityPacket createPacket(
            ServerLevel level,
            Entity entity
    ) {
        if (level == null
                || entity == null
                || !MCAIntegrationBridge
                .isMCAVillagerEntity(entity)) {
            return null;
        }

        UUID villagerId =
                entity.getUUID();

        VillagerIdentityData identity =
                VillagerIdentityService
                        .getIdentity(entity);

        String title =
                CapitalTitleResolver
                        .getDisplayTitleForEntity(
                                level,
                                villagerId
                        );

        String royalGuardOrderLine =
                resolveRoyalGuardOrderLine(
                        level,
                        villagerId
                );

        String courtOfficeLine =
                CapitalTitleResolver
                        .getCourtOfficeLineForEntity(
                                level,
                                villagerId
                        );

        if (courtOfficeLine == null
                || courtOfficeLine.isBlank()) {
            courtOfficeLine =
                    CapitalAsylumService
                            .getStatusLine(
                                    level,
                                    villagerId
                            );
        }

        boolean hasIdentity =
                identity != null
                        && (
                        identity.hasOrigin()
                                || identity.hasSurname()
                                || identity
                                .hasFoundedHouse()
                );

        boolean hasTitle =
                title != null
                        && !title.isBlank()
                        && !"None".equals(title)
                        && !"Commoner".equals(title);

        boolean hasCourtOfficeLine =
                courtOfficeLine != null
                        && !courtOfficeLine.isBlank();

        if (!hasIdentity
                && !hasTitle
                && !hasCourtOfficeLine) {
            return null;
        }

        return new SyncVillagerIdentityPacket(
                villagerId,
                resolveDisplayedOriginVillageName(
                        level,
                        identity
                ),
                identity == null
                        ? ""
                        : identity.originSource(),
                identity == null
                        ? ""
                        : identity.currentSurname(),
                title,
                royalGuardOrderLine,
                courtOfficeLine,
                identity != null
                        && identity.hasFoundedHouse(),
                identity == null
                        ? ""
                        : identity.houseName(),
                identity == null
                        ? ""
                        : identity.houseWords(),
                identity == null
                        ? ""
                        : identity
                        .houseWordsPersonality()
        );
    }

    private static String resolveDisplayedOriginVillageName(
            ServerLevel level,
            VillagerIdentityData identity
    ) {
        if (identity == null) {
            return "";
        }

        String historicalName =
                identity.originVillageName() == null
                        ? ""
                        : identity.originVillageName().trim();

        MCACapitalsConfig.OriginNameMode mode =
                MCACapitalsConfig.originNameMode();

        if (mode == MCACapitalsConfig.OriginNameMode.HISTORICAL
                || identity.originVillageId() == null) {
            return historicalName;
        }

        ServerLevel originLevel =
                resolveOriginLevel(
                        level,
                        identity.originDimension()
                );

        if (originLevel == null
                || !MCAIntegrationBridge.hasVillage(
                originLevel,
                identity.originVillageId()
        )) {
            return historicalName;
        }

        String currentName =
                MCAIntegrationBridge.getVillageName(
                        originLevel,
                        identity.originVillageId()
                );

        if (currentName == null
                || currentName.isBlank()
                || "Unknown Village".equals(currentName)) {
            return historicalName;
        }

        currentName = currentName.trim();

        if (mode == MCACapitalsConfig.OriginNameMode.CURRENT
                || historicalName.isBlank()
                || currentName.equals(historicalName)) {
            return currentName;
        }

        return currentName
                + ", formerly "
                + historicalName;
    }

    private static ServerLevel resolveOriginLevel(
            ServerLevel currentLevel,
            String originDimension
    ) {
        if (currentLevel == null
                || originDimension == null
                || originDimension.isBlank()) {
            return currentLevel;
        }

        ResourceLocation dimensionLocation =
                ResourceLocation.tryParse(
                        originDimension.trim()
                );

        if (dimensionLocation == null) {
            return currentLevel;
        }

        ResourceKey<Level> dimensionKey =
                ResourceKey.create(
                        Registries.DIMENSION,
                        dimensionLocation
                );

        ServerLevel originLevel =
                currentLevel.getServer()
                        .getLevel(dimensionKey);

        return originLevel == null
                ? currentLevel
                : originLevel;
    }

    private static void repairIdentityInheritance(
            ServerLevel level,
            Entity entity
    ) {
        boolean repairedFromPlayerHouse =
                PlayerHouseIdentityService
                        .repairFromParentsIfNeeded(
                                level,
                                entity
                        );

        boolean repairedFromBirth =
                repairedFromPlayerHouse
                        || BirthIdentityService
                        .repairFromParentsIfNeeded(
                                level,
                                entity
                        );

        if (!repairedFromBirth) {
            VillagerIdentityService.ensureAssigned(
                    level,
                    entity
            );

            if (!PlayerHouseIdentityService
                    .repairFromParentsIfNeeded(
                            level,
                            entity
                    )) {
                BirthIdentityService
                        .repairFromParentsIfNeeded(
                                level,
                                entity
                        );
            }
        }
    }

    private static String resolveRoyalGuardOrderLine(
            ServerLevel level,
            UUID villagerId
    ) {
        CapitalRecord capital =
                CapitalTitleResolver
                        .findCapitalForEntity(
                                level,
                                villagerId
                        );

        if (capital == null
                || !capital.isRoyalGuard(
                villagerId
        )) {
            return "";
        }

        return capital.isSovereignFemale()
                ? "Of the Queensguard"
                : "Of the Kingsguard";
    }
}