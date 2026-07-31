package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalCampaignAssemblyService {

    static final long ASSEMBLY_DURATION_TICKS =
            20L * 20L;

    static final long FORMATION_DURATION_TICKS =
            20L * 5L;

    private static final int MAX_TICKET_RADIUS = 6;
    private static final int MAX_PRIME_RADIUS = 2;

    private static final TicketType<UUID> ASSEMBLY_TICKET =
            TicketType.create(
                    "mcacapitals_campaign_assembly",
                    Comparator.comparing(UUID::toString),
                    (int) (ASSEMBLY_DURATION_TICKS + 20L * 15L)
            );

    private CapitalCampaignAssemblyService() {
    }

    static AssemblyResult tickAssembly(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        Village attackingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                attackingCapital
                        );

        Village defendingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (attackingVillage == null
                || defendingVillage == null) {
            return AssemblyResult.invalid(
                    "One of the campaign villages is unavailable."
            );
        }

        PlayerValidation playerValidation =
                findInitiatingPlayer(
                        level,
                        campaign,
                        attackingCapital,
                        defendingVillage
                );

        if (playerValidation.invalid()) {
            releaseSourceTicket(
                    level,
                    campaign,
                    attackingCapital
            );

            return AssemblyResult.invalid(
                    playerValidation.failureMessage()
            );
        }

        ServerPlayer player =
                playerValidation.player();

        if (player == null) {
            if (campaign.hasAssemblyStarted()) {
                releaseSourceTicket(
                        level,
                        campaign,
                        attackingCapital
                );

                campaign.resetAssembly();

                CapitalCampaignDataAccess
                        .get(level)
                        .setDirty();

                ServerPlayer initiatingPlayer =
                        findOnlineInitiatingPlayer(
                                level,
                                campaign
                        );

                if (initiatingPlayer != null) {
                    initiatingPlayer.sendSystemMessage(
                            Component.literal(
                                    "Campaign assembly was halted because you left the defending capital. Re-enter the capital to begin assembling again."
                            )
                    );
                }
            }

            return AssemblyResult.waiting();
        }

        boolean startedNow =
                !campaign.hasAssemblyStarted();

        if (startedNow) {
            campaign.beginAssembly(
                    level.getGameTime()
            );

            CapitalCampaignDataAccess
                    .get(level)
                    .setDirty();

            player.sendSystemMessage(
                    Component.literal(
                            "Your campaign force is assembling, get to an open area and prepare to fight."
                    )
            );
        }

        holdSourceTicket(
                level,
                campaign,
                attackingCapital
        );

        if (startedNow) {
            primeSourceVillageChunks(
                    level,
                    attackingVillage
            );
        }

        List<UUID> candidates =
                CapitalCampaignEligibilityService
                        .findEligibleAttackers(
                                level,
                                attackingCapital,
                                campaign.getCampaignId()
                        );

        int raisedTarget =
                Math.min(
                        CapitalCampaignRecord.MAX_ATTACKERS,
                        Math.max(
                                CapitalCampaignRecord.PREFERRED_ATTACKERS,
                                candidates.size()
                        )
                );

        int previousTarget =
                campaign.getTargetAttackerCount();

        campaign.raiseTargetAttackerCount(
                raisedTarget
        );

        List<UUID> assembledIds =
                assembleRoster(
                        campaign,
                        candidates
                );

        boolean rosterChanged =
                !assembledIds.isEmpty()
                        && !assembledIds.equals(
                        campaign.getAttackerIds()
                );

        if (rosterChanged) {
            campaign.replaceAttackerIds(
                    assembledIds
            );
        }

        List<VillagerEntityMCA> assembledAttackers =
                getLoadedAttackers(
                        level,
                        assembledIds
                );

        int assembledCount =
                assembledAttackers.size();

        boolean reportChanged =
                campaign.getLastAssemblyReportedCount()
                        != assembledCount;

        if (reportChanged) {
            campaign.markAssemblyReportedCount(
                    assembledCount
            );
        }

        if (previousTarget
                != campaign.getTargetAttackerCount()
                || rosterChanged
                || reportChanged) {
            CapitalCampaignDataAccess
                    .get(level)
                    .setDirty();
        }

        long elapsed =
                level.getGameTime()
                        - campaign.getAssemblyStartedAt();

        if (elapsed < ASSEMBLY_DURATION_TICKS) {
            return AssemblyResult.waiting();
        }

        releaseSourceTicket(
                level,
                campaign,
                attackingCapital
        );

        if (assembledAttackers.isEmpty()) {
            return AssemblyResult.invalid(
                    "The campaign force could not assemble any eligible Guards or Archers. The planned attack was cancelled before war began."
            );
        }

        String completionMessage =
                assembledCount
                        >= campaign.getTargetAttackerCount()
                        ? "Campaign assembly is complete: "
                        + assembledCount
                        + " soldiers are ready to deploy."
                        : "Campaign assembly ended with "
                        + assembledCount
                        + " of the preferred "
                        + campaign.getTargetAttackerCount()
                        + " soldiers available. The attack will proceed with the assembled force.";

        if (elapsed < ASSEMBLY_DURATION_TICKS + 10L) {
            player.sendSystemMessage(
                    Component.literal(
                            completionMessage
                    )
            );
        }

        return AssemblyResult.ready(
                player,
                assembledAttackers
        );
    }

    static ServerPlayer findFormationPlayer(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        Village defendingVillage =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (defendingVillage == null) {
            return null;
        }

        PlayerValidation validation =
                findInitiatingPlayer(
                        level,
                        campaign,
                        attackingCapital,
                        defendingVillage
                );

        return validation.invalid()
                ? null
                : validation.player();
    }

    static void holdSourceTicket(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital
    ) {
        Village village =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                attackingCapital
                        );

        if (village == null) {
            return;
        }

        ChunkPos center =
                new ChunkPos(
                        new BlockPos(
                                village.getCenter()
                        )
                );

        level.getChunkSource()
                .addRegionTicket(
                        ASSEMBLY_TICKET,
                        center,
                        calculateTicketRadius(village),
                        campaign.getCampaignId()
                );
    }

    static void releaseSourceTicket(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital
    ) {
        if (level == null
                || campaign == null
                || attackingCapital == null) {
            return;
        }

        Village village =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                attackingCapital
                        );

        if (village == null) {
            return;
        }

        ChunkPos center =
                new ChunkPos(
                        new BlockPos(
                                village.getCenter()
                        )
                );

        level.getChunkSource()
                .removeRegionTicket(
                        ASSEMBLY_TICKET,
                        center,
                        calculateTicketRadius(village),
                        campaign.getCampaignId()
                );
    }

    private static PlayerValidation findInitiatingPlayer(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            Village defendingVillage
    ) {
        UUID initiatingPlayerId =
                campaign.getInitiatingPlayerId();

        if (initiatingPlayerId == null) {
            return PlayerValidation.invalid(
                    "The planned attack has no initiating player and cannot assemble."
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        attackingCapital,
                        initiatingPlayerId
                )) {
            return PlayerValidation.invalid(
                    "The player who planned this attack no longer has authority to command the capital's foreign affairs."
            );
        }

        ServerPlayer player =
                findOnlineInitiatingPlayer(
                        level,
                        campaign
                );

        if (player == null
                || !defendingVillage.isWithinBorder(
                player
        )) {
            return PlayerValidation.waiting();
        }

        return PlayerValidation.success(player);
    }

    private static ServerPlayer findOnlineInitiatingPlayer(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        if (campaign.getInitiatingPlayerId()
                == null) {
            return null;
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                campaign.getInitiatingPlayerId()
                        );

        return player != null
                && player.level() == level
                && player.isAlive()
                && !player.isSpectator()
                ? player
                : null;
    }

    private static List<UUID> assembleRoster(
            CapitalCampaignRecord campaign,
            List<UUID> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<UUID> candidateSet =
                Set.copyOf(candidates);

        LinkedHashSet<UUID> selected =
                new LinkedHashSet<>();

        for (UUID currentId :
                campaign.getAttackerIds()) {
            if (candidateSet.contains(currentId)) {
                selected.add(currentId);
            }

            if (selected.size()
                    >= campaign.getTargetAttackerCount()) {
                break;
            }
        }

        for (UUID candidateId : candidates) {
            selected.add(candidateId);

            if (selected.size()
                    >= campaign.getTargetAttackerCount()) {
                break;
            }
        }

        return List.copyOf(selected);
    }

    private static List<VillagerEntityMCA> getLoadedAttackers(
            ServerLevel level,
            List<UUID> attackerIds
    ) {
        List<VillagerEntityMCA> result =
                new ArrayList<>();

        for (UUID attackerId : attackerIds) {
            if (MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            attackerId
                    )
                    instanceof VillagerEntityMCA attacker
                    && attacker.isAlive()
                    && !attacker.isRemoved()) {
                result.add(attacker);
            }
        }

        return List.copyOf(result);
    }

    private static int calculateTicketRadius(
            Village village
    ) {
        ChunkPos center =
                new ChunkPos(
                        new BlockPos(
                                village.getCenter()
                        )
                );

        BoundingBox box = village.getBox();

        if (box == null) {
            return 2;
        }

        int minChunkX = Math.floorDiv(box.minX(), 16);
        int maxChunkX = Math.floorDiv(box.maxX(), 16);
        int minChunkZ = Math.floorDiv(box.minZ(), 16);
        int maxChunkZ = Math.floorDiv(box.maxZ(), 16);

        int radius = Math.max(
                Math.max(
                        Math.abs(center.x - minChunkX),
                        Math.abs(center.x - maxChunkX)
                ),
                Math.max(
                        Math.abs(center.z - minChunkZ),
                        Math.abs(center.z - maxChunkZ)
                )
        ) + 1;

        return Math.max(
                1,
                Math.min(MAX_TICKET_RADIUS, radius)
        );
    }

    private static void primeSourceVillageChunks(
            ServerLevel level,
            Village village
    ) {
        ChunkPos center =
                new ChunkPos(
                        new BlockPos(
                                village.getCenter()
                        )
                );

        int radius = Math.min(
                MAX_PRIME_RADIUS,
                calculateTicketRadius(village)
        );

        for (int distance = 0;
             distance <= radius;
             distance++) {
            for (int x = center.x - distance;
                 x <= center.x + distance;
                 x++) {
                for (int z = center.z - distance;
                     z <= center.z + distance;
                     z++) {
                    if (Math.max(
                            Math.abs(x - center.x),
                            Math.abs(z - center.z)
                    ) != distance) {
                        continue;
                    }

                    level.getChunkSource()
                            .getChunk(
                                    x,
                                    z,
                                    ChunkStatus.FULL,
                                    true
                            );
                }
            }
        }
    }

    record AssemblyResult(
            boolean ready,
            boolean invalid,
            String failureMessage,
            ServerPlayer player,
            List<VillagerEntityMCA> attackers
    ) {

        static AssemblyResult waiting() {
            return new AssemblyResult(
                    false,
                    false,
                    null,
                    null,
                    List.of()
            );
        }

        static AssemblyResult ready(
                ServerPlayer player,
                List<VillagerEntityMCA> attackers
        ) {
            return new AssemblyResult(
                    true,
                    false,
                    null,
                    player,
                    List.copyOf(attackers)
            );
        }

        static AssemblyResult invalid(
                String message
        ) {
            return new AssemblyResult(
                    false,
                    true,
                    message,
                    null,
                    List.of()
            );
        }
    }

    private record PlayerValidation(
            ServerPlayer player,
            boolean invalid,
            String failureMessage
    ) {

        static PlayerValidation success(
                ServerPlayer player
        ) {
            return new PlayerValidation(
                    player,
                    false,
                    null
            );
        }

        static PlayerValidation waiting() {
            return new PlayerValidation(
                    null,
                    false,
                    null
            );
        }

        static PlayerValidation invalid(
                String message
        ) {
            return new PlayerValidation(
                    null,
                    true,
                    message
            );
        }
    }
}