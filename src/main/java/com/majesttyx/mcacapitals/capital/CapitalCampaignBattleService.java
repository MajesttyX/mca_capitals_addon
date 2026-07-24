package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class CapitalCampaignBattleService {

    private static final int
            PEACE_REQUEST_CHANCE_PERCENT = 60;

    private CapitalCampaignBattleService() {
    }

    static void processCampaign(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        CapitalRecord attackingCapital =
                CapitalManager.getCapital(
                        campaign.getAttackingCapitalId()
                );

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign.getDefendingCapitalId()
                );

        if (attackingCapital == null
                || defendingCapital == null) {
            CapitalCampaignTargetingService
                    .clearCampaignTargets(
                            level,
                            campaign
                    );

            CapitalCampaignService.completeCampaign(
                    level,
                    campaign.getCampaignId()
            );

            return;
        }

        if (campaign.getPhase()
                == CapitalCampaignPhase.MUSTERING) {
            processMustering(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital
            );

            return;
        }

        if (campaign.getPhase()
                == CapitalCampaignPhase.RETREATING) {
            if (CapitalCampaignReturnService
                    .processRetreat(
                            level,
                            campaign,
                            attackingCapital,
                            defendingCapital
                    )) {
                CapitalCampaignService
                        .completeCampaign(
                                level,
                                campaign.getCampaignId()
                        );
            }

            return;
        }

        processActiveBattle(
                level,
                campaign,
                attackingCapital,
                defendingCapital
        );
    }

    private static void processMustering(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        CapitalCampaignDeploymentService
                .DeploymentResult result =
                CapitalCampaignDeploymentService
                        .deploy(
                                level,
                                campaign,
                                attackingCapital,
                                defendingCapital
                        );

        if (result.invalid()) {
            invalidateCampaign(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    result.failureMessage()
            );
        }
    }

    private static void processActiveBattle(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        if (CapitalDiplomacyDataAccess
                .getDiplomaticState(
                        level,
                        attackingCapital.getCapitalId(),
                        defendingCapital.getCapitalId()
                )
                != CapitalDiplomaticState.WAR) {
            beginRetreat(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    CapitalCampaignEndReason.PEACE_ACCEPTED,
                    "The campaign ended when peace was accepted."
            );

            return;
        }

        if (allAttackersDefeated(
                level,
                campaign
        )) {
            finishAttackersDefeated(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital
            );

            return;
        }

        if (isSovereignDead(
                level,
                defendingCapital.getSovereign()
        )) {
            beginRetreat(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    CapitalCampaignEndReason
                            .DEFENDING_SOVEREIGN_DIED,
                    "The campaign ended when the defending sovereign died."
            );

            return;
        }

        if (!hasAnyPlayerInsideCapital(
                level,
                defendingCapital
        )) {
            CapitalCampaignTargetingService
                    .clearCampaignTargets(
                            level,
                            campaign
                    );

            return;
        }

        if (!campaign
                .didDefendingSovereignRefusePeace()
                && allFieldDefendersDefeated(
                level,
                campaign
        )) {
            resolveDefendingSovereignDecision(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital
            );

            if (campaign.getPhase()
                    == CapitalCampaignPhase.RETREATING) {
                return;
            }
        }

        CapitalCampaignTargetingService.applyTargets(
                level,
                campaign,
                defendingCapital
        );
    }

    private static boolean hasAnyPlayerInsideCapital(
            ServerLevel level,
            CapitalRecord defendingCapital
    ) {
        Village village =
                CapitalCampaignEligibilityService
                        .getVillage(
                                level,
                                defendingCapital
                        );

        if (village == null) {
            return false;
        }

        return level.players()
                .stream()
                .filter(player ->
                        player != null
                )
                .filter(ServerPlayer::isAlive)
                .filter(player ->
                        !player.isSpectator()
                )
                .anyMatch(village::isWithinBorder);
    }

    private static void
    resolveDefendingSovereignDecision(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        String defendingName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                defendingCapital
                        );

        String attackingName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                attackingCapital
                        );

        if (level.random.nextInt(100)
                < PEACE_REQUEST_CHANCE_PERCENT) {
            CapitalAgreementDataAccess
                    .removeProposalsBetween(
                            level,
                            attackingCapital
                                    .getCapitalId(),
                            defendingCapital
                                    .getCapitalId()
                    );

            CapitalDiplomacyDataAccess
                    .setDiplomaticState(
                            level,
                            attackingCapital
                                    .getCapitalId(),
                            defendingCapital
                                    .getCapitalId(),
                            CapitalDiplomaticState.PEACE,
                            0L
                    );

            beginRetreat(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    CapitalCampaignEndReason
                            .DEFENDERS_SURRENDERED,
                    "The sovereign of "
                            + defendingName
                            + " sued for peace after the capital's field defenders fell."
            );

            return;
        }

        campaign.markDefendingSovereignRefusedPeace();

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

        String entry =
                "The sovereign of "
                        + defendingName
                        + " refused to sue for peace after the capital's field defenders fell; the campaign from "
                        + attackingName
                        + " continued against the Crown.";

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

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(
                                "The defending sovereign has refused to sue for peace."
                        )
                );
    }

    private static void beginRetreat(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            CapitalCampaignEndReason reason,
            String entry
    ) {
        if (!CapitalCampaignService.beginRetreat(
                level,
                campaign.getCampaignId(),
                reason
        )) {
            return;
        }

        CapitalCampaignTargetingService
                .clearCampaignTargets(
                        level,
                        campaign
                );

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

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(
                                "The campaign is ending and the surviving attackers are retreating."
                        )
                );
    }

    private static void finishAttackersDefeated(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        CapitalCampaignTargetingService
                .clearCampaignTargets(
                        level,
                        campaign
                );

        String entry =
                "The campaign from "
                        + CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                attackingCapital
                        )
                        + " against "
                        + CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                defendingCapital
                        )
                        + " ended when all campaign attackers were defeated.";

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

        CapitalCampaignService.completeCampaign(
                level,
                campaign.getCampaignId()
        );
    }

    private static void invalidateCampaign(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            String message
    ) {
        CapitalCampaignTargetingService
                .clearCampaignTargets(
                        level,
                        campaign
                );

        String entry =
                message == null
                        || message.isBlank()
                        ? "The planned military attack was invalidated."
                        : message;

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

        CapitalCampaignService.completeCampaign(
                level,
                campaign.getCampaignId()
        );
    }

    private static boolean allAttackersDefeated(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (!isKnownDead(
                    level,
                    attackerId
            )) {
                return false;
            }
        }

        return true;
    }

    private static boolean allFieldDefendersDefeated(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        for (UUID defenderId :
                campaign.getDefenderIds()) {
            if (!isKnownDead(
                    level,
                    defenderId
            )) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSovereignDead(
            ServerLevel level,
            UUID sovereignId
    ) {
        return sovereignId == null
                || isKnownDead(
                level,
                sovereignId
        );
    }

    private static boolean isKnownDead(
            ServerLevel level,
            UUID villagerId
    ) {
        if (villagerId == null) {
            return true;
        }

        if (MCAIntegrationBridge
                .isFamilyNodeDeceased(
                        level,
                        villagerId
                )) {
            return true;
        }

        Entity entity =
                MCAIntegrationBridge
                        .findLoadedEntityByUuid(
                                level,
                                villagerId
                        );

        return entity != null
                && (
                !entity.isAlive()
                        || entity.isRemoved()
        );
    }
}