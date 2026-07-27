package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class CapitalDiplomaticAgreementValidation {

    private static final double MAX_AMBASSADOR_DISTANCE_SQR =
            144.0D;

    private CapitalDiplomaticAgreementValidation() {
    }

    static AudienceValidation validateAudience(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        if (player == null || ambassadorId == null) {
            return AudienceValidation.failure(
                    "The Ambassador is unavailable."
            );
        }

        ServerLevel level = player.serverLevel();
        Entity ambassador = level.getEntity(ambassadorId);

        if (ambassador == null || !ambassador.isAlive()) {
            return AudienceValidation.failure(
                    "The Ambassador is unavailable."
            );
        }

        if (player.level() != ambassador.level()
                || player.distanceToSqr(ambassador)
                > MAX_AMBASSADOR_DISTANCE_SQR) {
            return AudienceValidation.failure(
                    "You must remain near the Ambassador."
            );
        }

        CapitalRecord source = null;

        for (CapitalRecord capital :
                CapitalManager.getAllCapitalRecords()) {
            if (capital != null
                    && CapitalAmbassadorService.isAmbassador(
                    level,
                    capital,
                    ambassadorId
            )) {
                source = capital;
                break;
            }
        }

        if (source == null
                || source.getState() != CapitalState.ACTIVE) {
            return AudienceValidation.failure(
                    "This villager is not the Ambassador of an active capital."
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayManageForeignRelations(
                        level,
                        source,
                        player.getUUID()
                )) {
            return AudienceValidation.failure(
                    "Only the player sovereign, or the player Hand serving a villager sovereign, may conduct formal diplomacy."
            );
        }

        if (!CapitalBuildingService.hasAmbassadorBuildings(
                level,
                source
        )) {
            return AudienceValidation.failure(
                    "The capital requires an operational Inn and Storage building before formal diplomacy can be conducted."
            );
        }

        return AudienceValidation.success(source);
    }

    static String validateTarget(
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (target == null
                || target.getCapitalId() == null
                || target.getState() != CapitalState.ACTIVE) {
            return "That capital is not available for formal diplomacy.";
        }

        if (source == null
                || source.getCapitalId() == null) {
            return "The sending capital is unavailable.";
        }

        if (source.getCapitalId().equals(
                target.getCapitalId()
        )) {
            return "A capital cannot conduct foreign diplomacy with itself.";
        }

        return null;
    }

    static String validateProposal(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposalType type,
            CapitalDiplomaticState state,
            int score
    ) {
        if (type == null || state == null) {
            return "That diplomatic proposal is invalid.";
        }

        if (type == DiplomaticProposalType.ROYAL_BETROTHAL) {
            return CapitalRoyalBetrothalService
                    .validateProposal(
                            level,
                            source,
                            target,
                            state,
                            score
                    );
        }

        if (type == DiplomaticProposalType.TRADE_AGREEMENT) {
            return CapitalDiplomaticTradeAgreementService
                    .validateEstablishment(
                            level,
                            source,
                            target
                    );
        }

        if (type == DiplomaticProposalType.NON_AGGRESSION_PACT) {
            if (state != CapitalDiplomaticState.PEACE) {
                return "A Non-Aggression Pact can only be proposed while the capitals are at peace without another agreement.";
            }

            if (score < type.getMinimumRelationship()) {
                return "Relations must be Cordial or better before proposing a Non-Aggression Pact.";
            }
        }

        if (type == DiplomaticProposalType.ALLIANCE) {
            if (state != CapitalDiplomaticState.PEACE
                    && state != CapitalDiplomaticState.NON_AGGRESSION_PACT) {
                return "An Alliance can only be proposed while the capitals are at peace or under a Non-Aggression Pact.";
            }

            if (score < type.getMinimumRelationship()) {
                return "Relations must be Friendly or better before proposing an Alliance.";
            }
        }

        if (type == DiplomaticProposalType.TRUCE
                && state != CapitalDiplomaticState.WAR) {
            return "A Truce can only be proposed while the capitals are at war.";
        }

        return null;
    }

    static PlayerProposalValidation validatePlayerProposal(
            ServerPlayer player,
            UUID proposalId
    ) {
        if (player == null || proposalId == null) {
            return PlayerProposalValidation.failure(
                    "That diplomatic proposal is invalid."
            );
        }

        ServerLevel level = player.serverLevel();

        DiplomaticProposal proposal =
                CapitalAgreementDataAccess.getProposal(
                        level,
                        proposalId
                );

        if (proposal == null) {
            return PlayerProposalValidation.failure(
                    "That diplomatic proposal is no longer pending."
            );
        }

        CapitalRecord source = CapitalManager.getCapital(
                proposal.getSourceCapitalId()
        );

        CapitalRecord target = CapitalManager.getCapital(
                proposal.getTargetCapitalId()
        );

        if (source == null
                || target == null
                || target.getState() != CapitalState.ACTIVE) {
            return PlayerProposalValidation.failure(
                    "One of the capitals connected to this proposal no longer exists."
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        target,
                        player.getUUID()
                )) {
            return PlayerProposalValidation.failure(
                    "Only the player sovereign, or the player Hand serving a villager sovereign, may answer this proposal."
            );
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                source,
                target
        );

        String failure = validateProposal(
                level,
                source,
                target,
                proposal.getType(),
                CapitalDiplomacyDataAccess.getDiplomaticState(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                ),
                CapitalDiplomacyDataAccess.getRelationshipScore(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                )
        );

        if (failure != null) {
            CapitalAgreementDataAccess.removeProposal(
                    level,
                    proposalId
            );

            return PlayerProposalValidation.failure(
                    "The diplomatic situation changed and this proposal is no longer valid."
            );
        }

        return PlayerProposalValidation.success(
                proposal,
                source,
                target
        );
    }

    static UUID getCurrentSovereignId(
            CapitalRecord capital
    ) {
        if (capital == null) {
            return null;
        }

        return capital.getPlayerSovereignId() != null
                ? capital.getPlayerSovereignId()
                : capital.getSovereign();
    }

    record AudienceValidation(
            boolean valid,
            CapitalRecord sourceCapital,
            String failureMessage
    ) {

        static AudienceValidation success(
                CapitalRecord capital
        ) {
            return new AudienceValidation(
                    true,
                    capital,
                    null
            );
        }

        static AudienceValidation failure(
                String message
        ) {
            return new AudienceValidation(
                    false,
                    null,
                    message
            );
        }
    }

    record PlayerProposalValidation(
            boolean valid,
            DiplomaticProposal proposal,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital,
            String failureMessage
    ) {

        static PlayerProposalValidation success(
                DiplomaticProposal proposal,
                CapitalRecord source,
                CapitalRecord target
        ) {
            return new PlayerProposalValidation(
                    true,
                    proposal,
                    source,
                    target,
                    null
            );
        }

        static PlayerProposalValidation failure(
                String message
        ) {
            return new PlayerProposalValidation(
                    false,
                    null,
                    null,
                    null,
                    message
            );
        }
    }
}