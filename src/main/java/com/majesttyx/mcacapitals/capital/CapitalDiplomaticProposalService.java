package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import com.majesttyx.mcacapitals.data.DiplomaticProposalStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class CapitalDiplomaticProposalService {

    private CapitalDiplomaticProposalService() {
    }

    static int propose(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            DiplomaticProposalType type
    ) {
        if (type == DiplomaticProposalType.ROYAL_BETROTHAL) {
            return CapitalRoyalBetrothalService.openSourceRoyalSelection(
                    player,
                    ambassadorId,
                    targetCapitalId
            );
        }

        ProposalContext context = validateProposal(
                player,
                ambassadorId,
                targetCapitalId,
                type
        );
        if (!context.valid()) {
            sendFailure(player, context.failureMessage());
            return 0;
        }

        long createdAt = context.level().getGameTime();
        DiplomaticProposal proposal = new DiplomaticProposal(
                UUID.randomUUID(),
                context.source().getCapitalId(),
                context.target().getCapitalId(),
                player.getUUID(),
                context.targetSovereignId(),
                null,
                type,
                createdAt,
                CapitalDiplomaticDelayService.schedule(context.level()),
                null,
                null,
                null,
                null
        );

        return dispatch(player, context, proposal);
    }

    static int proposeRoyalBetrothal(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            CapitalRoyalBetrothalService.Match match
    ) {
        ProposalContext context = validateProposal(
                player,
                ambassadorId,
                targetCapitalId,
                DiplomaticProposalType.ROYAL_BETROTHAL
        );
        if (!context.valid()) {
            sendFailure(player, context.failureMessage());
            return 0;
        }

        if (match == null) {
            sendFailure(player, "That royal match is invalid.");
            return 0;
        }

        long createdAt = context.level().getGameTime();
        DiplomaticProposal proposal = new DiplomaticProposal(
                UUID.randomUUID(),
                context.source().getCapitalId(),
                context.target().getCapitalId(),
                player.getUUID(),
                context.targetSovereignId(),
                null,
                DiplomaticProposalType.ROYAL_BETROTHAL,
                createdAt,
                CapitalDiplomaticDelayService.schedule(context.level()),
                match.sourceRoyalId(),
                match.targetRoyalId(),
                match.relocatingRoyalId(),
                match.destinationCapitalId()
        );

        return dispatch(player, context, proposal);
    }

    private static ProposalContext validateProposal(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            DiplomaticProposalType type
    ) {
        if (player == null || ambassadorId == null || targetCapitalId == null || type == null) {
            return ProposalContext.failure("That diplomatic proposal is invalid.");
        }

        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);
        if (!audience.valid()) {
            return ProposalContext.failure(audience.failureMessage());
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
        CapitalRecord target = CapitalManager.getCapital(targetCapitalId);
        String targetFailure = CapitalDiplomaticAgreementValidation.validateTarget(source, target);
        if (targetFailure != null) {
            return ProposalContext.failure(targetFailure);
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(level, source, target);
        CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        String proposalFailure = CapitalDiplomaticAgreementValidation.validateProposal(
                level,
                source,
                target,
                type,
                state,
                score
        );
        if (proposalFailure != null) {
            return ProposalContext.failure(proposalFailure);
        }

        if (CapitalAgreementDataAccess.findPendingBetween(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        ) != null) {
            return ProposalContext.failure(
                    "A diplomatic proposal is already pending between these capitals."
            );
        }

        UUID targetSovereignId = CapitalDiplomaticAgreementValidation.getCurrentSovereignId(target);
        if (targetSovereignId == null) {
            return ProposalContext.failure(
                    "That capital currently has no sovereign to answer the proposal."
            );
        }

        return ProposalContext.success(level, source, target, targetSovereignId, type);
    }

    private static int dispatch(
            ServerPlayer player,
            ProposalContext context,
            DiplomaticProposal proposal
    ) {
        CapitalAgreementDataAccess.addProposal(context.level(), proposal);
        CapitalChronicleService.addEntry(
                context.level(),
                context.source(),
                CapitalDiplomaticAgreementText.capitalizedWithIndefiniteArticle(
                        context.type().getDisplayName()
                )
                        + " was dispatched to "
                        + CapitalDiplomaticAgreementText.capitalName(
                        context.level(),
                        context.target()
                )
                        + "."
        );

        player.sendSystemMessage(Component.literal(
                CapitalDiplomaticDelayService.dispatchMessage()
        ));
        return 1;
    }

    static List<DiplomaticProposal> getPendingForPlayer(
            ServerLevel level,
            UUID playerId
    ) {
        if (level == null || playerId == null) {
            return List.of();
        }

        List<DiplomaticProposal> result = new ArrayList<>();
        long now = level.getGameTime();
        for (DiplomaticProposal proposal :
                CapitalAgreementDataAccess.getProposalsSnapshot(level).values()) {
            if (proposal == null
                    || now < proposal.getAvailableAt()
                    || !proposal.isAwaitingPlayerResponse()) {
                continue;
            }

            CapitalRecord target = CapitalManager.getCapital(proposal.getTargetCapitalId());
            if (target != null
                    && CapitalDiplomaticAuthorityService.mayExerciseSovereignAuthority(
                    level,
                    target,
                    playerId
            )) {
                result.add(proposal);
            }
        }

        result.sort(Comparator.comparingLong(DiplomaticProposal::getCreatedAt));
        return List.copyOf(result);
    }

    static void processPendingProposal(
            ServerLevel level,
            DiplomaticProposal proposal
    ) {
        if (level == null || proposal == null) {
            return;
        }

        DiplomaticProposalStatus status = proposal.getStatus();
        if (status == DiplomaticProposalStatus.ACCEPTED_RESPONSE_IN_TRANSIT
                || status == DiplomaticProposalStatus.REJECTED_RESPONSE_IN_TRANSIT) {
            if (level.getGameTime() >= proposal.getAvailableAt()) {
                CapitalDiplomaticProposalResolutionService.resolveQueuedPlayerResponse(
                        level,
                        proposal
                );
            }
            return;
        }

        if (status == DiplomaticProposalStatus.AWAITING_PLAYER_RESPONSE
                || level.getGameTime() < proposal.getAvailableAt()) {
            return;
        }

        CapitalRecord source = CapitalManager.getCapital(proposal.getSourceCapitalId());
        CapitalRecord target = CapitalManager.getCapital(proposal.getTargetCapitalId());
        if (source == null || target == null) {
            CapitalAgreementDataAccess.removeProposal(level, proposal.getProposalId());
            if (source != null) {
                CapitalDiplomaticProposalResolutionService.notifySource(
                        level,
                        proposal,
                        source,
                        "Proposal Undeliverable",
                        "The proposed " + proposal.getType().getDisplayName()
                                + " could not be delivered because the receiving capital no longer exists."
                );
            }
            return;
        }

        UUID targetPlayerId = CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(level, target);
        if (targetPlayerId != null) {
            if (!proposal.wasNotifiedTo(targetPlayerId)) {
                sendProposalToPlayer(level, proposal, source, target, targetPlayerId);
            }
            return;
        }

        if (target.getSovereign() != null) {
            CapitalDiplomaticProposalResolutionService.resolveNpcProposal(level, proposal);
        }
    }

    static void sendProposalToPlayer(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target,
            UUID playerId
    ) {
        CapitalDiplomaticAgreementCorrespondenceService.sendProposalLetter(
                level,
                playerId,
                proposal,
                source,
                target
        );
        proposal.setNotifiedPlayerId(playerId);
        proposal.setStatus(DiplomaticProposalStatus.AWAITING_PLAYER_RESPONSE);
        CapitalAgreementDataAccess.get(level).setDirty();
    }

    private static void sendFailure(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private record ProposalContext(
            boolean valid,
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            UUID targetSovereignId,
            DiplomaticProposalType type,
            String failureMessage
    ) {
        private static ProposalContext success(
                ServerLevel level,
                CapitalRecord source,
                CapitalRecord target,
                UUID targetSovereignId,
                DiplomaticProposalType type
        ) {
            return new ProposalContext(
                    true,
                    level,
                    source,
                    target,
                    targetSovereignId,
                    type,
                    null
            );
        }

        private static ProposalContext failure(String message) {
            return new ProposalContext(false, null, null, null, null, null, message);
        }
    }
}