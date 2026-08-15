package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class CapitalDiplomaticAgreementValidation {
    private static final double MAX_AMBASSADOR_DISTANCE_SQR = 144.0D;

    private CapitalDiplomaticAgreementValidation() {
    }

    static AudienceValidation validateMenuAudience(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        if (player == null || ambassadorId == null) {
            return AudienceValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.ambassador_unavailable")
            );
        }

        ServerLevel level = player.serverLevel();
        Entity ambassador = level.getEntity(ambassadorId);

        if (ambassador == null || !ambassador.isAlive()) {
            return AudienceValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.ambassador_unavailable")
            );
        }

        if (player.level() != ambassador.level()
                || player.distanceToSqr(ambassador)
                > MAX_AMBASSADOR_DISTANCE_SQR) {
            return AudienceValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.remain_near_ambassador")
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
                    Component.translatable("mcacapitals.diplomacy.validation.not_active_ambassador")
            );
        }

        return AudienceValidation.success(source);
    }

    static AudienceValidation validateAudience(
            ServerPlayer player,
            UUID ambassadorId
    ) {
        AudienceValidation menuAudience =
                validateMenuAudience(
                        player,
                        ambassadorId
                );

        if (!menuAudience.valid()) {
            return menuAudience;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source =
                menuAudience.sourceCapital();

        if (!CapitalDiplomaticAuthorityService
                .mayManageForeignRelations(
                        level,
                        source,
                        player.getUUID()
                )) {
            return AudienceValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.formal_authority")
            );
        }

        if (!CapitalBuildingService
                .hasAmbassadorBuildings(
                        level,
                        source
                )) {
            return AudienceValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.requires_inn_storage")
            );
        }

        return AudienceValidation.success(source);
    }

    static Component validateTarget(
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (target == null
                || target.getCapitalId() == null
                || target.getState() != CapitalState.ACTIVE) {
            return Component.translatable("mcacapitals.diplomacy.validation.target_unavailable");
        }

        if (source == null
                || source.getCapitalId() == null) {
            return Component.translatable("mcacapitals.diplomacy.validation.source_unavailable");
        }

        if (source.getCapitalId().equals(
                target.getCapitalId()
        )) {
            return Component.translatable("mcacapitals.diplomacy.validation.self_target");
        }

        return null;
    }

    static Component validateProposal(
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposalType type,
            CapitalDiplomaticState state,
            int score
    ) {
        if (type == null || state == null) {
            return Component.translatable("mcacapitals.diplomacy.validation.proposal_invalid");
        }

        if (type != DiplomaticProposalType.TRUCE
                && score < type.getMinimumRelationship()) {
            return switch (type) {
                case NON_AGGRESSION_PACT,
                     TRADE_AGREEMENT ->
                        Component.translatable(
                                "mcacapitals.diplomacy.validation.cordial_before_proposal",
                                type.getDisplayComponent()
                        );
                case ROYAL_BETROTHAL ->
                        Component.translatable("mcacapitals.diplomacy.validation.royal_betrothal_relation");
                case ALLIANCE ->
                        Component.translatable("mcacapitals.diplomacy.validation.alliance_relation");
                case TRUCE -> null;
            };
        }

        if (type == DiplomaticProposalType.ROYAL_BETROTHAL) {
            return CapitalRoyalBetrothalService.validateProposal(
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
                return Component.translatable("mcacapitals.diplomacy.validation.nap_state");
            }
        }

        if (type == DiplomaticProposalType.ALLIANCE) {
            if (state != CapitalDiplomaticState.PEACE
                    && state != CapitalDiplomaticState
                    .NON_AGGRESSION_PACT) {
                return Component.translatable("mcacapitals.diplomacy.validation.alliance_state");
            }
        }

        if (type == DiplomaticProposalType.TRUCE
                && state != CapitalDiplomaticState.WAR) {
            return Component.translatable("mcacapitals.diplomacy.validation.truce_state");
        }

        return null;
    }

    static PlayerProposalValidation validatePlayerProposal(
            ServerPlayer player,
            UUID proposalId
    ) {
        if (player == null || proposalId == null) {
            return PlayerProposalValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.proposal_invalid")
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
                    Component.translatable("mcacapitals.diplomacy.validation.proposal_not_pending")
            );
        }

        if (!proposal.isAwaitingPlayerResponse()
                || level.getGameTime()
                < proposal.getAvailableAt()) {
            return PlayerProposalValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.proposal_not_awaiting_answer")
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
                    Component.translatable("mcacapitals.diplomacy.validation.proposal_capital_missing")
            );
        }

        if (!CapitalDiplomaticAuthorityService
                .mayExerciseSovereignAuthority(
                        level,
                        target,
                        player.getUUID()
                )) {
            return PlayerProposalValidation.failure(
                    Component.translatable("mcacapitals.diplomacy.validation.answer_authority")
            );
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                source,
                target
        );

        Component failure = validateProposal(
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
                    Component.translatable("mcacapitals.diplomacy.validation.situation_changed")
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
            Component failureMessage
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
                Component message
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
            Component failureMessage
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
                Component message
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
