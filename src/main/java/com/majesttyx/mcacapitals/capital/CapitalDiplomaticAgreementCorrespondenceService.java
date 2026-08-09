package com.majesttyx.mcacapitals.capital;

import com.majesttyx.mcacapitals.data.DiplomaticProposal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CapitalDiplomaticAgreementCorrespondenceService {

    private CapitalDiplomaticAgreementCorrespondenceService() {
    }

    public static void sendProposalLetter(
            ServerLevel level,
            UUID recipientPlayerId,
            DiplomaticProposal proposal,
            CapitalRecord sourceCapital,
            CapitalRecord targetCapital
    ) {
        if (level == null
                || recipientPlayerId == null
                || proposal == null
                || sourceCapital == null
                || targetCapital == null) {
            return;
        }

        ServerPlayer online = level.getServer()
                .getPlayerList()
                .getPlayer(recipientPlayerId);
        if (online == null) {
            return;
        }

        String sourceName = CapitalDiplomaticCorrespondenceService.getCapitalName(
                level,
                sourceCapital
        );
        online.sendSystemMessage(
                Component.literal(
                        "Your Ambassador reports an urgent "
                                + proposal.getType().getDisplayName()
                                + " proposal from "
                                + sourceName
                                + ". Speak to the Ambassador to answer it."
                ).withStyle(ChatFormatting.GOLD)
        );
    }

    public static void sendNotice(
            ServerLevel level,
            UUID recipientPlayerId,
            String title,
            String message
    ) {
        if (level == null
                || recipientPlayerId == null
                || message == null
                || message.isBlank()) {
            return;
        }

        ServerPlayer online = level.getServer()
                .getPlayerList()
                .getPlayer(recipientPlayerId);
        if (online == null) {
            return;
        }

        String heading = title == null || title.isBlank()
                ? "Diplomatic Correspondence"
                : title;
        online.sendSystemMessage(
                Component.literal(heading + ": " + message)
                        .withStyle(ChatFormatting.GOLD)
        );
    }
}
