package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalRelationRecord;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalWarCause;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class CapitalDiplomaticWarService {

    private static final int
            WAR_DECLARATION_RELATIONSHIP_CHANGE = -200;

    private CapitalDiplomaticWarService() {
    }

    static int declareWar(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        if (player == null) {
            return 0;
        }

        player.sendSystemMessage(
                Component.translatable("mcacapitals.system.capital_diplomatic_war_service.war_must_be_begun_through_a_planned_punitive_war_or_war_of_deposition")
        );

        return 0;
    }

    static boolean beginCampaignWar(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (level == null
                || campaign == null
                || source == null
                || target == null
                || source.getCapitalId() == null
                || target.getCapitalId() == null
                || source.getCapitalId().equals(
                target.getCapitalId()
        )) {
            return false;
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                source,
                target
        );

        CapitalDiplomaticState currentState =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        if (currentState
                == CapitalDiplomaticState.WAR) {
            return true;
        }

        boolean established = applyWarState(
                level,
                source,
                target,
                true
        );

        if (!established) {
            return false;
        }

        CapitalWarDataAccess.recordGrievance(
                level,
                target.getCapitalId(),
                source.getCapitalId(),
                currentState == CapitalDiplomaticState.NON_AGGRESSION_PACT
                        || currentState == CapitalDiplomaticState.ALLIANCE
                        || currentState == CapitalDiplomaticState.TRUCE
                        ? CapitalWarCause.TREATY_BROKEN
                        : CapitalWarCause.PREVIOUS_AGGRESSION,
                10L
        );

        if (currentState == CapitalDiplomaticState.TRUCE) {
            CapitalWarPenaltyService.applyTruceBreakingPenalty(
                    level,
                    source,
                    target
            );
        }

        if (campaign.getWarCause() == CapitalWarCause.UNJUST) {
            CapitalWarPenaltyService.applyUnjustWarPenalty(
                    level,
                    source,
                    target
            );
        }

        return true;
    }

    private static boolean applyWarState(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            boolean beganWithAttack
    ) {
        CapitalDiplomaticState previousState =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        CapitalAgreementDataAccess
                .removeProposalsBetween(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                );

        CapitalDiplomaticTradeAgreementService.end(
                level,
                source,
                target,
                beganWithAttack
                        ? CapitalDiplomaticTradeAgreementService.TradeAgreementEndReason.MILITARY_ATTACK
                        : CapitalDiplomaticTradeAgreementService.TradeAgreementEndReason.WAR_DECLARED
        );

        CapitalDiplomacyDataAccess
                .setDiplomaticState(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId(),
                        CapitalDiplomaticState.WAR,
                        0L
                );

        CapitalDiplomacyDataAccess
                .adjustRelationship(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId(),
                        WAR_DECLARATION_RELATIONSHIP_CHANGE,
                        beganWithAttack
                                ? "mcacapitals.relationship_reason.military_attack_begun"
                                : "mcacapitals.relationship_reason.war_declared",
                        source.getCapitalId()
                );

        String sourceName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                source
                        );

        String targetName =
                CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                target
                        );

        CapitalChronicleEventId eventId = beganWithAttack
                ? previousState == CapitalDiplomaticState.PEACE
                ? CapitalChronicleEventId.WAR_BEGAN_ATTACK
                : CapitalChronicleEventId.WAR_BEGAN_ATTACK_TREATY_BROKEN
                : CapitalChronicleEventId.WAR_DECLARED;

        if (eventId == CapitalChronicleEventId.WAR_BEGAN_ATTACK_TREATY_BROKEN) {
            CapitalChronicleEntry.Argument previousAgreement = CapitalChronicleService.translatable(
                    "mcacapitals.chronicle.diplomatic_state." + previousState.getSerializedName()
            );
            CapitalChronicleService.addEvent(level, source, eventId, sourceName, targetName, previousAgreement);
            CapitalChronicleService.addEvent(level, target, eventId, sourceName, targetName, previousAgreement);
        } else {
            CapitalChronicleService.addEvent(level, source, eventId, sourceName, targetName);
            CapitalChronicleService.addEvent(level, target, eventId, sourceName, targetName);
        }

        Component notificationMessage = beganWithAttack
                ? previousState == CapitalDiplomaticState.PEACE
                ? Component.translatable(
                        "mcacapitals.diplomacy.war.attack_message",
                        sourceName,
                        targetName
                )
                : Component.translatable(
                        "mcacapitals.diplomacy.war.attack_broke_treaty_message",
                        sourceName,
                        targetName,
                        CapitalDiplomaticAgreementText.stateDisplay(
                                previousState
                        )
                )
                : Component.translatable(
                        "mcacapitals.diplomacy.war.declaration_message",
                        sourceName,
                        targetName
                );

        UUID targetDecisionMaker =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        if (targetDecisionMaker != null
                && !isPlayerInsideCapital(
                level,
                target,
                targetDecisionMaker
        )) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            targetDecisionMaker,
                            Component.translatable(
                                    beganWithAttack
                                            ? "mcacapitals.diplomacy.war.attack_title"
                                            : "mcacapitals.diplomacy.war.declaration_title"
                            ),
                            notificationMessage
                    );
        }

        return true;
    }

    private static boolean isPlayerInsideCapital(
            ServerLevel level,
            CapitalRecord capital,
            UUID playerId
    ) {
        if (level == null
                || capital == null
                || playerId == null) {
            return false;
        }

        ServerPlayer player = level.getServer()
                .getPlayerList()
                .getPlayer(playerId);

        if (player == null
                || player.level() != level
                || !player.isAlive()
                || player.isSpectator()) {
            return false;
        }

        var village = CapitalCampaignEligibilityService.getVillage(
                level,
                capital
        );

        return village != null
                && village.isWithinBorder(player);
    }
}