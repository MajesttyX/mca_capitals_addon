package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
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
        if (player == null
                || ambassadorId == null
                || targetCapitalId == null
                || type == null) {
            return 0;
        }

        CapitalDiplomaticAgreementValidation
                .AudienceValidation audience =
                CapitalDiplomaticAgreementValidation
                        .validateAudience(
                                player,
                                ambassadorId
                        );

        if (!audience.valid()) {
            player.sendSystemMessage(
                    Component.literal(
                            audience.failureMessage()
                    )
            );

            return 0;
        }

        ServerLevel level = player.serverLevel();
        CapitalRecord source = audience.sourceCapital();

        CapitalRecord target =
                CapitalManager.getCapital(
                        targetCapitalId
                );

        String targetFailure =
                CapitalDiplomaticAgreementValidation
                        .validateTarget(
                                source,
                                target
                        );

        if (targetFailure != null) {
            player.sendSystemMessage(
                    Component.literal(targetFailure)
            );

            return 0;
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                source,
                target
        );

        CapitalDiplomaticState state =
                CapitalDiplomacyDataAccess
                        .getDiplomaticState(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        int score =
                CapitalDiplomacyDataAccess
                        .getRelationshipScore(
                                level,
                                source.getCapitalId(),
                                target.getCapitalId()
                        );

        String proposalFailure =
                CapitalDiplomaticAgreementValidation
                        .validateProposal(
                                level,
                                source,
                                target,
                                type,
                                state,
                                score
                        );

        if (proposalFailure != null) {
            player.sendSystemMessage(
                    Component.literal(proposalFailure)
            );

            return 0;
        }

        if (CapitalAgreementDataAccess
                .findPendingBetween(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                ) != null) {
            player.sendSystemMessage(
                    Component.literal(
                            "A diplomatic proposal is already pending between these capitals."
                    )
            );

            return 0;
        }

        UUID targetSovereignId =
                CapitalDiplomaticAgreementValidation
                        .getCurrentSovereignId(
                                target
                        );

        if (targetSovereignId == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "That capital currently has no sovereign to answer the proposal."
                    )
            );

            return 0;
        }

        DiplomaticProposal proposal;

        if (type == DiplomaticProposalType.ROYAL_BETROTHAL) {
            CapitalRoyalBetrothalService.Match match =
                    CapitalRoyalBetrothalService
                            .findMatch(
                                    level,
                                    source,
                                    target
                            );

            if (match == null) {
                player.sendSystemMessage(
                        Component.literal(
                                "These capitals no longer have an eligible royal match."
                        )
                );

                return 0;
            }

            proposal = new DiplomaticProposal(
                    UUID.randomUUID(),
                    source.getCapitalId(),
                    target.getCapitalId(),
                    player.getUUID(),
                    targetSovereignId,
                    null,
                    type,
                    level.getGameTime(),
                    match.sourceRoyalId(),
                    match.targetRoyalId(),
                    match.relocatingRoyalId(),
                    match.destinationCapitalId()
            );
        } else {
            proposal = new DiplomaticProposal(
                    UUID.randomUUID(),
                    source.getCapitalId(),
                    target.getCapitalId(),
                    player.getUUID(),
                    targetSovereignId,
                    type,
                    level.getGameTime()
            );
        }

        CapitalAgreementDataAccess.addProposal(
                level,
                proposal
        );

        CapitalChronicleService.addEntry(
                level,
                source,
                "A "
                        + type.getDisplayName()
                        + " was proposed to "
                        + CapitalDiplomaticAgreementText
                        .capitalName(
                                level,
                                target
                        )
                        + "."
        );

        UUID targetPlayerId =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        if (targetPlayerId != null) {
            sendProposalToPlayer(
                    level,
                    proposal,
                    source,
                    target,
                    targetPlayerId
            );

            player.sendSystemMessage(
                    Component.literal(
                            "The "
                                    + type.getDisplayName()
                                    + " proposal has been delivered to "
                                    + CapitalDiplomaticAgreementText
                                    .capitalName(
                                            level,
                                            target
                                    )
                                    + "."
                    )
            );

            return 1;
        }

        CapitalDiplomaticProposalResolutionService
                .resolveNpcProposal(
                        level,
                        proposal
                );

        return 1;
    }

    static List<DiplomaticProposal> getPendingForPlayer(
            ServerLevel level,
            UUID playerId
    ) {
        if (level == null || playerId == null) {
            return List.of();
        }

        List<DiplomaticProposal> result =
                new ArrayList<>();

        for (DiplomaticProposal proposal :
                CapitalAgreementDataAccess
                        .getProposalsSnapshot(level)
                        .values()) {
            CapitalRecord target =
                    CapitalManager.getCapital(
                            proposal.getTargetCapitalId()
                    );

            if (target != null
                    && CapitalDiplomaticAuthorityService
                    .mayExerciseSovereignAuthority(
                            level,
                            target,
                            playerId
                    )) {
                result.add(proposal);
            }
        }

        result.sort(
                Comparator.comparingLong(
                        DiplomaticProposal::getCreatedAt
                )
        );

        return List.copyOf(result);
    }

    static void processPendingProposal(
            ServerLevel level,
            DiplomaticProposal proposal
    ) {
        if (level == null || proposal == null) {
            return;
        }

        CapitalRecord source =
                CapitalManager.getCapital(
                        proposal.getSourceCapitalId()
                );

        CapitalRecord target =
                CapitalManager.getCapital(
                        proposal.getTargetCapitalId()
                );

        if (source == null || target == null) {
            CapitalAgreementDataAccess.removeProposal(
                    level,
                    proposal.getProposalId()
            );

            if (source != null) {
                CapitalDiplomaticProposalResolutionService
                        .notifySource(
                                level,
                                proposal,
                                source,
                                "Proposal Undeliverable",
                                "The proposed "
                                        + proposal.getType()
                                        .getDisplayName()
                                        + " could not be delivered because the receiving capital no longer exists."
                        );
            }

            return;
        }

        UUID targetPlayerId =
                CapitalDiplomaticAuthorityService
                        .getPlayerDecisionMaker(
                                level,
                                target
                        );

        if (targetPlayerId != null) {
            if (!proposal.wasNotifiedTo(
                    targetPlayerId
            )) {
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
            CapitalDiplomaticProposalResolutionService
                    .resolveNpcProposal(
                            level,
                            proposal
                    );
        }
    }

    static void sendProposalToPlayer(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target,
            UUID playerId
    ) {
        CapitalDiplomaticAgreementCorrespondenceService
                .sendProposalLetter(
                        level,
                        playerId,
                        proposal,
                        source,
                        target
                );

        proposal.setNotifiedPlayerId(playerId);

        CapitalAgreementDataAccess
                .get(level)
                .setDirty();
    }
}