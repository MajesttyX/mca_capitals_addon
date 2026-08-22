package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignPhase;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
import forge.net.conczin.mca.server.world.data.Village;
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
                    Component.translatable("mcacapitals.system.campaign.assembler_lost_authority")
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
                    Component.translatable("mcacapitals.system.campaign.force_lost_before_battle")
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
                        campaign,
                        attackingCapital,
                        defendingCapital
                )) {
            invalidateCampaign(
                    level,
                    campaign,
                    attackingCapital,
                    defendingCapital,
                    Component.translatable("mcacapitals.system.campaign.war_state_not_established")
            );

            return;
        }

        campaign.activate(
                level.getGameTime()
        );

        CapitalCampaignDataAccess
                .get(level)
                .setDirty();

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
                    CapitalChronicleEventId.CAMPAIGN_PEACE_ACCEPTED,
                    CapitalDiplomaticAgreementText.capitalName(level, attackingCapital),
                    CapitalDiplomaticAgreementText.capitalName(level, defendingCapital)
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
                    CapitalChronicleEventId.CAMPAIGN_SOVEREIGN_DEATH_RETREAT,
                    CapitalDiplomaticAgreementText.capitalName(level, attackingCapital),
                    CapitalDiplomaticAgreementText.capitalName(level, defendingCapital)
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

        CapitalChronicleService.addEvent(
                level,
                attackingCapital,
                CapitalChronicleEventId.FIELD_DEFENDERS_DEFEATED,
                defendingName
        );

        CapitalChronicleService.addEvent(
                level,
                defendingCapital,
                CapitalChronicleEventId.FIELD_DEFENDERS_DEFEATED,
                defendingName
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
                    CapitalChronicleEventId.CAMPAIGN_DEFENDERS_SURRENDERED_NO_SOVEREIGN,
                    defendingName
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
                    CapitalChronicleEventId.CAMPAIGN_DEFENDERS_SUED_FOR_PEACE,
                    defendingName
            );

            if (campaign.getPhase()
                    == CapitalCampaignPhase.RETREATING) {
                beginVictoriousDepositionIfApplicable(
                        level,
                        campaign,
                        defendingCapital,
                        "after the defending sovereign sued for peace."
                );
            }

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

        CapitalChronicleService.addEvent(
                level,
                attackingCapital,
                CapitalChronicleEventId.DEFENDING_SOVEREIGN_REFUSED_PEACE,
                defendingName,
                attackingName
        );

        CapitalChronicleService.addEvent(
                level,
                defendingCapital,
                CapitalChronicleEventId.DEFENDING_SOVEREIGN_REFUSED_PEACE,
                defendingName,
                attackingName
        );

    }

    private static void beginVictoriousDepositionIfApplicable(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord defendingCapital,
            String reason
    ) {
        if (campaign.getWarGoal()
                != com.majesttyx.mcacapitals.data.CapitalWarGoal.DEPOSITION
                || defendingCapital.getSovereign() == null) {
            return;
        }

        CapitalWartimeSuccessionService.beginDepositionInterregnum(
                level,
                defendingCapital,
                reason,
                campaign.getInitiatingPlayerId()
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
            CapitalChronicleEventId chronicleEventId,
            Object... chronicleArguments
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

        CapitalChronicleService.addEvent(
                level,
                attackingCapital,
                chronicleEventId,
                chronicleArguments
        );

        CapitalChronicleService.addEvent(
                level,
                defendingCapital,
                chronicleEventId,
                chronicleArguments
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

        CapitalChronicleService.addEvent(
                level,
                attackingCapital,
                CapitalChronicleEventId.CAMPAIGN_ENDED_RETURNED,
                attackingName,
                defendingName,
                returned
        );

        CapitalChronicleService.addEvent(
                level,
                defendingCapital,
                CapitalChronicleEventId.CAMPAIGN_ENDED_RETURNED,
                attackingName,
                defendingName,
                returned
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

        String attackingName = CapitalDiplomaticAgreementText.capitalName(level, attackingCapital);
        String defendingName = CapitalDiplomaticAgreementText.capitalName(level, defendingCapital);

        CapitalChronicleService.addEvent(
                level,
                attackingCapital,
                CapitalChronicleEventId.CAMPAIGN_ATTACKERS_DEFEATED,
                attackingName,
                defendingName
        );

        CapitalChronicleService.addEvent(
                level,
                defendingCapital,
                CapitalChronicleEventId.CAMPAIGN_ATTACKERS_DEFEATED,
                attackingName,
                defendingName
        );

        campaign.finishWithoutRetreat(
                CapitalCampaignEndReason.ATTACKERS_DEFEATED
        );
        CapitalCampaignDataAccess.get(level).setDirty();

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
            Component message
    ) {
        CapitalCampaignTargetingService
                .clearCampaignTargets(
                        level,
                        campaign
                );

        Component entry =
                message == null
                        ? Component.translatable(
                                "mcacapitals.system.campaign.attack_invalidated"
                        )
                        : message;

        CapitalChronicleService.addEventWithoutHerald(
                level,
                attackingCapital,
                CapitalChronicleEventId.CAMPAIGN_PLANNED_ATTACK_INVALIDATED,
                CapitalDiplomaticAgreementText.capitalName(level, attackingCapital),
                CapitalDiplomaticAgreementText.capitalName(level, defendingCapital)
        );

        CapitalChronicleService.addEventWithoutHerald(
                level,
                defendingCapital,
                CapitalChronicleEventId.CAMPAIGN_PLANNED_ATTACK_INVALIDATED,
                CapitalDiplomaticAgreementText.capitalName(level, attackingCapital),
                CapitalDiplomaticAgreementText.capitalName(level, defendingCapital)
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
            Component message
    ) {
        if (campaign.getInitiatingPlayerId()
                == null
                || message == null) {
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
            player.sendSystemMessage(message);
        }
    }

    private static boolean allAttackersDefeated(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        for (UUID attackerId :
                campaign.getAttackerIds()) {
            if (!CapitalCampaignCasualtyService
                    .isAttackerDefeated(
                            level,
                            campaign,
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