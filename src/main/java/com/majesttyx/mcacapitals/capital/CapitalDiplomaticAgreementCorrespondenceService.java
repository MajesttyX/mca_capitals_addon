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

        Component sourceNameComponent = sourceName == null || sourceName.isBlank()
                ? Component.translatable("mcacapitals.diplomacy.unknown_capital")
                : Component.literal(sourceName);

        online.sendSystemMessage(
                Component.translatable(
                        "mcacapitals.diplomacy.correspondence.proposal_arrival",
                        proposal.getType().getDisplayComponent(),
                        sourceNameComponent
                ).withStyle(ChatFormatting.GOLD)
        );
    }

    public static void sendNotice(
            ServerLevel level,
            UUID recipientPlayerId,
            Component title,
            Component message
    ) {
        if (level == null
                || recipientPlayerId == null
                || message == null
                || message.getString().isBlank()) {
            return;
        }

        ServerPlayer online = level.getServer()
                .getPlayerList()
                .getPlayer(recipientPlayerId);

        if (online == null) {
            return;
        }

        Component heading = title == null || title.getString().isBlank()
                ? Component.translatable("mcacapitals.diplomacy.correspondence.heading")
                : title;

        online.sendSystemMessage(
                Component.translatable(
                        "mcacapitals.diplomacy.correspondence.notice",
                        heading,
                        message
                ).withStyle(ChatFormatting.GOLD)
        );
    }
}
