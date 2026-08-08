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
                Component.literal(
                        "War must be begun through a planned Punitive War or War of Deposition. War starts when the campaign deploys inside the target capital."
                )
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
                        ? "because a military attack began."
                        : "because war was declared."
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
                                ? "Military attack begun"
                                : "War declared",
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

        String previousAgreement =
                previousState
                        == CapitalDiplomaticState.PEACE
                        ? ""
                        : " The attack broke the existing "
                        + CapitalDiplomaticAgreementText
                        .stateDisplay(previousState)
                        + ".";

        String entry =
                beganWithAttack
                        ? sourceName
                        + " began a military attack on "
                        + targetName
                        + ", bringing the capitals to war."
                        + previousAgreement
                        : sourceName
                        + " declared war on "
                        + targetName
                        + ".";

        CapitalChronicleService.addEntry(
                level,
                source,
                entry
        );

        CapitalChronicleService.addEntry(
                level,
                target,
                entry
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
                            beganWithAttack
                                    ? "Military Attack"
                                    : "Declaration of War",
                            entry
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