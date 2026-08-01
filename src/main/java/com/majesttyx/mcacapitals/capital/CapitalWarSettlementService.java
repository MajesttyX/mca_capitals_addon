package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalCampaignEndReason;
import com.majesttyx.mcacapitals.data.CapitalCampaignRecord;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarCause;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CapitalWarSettlementService {

    private static final long TRUCE_TICKS = 48000L;
    private static final long WINNER_RECOVERY_DAYS = 3L;
    private static final long LOSER_RECOVERY_DAYS = 5L;
    private static final long UNJUST_AGGRESSOR_LOSS_RECOVERY_DAYS = 7L;

    private CapitalWarSettlementService() {
    }

    public static void resolve(
            ServerLevel level,
            CapitalCampaignRecord campaign
    ) {
        if (level == null
                || campaign == null
                || campaign.getEndReason() == CapitalCampaignEndReason.NONE
                || campaign.getEndReason() == CapitalCampaignEndReason.INVALIDATED) {
            return;
        }

        Outcome outcome = outcome(campaign.getEndReason());
        if (outcome == Outcome.NEGOTIATED) {
            establishSettlementState(level, campaign, -50);
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
                    "The war ended by negotiated peace. Neither capital achieved its war goal."
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

        CapitalRecord winner = outcome == Outcome.ATTACKER_VICTORY
                ? attacker
                : defender;
        CapitalRecord loser = outcome == Outcome.ATTACKER_VICTORY
                ? defender
                : attacker;

        boolean attackerWon = outcome == Outcome.ATTACKER_VICTORY;
        boolean unjust = campaign.getWarCause() == CapitalWarCause.UNJUST;
        boolean reparationsDue = campaign.getWarGoal() == CapitalWarGoal.PUNITIVE
                && (attackerWon
                || unjust
                && campaign.getEndReason()
                != CapitalCampaignEndReason.COMMANDER_ORDERED_RETREAT);

        CapitalDiplomaticStorageService.ReparationsResult reparations =
                reparationsDue
                        ? CapitalDiplomaticStorageService.transferReparations(
                        level,
                        loser,
                        winner,
                        campaign.getCampaignId()
                                .getMostSignificantBits()
                )
                        : new CapitalDiplomaticStorageService.ReparationsResult(
                        false,
                        List.of()
                );

        boolean deposition = attackerWon
                && campaign.getWarGoal() == CapitalWarGoal.DEPOSITION;
        boolean victoriousClaimantAlreadySeized =
                campaign.getInitiatingPlayerId() != null
                        && defender.isPlayerSovereign()
                        && campaign.getInitiatingPlayerId().equals(
                        defender.getPlayerSovereignId()
                );
        boolean depositionStartedNow = false;

        if (deposition
                && defender.getSovereign() != null
                && campaign.getEndReason()
                != CapitalCampaignEndReason.DEFENDING_SOVEREIGN_DIED
                && !victoriousClaimantAlreadySeized
                && !CapitalWartimeSuccessionService.isInInterregnum(
                level,
                defender.getCapitalId()
        )) {
            depositionStartedNow =
                    CapitalWartimeSuccessionService.beginDepositionInterregnum(
                            level,
                            defender,
                            "under the victorious settlement.",
                            campaign.getInitiatingPlayerId()
                    );
        }

        establishSettlementState(
                level,
                campaign,
                attackerWon ? -60 : -70
        );

        if (reparations.successful()) {
            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    winner.getCapitalId(),
                    loser.getCapitalId(),
                    10,
                    "Valid reparations paid",
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

        String settlement = buildSettlementText(
                level,
                campaign,
                winner,
                loser,
                reparations,
                reparationsDue,
                depositionStartedNow
        );
        recordSettlement(
                level,
                campaign,
                attacker,
                defender,
                settlement
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

        int current = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                campaign.getAttackingCapitalId(),
                campaign.getDefendingCapitalId()
        );
        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                campaign.getAttackingCapitalId(),
                campaign.getDefendingCapitalId(),
                finalScore - current,
                "Post-war settlement",
                campaign.getAttackingCapitalId()
        );
    }

    private static String buildSettlementText(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord winner,
            CapitalRecord loser,
            CapitalDiplomaticStorageService.ReparationsResult reparations,
            boolean reparationsDue,
            boolean depositionStartedNow
    ) {
        String winnerName = CapitalDiplomaticAgreementText.capitalName(
                level,
                winner
        );
        String loserName = CapitalDiplomaticAgreementText.capitalName(
                level,
                loser
        );
        StringBuilder text = new StringBuilder()
                .append(winnerName)
                .append(" defeated ")
                .append(loserName)
                .append(" in a ")
                .append(campaign.getWarGoal().getDisplayName())
                .append(" fought for ")
                .append(campaign.getWarCause().getDisplayName())
                .append(". A two-day truce began.");

        if (depositionStartedNow) {
            text.append(" The defending sovereign was removed and an interregnum began.");
        }

        if (reparations.successful()) {
            text.append(" Reparations transferred: ")
                    .append(describeItems(reparations.transferredItems()))
                    .append(".");
        } else if (reparationsDue) {
            text.append(" No eligible goods were available for reparations.");
        }

        if (campaign.getWarCause() == CapitalWarCause.UNJUST) {
            text.append(" The aggressor remains recorded as having begun an unjust war.");
        }

        return text.toString();
    }

    private static String describeItems(List<ItemStack> items) {
        List<String> descriptions = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) {
                descriptions.add(
                        stack.getCount()
                                + " "
                                + stack.getHoverName().getString()
                );
            }
        }
        return descriptions.isEmpty()
                ? "none"
                : String.join(", ", descriptions);
    }

    private static void recordSettlement(
            ServerLevel level,
            CapitalCampaignRecord campaign,
            CapitalRecord attacker,
            CapitalRecord defender,
            String settlement
    ) {
        CapitalRecord resolvedAttacker = attacker == null
                ? CapitalManager.getCapital(campaign.getAttackingCapitalId())
                : attacker;
        CapitalRecord resolvedDefender = defender == null
                ? CapitalManager.getCapital(campaign.getDefendingCapitalId())
                : defender;

        if (resolvedAttacker != null) {
            CapitalChronicleService.addEntry(
                    level,
                    resolvedAttacker,
                    settlement
            );
        }
        if (resolvedDefender != null
                && resolvedDefender != resolvedAttacker) {
            CapitalChronicleService.addEntry(
                    level,
                    resolvedDefender,
                    settlement
            );
        }
    }

    private static Outcome outcome(CapitalCampaignEndReason reason) {
        return switch (reason) {
            case DEFENDING_SOVEREIGN_DIED,
                 DEFENDERS_SURRENDERED -> Outcome.ATTACKER_VICTORY;
            case ATTACKING_SOVEREIGN_DIED,
                 ATTACKERS_DEFEATED,
                 COMMANDER_ORDERED_RETREAT -> Outcome.DEFENDER_VICTORY;
            case PEACE_ACCEPTED -> Outcome.NEGOTIATED;
            case NONE, INVALIDATED -> Outcome.NEGOTIATED;
        };
    }

    private enum Outcome {
        ATTACKER_VICTORY,
        DEFENDER_VICTORY,
        NEGOTIATED
    }
}