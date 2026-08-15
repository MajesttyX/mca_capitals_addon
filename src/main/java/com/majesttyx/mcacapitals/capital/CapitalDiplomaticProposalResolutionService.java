package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.CapitalWarDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalStatus;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import com.majesttyx.mcacapitals.util.MCAIntegrationBridge;
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
        CapitalDiplomaticAgreementValidation
                .PlayerProposalValidation validation =
                CapitalDiplomaticAgreementValidation
                        .validatePlayerProposal(
                                player,
                                proposalId
                        );

        if (!validation.valid()) {
            if (player != null) {
                player.sendSystemMessage(
                        validation.failureMessage()
                );
            }

            return 0;
        }

        queuePlayerResponse(
                player.serverLevel(),
                validation.proposal(),
                DiplomaticProposalStatus
                        .ACCEPTED_RESPONSE_IN_TRANSIT
        );

        player.sendSystemMessage(
                Component.translatable("mcacapitals.system.capital_diplomatic_proposal_resolution_service.your_acceptance_has_been_dispatched_the_other_court_may_receive_it_wit")
        );

        return 1;
    }

    static int reject(
            ServerPlayer player,
            UUID proposalId
    ) {
        CapitalDiplomaticAgreementValidation
                .PlayerProposalValidation validation =
                CapitalDiplomaticAgreementValidation
                        .validatePlayerProposal(
                                player,
                                proposalId
                        );

        if (!validation.valid()) {
            if (player != null) {
                player.sendSystemMessage(
                        validation.failureMessage()
                );
            }

            return 0;
        }

        queuePlayerResponse(
                player.serverLevel(),
                validation.proposal(),
                DiplomaticProposalStatus
                        .REJECTED_RESPONSE_IN_TRANSIT
        );

        player.sendSystemMessage(
                Component.translatable("mcacapitals.system.capital_diplomatic_proposal_resolution_service.your_rejection_has_been_dispatched_the_other_court_may_receive_it_with")
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
                        Component.translatable(
                                "mcacapitals.diplomacy.correspondence.undeliverable_title"
                        ),
                        Component.translatable(
                                "mcacapitals.diplomacy.correspondence.response_undeliverable"
                        )
                );
            }

            return;
        }

        if (proposal.getStatus()
                == DiplomaticProposalStatus
                .REJECTED_RESPONSE_IN_TRANSIT) {
            rejectProposal(
                    level,
                    proposal,
                    source,
                    target
            );
            return;
        }

        if (proposal.getStatus()
                != DiplomaticProposalStatus
                .ACCEPTED_RESPONSE_IN_TRANSIT) {
            return;
        }

        CapitalDiplomaticTruceService.refreshExpiredTruce(
                level,
                source,
                target
        );

        Component failure =
                CapitalDiplomaticAgreementValidation
                        .validateProposal(
                                level,
                                source,
                                target,
                                proposal.getType(),
                                CapitalDiplomacyDataAccess
                                        .getDiplomaticState(
                                                level,
                                                source.getCapitalId(),
                                                target.getCapitalId()
                                        ),
                                CapitalDiplomacyDataAccess
                                        .getRelationshipScore(
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
                    Component.translatable(
                            "mcacapitals.diplomacy.proposal.failed_title",
                            proposal.getType().getDisplayComponent()
                    ),
                    Component.translatable(
                            "mcacapitals.diplomacy.proposal.acceptance_situation_changed"
                    )
            );
            return;
        }

        acceptProposal(
                level,
                proposal,
                source,
                target
        );
    }

    private static void queuePlayerResponse(
            ServerLevel level,
            DiplomaticProposal proposal,
            DiplomaticProposalStatus status
    ) {
        proposal.setStatus(status);
        proposal.setAvailableAt(
                CapitalDiplomaticDelayService.schedule(level)
        );
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

        Component validationFailure =
                CapitalDiplomaticAgreementValidation
                        .validateProposal(
                                level,
                                source,
                                target,
                                proposal.getType(),
                                state,
                                score
                        );

        int requiredScore =
                proposal.getType()
                        .getMinimumRelationship();

        int acceptanceChance = Math.min(
                90,
                50 + Math.max(
                        0,
                        score - requiredScore
                )
        );

        acceptanceChance = Math.min(
                100,
                acceptanceChance
                        + personalCourtAcceptanceBonus(
                        level,
                        proposal,
                        target
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

    private static int personalCourtAcceptanceBonus(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord target
    ) {
        if (level == null
                || proposal == null
                || target == null
                || proposal.getSourceSovereignId() == null) {
            return 0;
        }

        UUID playerId = proposal.getSourceSovereignId();
        int bonus = 0;

        if (target.getSovereign() != null) {
            int hearts = MCAIntegrationBridge
                    .getHeartsWithPlayer(
                            level,
                            target.getSovereign(),
                            playerId
                    );

            if (hearts >= 200) {
                bonus += 20;
            } else if (hearts >= 100) {
                bonus += 10;
            }
        }

        if (target.getHand() != null) {
            int hearts = MCAIntegrationBridge
                    .getHeartsWithPlayer(
                            level,
                            target.getHand(),
                            playerId
                    );

            if (hearts >= 200) {
                bonus += 10;
            } else if (hearts >= 100) {
                bonus += 5;
            }
        }

        return bonus;
    }

    static void notifySource(
            ServerLevel level,
            DiplomaticProposal proposal,
            CapitalRecord source,
            Component title,
            Component message
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
                        Component.translatable(
                                "mcacapitals.diplomacy.royal_betrothal.failed_title"
                        ),
                        Component.translatable(
                                "mcacapitals.diplomacy.royal_betrothal.match_no_longer_eligible"
                        )
                );
                return false;
            }

            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId(),
                    proposal.getType().getAcceptanceBonus(),
                    "mcacapitals.relationship_reason.royal_betrothal_accepted",
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
                    Component.translatable(
                            "mcacapitals.diplomacy.royal_betrothal.accepted_title"
                    ),
                    Component.translatable(
                            "mcacapitals.diplomacy.royal_betrothal.accepted_message",
                            CapitalDiplomaticAgreementText
                                    .capitalName(level, target),
                            CapitalRoyalBetrothalService
                                    .proposalDescription(
                                            level,
                                            proposal,
                                            source,
                                            target
                                    )
                    )
            );
            return true;
        }

        if (proposal.getType()
                == DiplomaticProposalType.TRADE_AGREEMENT) {
            boolean renewal =
                    CapitalDiplomaticTradeAgreementService
                            .isRenewal(
                                    level,
                                    source,
                                    target
                            );

            if (!CapitalDiplomaticTradeAgreementService
                    .establish(
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
                        Component.translatable(
                                "mcacapitals.diplomacy.trade.failed_title"
                        ),
                        Component.translatable(
                                "mcacapitals.diplomacy.trade.requirements_no_longer_met"
                        )
                );
                return false;
            }

            CapitalDiplomacyDataAccess.adjustRelationship(
                    level,
                    source.getCapitalId(),
                    target.getCapitalId(),
                    proposal.getType().getAcceptanceBonus(),
                    renewal
                            ? "mcacapitals.relationship_reason.trade_agreement_renewed"
                            : "mcacapitals.relationship_reason.trade_agreement_accepted",
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
                    Component.translatable(
                            renewal
                                    ? "mcacapitals.diplomacy.trade.renewed_title"
                                    : "mcacapitals.diplomacy.trade.accepted_title"
                    ),
                    Component.translatable(
                            renewal
                                    ? "mcacapitals.diplomacy.trade.renewed_message"
                                    : "mcacapitals.diplomacy.trade.accepted_message",
                            CapitalDiplomaticAgreementText.capitalName(
                                    level,
                                    target
                            )
                    )
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
                "mcacapitals.relationship_reason.proposal_accepted."
                        + proposal.getType().getSerializedName(),
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

        CapitalChronicleEntry.Argument agreementType =
                CapitalChronicleService.translatable(
                        "mcacapitals.chronicle.agreement_type."
                                + proposal.getType().getSerializedName()
                );

        CapitalChronicleService.addEvent(
                level,
                source,
                CapitalChronicleEventId.DIPLOMATIC_AGREEMENT_ACCEPTED,
                sourceName,
                targetName,
                agreementType
        );

        CapitalChronicleService.addEvent(
                level,
                target,
                CapitalChronicleEventId.DIPLOMATIC_AGREEMENT_ACCEPTED,
                sourceName,
                targetName,
                agreementType
        );

        notifySource(
                level,
                proposal,
                source,
                Component.translatable(
                        "mcacapitals.diplomacy.proposal.accepted_title",
                        proposal.getType().getDisplayComponent()
                ),
                Component.translatable(
                        "mcacapitals.diplomacy.proposal.accepted_message",
                        targetName,
                        proposal.getType().getDisplayComponent()
                )
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

        CapitalChronicleEntry.Argument agreementType =
                CapitalChronicleService.translatable(
                        "mcacapitals.chronicle.agreement_type."
                                + proposal.getType().getSerializedName()
                );

        CapitalChronicleService.addEvent(
                level,
                source,
                CapitalChronicleEventId.DIPLOMATIC_AGREEMENT_REJECTED,
                targetName,
                agreementType,
                sourceName
        );

        CapitalChronicleService.addEvent(
                level,
                target,
                CapitalChronicleEventId.DIPLOMATIC_AGREEMENT_REJECTED,
                targetName,
                agreementType,
                sourceName
        );

        notifySource(
                level,
                proposal,
                source,
                Component.translatable(
                        "mcacapitals.diplomacy.proposal.rejected_title",
                        proposal.getType().getDisplayComponent()
                ),
                Component.translatable(
                        "mcacapitals.diplomacy.proposal.rejected_message",
                        targetName,
                        proposal.getType().getDisplayComponent()
                )
        );
    }
}
