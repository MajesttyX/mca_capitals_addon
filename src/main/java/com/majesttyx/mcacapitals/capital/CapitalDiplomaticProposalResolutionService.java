package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.CapitalAgreementDataAccess;
import com.majesttyx.mcacapitals.data.CapitalDiplomacyDataAccess;
import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
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

        if (!acceptProposal(
                player.serverLevel(),
                validation.proposal(),
                validation.sourceCapital(),
                validation.targetCapital()
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "The diplomatic proposal could not be accepted."
                    )
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.literal(
                        "The "
                                + validation.proposal()
                                .getType()
                                .getDisplayName()
                                + " has been accepted."
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

        rejectProposal(
                player.serverLevel(),
                validation.proposal(),
                validation.sourceCapital(),
                validation.targetCapital()
        );

        player.sendSystemMessage(
                Component.literal(
                        "The "
                                + validation.proposal()
                                .getType()
                                .getDisplayName()
                                + " has been rejected."
                )
        );

        return 1;
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

        boolean accepted =
                validationFailure == null
                        && switch (proposal.getType()) {
                    case NON_AGGRESSION_PACT ->
                            score >= 10;
                    case ALLIANCE ->
                            score >= 40;
                    case TRUCE ->
                            score >= -74;
                    case TRADE_AGREEMENT ->
                            score >= 10;
                };

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
                        + " entered into a "
                        + proposal.getType().getDisplayName()
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
                        + " rejected a "
                        + proposal.getType().getDisplayName()
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