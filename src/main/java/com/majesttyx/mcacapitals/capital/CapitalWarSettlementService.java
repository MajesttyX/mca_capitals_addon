package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarCause;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarGoal;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CapitalWarSettlementService {

    private static final long TRUCE_TICKS = 48000L;
    private static final long WINNER_RECOVERY_DAYS = 3L;
    private static final long LOSER_RECOVERY_DAYS = 5L;
    private static final long UNJUST_AGGRESSOR_LOSS_RECOVERY_DAYS = 7L;
    private static final int NEGOTIATED_PEACE_SCORE = -50;
    private static final int ATTACKER_VICTORY_SCORE = -60;
    private static final int DEFENDER_VICTORY_SCORE = -70;

    private CapitalWarSettlementService() {
    }

    public static void resolve(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        if (level == null
                || campaign == null
                || campaign.getEndReason()
                == CapitalCampaignEndReason.NONE
                || campaign.getEndReason()
                == CapitalCampaignEndReason.INVALIDATED) {
            return;
        }

        Outcome outcome = outcome(
                campaign.getEndReason()
        );

        if (outcome == Outcome.NEGOTIATED) {
            establishSettlementState(
                    level,
                    campaign,
                    NEGOTIATED_PEACE_SCORE
            );

            CapitalWarDataAccess.setCampaignRecovery(
                    level,
                    campaign.getAttackingCapitalId(),
                    WINNER_RECOVERY_DAYS
            );

            CapitalWarDataAccess.setCampaignRecovery(
                    level,
                    campaign.getDefendingCapitalId(),
                    WINNER_RECOVERY_DAYS
            );

            recordSettlement(
                    level,
                    campaign,
                    null,
                    null,
                    CapitalChronicleEventId.WAR_SETTLEMENT_NEGOTIATED
            );
            return;
        }

        CapitalRecord attacker = CapitalManager.getCapital(
                campaign.getAttackingCapitalId()
        );

        CapitalRecord defender = CapitalManager.getCapital(
                campaign.getDefendingCapitalId()
        );

        if (attacker == null || defender == null) {
            return;
        }

        CapitalRecord winner =
                outcome == Outcome.ATTACKER_VICTORY
                        ? attacker
                        : defender;

        CapitalRecord loser =
                outcome == Outcome.ATTACKER_VICTORY
                        ? defender
                        : attacker;

        boolean attackerWon =
                outcome == Outcome.ATTACKER_VICTORY;

        boolean unjust =
                campaign.getWarCause()
                        == CapitalWarCause.UNJUST;

        boolean reparationsDue =
                campaign.getWarGoal()
                == CapitalWarGoal.PUNITIVE
                        && (attackerWon
                        || unjust
                        && campaign.getEndReason()
                        != CapitalCampaignEndReason
                        .COMMANDER_ORDERED_RETREAT);

        CapitalDiplomaticStorageService.ReparationsResult reparations =
                reparationsDue
                        ? CapitalDiplomaticStorageService
                        .transferReparations(
                                level,
                                loser,
                                winner,
                                campaign.getCampaignId()
                                        .getMostSignificantBits()
                        )
                        : new CapitalDiplomaticStorageService
                        .ReparationsResult(
                                false,
                                List.of()
                        );

        boolean deposition = attackerWon
                && campaign.getWarGoal()
                == CapitalWarGoal.DEPOSITION;

        boolean victoriousClaimantAlreadySeized =
                campaign.getInitiatingPlayerId() != null
                        && defender.isPlayerSovereign()
                        && campaign.getInitiatingPlayerId()
                        .equals(
                                defender.getPlayerSovereignId()
                        );

        boolean depositionStartedNow = false;

        if (deposition
                && defender.getSovereign() != null
                && campaign.getEndReason()
                != CapitalCampaignEndReason
                .DEFENDING_SOVEREIGN_DIED
                && !victoriousClaimantAlreadySeized
                && !CapitalWartimeSuccessionService
                .isInInterregnum(
                        level,
                        defender.getCapitalId()
                )) {
            depositionStartedNow =
                    CapitalWartimeSuccessionService
                            .beginDepositionInterregnum(
                                    level,
                                    defender,
                                    "under the victorious settlement.",
                                    campaign.getInitiatingPlayerId()
                            );
        }

        establishSettlementState(
                level,
                campaign,
                attackerWon
                        ? ATTACKER_VICTORY_SCORE
                        : DEFENDER_VICTORY_SCORE
        );

        if (reparations.successful()) {
            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    winner.getCapitalId(),
                    loser.getCapitalId(),
                    10,
                    "mcacapitals.relationship_reason.valid_reparations_paid",
                    loser.getCapitalId()
            );
        }

        CapitalWarDataAccess.setCampaignRecovery(
                level,
                winner.getCapitalId(),
                WINNER_RECOVERY_DAYS
        );

        CapitalWarDataAccess.setCampaignRecovery(
                level,
                loser.getCapitalId(),
                !attackerWon && unjust
                        ? UNJUST_AGGRESSOR_LOSS_RECOVERY_DAYS
                        : LOSER_RECOVERY_DAYS
        );

        CapitalWarDataAccess.recordGrievance(
                level,
                defender.getCapitalId(),
                attacker.getCapitalId(),
                CapitalWarCause.PREVIOUS_AGGRESSION,
                10L
        );

        if (campaign.getWarCause().isJustified()) {
            CapitalWarDataAccess.consumeGrievance(
                    level,
                    attacker.getCapitalId(),
                    defender.getCapitalId()
            );
        }

        String winnerName = CapitalDiplomaticAgreementText.capitalName(level, winner);
        String loserName = CapitalDiplomaticAgreementText.capitalName(level, loser);
        Object depositionClause = depositionStartedNow
                ? CapitalChronicleService.translatable("mcacapitals.chronicle.settlement.deposition")
                : CapitalChronicleService.literal("");
        Object reparationsClause = reparations.successful()
                ? CapitalChronicleService.translatable("mcacapitals.chronicle.settlement.reparations_transferred")
                : reparationsDue
                ? CapitalChronicleService.translatable("mcacapitals.chronicle.settlement.reparations_unavailable")
                : CapitalChronicleService.literal("");
        Object reparationsItems = reparations.successful()
                ? CapitalChronicleService.itemList(reparations.transferredItems())
                : CapitalChronicleService.literal("");
        Object unjustClause = campaign.getWarCause() == CapitalWarCause.UNJUST
                ? CapitalChronicleService.translatable("mcacapitals.chronicle.settlement.unjust_war")
                : CapitalChronicleService.literal("");

        recordSettlement(
                level,
                campaign,
                attacker,
                defender,
                CapitalChronicleEventId.WAR_SETTLEMENT,
                winnerName,
                loserName,
                CapitalChronicleService.translatable(
                        "mcacapitals.chronicle.war_goal." + campaign.getWarGoal().getSerializedName()
                ),
                CapitalChronicleService.translatable(
                        "mcacapitals.chronicle.war_cause." + campaign.getWarCause().getSerializedName()
                ),
                depositionClause,
                reparationsClause,
                reparationsItems,
                unjustClause
        );
    }

    private static void establishSettlementState(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            int finalScore
    ) {
        CapitalAgreementDataAccess.removeProposalsBetween(
                level,
                campaign.getAttackingCapitalId(),
                campaign.getDefendingCapitalId()
        );

        CapitalDiplomacyDataAccess.setDiplomaticState(
                level,
                campaign.getAttackingCapitalId(),
                campaign.getDefendingCapitalId(),
                CapitalDiplomaticState.TRUCE,
                level.getGameTime() + TRUCE_TICKS
        );

        int current =
                CapitalDiplomacyDataAccess.getRelationshipScore(
                        level,
                        campaign.getAttackingCapitalId(),
                        campaign.getDefendingCapitalId()
                );

        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                campaign.getAttackingCapitalId(),
                campaign.getDefendingCapitalId(),
                finalScore - current,
                "mcacapitals.relationship_reason.post_war_settlement",
                campaign.getAttackingCapitalId()
        );
    }

    private static void recordSettlement(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attacker,
            CapitalRecord defender,
            CapitalChronicleEventId eventId,
            Object... arguments
    ) {
        CapitalRecord resolvedAttacker =
                attacker == null
                        ? CapitalManager.getCapital(
                        campaign.getAttackingCapitalId()
                )
                        : attacker;

        CapitalRecord resolvedDefender =
                defender == null
                        ? CapitalManager.getCapital(
                        campaign.getDefendingCapitalId()
                )
                        : defender;

        Component notification = renderEvent(eventId, arguments);

        if (resolvedAttacker != null) {
            CapitalChronicleService.addEvent(
                    level,
                    resolvedAttacker,
                    eventId,
                    arguments
            );
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    resolvedAttacker,
                    notification
            );
        }

        if (resolvedDefender != null
                && resolvedDefender != resolvedAttacker) {
            CapitalChronicleService.addEvent(
                    level,
                    resolvedDefender,
                    eventId,
                    arguments
            );
            CapitalPlayerNotificationService.notifyPlayersInCapital(
                    level,
                    resolvedDefender,
                    notification
            );
        }
    }

    private static Component renderEvent(
            CapitalChronicleEventId eventId,
            Object... arguments
    ) {
        Object[] rendered = new Object[arguments == null ? 0 : arguments.length];
        for (int index = 0; index < rendered.length; index++) {
            Object argument = arguments[index];
            rendered[index] = argument instanceof CapitalChronicleEntry.Argument semantic
                    ? semantic.component()
                    : Component.literal(argument == null ? "" : String.valueOf(argument));
        }
        return Component.translatable(eventId.chronicleKey(), rendered);
    }

    private static Outcome outcome(
            CapitalCampaignEndReason reason
    ) {
        return switch (reason) {
            case DEFENDING_SOVEREIGN_DIED,
                 DEFENDERS_SURRENDERED ->
                    Outcome.ATTACKER_VICTORY;
            case ATTACKING_SOVEREIGN_DIED,
                 ATTACKERS_DEFEATED,
                 COMMANDER_ORDERED_RETREAT ->
                    Outcome.DEFENDER_VICTORY;
            case PEACE_ACCEPTED ->
                    Outcome.NEGOTIATED;
            case NONE, INVALIDATED ->
                    Outcome.NEGOTIATED;
        };
    }

    private enum Outcome {
        ATTACKER_VICTORY,
        DEFENDER_VICTORY,
        NEGOTIATED
    }
}
