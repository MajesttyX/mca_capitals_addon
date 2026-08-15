package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalStatus;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
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

        ProposalContext context = validateProposal(player, ambassadorId, targetCapitalId, type);
        if (!context.valid()) {
            if (player != null) {
                player.sendSystemMessage(context.failureMessage());
            }
            return 0;
        }

        DiplomaticProposal proposal = new DiplomaticProposal(
                UUID.randomUUID(),
                context.source().getCapitalId(),
                context.target().getCapitalId(),
                player.getUUID(),
                CapitalDiplomaticAgreementValidation.getCurrentSovereignId(context.target()),
                type,
                context.level().getGameTime()
        );
        proposal.setAvailableAt(CapitalDiplomaticDelayService.schedule(context.level()));
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
            if (player != null) {
                player.sendSystemMessage(context.failureMessage());
            }
            return 0;
        }
        if (match == null) {
            player.sendSystemMessage(Component.translatable(
                    "mcacapitals.diplomacy.royal_betrothal.validation.match_invalid"
            ));
            return 0;
        }

        DiplomaticProposal proposal = new DiplomaticProposal(
                UUID.randomUUID(),
                context.source().getCapitalId(),
                context.target().getCapitalId(),
                player.getUUID(),
                CapitalDiplomaticAgreementValidation.getCurrentSovereignId(context.target()),
                null,
                DiplomaticProposalType.ROYAL_BETROTHAL,
                context.level().getGameTime(),
                match.sourceRoyalId(),
                match.targetRoyalId(),
                match.relocatingRoyalId(),
                match.destinationCapitalId()
        );
        proposal.setAvailableAt(CapitalDiplomaticDelayService.schedule(context.level()));
        return dispatch(player, context, proposal);
    }

    private static ProposalContext validateProposal(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            DiplomaticProposalType type
    ) {
        if (player == null || ambassadorId == null || targetCapitalId == null || type == null) {
            return ProposalContext.failure(Component.translatable(
                    "mcacapitals.diplomacy.validation.proposal_invalid"
            ));
        }
        CapitalDiplomaticAgreementValidation.AudienceValidation audience =
                CapitalDiplomaticAgreementValidation.validateAudience(player, ambassadorId);
        if (!audience.valid()) {
            return ProposalContext.failure(audience.failureMessage());
        }
        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();
        CapitalRecord target = CapitalManager.getCapital(targetCapitalId);
        Component targetFailure = CapitalDiplomaticAgreementValidation.validateTarget(source, target);
        if (targetFailure != null) {
            return ProposalContext.failure(targetFailure);
        }
        if (CapitalAgreementDataAccess.findPendingBetween(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        ) != null) {
            return ProposalContext.failure(Component.translatable(
                    "mcacapitals.diplomacy.validation.pending_between_capitals"
            ));
        }
        if (CapitalDiplomaticAgreementValidation.getCurrentSovereignId(target) == null) {
            return ProposalContext.failure(Component.translatable(
                    "mcacapitals.diplomacy.validation.no_sovereign_to_answer"
            ));
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(level, source, target);
        int score = CapitalDiplomacyDataAccess.getRelationshipScore(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        CapitalDiplomaticState state = CapitalDiplomacyDataAccess.getDiplomaticState(
                level,
                source.getCapitalId(),
                target.getCapitalId()
        );
        Component failure = CapitalDiplomaticAgreementValidation.validateProposal(
                level,
                source,
                target,
                type,
                state,
                score
        );
        return failure == null
                ? ProposalContext.success(level, source, target, type)
                : ProposalContext.failure(failure);
    }

    private static int dispatch(
            ServerPlayer player,
            ProposalContext context,
            DiplomaticProposal proposal
    ) {
        CapitalAgreementDataAccess.addProposal(context.level(), proposal);
        CapitalChronicleService.addEvent(
                context.level(),
                context.source(),
                CapitalChronicleEventId.DIPLOMATIC_AGREEMENT_DISPATCHED,
                CapitalChronicleService.translatable(
                        "mcacapitals.chronicle.agreement_type." + context.type().getSerializedName()
                ),
                CapitalDiplomaticAgreementText.capitalName(
                        context.level(),
                        context.target()
                )
        );
        player.sendSystemMessage(CapitalDiplomaticDelayService.dispatchMessage());
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

    static void processPendingProposal(ServerLevel level, DiplomaticProposal proposal) {
        if (level == null || proposal == null) {
            return;
        }
        if (!CapitalDiplomaticDelayService.isReady(level, proposal.getAvailableAt())) {
            return;
        }

        DiplomaticProposalStatus status = proposal.getStatus();
        if (status == DiplomaticProposalStatus.ACCEPTED_RESPONSE_IN_TRANSIT
                || status == DiplomaticProposalStatus.REJECTED_RESPONSE_IN_TRANSIT) {
            CapitalDiplomaticProposalResolutionService.resolveQueuedPlayerResponse(level, proposal);
            return;
        }
        if (status == DiplomaticProposalStatus.AWAITING_PLAYER_RESPONSE) {
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
                        Component.translatable("mcacapitals.diplomacy.correspondence.undeliverable_title"),
                        Component.translatable(
                                "mcacapitals.diplomacy.correspondence.undeliverable_message",
                                proposal.getType().getDisplayComponent()
                        )
                );
            }
            return;
        }

        UUID targetPlayerId = CapitalDiplomaticAuthorityService.getPlayerDecisionMaker(
                level,
                target
        );
        if (targetPlayerId != null) {
            if (!proposal.wasNotifiedTo(targetPlayerId)
                    || !proposal.isAwaitingPlayerResponse()) {
                sendProposalToPlayer(
                        level,
                        proposal,
                        source,
                        target,
                        targetPlayerId
                );
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

    private record ProposalContext(
            boolean valid,
            ServerLevel level,
            CapitalRecord source,
            CapitalRecord target,
            DiplomaticProposalType type,
            Component failureMessage
    ) {
        static ProposalContext success(
                ServerLevel level,
                CapitalRecord source,
                CapitalRecord target,
                DiplomaticProposalType type
        ) {
            return new ProposalContext(true, level, source, target, type, null);
        }

        static ProposalContext failure(Component message) {
            return new ProposalContext(false, null, null, null, null, message);
        }
    }
}
