package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import com.majesttyx.mcacapitals.data.DiplomaticProposalStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class CapitalDiplomaticProposalResolutionService {

    private CapitalDiplomaticProposalResolutionService() {
    }

    static int accept(
            ServerPlayer player,
            UUID proposalId
    ) {
        CapitalDiplomaticAgreementValidation.PlayerProposalValidation validation =
                CapitalDiplomaticAgreementValidation.validatePlayerProposal(
                        player,
                        proposalId
                );

        if (!validation.valid()) {
            if (player != null) {
                player.sendSystemMessage(
                        Component.literal(
                                validation.failureMessage()
                        )
                );
            }

            return 0;
        }

        queuePlayerResponse(
                player.serverLevel(),
                validation.proposal(),
                DiplomaticProposalStatus.ACCEPTED_RESPONSE_IN_TRANSIT
        );

        player.sendSystemMessage(
                Component.literal(
                        "Your acceptance has been dispatched. The other court may receive it within one to five minutes."
                )
        );

        return 1;
    }

    static int reject(
            ServerPlayer player,
            UUID proposalId
    ) {
        CapitalDiplomaticAgreementValidation.PlayerProposalValidation validation =
                CapitalDiplomaticAgreementValidation.validatePlayerProposal(
                        player,
                        proposalId
                );

        if (!validation.valid()) {
            if (player != null) {
                player.sendSystemMessage(
                        Component.literal(
                                validation.failureMessage()
                        )
                );
            }

            return 0;
        }

        queuePlayerResponse(
                player.serverLevel(),
                validation.proposal(),
                DiplomaticProposalStatus.REJECTED_RESPONSE_IN_TRANSIT
        );

        player.sendSystemMessage(
                Component.literal(
                        "Your rejection has been dispatched. The other court may receive it within one to five minutes."
                )
        );

        return 1;
    }

    static void resolveQueuedPlayerResponse(
            ServerLevel level,
            DiplomaticProposal proposal
    ) {
        if (level == null || proposal == null) {
            return;
        }

        CapitalRecord source = CapitalManager.getCapital(
                proposal.getSourceCapitalId()
        );
        CapitalRecord target = CapitalManager.getCapital(
                proposal.getTargetCapitalId()
        );
        if (source == null || target == null) {
            CapitalAgreementDataAccess.removeProposal(
                    level,
                    proposal.getProposalId()
            );
            if (source != null) {
                notifySource(
                        level,
                        proposal,
                        source,
                        "Proposal Undeliverable",
                        "The diplomatic response could not be delivered because the receiving capital no longer exists."
                );
            }
            return;
        }

        if (proposal.getStatus()
                == DiplomaticProposalStatus.REJECTED_RESPONSE_IN_TRANSIT) {
            rejectProposal(level, proposal, source, target);
            return;
        }

        if (proposal.getStatus()
                != DiplomaticProposalStatus.ACCEPTED_RESPONSE_IN_TRANSIT) {
            return;
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                source,
                target
        );
        String failure = CapitalDiplomaticAgreementValidation.validateProposal(
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
                    proposal.getProposalId()
            );
            notifySource(
                    level,
                    proposal,
                    source,
                    proposal.getType().getDisplayName() + " Failed",
                    "The acceptance arrived after the diplomatic situation changed, so the agreement could not be established."
            );
            return;
        }

        acceptProposal(level, proposal, source, target);
    }

    private static void queuePlayerResponse(
            ServerLevel level,
            DiplomaticProposal proposal,
            DiplomaticProposalStatus status
    ) {
        proposal.setStatus(status);
        proposal.setAvailableAt(CapitalDiplomaticDelayService.schedule(level));
        CapitalAgreementDataAccess.get(level).setDirty();
    }

    static void resolveNpcProposal(
            ServerLevel level,
            DiplomaticProposal proposal
    ) {
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

            return;
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                source,
                target
        );

        int score =
                CapitalDiplomacyDataAccess.getRelationshipScore(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                );

        CapitalDiplomaticState state =
                CapitalDiplomacyDataAccess.getDiplomaticState(
                        level,
                        source.getCapitalId(),
                        target.getCapitalId()
                );

        String validationFailure =
                CapitalDiplomaticAgreementValidation.validateProposal(
                        level,
                        source,
                        target,
                        proposal.getType(),
                        state,
                        score
                );

        int requiredScore = switch (proposal.getType()) {
            case NON_AGGRESSION_PACT,
                 TRADE_AGREEMENT -> 10;
            case ROYAL_BETROTHAL -> 20;
            case ALLIANCE -> 40;
            case TRUCE -> -74;
        };

        int acceptanceChance = Math.min(
                90,
                50 + Math.max(
                        0,
                        score - requiredScore
                )
        );

        if (CapitalWarDataAccess.hasActiveUnjustPenalty(
                level,
                source.getCapitalId()
        )) {
            acceptanceChance = Math.max(
                    5,
                    acceptanceChance - 25
            );
        }

        boolean accepted =
                validationFailure == null
                        && score >= requiredScore
                        && level.random.nextInt(100)
                        < acceptanceChance;

        if (accepted) {
            acceptProposal(
                    level,
                    proposal,
                    source,
                    target
            );
        } else {
            rejectProposal(
                    level,
                    proposal,
                    source,
                    target
            );
        }
    }

    static void notifySource(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            String title,
            String message
    ) {
        UUID recipient =
                source.getPlayerSovereignId() != null
                        ? source.getPlayerSovereignId()
                        : proposal.getSourceSovereignId();

        if (recipient != null) {
            CapitalDiplomaticAgreementCorrespondenceService
                    .sendNotice(
                            level,
                            recipient,
                            title,
                            message
                    );
        }
    }

    private static boolean acceptProposal(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        if (proposal.getType()
                == DiplomaticProposalType.ROYAL_BETROTHAL) {
            if (!CapitalRoyalBetrothalService.establish(
                    level,
                    proposal,
                    source,
                    target
            )) {
                CapitalAgreementDataAccess.removeProposal(
                        level,
                        proposal.getProposalId()
                );

                notifySource(
                        level,
                        proposal,
                        source,
                        "Royal Betrothal Failed",
                        "The proposed Royal Betrothal could not be established because the match was no longer eligible."
                );

                return false;
            }

            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId(),
                    proposal.getType().getAcceptanceBonus(),
                    "Royal Betrothal accepted",
                    target.getCapitalId()
            );

            CapitalAgreementDataAccess.removeProposal(
                    level,
                    proposal.getProposalId()
            );

            notifySource(
                    level,
                    proposal,
                    source,
                    "Royal Betrothal Accepted",
                    CapitalDiplomaticAgreementText
                            .capitalName(level, target)
                            + " accepted "
                            + CapitalRoyalBetrothalService
                            .proposalDescription(
                                    level,
                                    proposal,
                                    source,
                                    target
                            )
                            + "."
            );

            return true;
        }

        if (proposal.getType()
                == DiplomaticProposalType.TRADE_AGREEMENT) {
            if (!CapitalDiplomaticTradeAgreementService.establish(
                    level,
                    source,
                    target
            )) {
                CapitalAgreementDataAccess.removeProposal(
                        level,
                        proposal.getProposalId()
                );

                notifySource(
                        level,
                        proposal,
                        source,
                        "Trade Agreement Failed",
                        "The proposed Trade Agreement could not be established because its requirements were no longer met."
                );

                return false;
            }

            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId(),
                    proposal.getType().getAcceptanceBonus(),
                    "Trade Agreement accepted",
                    target.getCapitalId()
            );

            CapitalAgreementDataAccess.removeProposal(
                    level,
                    proposal.getProposalId()
            );

            notifySource(
                    level,
                    proposal,
                    source,
                    "Trade Agreement Accepted",
                    CapitalDiplomaticAgreementText.capitalName(
                            level,
                            target
                    )
                            + " accepted the proposed Trade Agreement."
            );

            return true;
        }

        long truceUntil =
                proposal.getType()
                        == DiplomaticProposalType.TRUCE
                        ? level.getGameTime()
                        + CapitalDiplomaticTruceService
                        .TRUCE_DURATION_TICKS
                        : 0L;

        CapitalDiplomacyDataAccess.setDiplomaticState(
                level,
                source.getCapitalId(),
                target.getCapitalId(),
                proposal.getType().getResultingState(),
                truceUntil
        );

        CapitalDiplomacyDataAccess.adjustRelationship(
                level,
                source.getCapitalId(),
                target.getCapitalId(),
                proposal.getType().getAcceptanceBonus(),
                proposal.getType().getDisplayName()
                        + " accepted",
                target.getCapitalId()
        );

        CapitalAgreementDataAccess.removeProposal(
                level,
                proposal.getProposalId()
        );

        String sourceName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        source
                );

        String targetName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        target
                );

        String entry =
                sourceName
                        + " and "
                        + targetName
                        + " entered into "
                        + CapitalDiplomaticAgreementText.withIndefiniteArticle(
                        proposal.getType().getDisplayName()
                )
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

        notifySource(
                level,
                proposal,
                source,
                proposal.getType().getDisplayName()
                        + " Accepted",
                targetName
                        + " accepted the proposed "
                        + proposal.getType().getDisplayName()
                        + "."
        );

        return true;
    }

    private static void rejectProposal(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            CapitalRecord target
    ) {
        CapitalAgreementDataAccess.removeProposal(
                level,
                proposal.getProposalId()
        );

        String sourceName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        source
                );

        String targetName =
                CapitalDiplomaticAgreementText.capitalName(
                        level,
                        target
                );

        String entry =
                targetName
                        + " rejected "
                        + CapitalDiplomaticAgreementText.withIndefiniteArticle(
                        proposal.getType().getDisplayName()
                )
                        + " proposed by "
                        + sourceName
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

        notifySource(
                level,
                proposal,
                source,
                proposal.getType().getDisplayName()
                        + " Rejected",
                targetName
                        + " rejected the proposed "
                        + proposal.getType().getDisplayName()
                        + "."
        );
    }
}