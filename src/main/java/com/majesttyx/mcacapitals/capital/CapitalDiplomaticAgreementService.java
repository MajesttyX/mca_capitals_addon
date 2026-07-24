package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import com.majesttyx.mcacapitals.data.DiplomaticProposalType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;

public final class CapitalDiplomaticAgreementService {

    public static final String DIALOGUE_COMMAND =
            "mcacapitals_manage_diplomacy";

    public static final long TRUCE_DURATION_TICKS =
            CapitalDiplomaticTruceService.TRUCE_DURATION_TICKS;

    private CapitalDiplomaticAgreementService() {
    }

    public static boolean canShowDialogueAnswer(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        if (player == null
                || ambassadorEntity == null) {
            return false;
        }

        return CapitalDiplomaticAgreementValidation
                .validateAudience(
                        player,
                        ambassadorEntity.getUUID()
                )
                .valid();
    }

    public static boolean openCapitalList(
            ServerPlayer player,
            Entity ambassadorEntity
    ) {
        CapitalAsylumService.sendReviewOption(
                player,
                ambassadorEntity
        );

        return CapitalDiplomaticAgreementMenuService
                .openCapitalList(
                        player,
                        ambassadorEntity
                );
    }

    public static int openActionList(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        return CapitalDiplomaticAgreementMenuService
                .openActionList(
                        player,
                        ambassadorId,
                        targetCapitalId
                );
    }

    public static int propose(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId,
            DiplomaticProposalType type
    ) {
        return CapitalDiplomaticProposalService
                .propose(
                        player,
                        ambassadorId,
                        targetCapitalId,
                        type
                );
    }

    public static int endTradeAgreement(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        return CapitalDiplomaticTradeAgreementService
                .endByPlayer(
                        player,
                        ambassadorId,
                        targetCapitalId
                );
    }

    public static int declareWar(
            ServerPlayer player,
            UUID ambassadorId,
            UUID targetCapitalId
    ) {
        return CapitalDiplomaticWarService
                .declareWar(
                        player,
                        ambassadorId,
                        targetCapitalId
                );
    }

    public static int accept(
            ServerPlayer player,
            UUID proposalId
    ) {
        return CapitalDiplomaticProposalResolutionService
                .accept(
                        player,
                        proposalId
                );
    }

    public static int reject(
            ServerPlayer player,
            UUID proposalId
    ) {
        return CapitalDiplomaticProposalResolutionService
                .reject(
                        player,
                        proposalId
                );
    }

    public static List<DiplomaticProposal>
    getPendingForPlayer(
            ServerLevel level,
            UUID playerId
    ) {
        return CapitalDiplomaticProposalService
                .getPendingForPlayer(
                        level,
                        playerId
                );
    }

    public static void processPendingProposal(
            ServerLevel level,
            DiplomaticProposal proposal
    ) {
        CapitalDiplomaticProposalService
                .processPendingProposal(
                        level,
                        proposal
                );
    }

    public static void expireTruces(
            ServerLevel level
    ) {
        CapitalDiplomaticTruceService
                .expireTruces(level);
    }
}