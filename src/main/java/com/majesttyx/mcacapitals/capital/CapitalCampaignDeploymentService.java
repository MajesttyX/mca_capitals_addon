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
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CapitalCampaignDeploymentService {

    private static final int[][] FORMATION_OFFSETS = {
            {-3, 2},
            {0, 3},
            {3, 2},
            {-4, 0},
            {4, 0},
            {-3, -2},
            {3, -2},
            {0, -4},
            {-5, 3},
            {5, 3},
            {-5, -3},
            {5, -3}
    };

    private CapitalCampaignDeploymentService() {
    }

    static DeploymentResult deploy(
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
            return DeploymentResult.invalid(
                    "The defending capital's MCA village is unavailable."
            );
        }

        AnchorValidation anchorValidation =
                findInitiatingPlayer(
                        level,
                        campaign,
                        attackingCapital,
                        defendingVillage
                );

        if (anchorValidation.invalid()) {
            return DeploymentResult.invalid(
                    anchorValidation.failureMessage()
            );
        }

        ServerPlayer anchor =
                anchorValidation.player();

        if (anchor == null) {
            return DeploymentResult.waiting();
        }

        List<VillagerEntityMCA> availableAttackers =
                findAvailableAttackers(
                        level,
                        campaign
                );

        if (availableAttackers.isEmpty()) {
            return DeploymentResult.invalid(
                    "No campaign attackers remained available for deployment."
            );
        }

        List<BlockPos> positions =
                createFormation(
                        level,
                        defendingVillage,
                        anchor.blockPosition(),
                        availableAttackers.size()
                );

        if (positions.isEmpty()) {
            return DeploymentResult.waiting();
        }

        if (!CapitalDiplomaticWarService
                .beginCampaignWar(
                        level,
                        attackingCapital,
                        defendingCapital
                )) {
            return DeploymentResult.invalid(
                    "The attack could not begin because the War state could not be established."
            );
        }

        List<UUID> deployedAttackers =
                new ArrayList<>();

        int deploymentCount =
                Math.min(
                        positions.size(),
                        availableAttackers.size()
                );

        for (int index = 0;
             index < deploymentCount;
             index++) {
            VillagerEntityMCA attacker =
                    availableAttackers.get(index);

            BlockPos position =
                    positions.get(index);

            CapitalCampaignTargetingService
                    .clearCombatTarget(attacker);

            attacker.getNavigation().stop();
            attacker.stopRiding();

            attacker.teleportTo(
                    position.getX() + 0.5D,
                    position.getY(),
                    position.getZ() + 0.5D
            );

            deployedAttackers.add(
                    attacker.getUUID()
            );
        }

        if (deployedAttackers.isEmpty()) {
            return DeploymentResult.invalid(
                    "No campaign attackers could arrive beside the sovereign."
            );
        }

        List<UUID> defenders =
                findFieldDefenders(
                        level,
                        defendingCapital
                );

        campaign.replaceAttackerIds(
                deployedAttackers
        );

        campaign.setDefenderIds(defenders);
        campaign.activate(level.getGameTime());

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

        String attackingName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                attackingCapital
                        );

        String defendingName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                defendingCapital
                        );

        String entry =
                deployedAttackers.size()
                        + " campaign attackers from "
                        + attackingName
                        + " arrived beside their sovereign inside "
                        + defendingName
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                attackingCapital,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                defendingCapital,
                entry
        );

        anchor.sendSystemMessage(
                Component.literal(
                        "Your campaign force has arrived and will fight beside you."
                )
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(
                                "A campaign force from "
                                        + attackingName
                                        + " has appeared inside the capital."
                        )
                );

        return DeploymentResult.success();
    }

    static List<UUID> findFieldDefenders(
            ServerLevel level,
            CapitalRecord defendingCapital
    ) {
        Set<UUID> residents =
                CapitalResidentScanner.scanResidents(
                        level,
                        defendingCapital.getCapitalId()
                );

        return residents.stream()
                .filter(id -> id != null)
                .filter(id ->
                        !id.equals(
                                defendingCapital.getSovereign()
                        )
                )
                .filter(id ->
                        !defendingCapital.isRoyalGuard(id)
                )
                .filter(id ->
                        MCAIntegrationBridge.isMCAGuard(
                                level,
                                id
                        )
                )
                .filter(id ->
                        MCAIntegrationBridge
                                .isTeenOrAdultVillager(
                                        level,
                                        id
                                )
                )
                .filter(id ->
                        MCAIntegrationBridge
                                .isLoadedAndAlive(
                                        level,
                                        id
                                )
                )
                .sorted(
                        Comparator.comparing(
                                UUID::toString
                        )
                )
                .toList();
    }

    private static AnchorValidation findInitiatingPlayer(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            Village defendingVillage
    ) {
        UUID initiatingPlayerId =
                campaign.getInitiatingPlayerId();

        if (initiatingPlayerId == null) {
            return AnchorValidation.invalid(
                    "The planned attack has no initiating player sovereign and cannot deploy."
            );
        }

        if (!initiatingPlayerId.equals(
                attackingCapital.getPlayerSovereignId()
        )) {
            return AnchorValidation.invalid(
                    "The player who planned this attack is no longer the attacking capital's sovereign."
            );
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(initiatingPlayerId);

        if (player == null
                || player.level() != level
                || !player.isAlive()
                || player.isSpectator()) {
            return AnchorValidation.waiting();
        }

        if (!defendingVillage.isWithinBorder(player)) {
            return AnchorValidation.waiting();
        }

        return AnchorValidation.success(player);
    }

    private static List<VillagerEntityMCA>
    findAvailableAttackers(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        List<VillagerEntityMCA> result =
                new ArrayList<>();

        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (MCAIntegrationBridge
                    .findLoadedMCAVillagerByUuid(
                            level,
                            attackerId
                    )
                    instanceof VillagerEntityMCA attacker
                    && attacker.isAlive()) {
                result.add(attacker);
            }
        }

        return result;
    }

    private static List<BlockPos> createFormation(
            ServerLevel level,
            Village defendingVillage,
            BlockPos anchor,
            int requiredPositions
    ) {
        List<BlockPos> positions =
                new ArrayList<>();

        for (int[] offset :
                FORMATION_OFFSETS) {
            int x =
                    anchor.getX() + offset[0];

            int z =
                    anchor.getZ() + offset[1];

            BlockPos horizontal =
                    new BlockPos(
                            x,
                            anchor.getY(),
                            z
                    );

            if (!level.hasChunkAt(horizontal)
                    || !defendingVillage
                    .isWithinBorder(
                            horizontal,
                            0
                    )) {
                continue;
            }

            int y =
                    level.getHeight(
                            Heightmap.Types
                                    .MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            positions.add(
                    new BlockPos(x, y, z)
            );

            if (positions.size()
                    >= requiredPositions) {
                break;
            }
        }

        return List.copyOf(positions);
    }

    record DeploymentResult(
            boolean deployed,
            boolean invalid,
            String failureMessage
    ) {

        static DeploymentResult success() {
            return new DeploymentResult(
                    true,
                    false,
                    null
            );
        }

        static DeploymentResult waiting() {
            return new DeploymentResult(
                    false,
                    false,
                    null
            );
        }

        static DeploymentResult invalid(
                String message
        ) {
            return new DeploymentResult(
                    false,
                    true,
                    message
            );
        }
    }

    private record AnchorValidation(
            ServerPlayer player,
            boolean invalid,
            String failureMessage
    ) {

        static AnchorValidation success(
                ServerPlayer player
        ) {
            return new AnchorValidation(
                    player,
                    false,
                    null
            );
        }

        static AnchorValidation waiting() {
            return new AnchorValidation(
                    null,
                    false,
                    null
            );
        }

        static AnchorValidation invalid(
                String message
        ) {
            return new AnchorValidation(
                    null,
                    true,
                    message
            );
        }
    }
}