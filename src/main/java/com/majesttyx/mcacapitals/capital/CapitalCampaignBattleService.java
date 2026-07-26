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

    private static final long
            FIELD_DEFEAT_PAUSE_TICKS =
            20L * 3L;

    private static final long
            CROWN_RALLY_TICKS =
            20L * 5L;

    private CapitalCampaignBattleService() {
    }

    static void processCampaign(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        CapitalRecord attackingCapital =
                CapitalManager.getCapital(
                        campaign
                                .getAttackingCapitalId()
                );

        CapitalRecord defendingCapital =
                CapitalManager.getCapital(
                        campaign
                                .getDefendingCapitalId()
                );

        if (attackingCapital == null
                || defendingCapital == null) {
            CapitalCampaignTargetingService
                    .clearCampaignTargets(
                            level,
                            campaign
                    );

            CapitalCampaignService
                    .completeCampaign(
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
                finishRetreat(
                        level,
                        campaign,
                        attackingCapital,
                        defendingCapital
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
        if (campaign.isFormationPending()) {
            processFormation(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital
            );

            return;
        }

        CapitalCampaignAssemblyService
                .AssemblyResult assemblyResult =
                CapitalCampaignAssemblyService
                        .tickAssembly(
                                level,
                                campaign,
                                attackingCapital,
                                defendingCapital
                        );

        if (assemblyResult.invalid()) {
            invalidateCampaign(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    assemblyResult.failureMessage()
            );

            return;
        }

        if (!assemblyResult.ready()) {
            return;
        }

        CapitalCampaignDeploymentService
                .DeploymentResult deploymentResult =
                CapitalCampaignDeploymentService
                        .deploy(
                                level,
                                campaign,
                                attackingCapital,
                                defendingCapital,
                                assemblyResult.player(),
                                assemblyResult.attackers()
                        );

        if (deploymentResult.invalid()) {
            invalidateCampaign(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    deploymentResult
                            .failureMessage()
            );

            return;
        }

        if (!deploymentResult.deployed()
                && deploymentResult
                .failureMessage() != null
                && level.getGameTime()
                % 100L == 0L) {
            notifyInitiatingPlayer(
                    level,
                    campaign,
                    deploymentResult
                            .failureMessage()
            );
        }
    }

    private static void processFormation(
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

        if (campaign.getInitiatingPlayerId()
                == null
                || !CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        attackingCapital,
                        campaign
                                .getInitiatingPlayerId()
                )) {
            invalidateCampaign(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    "The player who assembled this campaign no longer has authority to begin the battle."
            );

            return;
        }

        ServerPlayer initiatingPlayer =
                CapitalCampaignAssemblyService
                        .findFormationPlayer(
                                level,
                                campaign,
                                attackingCapital,
                                defendingCapital
                        );

        if (initiatingPlayer == null) {
            return;
        }

        if (level.getGameTime()
                < campaign.getFormationEndsAt()) {
            return;
        }

        if (allAttackersDefeated(
                level,
                campaign
        )) {
            invalidateCampaign(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    "The assembled campaign force was lost before the battle could begin."
            );

            return;
        }

        campaign.setDefenderIds(
                CapitalCampaignDeploymentService
                        .findFieldDefenders(
                                level,
                                defendingCapital,
                                initiatingPlayer.position()
                        )
        );

        if (!CapitalDiplomaticWarService
                .beginCampaignWar(
                        level,
                        attackingCapital,
                        defendingCapital
                )) {
            invalidateCampaign(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    "The attack could not begin because the War state could not be established."
            );

            return;
        }

        campaign.activate(
                level.getGameTime()
        );

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

        initiatingPlayer.sendSystemMessage(
                Component.literal(
                        "The campaign battle has begun: "
                                + campaign
                                .getAttackerIds()
                                .size()
                                + " attackers against "
                                + campaign
                                .getDefenderIds()
                                .size()
                                + " field defenders."
                )
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(
                                "The campaign battle has begun."
                        )
                );
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
                        attackingCapital
                                .getCapitalId(),
                        defendingCapital
                                .getCapitalId()
                )
                != CapitalDiplomaticState.WAR) {
            beginRetreat(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    CapitalCampaignEndReason
                            .PEACE_ACCEPTED,
                    "The campaign ended when peace was accepted."
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

        if (isDefendingSovereignDead(
                level,
                defendingCapital
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

        if (campaign.isCrownRallyPending()) {
            CapitalCampaignTargetingService
                    .clearCampaignTargets(
                            level,
                            campaign
                    );

            if (level.getGameTime()
                    < campaign
                    .getCrownRallyEndsAt()) {
                return;
            }

            campaign.finishCrownRally();

            CapitalCampaignDataAccess
                    .get(level)
                    .setDirty();

            notifyInitiatingPlayer(
                    level,
                    campaign,
                    "The defending Crown has rallied. The campaign battle resumes."
            );

            CapitalPlayerNotificationService
                    .notifyPlayersInCapital(
                            level,
                            defendingCapital,
                            Component.literal(
                                    "The defending Crown has rallied. The battle resumes."
                            )
                    );
        }

        if (!campaign
                .didDefendingSovereignRefusePeace()
                && allFieldDefendersDefeated(
                level,
                campaign
        )) {
            if (!campaign
                    .isFieldDefeatResolutionPending()) {
                beginFieldDefeatResolution(
                        level,
                        campaign,
                        attackingCapital,
                        defendingCapital
                );

                return;
            }

            CapitalCampaignTargetingService
                    .clearCampaignTargets(
                            level,
                            campaign
                    );

            if (level.getGameTime()
                    < campaign
                    .getFieldDefeatResolutionAt()) {
                return;
            }

            campaign.clearFieldDefeatResolution();

            CapitalCampaignDataAccess
                    .get(level)
                    .setDirty();

            resolveDefendingSovereignDecision(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital
            );

            if (campaign.getPhase()
                    == CapitalCampaignPhase.RETREATING
                    || campaign
                    .isCrownRallyPending()) {
                return;
            }
        }

        CapitalCampaignTargetingService
                .applyTargets(
                        level,
                        campaign,
                        defendingCapital
                );
    }

    private static void beginFieldDefeatResolution(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        long now =
                level.getGameTime();

        campaign.beginFieldDefeatResolution(
                now,
                now + FIELD_DEFEAT_PAUSE_TICKS
        );

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

        CapitalCampaignTargetingService
                .clearCampaignTargets(
                        level,
                        campaign
                );

        String defendingName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                defendingCapital
                        );

        String entry =
                "The field defenders of "
                        + defendingName
                        + " were defeated. The battle paused while the defending court prepared its answer.";

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

        notifyInitiatingPlayer(
                level,
                campaign,
                "The field defenders have fallen. The defending court will answer in 3 seconds."
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(
                                "The field defenders have fallen. The defending court is preparing its answer."
                        )
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
                .anyMatch(
                        village::isWithinBorder
                );
    }

    private static void resolveDefendingSovereignDecision(
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

        if (!hasSelectedSovereign(
                defendingCapital
        )) {
            establishPeace(
                    level,
                    attackingCapital,
                    defendingCapital
            );

            beginRetreat(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    CapitalCampaignEndReason
                            .DEFENDERS_SURRENDERED,
                    "With no sovereign to continue the war, the court of "
                            + defendingName
                            + " surrendered after the capital's field defenders fell."
            );

            return;
        }

        if (level.random.nextInt(100)
                < PEACE_REQUEST_CHANCE_PERCENT) {
            establishPeace(
                    level,
                    attackingCapital,
                    defendingCapital
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

        campaign
                .markDefendingSovereignRefusedPeace();

        campaign.beginCrownRally(
                level.getGameTime(),
                level.getGameTime()
                        + CROWN_RALLY_TICKS
        );

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

        String entry =
                "The sovereign of "
                        + defendingName
                        + " refused to sue for peace after the capital's field defenders fell; the campaign from "
                        + attackingName
                        + " continued against the Crown after a brief rally.";

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

        notifyInitiatingPlayer(
                level,
                campaign,
                "The sovereign of "
                        + defendingName
                        + " has refused peace. The defending Crown will rally for 5 seconds before the battle resumes."
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(
                                "The defending sovereign has refused peace. The Crown is rallying for 5 seconds."
                        )
                );
    }

    private static void establishPeace(
            ServerLevel level,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
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
    }

    private static void beginRetreat(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital,
            CapitalCampaignEndReason reason,
            String entry
    ) {
        if (!CapitalCampaignService
                .beginRetreat(
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

        notifyInitiatingPlayer(
                level,
                campaign,
                entry
                        + " Surviving attackers are returning home."
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(
                                entry
                                        + " The surviving attackers are retreating."
                        )
                );
    }

    private static void finishRetreat(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attackingCapital,
            CapitalRecord defendingCapital
    ) {
        int returned =
                campaign
                        .getReturnedAttackerIds()
                        .size();

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
                "The campaign from "
                        + attackingName
                        + " against "
                        + defendingName
                        + " has ended. "
                        + returned
                        + " surviving attackers returned home.";

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

        notifyInitiatingPlayer(
                level,
                campaign,
                entry
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(entry)
                );

        CapitalCampaignService
                .completeCampaign(
                        level,
                        campaign.getCampaignId()
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

        notifyInitiatingPlayer(
                level,
                campaign,
                entry
        );

        CapitalPlayerNotificationService
                .notifyPlayersInCapital(
                        level,
                        defendingCapital,
                        Component.literal(entry)
                );

        CapitalCampaignService
                .completeCampaign(
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

        notifyInitiatingPlayer(
                level,
                campaign,
                entry
        );

        CapitalCampaignService
                .completeCampaign(
                        level,
                        campaign.getCampaignId()
                );
    }

    private static void notifyInitiatingPlayer(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            String message
    ) {
        if (campaign.getInitiatingPlayerId()
                == null
                || message == null
                || message.isBlank()) {
            return;
        }

        ServerPlayer player =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(
                                campaign
                                        .getInitiatingPlayerId()
                        );

        if (player != null) {
            player.sendSystemMessage(
                    Component.literal(message)
            );
        }
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

    private static boolean isDefendingSovereignDead(
            ServerLevel level,
            CapitalRecord defendingCapital
    ) {
        if (defendingCapital == null
                || defendingCapital
                .getPlayerSovereignId()
                != null) {
            return false;
        }

        UUID sovereignId =
                defendingCapital.getSovereign();

        return sovereignId != null
                && isKnownDead(
                level,
                sovereignId
        );
    }

    private static boolean hasSelectedSovereign(
            CapitalRecord capital
    ) {
        return capital != null
                && (
                capital.getSovereign() != null
                        || capital
                        .getPlayerSovereignId()
                        != null
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